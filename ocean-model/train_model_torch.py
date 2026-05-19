"""PyTorch version — fast GPU training for ocean forecast model."""
import json
import argparse
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

# ====================== Config ======================
SEQ_LEN = 30
HORIZON = 7
LSTM_UNITS = 256
HEADS = 8
KEY_DIM = 64
DROPOUT = 0.3
EPOCHS = 100
BATCH_SIZE = 128
LR = 1e-4
SEED = 42
# ===================================================


class DepthEncoder(nn.Module):
    """TimeDistributed MLP: same weights applied to each timestep."""

    def __init__(self, input_dim):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(input_dim, 64),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(64, 32),
            nn.ReLU(),
            nn.Linear(32, 16),
        )

    def forward(self, x):
        # x: (batch, seq_len, input_dim)
        b, s, _ = x.shape
        x = x.reshape(b * s, -1)
        x = self.net(x)
        return x.reshape(b, s, 16)


class OceanForecastModel(nn.Module):
    def __init__(self, n_depths):
        super().__init__()
        self.temp_encoder = DepthEncoder(n_depths)
        self.so_encoder = DepthEncoder(n_depths)

        self.lstm1 = nn.LSTM(33, LSTM_UNITS, batch_first=True, dropout=DROPOUT)
        self.lstm2 = nn.LSTM(LSTM_UNITS, LSTM_UNITS, batch_first=True, dropout=DROPOUT)

        self.attn = nn.MultiheadAttention(LSTM_UNITS, HEADS, kdim=LSTM_UNITS,
                                          vdim=LSTM_UNITS, batch_first=True)
        self.attn_norm = nn.LayerNorm(LSTM_UNITS)
        self.attn_dropout = nn.Dropout(DROPOUT)

        self.space_net = nn.Sequential(
            nn.Linear(2, 64),
            nn.ReLU(),
            nn.Dropout(DROPOUT),
            nn.Linear(64, 32),
            nn.ReLU(),
        )

        self.head = nn.Sequential(
            nn.Linear(LSTM_UNITS + 32, 128),
            nn.ReLU(),
            nn.Dropout(DROPOUT),
            nn.Linear(128, 64),
            nn.ReLU(),
            nn.Dropout(DROPOUT),
            nn.Linear(64, 32),
            nn.ReLU(),
            nn.Linear(32, HORIZON),
        )

    def forward(self, chl, temp, so, space):
        # Encode depth profiles
        temp_enc = self.temp_encoder(temp)  # (B, 30, 16)
        so_enc = self.so_encoder(so)        # (B, 30, 16)

        # Fuse per timestep
        fused = torch.cat([chl, temp_enc, so_enc], dim=-1)  # (B, 30, 33)

        # LSTM
        x, _ = self.lstm1(fused)
        x, _ = self.lstm2(x)

        # Self-attention
        attn_out, _ = self.attn(x, x, x)
        x = self.attn_norm(x + attn_out)
        x = self.attn_dropout(x)

        # Temporal pooling
        x = x.mean(dim=1)  # (B, 256)

        # Spatial branch
        space_out = self.space_net(space)  # (B, 32)

        # Fusion + head
        combined = torch.cat([x, space_out], dim=-1)  # (B, 288)
        return self.head(combined)


def load_split(x, y, seed):
    """80/10/10 split, reproducible."""
    n = len(y)
    idx = np.random.RandomState(seed).permutation(n)
    n_test = n // 10
    n_train = n - 2 * n_test
    return idx[:n_train], idx[n_train:n_train + n_test], idx[n_train + n_test:]


def train_epoch(model, loader, optimizer, criterion, device):
    model.train()
    total_loss, total_mae, n = 0, 0, 0
    for chl, temp, so, space, y in loader:
        chl, temp, so = chl.to(device), temp.to(device), so.to(device)
        space, y = space.to(device), y.to(device)

        optimizer.zero_grad()
        pred = model(chl, temp, so, space)
        loss = criterion(pred, y)
        loss.backward()
        optimizer.step()

        total_loss += loss.item() * len(y)
        total_mae += (pred - y).abs().mean().item() * len(y)
        n += len(y)
    return total_loss / n, total_mae / n


@torch.no_grad()
def evaluate(model, loader, criterion, device):
    model.eval()
    total_loss, total_mae, n = 0, 0, 0
    for chl, temp, so, space, y in loader:
        chl, temp, so = chl.to(device), temp.to(device), so.to(device)
        space, y = space.to(device), y.to(device)

        pred = model(chl, temp, so, space)
        loss = criterion(pred, y)

        total_loss += loss.item() * len(y)
        total_mae += (pred - y).abs().mean().item() * len(y)
        n += len(y)
    return total_loss / n, total_mae / n


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=["chl", "thetao", "so"], default="chl")
    parser.add_argument("--data-dir", default="data")
    parser.add_argument("--epochs", type=int, default=EPOCHS)
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE)
    parser.add_argument("--lr", type=float, default=LR)
    args = parser.parse_args()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}")
    if device.type == "cuda":
        print(f"GPU: {torch.cuda.get_device_name(0)}")

    # Seed
    torch.manual_seed(SEED)
    np.random.seed(SEED)

    # Load
    print(f"Loading data from {args.data_dir}/...")
    X_chl = torch.from_numpy(np.load(f"{args.data_dir}/X_chl.npy")).float()
    X_temp = torch.from_numpy(np.load(f"{args.data_dir}/X_temp.npy")).float()
    X_so = torch.from_numpy(np.load(f"{args.data_dir}/X_so.npy")).float()
    X_space = torch.from_numpy(np.load(f"{args.data_dir}/X_space.npy")).float()
    y = torch.from_numpy(np.load(f"{args.data_dir}/y_{args.target}.npy")).float()

    n_depths = X_temp.shape[-1]
    print(f"Samples: {len(y)}, n_depths: {n_depths}, target: {args.target}")

    # Split
    idx_train, idx_val, idx_test = load_split(X_chl, y, SEED)
    print(f"Train: {len(idx_train)}, Val: {len(idx_val)}, Test: {len(idx_test)}")

    train_loader = DataLoader(
        TensorDataset(X_chl[idx_train], X_temp[idx_train], X_so[idx_train],
                       X_space[idx_train], y[idx_train]),
        batch_size=args.batch_size, shuffle=True, pin_memory=True,
        num_workers=0  # Windows: 0 avoids multiprocessing issues
    )
    val_loader = DataLoader(
        TensorDataset(X_chl[idx_val], X_temp[idx_val], X_so[idx_val],
                       X_space[idx_val], y[idx_val]),
        batch_size=args.batch_size, shuffle=False, pin_memory=True, num_workers=0
    )
    test_loader = DataLoader(
        TensorDataset(X_chl[idx_test], X_temp[idx_test], X_so[idx_test],
                       X_space[idx_test], y[idx_test]),
        batch_size=args.batch_size, shuffle=False, pin_memory=True, num_workers=0
    )

    # Model
    model = OceanForecastModel(n_depths).to(device)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Parameters: {total_params:,}")

    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=args.lr)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", factor=0.5, patience=7, min_lr=1e-6
    )

    best_val_loss = float("inf")
    best_epoch = 0
    patience_counter = 0
    patience = 15
    history = {"loss": [], "val_loss": [], "mae": [], "val_mae": []}

    for epoch in range(1, args.epochs + 1):
        train_loss, train_mae = train_epoch(model, train_loader, optimizer, criterion, device)
        val_loss, val_mae = evaluate(model, val_loader, criterion, device)

        history["loss"].append(train_loss)
        history["val_loss"].append(val_loss)
        history["mae"].append(train_mae)
        history["val_mae"].append(val_mae)

        scheduler.step(val_loss)

        print(f"Epoch {epoch:3d} | loss={train_loss:.6f} val_loss={val_loss:.6f} "
              f"mae={train_mae:.4f} val_mae={val_mae:.4f}", end="")

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_epoch = epoch
            patience_counter = 0
            torch.save(model.state_dict(), f"best_model_{args.target}.pt")
            print(" *")
        else:
            patience_counter += 1
            print()
            if patience_counter >= patience:
                print(f"Early stopping at epoch {epoch}")
                break

    # Load best weights
    model.load_state_dict(torch.load(f"best_model_{args.target}.pt", weights_only=True))

    # Test
    test_loss, test_mae = evaluate(model, test_loader, criterion, device)
    print(f"Test Loss (MSE): {test_loss:.6f}")
    print(f"Test MAE: {test_mae:.6f}")

    # Save
    with open(f"history_{args.target}.json", "w") as f:
        json.dump(history, f)

    torch.save(model.state_dict(), f"model_{args.target}.pt")
    print(f"Model saved to model_{args.target}.pt (best epoch: {best_epoch})")


if __name__ == "__main__":
    main()

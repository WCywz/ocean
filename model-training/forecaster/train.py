"""Train PhyLSTM forecaster with time-decay weighted loss."""
import os
import sys
import json
import torch
import torch.nn as nn
from torch.cuda.amp import GradScaler, autocast

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from config import (
    DATA_DIR, MODEL_DIR, N_DEPTHS, BATCH_SIZE_FORECAST, GRADIENT_ACCUMULATION,
    FORECAST_LR, SEED, DEVICE,
)
from forecaster.model import PhyLSTMForecaster
from forecaster.dataset import create_dataloaders

EPOCHS = 60
PATIENCE = 10
HORIZON_WEIGHTS = [1.0, 0.8, 0.5, 0.3]  # 1d, 3d, 5d, 7d


def weighted_mse_loss(pred, target, weights=HORIZON_WEIGHTS):
    """Time-decay weighted MSE across horizons."""
    w = torch.tensor(weights, device=pred.device).view(1, -1, 1)
    diff = (pred - target) ** 2
    return (diff * w).mean()


def train():
    os.makedirs(MODEL_DIR, exist_ok=True)
    torch.manual_seed(SEED)

    data_dir = os.path.join(DATA_DIR, "forecaster")
    train_loader, val_loader, test_loader = create_dataloaders(data_dir, BATCH_SIZE_FORECAST)

    model = PhyLSTMForecaster(n_depths=N_DEPTHS, lstm_units=192).to(DEVICE)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Parameters: {total_params:,}")

    optimizer = torch.optim.Adam(model.parameters(), lr=FORECAST_LR)
    scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", factor=0.5, patience=5, min_lr=1e-6
    )
    scaler = GradScaler(enabled=(DEVICE == "cuda"))

    best_val_loss = float("inf")
    patience_counter = 0
    history = {"loss": [], "val_loss": []}

    for epoch in range(1, EPOCHS + 1):
        model.train()
        total_loss = 0.0
        optimizer.zero_grad()

        for step, (x, y) in enumerate(train_loader):
            x, y = x.to(DEVICE), y.to(DEVICE)

            with autocast(enabled=(DEVICE == "cuda")):
                pred = model(x)
                loss = weighted_mse_loss(pred, y) / GRADIENT_ACCUMULATION

            scaler.scale(loss).backward()

            if (step + 1) % GRADIENT_ACCUMULATION == 0:
                scaler.step(optimizer)
                scaler.update()
                optimizer.zero_grad()

            total_loss += loss.item() * GRADIENT_ACCUMULATION

        # Step remaining gradients after last incomplete accumulation batch
        if len(train_loader) % GRADIENT_ACCUMULATION != 0:
            scaler.step(optimizer)
            scaler.update()
            optimizer.zero_grad()

        avg_loss = total_loss / max(len(train_loader), 1)
        history["loss"].append(avg_loss)

        # Validation
        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for x, y in val_loader:
                x, y = x.to(DEVICE), y.to(DEVICE)
                pred = model(x)
                val_loss += weighted_mse_loss(pred, y).item()

        avg_val_loss = val_loss / max(len(val_loader), 1)
        history["val_loss"].append(avg_val_loss)
        scheduler.step(avg_val_loss)

        print(f"Epoch {epoch:3d} | loss={avg_loss:.6f} val_loss={avg_val_loss:.6f}", end="")

        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss
            patience_counter = 0
            torch.save(model.state_dict(), os.path.join(MODEL_DIR, "forecaster_best.pt"))
            print(" *")
        else:
            patience_counter += 1
            print()
            if patience_counter >= PATIENCE:
                print(f"Early stopping at epoch {epoch}")
                break

    # Test
    model.load_state_dict(torch.load(
        os.path.join(MODEL_DIR, "forecaster_best.pt"), map_location=DEVICE, weights_only=True))
    model.eval()

    test_loss = 0.0
    test_mae = [0.0] * len(HORIZON_WEIGHTS)
    n_test = 0
    with torch.no_grad():
        for x, y in test_loader:
            x, y = x.to(DEVICE), y.to(DEVICE)
            pred = model(x)
            test_loss += weighted_mse_loss(pred, y).item()
            for h in range(len(HORIZON_WEIGHTS)):
                test_mae[h] += (pred[:, h] - y[:, h]).abs().mean().item()
            n_test += 1

    print(f"\nTest Loss: {test_loss / max(n_test, 1):.6f}")
    for h, days in enumerate([1, 3, 5, 7]):
        print(f"  {days}d MAE: {test_mae[h] / max(n_test, 1):.6f}")

    with open(os.path.join(MODEL_DIR, "forecaster_history.json"), "w") as f:
        json.dump(history, f)

    torch.save(model.state_dict(), os.path.join(MODEL_DIR, "forecaster_final.pt"))
    print("Done.")


if __name__ == "__main__":
    train()

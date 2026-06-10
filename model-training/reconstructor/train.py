"""Train DepthProfileMLP reconstructor with thermocline-weighted loss."""
import os
import sys
import json
import torch
import torch.nn as nn
from torch.cuda.amp import GradScaler, autocast

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from config import (
    DATA_DIR, MODEL_DIR, N_DEPTHS, THERMOCLINE_IDX, BATCH_SIZE_RECON,
    GRADIENT_ACCUMULATION, LEARNING_RATE, SEED, DEVICE,
)
from reconstructor.model import DepthProfileMLP
from reconstructor.dataset import create_dataloaders

EPOCHS = 100
PATIENCE = 15


def thermocline_weighted_loss(pred, target, therm_idx=None):
    """MSE with 3x weight on thermocline depths (5-25m), 1x elsewhere.

    pred, target: [B, 18]
    """
    if therm_idx is None:
        therm_idx = THERMOCLINE_IDX

    se = (pred - target) ** 2

    weights = torch.ones(N_DEPTHS, device=pred.device)
    weights[therm_idx] = 3.0

    weighted = se * weights.unsqueeze(0)
    return weighted.mean()


def pearson_corr(pred, target):
    pred = pred.detach().flatten()
    target = target.flatten()
    mean_p, mean_t = pred.mean(), target.mean()
    num = ((pred - mean_p) * (target - mean_t)).sum()
    den = torch.sqrt(((pred - mean_p) ** 2).sum() * ((target - mean_t) ** 2).sum())
    result = num / (den + 1e-8)
    return result.item()


def train():
    os.makedirs(MODEL_DIR, exist_ok=True)
    torch.manual_seed(SEED)

    data_dir = os.path.join(DATA_DIR, "reconstructor")
    if not os.path.exists(os.path.join(data_dir, "X_surface.npy")):
        raise FileNotFoundError(
            f"Reconstructor data not found in {data_dir}. "
            "Run prepare_reconstructor_data.py first."
        )

    train_loader, val_loader, test_loader = create_dataloaders(data_dir, BATCH_SIZE_RECON)

    model = DepthProfileMLP().to(DEVICE)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Parameters: {total_params:,}")

    optimizer = torch.optim.AdamW(model.parameters(), lr=LEARNING_RATE, weight_decay=1e-5)
    scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", factor=0.5, patience=8, min_lr=1e-6
    )
    scaler = GradScaler(enabled=DEVICE == "cuda")

    best_val_loss = float("inf")
    patience_counter = 0
    history = {"loss": [], "val_loss": [], "corr_thetao": [], "corr_so": []}

    for epoch in range(1, EPOCHS + 1):
        model.train()
        total_loss = 0.0
        optimizer.zero_grad()

        for step, (x, y_thetao, y_so) in enumerate(train_loader):
            x = x.to(DEVICE)
            y_thetao = y_thetao.to(DEVICE)
            y_so = y_so.to(DEVICE)

            with autocast(enabled=(DEVICE == "cuda")):
                out = model(x)
                pred_thetao = out[:, :N_DEPTHS]
                pred_so = out[:, N_DEPTHS:]
                loss_thetao = thermocline_weighted_loss(pred_thetao, y_thetao)
                loss_so = thermocline_weighted_loss(pred_so, y_so)
                loss = (loss_thetao + loss_so) / GRADIENT_ACCUMULATION

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
        corr_t_sum, corr_s_sum = 0.0, 0.0
        n_batches = 0

        with torch.no_grad():
            for x, y_thetao, y_so in val_loader:
                x = x.to(DEVICE)
                y_thetao = y_thetao.to(DEVICE)
                y_so = y_so.to(DEVICE)

                out = model(x)
                pred_thetao = out[:, :N_DEPTHS]
                pred_so = out[:, N_DEPTHS:]
                v_loss = (thermocline_weighted_loss(pred_thetao, y_thetao)
                          + thermocline_weighted_loss(pred_so, y_so))
                val_loss += v_loss.item()
                corr_t_sum += pearson_corr(pred_thetao, y_thetao)
                corr_s_sum += pearson_corr(pred_so, y_so)
                n_batches += 1

        avg_val_loss = val_loss / max(n_batches, 1)
        history["val_loss"].append(avg_val_loss)
        history["corr_thetao"].append(corr_t_sum / max(n_batches, 1))
        history["corr_so"].append(corr_s_sum / max(n_batches, 1))

        scheduler.step(avg_val_loss)

        print(f"Epoch {epoch:3d} | loss={avg_loss:.6f} val_loss={avg_val_loss:.6f} "
              f"corr_t={history['corr_thetao'][-1]:.4f} corr_s={history['corr_so'][-1]:.4f}", end="")

        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss
            patience_counter = 0
            torch.save(model.state_dict(), os.path.join(MODEL_DIR, "reconstructor_best.pt"))
            print(" *")
        else:
            patience_counter += 1
            print()
            if patience_counter >= PATIENCE:
                print(f"Early stopping at epoch {epoch}")
                break

    # Test evaluation
    model.load_state_dict(torch.load(
        os.path.join(MODEL_DIR, "reconstructor_best.pt"), map_location=DEVICE, weights_only=True))
    model.eval()

    test_loss, test_corr_t, test_corr_s, n_test = 0.0, 0.0, 0.0, 0
    with torch.no_grad():
        for x, y_thetao, y_so in test_loader:
            x = x.to(DEVICE)
            y_thetao = y_thetao.to(DEVICE)
            y_so = y_so.to(DEVICE)
            out = model(x)
            pred_t, pred_s = out[:, :N_DEPTHS], out[:, N_DEPTHS:]
            test_loss += (thermocline_weighted_loss(pred_t, y_thetao)
                          + thermocline_weighted_loss(pred_s, y_so)).item()
            test_corr_t += pearson_corr(pred_t, y_thetao)
            test_corr_s += pearson_corr(pred_s, y_so)
            n_test += 1

    print(f"\nTest Loss: {test_loss / max(n_test, 1):.6f}")
    print(f"Test Corr thetao: {test_corr_t / max(n_test, 1):.4f}")
    print(f"Test Corr so: {test_corr_s / max(n_test, 1):.4f}")

    with open(os.path.join(MODEL_DIR, "reconstructor_history.json"), "w") as f:
        json.dump(history, f)

    torch.save(model.state_dict(), os.path.join(MODEL_DIR, "reconstructor_final.pt"))
    print("Done.")


if __name__ == "__main__":
    train()

"""Validate forecaster: compute MAE per forecast horizon."""
import os
import sys
import numpy as np
import torch

sys.path.insert(0, os.path.dirname(__file__))
from config import DATA_DIR, MODEL_DIR, N_DEPTHS, FORECAST_HORIZONS
from forecaster.model import PhyLSTMForecaster
from forecaster.dataset import create_dataloaders


def mae_per_horizon(pred, target):
    mae = []
    for h in range(len(FORECAST_HORIZONS)):
        m = (pred[:, h] - target[:, h]).abs().mean().item()
        mae.append(m)
    return mae


def main():
    device = "cuda" if torch.cuda.is_available() else "cpu"

    data_dir = os.path.join(DATA_DIR, "forecaster")
    _, _, test_loader = create_dataloaders(data_dir, batch_size=32)

    model = PhyLSTMForecaster(n_depths=N_DEPTHS, lstm_units=192).to(device)
    ckpt = os.path.join(MODEL_DIR, "forecaster_best.pt")
    if not os.path.exists(ckpt):
        print(f"Checkpoint not found: {ckpt}")
        return
    model.load_state_dict(torch.load(ckpt, map_location=device, weights_only=True))
    model.eval()

    mae_sum = [0.0] * len(FORECAST_HORIZONS)
    n_batches = 0

    with torch.no_grad():
        for x, y in test_loader:
            x, y = x.to(device), y.to(device)
            pred = model(x)
            mae = mae_per_horizon(pred, y)
            for h in range(len(FORECAST_HORIZONS)):
                mae_sum[h] += mae[h]
            n_batches += 1

    print("Forecast MAE per horizon:")
    for h, days in enumerate(FORECAST_HORIZONS):
        print(f"  {days}d: {mae_sum[h] / max(n_batches, 1):.6f}")
    print(f"Batches evaluated: {n_batches}")


if __name__ == "__main__":
    main()

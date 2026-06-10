"""Validate reconstructor: compute deep-layer Pearson correlation per depth level."""
import os
import sys
import numpy as np
import torch

sys.path.insert(0, os.path.dirname(__file__))
from config import DATA_DIR, MODEL_DIR, N_DEPTHS, DEPTH_LEVELS
from reconstructor.model import DepthProfileMLP
from reconstructor.dataset import create_dataloaders


def main():
    device = "cuda" if torch.cuda.is_available() else "cpu"

    data_dir = os.path.join(DATA_DIR, "reconstructor")
    _, _, test_loader = create_dataloaders(data_dir, batch_size=512)

    model = DepthProfileMLP().to(device)
    ckpt = os.path.join(MODEL_DIR, "reconstructor_best.pt")
    if not os.path.exists(ckpt):
        print(f"Checkpoint not found: {ckpt}")
        return
    model.load_state_dict(torch.load(ckpt, map_location=device, weights_only=True))
    model.eval()

    corr_thetao = np.zeros(N_DEPTHS)
    corr_so = np.zeros(N_DEPTHS)
    n_batches = 0

    with torch.no_grad():
        for x, y_t, y_s in test_loader:
            x = x.to(device)
            out = model(x)
            pred_t = out[:, :N_DEPTHS].cpu().numpy()
            pred_s = out[:, N_DEPTHS:].cpu().numpy()
            y_t = y_t.cpu().numpy()
            y_s = y_s.cpu().numpy()

            for d in range(N_DEPTHS):
                if np.std(pred_t[:, d]) > 0 and np.std(y_t[:, d]) > 0:
                    corr_thetao[d] += np.corrcoef(pred_t[:, d], y_t[:, d])[0, 1]
                if np.std(pred_s[:, d]) > 0 and np.std(y_s[:, d]) > 0:
                    corr_so[d] += np.corrcoef(pred_s[:, d], y_s[:, d])[0, 1]
            n_batches += 1

    corr_thetao /= max(n_batches, 1)
    corr_so /= max(n_batches, 1)

    print("Depth-level Pearson correlation:")
    print(f"{'Depth':>8}  {'thetao':>8}  {'so':>8}")
    print("-" * 30)
    for d in range(N_DEPTHS):
        print(f"{DEPTH_LEVELS[d]:6.1f}m  {corr_thetao[d]:8.4f}  {corr_so[d]:8.4f}")
    print("-" * 30)
    mean_corr_t = corr_thetao.mean()
    mean_corr_s = corr_so.mean()
    deep_corr_t = corr_thetao[4:].mean()
    deep_corr_s = corr_so[4:].mean()
    print(f"{'Mean':>8}  {mean_corr_t:8.4f}  {mean_corr_s:8.4f}")
    print(f"{'Deep mean':>8}  {deep_corr_t:8.4f}  {deep_corr_s:8.4f}")

    if mean_corr_t > 0.7 and mean_corr_s > 0.7:
        print("PASS: Deep correlation > 0.7")
    elif mean_corr_t < 0.5 or mean_corr_s < 0.5:
        print("FAIL: Correlation < 0.5 - surface inputs insufficient")
    else:
        print("WARN: Correlation 0.5-0.7 - marginal, consider additional inputs")


if __name__ == "__main__":
    main()

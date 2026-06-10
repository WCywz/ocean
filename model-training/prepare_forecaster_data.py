"""Build 60-day to multi-horizon 3D training sequences for PhyLSTM forecaster.

Uses trained reconstructor to produce 3D fields, then builds sliding window
sequences PER SPATIAL LOCATION to avoid OOM. Results saved incrementally.

Memory: processes one grid point at a time (~12K time steps x 37 channels
= ~1.8 MB per point), total disk output ~700 MB for ~400 ocean points.
"""
import os
import sys
import numpy as np
import torch
import joblib
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(__file__))
from config import (
    DATA_DIR, MODEL_DIR, N_DEPTHS, INPUT_DAYS, FORECAST_HORIZONS, GRID_H, GRID_W,
    GRID_LAT, GRID_LON, REGION, TRAIN_RATIO, VAL_RATIO, SEED, DEVICE,
)
from reconstructor.model import DepthProfileMLP

OUTPUT_DIR = os.path.join(DATA_DIR, "forecaster")
MAX_HORIZON = max(FORECAST_HORIZONS)
MIN_SEQ = INPUT_DAYS + MAX_HORIZON


def build_sequences_for_point(data_3d, data_chl):
    """Build sliding windows for a single grid point.

    data_3d:  [T, n_depths * 2]  reconstructed thetao + so
    data_chl: [T, 1]             surface chl

    Returns: X [N_seq, 60, C], y [N_seq, 4, C]
    where C = n_depths * 2 + 1 + 2 (thetao + so + chl + lat + lon)
    """
    T = len(data_3d)
    if T < MIN_SEQ:
        return None, None

    sequences = []
    for start in range(0, T - MIN_SEQ + 1):
        x_seq = np.zeros((INPUT_DAYS, N_DEPTHS * 2 + 1 + 2), dtype=np.float32)
        x_seq[:, :N_DEPTHS * 2] = data_3d[start:start + INPUT_DAYS]
        x_seq[:, N_DEPTHS * 2:N_DEPTHS * 2 + 1] = data_chl[start:start + INPUT_DAYS]

        y_horizons = []
        for h in FORECAST_HORIZONS:
            t_idx = start + INPUT_DAYS + h - 1
            y_h = np.zeros(N_DEPTHS * 2 + 1, dtype=np.float32)
            y_h[:N_DEPTHS * 2] = data_3d[t_idx]
            y_h[N_DEPTHS * 2] = data_chl[t_idx, 0]
            y_horizons.append(y_h)

        sequences.append((x_seq, np.stack(y_horizons, axis=0)))

    if not sequences:
        return None, None

    X = np.stack([s[0] for s in sequences], axis=0)
    y = np.stack([s[1] for s in sequences], axis=0)
    return X, y


def main():
    print(f"Device: {DEVICE}")

    # Load reconstructor
    model = DepthProfileMLP().to(DEVICE)
    ckpt = os.path.join(MODEL_DIR, "reconstructor_best.pt")
    if not os.path.exists(ckpt):
        raise FileNotFoundError(f"Reconstructor checkpoint not found: {ckpt}. Train it first.")
    model.load_state_dict(torch.load(ckpt, map_location=DEVICE, weights_only=True))
    model.eval()

    # Load surface data and scalers
    reco_dir = os.path.join(DATA_DIR, "reconstructor")
    X_surface = np.load(os.path.join(reco_dir, "X_surface.npy"))
    times = np.load(os.path.join(reco_dir, "times.npy"), allow_pickle=True)
    land_mask = np.load(os.path.join(reco_dir, "land_mask.npy"))
    train_idx = np.load(os.path.join(reco_dir, "train_idx.npy"))
    val_idx = np.load(os.path.join(reco_dir, "val_idx.npy"))
    test_idx = np.load(os.path.join(reco_dir, "test_idx.npy"))

    n_samples = len(X_surface)
    print(f"Reconstructing 3D for {n_samples} samples...")

    # Reconstruct 3D for ALL samples in batches
    batch_size = 4096
    thetao_recon = np.zeros((n_samples, N_DEPTHS), dtype=np.float32)
    so_recon = np.zeros((n_samples, N_DEPTHS), dtype=np.float32)

    for start in tqdm(range(0, n_samples, batch_size), desc="Reconstructing 3D"):
        end = min(start + batch_size, n_samples)
        x_batch = torch.from_numpy(X_surface[start:end]).float().to(DEVICE)

        with torch.no_grad():
            out = model(x_batch)
            thetao_recon[start:end] = out[:, :N_DEPTHS].cpu().numpy()
            so_recon[start:end] = out[:, N_DEPTHS:].cpu().numpy()

    # Chl from surface data (column index 2)
    chl_surface = X_surface[:, 2:3].copy()

    # Recover grid point index from spatial encoding (columns 5, 6)
    lat_norm = X_surface[:, 5]
    lon_norm = X_surface[:, 6]

    lat_val = lat_norm * (REGION["lat_max"] - REGION["lat_min"]) + REGION["lat_min"]
    lon_val = lon_norm * (REGION["lon_max"] - REGION["lon_min"]) + REGION["lon_min"]

    grid_i = np.round((lat_val - GRID_LAT[0]) / (GRID_LAT[1] - GRID_LAT[0])).astype(int)
    grid_j = np.round((lon_val - GRID_LON[0]) / (GRID_LON[1] - GRID_LON[0])).astype(int)
    grid_i = np.clip(grid_i, 0, GRID_H - 1)
    grid_j = np.clip(grid_j, 0, GRID_W - 1)
    point_key = grid_i * GRID_W + grid_j

    unique_points = np.unique(point_key)
    print(f"Unique ocean grid points: {len(unique_points)}")

    # Per-point build sequences
    all_X, all_y, all_splits = [], [], []
    train_set = set(train_idx)
    val_set = set(val_idx)
    test_set = set(test_idx)

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for pk in tqdm(unique_points, desc="Building per-point sequences"):
        pi = pk // GRID_W
        pj = pk % GRID_W

        point_samples = np.where(point_key == pk)[0]
        if len(point_samples) < MIN_SEQ:
            continue

        # Sort by time
        time_order = np.argsort(times[point_samples])
        point_samples = point_samples[time_order]

        data_3d = np.concatenate(
            [thetao_recon[point_samples], so_recon[point_samples]], axis=1)
        data_chl = chl_surface[point_samples]

        # Add lat/lon encoding
        lat_enc = (GRID_LAT[pi] - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
        lon_enc = (GRID_LON[pj] - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

        X, y = build_sequences_for_point(data_3d, data_chl)
        if X is None:
            continue

        X[:, :, -2] = lat_enc
        X[:, :, -1] = lon_enc

        # Assign each sequence to split based on where its LAST input day falls
        for seq_idx in range(len(X)):
            last_day_sample_idx = point_samples[seq_idx + INPUT_DAYS - 1]
            # Closest sample index in the original dataset
            actual_idx = last_day_sample_idx

            if actual_idx in train_set:
                split = "train"
            elif actual_idx in val_set:
                split = "val"
            elif actual_idx in test_set:
                split = "test"
            else:
                continue

            all_X.append(X[seq_idx])
            all_y.append(y[seq_idx])
            all_splits.append(split)

    # Save by split
    for split_name in ("train", "val", "test"):
        split_X = [x for x, s in zip(all_X, all_splits) if s == split_name]
        split_y = [y for y, s in zip(all_y, all_splits) if s == split_name]

        print(f"{split_name}: {len(split_X)} sequences")

        if len(split_X) > 0:
            np.save(os.path.join(OUTPUT_DIR, f"X_{split_name}.npy"),
                    np.array(split_X, dtype=np.float32))
            np.save(os.path.join(OUTPUT_DIR, f"y_{split_name}.npy"),
                    np.array(split_y, dtype=np.float32))
        else:
            print(f"  WARNING: No {split_name} sequences — using empty array")
            np.save(os.path.join(OUTPUT_DIR, f"X_{split_name}.npy"),
                    np.array([], dtype=np.float32).reshape(0, INPUT_DAYS, N_DEPTHS * 2 + 3))
            np.save(os.path.join(OUTPUT_DIR, f"y_{split_name}.npy"),
                    np.array([], dtype=np.float32).reshape(0, len(FORECAST_HORIZONS), N_DEPTHS * 2 + 1))

    if len(all_X) > 0:
        print(f"Sample X shape: {all_X[0].shape}, y shape: {all_y[0].shape}")
    total_sequences = len(all_X)
    print(f"Total sequences: {total_sequences}")
    print(f"Saved to {OUTPUT_DIR}/")


if __name__ == "__main__":
    main()

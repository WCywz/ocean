"""Build surface to 3D training pairs from MY reanalysis NetCDF files.

Steps:
1. Load phy_raw.nc, bgc_raw.nc, my_surface.nc
2. Interpolate phy data (0.083 deg) to unified grid (0.25 deg)
3. Compute wind stress curl on unified grid
4. Extract surface features: thetao_sfc, so_sfc, chl(log10), zos, wind_curl
5. Extract full depth profiles: thetao[18], so[18]
6. Apply land mask (NaN in thetao)
7. Time-based train/val/test split
"""
import os
import sys
import numpy as np
import xarray as xr
import pandas as pd
import joblib
from sklearn.preprocessing import StandardScaler
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(__file__))
from config import (
    REGION, N_DEPTHS, MY_PHY_NC, MY_BGC_NC, MY_SURFACE_NC, DATA_DIR,
    GRID_LAT, GRID_LON, GRID_H, GRID_W, GRID_RES,
    TRAIN_RATIO, VAL_RATIO, TEST_RATIO, CHL_LOG10, SEED,
)

OUTPUT_DIR = os.path.join(DATA_DIR, "reconstructor")


def compute_wind_curl(tauuo, tauvo, lat_vals, lon_vals):
    """Compute wind stress curl: curl = d(tauvo)/dx - d(tauuo)/dy.

    tauuo, tauvo: [T, H, W] numpy arrays
    Returns: [T, H, W] numpy array
    """
    T, H, W = tauuo.shape
    curl = np.zeros_like(tauuo)

    for i in range(H):
        dlat_m = 111320.0 * (GRID_LAT[1] - GRID_LAT[0])
        dy = dlat_m
        dlon_m = 111320.0 * np.cos(np.deg2rad(lat_vals[i])) * (GRID_LON[1] - GRID_LON[0])
        dx = dlon_m

        # d(tauvo)/dx along longitude
        dtauvo_dx = np.gradient(tauvo[:, i, :], axis=-1) / dx
        # d(tauuo)/dy along latitude (central difference where possible)
        if i > 0 and i < H - 1:
            dtauuo_dy = (tauuo[:, i + 1, :] - tauuo[:, i - 1, :]) / (2 * dy)
        elif i == 0:
            dtauuo_dy = (tauuo[:, 1, :] - tauuo[:, 0, :]) / dy
        else:
            dtauuo_dy = (tauuo[:, -1, :] - tauuo[:, -2, :]) / dy

        curl[:, i, :] = dtauvo_dx - dtauuo_dy

    return curl.astype(np.float32)


def interpolate_2d(da, target_lat, target_lon):
    """Bilinear interpolation of a 2D (time, lat, lon) variable to target grid."""
    interp = da.interp(latitude=target_lat, longitude=target_lon, method="linear")
    return interp.values.astype(np.float32)


def interpolate_3d(da, target_lat, target_lon):
    """Bilinear interpolation of a 3D (time, depth, lat, lon) variable to target grid."""
    interp = da.interp(latitude=target_lat, longitude=target_lon, method="linear")
    return interp.values.astype(np.float32)


def main():
    print("Loading MY reanalysis data...")
    phy_ds = xr.open_dataset(MY_PHY_NC)
    bgc_ds = xr.open_dataset(MY_BGC_NC)

    print(f"phy grid: {len(phy_ds.latitude)}x{len(phy_ds.longitude)} (0.083 deg)")
    print(f"bgc grid: {len(bgc_ds.latitude)}x{len(bgc_ds.longitude)} (0.25 deg)")

    # Load surface forcing (zos, tauuo, tauvo)
    if os.path.exists(MY_SURFACE_NC):
        sfc_ds = xr.open_dataset(MY_SURFACE_NC)
        print(f"Surface vars: {list(sfc_ds.data_vars.keys())}")
        has_sfc = True
    else:
        print(f"WARNING: {MY_SURFACE_NC} not found. Run download_my_surface.py first.")
        print("Proceeding without SSH/wind stress (will use zeros).")
        has_sfc = False

    # ── Step 1: Interpolate phy 3D data to unified grid ──
    print("Interpolating phy data to unified 0.25 deg grid...")
    thetao_3d = interpolate_3d(phy_ds.thetao, GRID_LAT, GRID_LON)  # [T, 18, H, W]
    so_3d = interpolate_3d(phy_ds.so, GRID_LAT, GRID_LON)

    T_phy, _, H, W = thetao_3d.shape
    print(f"phy 3D shape: {thetao_3d.shape}")

    # Surface layer
    thetao_sfc = thetao_3d[:, 0, :, :]   # [T, H, W]
    so_sfc = so_3d[:, 0, :, :]

    # ── Step 2: Process surface forcing ──
    if has_sfc:
        print("Interpolating surface forcing to unified grid...")
        zos_2d = interpolate_2d(sfc_ds.zos, GRID_LAT, GRID_LON)

        tauuo_2d = interpolate_2d(sfc_ds.tauuo, GRID_LAT, GRID_LON)
        tauvo_2d = interpolate_2d(sfc_ds.tauvo, GRID_LAT, GRID_LON)
        wind_curl = compute_wind_curl(tauuo_2d, tauvo_2d, GRID_LAT, GRID_LON)
        T_sfc = zos_2d.shape[0]
    else:
        T_sfc = 0

    # ── Step 3: Process BGC CHL ──
    print("Processing BGC CHL data...")
    chl_2d = interpolate_2d(bgc_ds.chl, GRID_LAT, GRID_LON)  # [T, H, W]
    T_bgc = chl_2d.shape[0]

    if CHL_LOG10:
        chl_2d = np.log10(np.maximum(chl_2d, 1e-4))

    # ── Step 4: Time alignment ──
    phy_times = phy_ds.time.values
    bgc_times = bgc_ds.time.values
    sfc_times = sfc_ds.time.values if has_sfc else phy_times

    print(f"phy times: {len(phy_times)}, bgc times: {len(bgc_times)}, sfc times: {len(sfc_times)}")

    # For each bgc time step, find nearest phy and sfc time step
    common_indices = []
    for t_bgc_idx, t_bgc in enumerate(tqdm(bgc_times, desc="Aligning times")):
        phy_idx = int(np.argmin(np.abs(phy_times - t_bgc)))
        sfc_idx = int(np.argmin(np.abs(sfc_times - t_bgc)))
        t_phy = phy_times[phy_idx]
        t_sfc = sfc_times[sfc_idx]

        dt_phy = abs((pd.Timestamp(t_phy) - pd.Timestamp(t_bgc)).total_seconds() / 3600)
        dt_sfc = abs((pd.Timestamp(t_sfc) - pd.Timestamp(t_bgc)).total_seconds() / 3600)

        if dt_phy <= 12 and dt_sfc <= 12:
            common_indices.append((t_bgc_idx, phy_idx, sfc_idx))

    print(f"Common timesteps: {len(common_indices)}")

    # ── Step 5: Build land mask ──
    land_mask = np.isnan(thetao_3d[0, 0, :, :])
    ocean_mask = ~land_mask
    n_ocean = int(ocean_mask.sum())
    n_total = H * W
    print(f"Ocean pixels: {n_ocean} / {n_total} ({n_ocean / n_total * 100:.0f}%)")

    # ── Step 6: Build samples ──
    X_list, y_thetao_list, y_so_list = [], [], []
    time_list = []

    for t_bgc_idx, phy_idx, sfc_idx in tqdm(common_indices, desc="Building samples"):
        for i in range(H):
            for j in range(W):
                if land_mask[i, j]:
                    continue

                thetao_s = thetao_sfc[phy_idx, i, j]
                so_s = so_sfc[phy_idx, i, j]
                chl_s = chl_2d[t_bgc_idx, i, j]
                zos_s = zos_2d[sfc_idx, i, j] if has_sfc else 0.0
                curl_s = wind_curl[sfc_idx, i, j] if has_sfc else 0.0

                if np.isnan(thetao_s) or np.isnan(so_s):
                    continue

                lat_norm = (GRID_LAT[i] - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
                lon_norm = (GRID_LON[j] - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

                X_list.append([
                    thetao_s, so_s, chl_s, zos_s, curl_s, lat_norm, lon_norm
                ])

                y_thetao_list.append(thetao_3d[phy_idx, :, i, j])
                y_so_list.append(so_3d[phy_idx, :, i, j])
                time_list.append(str(t_bgc)[:10])

    phy_ds.close()
    bgc_ds.close()
    if has_sfc:
        sfc_ds.close()

    X = np.array(X_list, dtype=np.float32)
    y_thetao = np.array(y_thetao_list, dtype=np.float32)
    y_so = np.array(y_so_list, dtype=np.float32)
    times_arr = np.array(time_list)

    print(f"Total ocean samples: {len(X)}")
    print(f"X shape: {X.shape}, y_thetao: {y_thetao.shape}, y_so: {y_so.shape}")

    # ── Step 7: Handle NaNs ──
    for c in range(X.shape[1]):
        col_mean = np.nanmean(X[:, c])
        X[:, c] = np.nan_to_num(X[:, c], nan=col_mean if not np.isnan(col_mean) else 0.0)
    for d in range(N_DEPTHS):
        col_mean = np.nanmean(y_thetao[:, d])
        y_thetao[:, d] = np.nan_to_num(y_thetao[:, d], nan=col_mean if not np.isnan(col_mean) else 0.0)
        col_mean = np.nanmean(y_so[:, d])
        y_so[:, d] = np.nan_to_num(y_so[:, d], nan=col_mean if not np.isnan(col_mean) else 0.0)

    # ── Step 8: Time-based split ──
    unique_times = sorted(set(time_list))
    n_times = len(unique_times)
    n_train_t = int(n_times * TRAIN_RATIO)
    n_val_t = int(n_times * VAL_RATIO)

    train_times = set(unique_times[:n_train_t])
    val_times = set(unique_times[n_train_t:n_train_t + n_val_t])
    test_times = set(unique_times[n_train_t + n_val_t:])

    train_idx = [i for i, t in enumerate(time_list) if t in train_times]
    val_idx = [i for i, t in enumerate(time_list) if t in val_times]
    test_idx = [i for i, t in enumerate(time_list) if t in test_times]

    print(f"Split - train: {len(train_idx)}, val: {len(val_idx)}, test: {len(test_idx)}")
    print(f"Time ranges - train: {unique_times[0]}~{unique_times[n_train_t-1]}, "
          f"val: {unique_times[n_train_t]}~{unique_times[n_train_t+n_val_t-1]}, "
          f"test: {unique_times[n_train_t+n_val_t]}~{unique_times[-1]}")

    # ── Step 9: Scale and save ──
    scaler_X = StandardScaler()
    scaler_y_thetao = StandardScaler()
    scaler_y_so = StandardScaler()

    X_scaled = scaler_X.fit_transform(X)
    y_thetao_scaled = scaler_y_thetao.fit_transform(y_thetao)
    y_so_scaled = scaler_y_so.fit_transform(y_so)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    np.save(os.path.join(OUTPUT_DIR, "X_surface.npy"), X_scaled)
    np.save(os.path.join(OUTPUT_DIR, "y_thetao.npy"), y_thetao_scaled)
    np.save(os.path.join(OUTPUT_DIR, "y_so.npy"), y_so_scaled)
    np.save(os.path.join(OUTPUT_DIR, "times.npy"), times_arr)
    np.save(os.path.join(OUTPUT_DIR, "train_idx.npy"), np.array(train_idx))
    np.save(os.path.join(OUTPUT_DIR, "val_idx.npy"), np.array(val_idx))
    np.save(os.path.join(OUTPUT_DIR, "test_idx.npy"), np.array(test_idx))
    np.save(os.path.join(OUTPUT_DIR, "land_mask.npy"), land_mask)
    np.save(os.path.join(OUTPUT_DIR, "grid_lat.npy"), np.array(GRID_LAT, dtype=np.float32))
    np.save(os.path.join(OUTPUT_DIR, "grid_lon.npy"), np.array(GRID_LON, dtype=np.float32))
    joblib.dump(scaler_X, os.path.join(OUTPUT_DIR, "scaler_X.pkl"))
    joblib.dump(scaler_y_thetao, os.path.join(OUTPUT_DIR, "scaler_y_thetao.pkl"))
    joblib.dump(scaler_y_so, os.path.join(OUTPUT_DIR, "scaler_y_so.pkl"))

    print(f"Saved to {OUTPUT_DIR}/")
    print(f"X scaled min/max: {X_scaled.min():.2f} / {X_scaled.max():.2f}")
    print("Done.")


if __name__ == "__main__":
    main()

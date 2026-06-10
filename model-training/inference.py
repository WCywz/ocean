"""Production inference: surface inputs -> reconstructor -> forecaster -> CSV.

Calls reconstructor on ALL 60 days of history for training-inference consistency.
Keeps values in normalized space until after forecaster output, then inverse-transforms
to physical units. This matches the forecaster training data distribution.

Called by Java backend via ProcessBuilder:
  python inference.py --date 2026-06-01 --output <path>
"""
import os
import sys
import argparse
import numpy as np
import pandas as pd
import torch
import joblib
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(__file__))
from config import (
    DATA_DIR, MODEL_DIR, N_DEPTHS, INPUT_DAYS, FORECAST_HORIZONS,
    GRID_LAT, GRID_LON, REGION,
)
from reconstructor.model import DepthProfileMLP
from forecaster.model import PhyLSTMForecaster


def load_surface_data(data_dir, date_str):
    """Load surface CSV and return raw features [N_points, 7] + coords."""
    csv_path = os.path.join(data_dir, f"surface_{date_str}.csv")
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Surface data not found: {csv_path}")

    df = pd.read_csv(csv_path)
    features = df[["thetao", "so", "chl", "zos"]].values.astype(np.float32)

    # Fill NaN chl with a plausible default (0.1 mg/m^3 -> log10 = -1.0)
    features[:, 2] = np.nan_to_num(features[:, 2], nan=0.1)
    features[:, 2] = np.log10(np.maximum(features[:, 2], 1e-4))

    # Spatial encoding
    lat_norm = (df["lat"].values - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
    lon_norm = (df["lon"].values - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

    # NRT does not have tauuo/tauvo - wind_curl set to 0
    wind_curl = np.zeros(len(df), dtype=np.float32)

    X = np.column_stack([
        features[:, 0],  # thetao_sfc
        features[:, 1],  # so_sfc
        features[:, 2],  # chl(log10)
        features[:, 3],  # zos
        wind_curl,       # wind curl (0 for NRT)
        lat_norm,
        lon_norm,
    ]).astype(np.float32)

    return X, df[["lat", "lon"]].values, features[:, 2]  # also return raw log10 chl for later


def run_inference(date_str, data_dir, output_path):
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Inference on: {device}")

    # Load scalers
    reco_dir = os.path.join(DATA_DIR, "reconstructor")
    scaler_X = joblib.load(os.path.join(reco_dir, "scaler_X.pkl"))
    scaler_y_thetao = joblib.load(os.path.join(reco_dir, "scaler_y_thetao.pkl"))
    scaler_y_so = joblib.load(os.path.join(reco_dir, "scaler_y_so.pkl"))

    # Extract chl scaling params (column index 2 in the 7-feature scaler)
    chl_mean = float(scaler_X.mean_[2])
    chl_std = float(scaler_X.scale_[2])

    # Load models
    reconstructor = DepthProfileMLP().to(device)
    rec_ckpt = os.path.join(MODEL_DIR, "reconstructor_best.pt")
    if not os.path.exists(rec_ckpt):
        raise FileNotFoundError(f"Reconstructor not found: {rec_ckpt}")
    reconstructor.load_state_dict(torch.load(rec_ckpt, map_location=device, weights_only=True))
    reconstructor.eval()

    forecaster = PhyLSTMForecaster(n_depths=N_DEPTHS, lstm_units=192).to(device)
    fc_ckpt = os.path.join(MODEL_DIR, "forecaster_best.pt")
    if not os.path.exists(fc_ckpt):
        raise FileNotFoundError(f"Forecaster not found: {fc_ckpt}")
    forecaster.load_state_dict(torch.load(fc_ckpt, map_location=device, weights_only=True))
    forecaster.eval()

    # Load land mask
    land_mask = np.load(os.path.join(reco_dir, "land_mask.npy"))

    dt = datetime.fromisoformat(date_str)
    n_points = len(GRID_LAT) * len(GRID_LON)

    # Check which points are ocean vs land
    ocean_indices = np.where(~land_mask.flatten())[0]
    print(f"Ocean points: {len(ocean_indices)} / {n_points}")

    coords = np.column_stack([
        np.repeat(GRID_LAT, len(GRID_LON)),
        np.tile(GRID_LON, len(GRID_LAT)),
    ]).astype(np.float32)

    lat_enc = (coords[:, 0] - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
    lon_enc = (coords[:, 1] - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

    # Stage 1: Reconstruct 3D for ALL 60 days IN SCALED SPACE
    # Forecaster was trained on scaled values, so keep everything scaled until after forecast
    print(f"Reconstructing 3D for {INPUT_DAYS} days of history...")
    seq_3d = np.zeros((n_points, INPUT_DAYS, N_DEPTHS * 2 + 1), dtype=np.float32)
    last_valid_day = None  # track last valid day for gap filling

    for day_offset in range(INPUT_DAYS, 0, -1):
        d = (dt - timedelta(days=day_offset)).strftime("%Y-%m-%d")
        try:
            X_day, _, _ = load_surface_data(data_dir, d)
            last_valid_day = (X_day, day_offset)
        except FileNotFoundError:
            if last_valid_day is not None:
                # Fill with last available day (better than zeros)
                X_day = last_valid_day[0]
                print(f"  WARNING: No data for {d}, reusing nearest available day")
            else:
                print(f"  WARNING: No data for {d}, skipping")
                continue

        X_scaled = scaler_X.transform(X_day)

        # Reconstruct 3D (in scaled space)
        x_t = torch.from_numpy(X_scaled).float().to(device)
        with torch.no_grad():
            out = reconstructor(x_t)
            thetao_rec = out[:, :N_DEPTHS].cpu().numpy()      # scaled
            so_rec = out[:, N_DEPTHS:].cpu().numpy()          # scaled

        seq_idx = INPUT_DAYS - day_offset
        seq_3d[:, seq_idx, :N_DEPTHS] = thetao_rec
        seq_3d[:, seq_idx, N_DEPTHS:2 * N_DEPTHS] = so_rec
        seq_3d[:, seq_idx, 2 * N_DEPTHS] = X_scaled[:, 2]     # scaled log10 chl

    # Build forecaster input (all in scaled space)
    n_total = N_DEPTHS * 2 + 1 + 2
    seq_input = np.zeros((n_points, INPUT_DAYS, n_total), dtype=np.float32)
    seq_input[:, :, :N_DEPTHS * 2 + 1] = seq_3d
    seq_input[:, :, -2] = lat_enc[:, np.newaxis]
    seq_input[:, :, -1] = lon_enc[:, np.newaxis]

    # Stage 2: Run forecaster
    print("Running forecaster...")
    results = []
    batch_size = 64

    for start in range(0, n_points, batch_size):
        end = min(start + batch_size, n_points)
        x_batch = torch.from_numpy(seq_input[start:end]).float().to(device)

        with torch.no_grad():
            fc = forecaster(x_batch)

        fc_np = fc.cpu().numpy()
        for b in range(end - start):
            pt = start + b
            if land_mask.flatten()[pt]:
                continue  # skip land

            lat, lon = coords[pt]

            for h_idx, horizon_days in enumerate(FORECAST_HORIZONS):
                fc_date = (dt + timedelta(days=horizon_days)).strftime("%Y-%m-%d")
                pred = fc_np[b, h_idx]

                # Inverse-transform thetao: scaled -> physical
                thetao_scaled = pred[:N_DEPTHS]
                thetao_phys = scaler_y_thetao.inverse_transform(
                    thetao_scaled.reshape(1, -1)).flatten()

                # Inverse-transform so: scaled -> physical
                so_scaled = pred[N_DEPTHS:2 * N_DEPTHS]
                so_phys = scaler_y_so.inverse_transform(
                    so_scaled.reshape(1, -1)).flatten()

                # Inverse-transform chl: scaled log10 -> physical mg/m^3
                chl_scaled = float(pred[2 * N_DEPTHS])
                chl_log10 = chl_scaled * chl_std + chl_mean
                chl_phys = float(10.0 ** chl_log10)

                results.append({
                    "forecast_date": fc_date,
                    "latitude": round(float(lat), 6),
                    "longitude": round(float(lon), 6),
                    "forecast_day": horizon_days,
                    "variable": "chl",
                    "depth": -1,
                    "value": round(chl_phys, 6),
                })

                for di in range(N_DEPTHS):
                    results.append({
                        "forecast_date": fc_date,
                        "latitude": round(float(lat), 6),
                        "longitude": round(float(lon), 6),
                        "forecast_day": horizon_days,
                        "variable": "thetao",
                        "depth": di,
                        "value": round(float(thetao_phys[di]), 6),
                    })
                    results.append({
                        "forecast_date": fc_date,
                        "latitude": round(float(lat), 6),
                        "longitude": round(float(lon), 6),
                        "forecast_day": horizon_days,
                        "variable": "so",
                        "depth": di,
                        "value": round(float(so_phys[di]), 6),
                    })

    df_out = pd.DataFrame(results)
    df_out.to_csv(output_path, index=False, encoding="utf-8-sig")
    print(f"Saved {len(df_out)} predictions to {output_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--date", required=True, help="Forecast date YYYY-MM-DD")
    parser.add_argument("--data-dir", default=os.path.join(DATA_DIR, "nrt_daily"))
    parser.add_argument("--output", default="forecast_output.csv")
    args = parser.parse_args()
    run_inference(args.date, args.data_dir, args.output)


if __name__ == "__main__":
    main()

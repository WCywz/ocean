"""Run forecast models with external CSV data, output JSON for DB insertion.

Usage:
  python run_forecast.py --date 2026-01-15 --csv ./simulated_data.csv
  python run_forecast.py --date 2026-01-15 --csv ./simulated_data.csv --output forecast.json

Supports two CSV formats:
  Long:  time,depth,latitude,longitude,chl,thetao,so
  Wide:  time,lat,lon,chl,thetao_d0,...,thetao_d17,so_d0,...,so_d17
"""

import os
import sys
import json
import argparse
import numpy as np
import pandas as pd
import torch
import joblib
from datetime import datetime, timedelta

# Fix Windows GBK encoding for stdout
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, BASE_DIR)

from train_model_torch import OceanForecastModel

SEQ_LEN = 30
HORIZON = 7
DATA_DIR = os.path.join(BASE_DIR, "data")

DEPTH_LEVELS = [0.5, 1.5, 2.6, 3.8, 5.1, 6.4, 7.9, 9.6, 11.4, 13.5,
                15.8, 18.5, 21.6, 25.2, 29.4, 34.4, 40.3, 47.4]
N_DEPTHS = len(DEPTH_LEVELS)

MODELS = {
    "chl": ("best_model_chl.pt", "y_chl", 1, "mg/m³", "CHL"),
    "sst": ("best_model_thetao.pt", "y_temp", 2, "degree_C", "SST"),
    "so": ("best_model_so.pt", "y_so", 3, "PSU", "SALINITY"),
}


def detect_format(df):
    return "long" if "depth" in df.columns else "wide"


def pivot_long_to_wide(df):
    """Pivot long-format CSV (time,depth,lat,lon,chl,thetao,so) to wide format.

    Rounded depths are snapped to the nearest model depth level. Missing deeper
    depths (e.g. shallow-water stations) are filled by copying the deepest
    available value downward (ffill along depth axis).
    """
    df = df.copy()
    df["time"] = pd.to_datetime(df["time"])
    df["depth"] = df["depth"].astype(float).round(1)

    # Snap each rounded depth to the nearest model depth level
    levels = np.array(DEPTH_LEVELS)
    depth_to_model = {d: levels[np.abs(levels - d).argmin()] for d in df["depth"].unique()}
    df["depth"] = df["depth"].map(depth_to_model)

    thetao_cols = [f"thetao_d{i}" for i in range(N_DEPTHS)]
    so_cols = [f"so_d{i}" for i in range(N_DEPTHS)]

    # Pivot thetao and so with depth snapped to model levels
    df_pivot = df[df["thetao"].notna() | df["so"].notna()]

    temp_pivot = df_pivot.pivot_table(
        index=["time", "latitude", "longitude"],
        columns="depth", values="thetao", aggfunc="first"
    )
    # Ensure all model depth columns exist
    temp_pivot = temp_pivot.reindex(columns=levels, fill_value=np.nan)
    temp_pivot = temp_pivot.sort_index(axis=1).reset_index()
    temp_pivot.columns = ["time", "lat", "lon"] + thetao_cols

    so_pivot = df_pivot.pivot_table(
        index=["time", "latitude", "longitude"],
        columns="depth", values="so", aggfunc="first"
    )
    so_pivot = so_pivot.reindex(columns=levels, fill_value=np.nan)
    so_pivot = so_pivot.sort_index(axis=1).reset_index()
    so_pivot.columns = ["time", "lat", "lon"] + so_cols

    # CHL: surface only — must exclude rows where chl is NaN (SQL PIVOT mixes chl/thetao/so depths)
    surface_depth = DEPTH_LEVELS[0]
    chl_df = df[df["chl"].notna() & (df["depth"] == surface_depth)]
    chl_surface = chl_df[["time", "latitude", "longitude", "chl"]].drop_duplicates(
        subset=["time", "latitude", "longitude"]
    )
    chl_surface = chl_surface.rename(columns={"latitude": "lat", "longitude": "lon"})

    merged = chl_surface.merge(temp_pivot, on=["time", "lat", "lon"], how="left")
    merged = merged.merge(so_pivot, on=["time", "lat", "lon"], how="left")

    # Drop rows where ALL thetao or ALL so are missing
    merged = merged.dropna(subset=thetao_cols, how="all")
    merged = merged.dropna(subset=so_cols, how="all")

    # Forward-fill missing deep depths from the deepest available value
    merged[thetao_cols] = merged[thetao_cols].ffill(axis=1)
    merged[so_cols] = merged[so_cols].ffill(axis=1)

    merged = merged.sort_values(["lat", "lon", "time"]).reset_index(drop=True)
    return merged


def load_wide_format(csv_path):
    df = pd.read_csv(csv_path)
    df["time"] = pd.to_datetime(df["time"])
    df = df.sort_values(["lat", "lon", "time"]).reset_index(drop=True)
    return df


def load_scalers():
    return {
        "chl": joblib.load(os.path.join(DATA_DIR, "scaler_chl.pkl")),
        "temp": joblib.load(os.path.join(DATA_DIR, "scaler_temp.pkl")),
        "so": joblib.load(os.path.join(DATA_DIR, "scaler_so.pkl")),
        "lon": joblib.load(os.path.join(DATA_DIR, "scaler_lon.pkl")),
        "lat": joblib.load(os.path.join(DATA_DIR, "scaler_lat.pkl")),
        "y_chl": joblib.load(os.path.join(DATA_DIR, "scaler_y_chl.pkl")),
        "y_temp": joblib.load(os.path.join(DATA_DIR, "scaler_y_temp.pkl")),
        "y_so": joblib.load(os.path.join(DATA_DIR, "scaler_y_so.pkl")),
    }


def load_models(device):
    models = {}
    for var_name, (pt_file, _, model_id, unit, data_type) in MODELS.items():
        model_path = os.path.join(BASE_DIR, pt_file)
        model = OceanForecastModel(N_DEPTHS).to(device)
        model.load_state_dict(torch.load(model_path, map_location=device, weights_only=True))
        model.eval()
        models[var_name] = model
    return models


def build_sequences(df, forecast_start_date):
    thetao_cols = [f"thetao_d{i}" for i in range(N_DEPTHS)]
    so_cols = [f"so_d{i}" for i in range(N_DEPTHS)]

    cutoff_date = pd.to_datetime(forecast_start_date)
    df = df[df["time"] < cutoff_date].copy()

    print(f"  After cutoff filter: {len(df)} rows, {df['time'].nunique()} unique dates", file=sys.stderr)

    X_chl_list, X_temp_list, X_so_list = [], [], []
    X_space_list = []
    meta_list = []

    n_total = 0
    n_short = 0
    n_nan = 0

    for (lat, lon), group in df.groupby(["lat", "lon"]):
        n_total += 1
        group = group.sort_values("time").reset_index(drop=True)
        if len(group) < SEQ_LEN:
            n_short += 1
            continue

        group = group.tail(SEQ_LEN)

        chl_vals = group["chl"].values.astype(np.float32)
        temp_vals = group[thetao_cols].values.astype(np.float32)
        so_vals = group[so_cols].values.astype(np.float32)

        chl_nan = np.isnan(chl_vals).any()
        temp_nan = np.isnan(temp_vals).any()
        so_nan = np.isnan(so_vals).any()

        if chl_nan or temp_nan or so_nan:
            if n_nan == 0:
                print(f"  First NaN: chl={chl_nan} temp={temp_nan} so={so_nan} at lat={lat} lon={lon}", file=sys.stderr)
                if chl_nan:
                    print(f"    chl unique vals: {np.unique(chl_vals[~np.isnan(chl_vals)])[:5]}", file=sys.stderr)
                if temp_nan:
                    nan_cols = [thetao_cols[i] for i in range(N_DEPTHS) if np.isnan(temp_vals[:, i]).any()]
                    print(f"    temp NaN cols ({len(nan_cols)}): {nan_cols[:5]}...", file=sys.stderr)
                if so_nan:
                    nan_cols = [so_cols[i] for i in range(N_DEPTHS) if np.isnan(so_vals[:, i]).any()]
                    print(f"    so NaN cols ({len(nan_cols)}): {nan_cols[:5]}...", file=sys.stderr)
            n_nan += 1
            continue

        X_chl_list.append(chl_vals.reshape(-1, 1))
        X_temp_list.append(temp_vals)
        X_so_list.append(so_vals)
        X_space_list.append([lon, lat])
        meta_list.append({"lat": lat, "lon": lon})

    print(f"  Grid points: {n_total} total, {n_short} short, {n_nan} NaN, {len(meta_list)} valid", file=sys.stderr)

    if not X_chl_list:
        return None, None

    X_chl = np.array(X_chl_list).astype(np.float32)
    X_temp = np.array(X_temp_list).astype(np.float32)
    X_so = np.array(X_so_list).astype(np.float32)
    X_space = np.array(X_space_list).astype(np.float32)

    return (X_chl, X_temp, X_so, X_space), meta_list


def normalize_inputs(data, scalers):
    X_chl, X_temp, X_so, X_space = data
    N = len(X_chl)

    X_chl_flat = X_chl.reshape(N, -1)
    X_chl_norm = scalers["chl"].transform(X_chl_flat).reshape(N, SEQ_LEN, 1)

    X_temp_flat = X_temp.reshape(N, -1)
    X_temp_norm = scalers["temp"].transform(X_temp_flat).reshape(N, SEQ_LEN, N_DEPTHS)

    X_so_flat = X_so.reshape(N, -1)
    X_so_norm = scalers["so"].transform(X_so_flat).reshape(N, SEQ_LEN, N_DEPTHS)

    X_space_norm = np.column_stack([
        scalers["lon"].transform(X_space[:, [0]]).ravel(),
        scalers["lat"].transform(X_space[:, [1]]).ravel(),
    ]).astype(np.float32)

    return X_chl_norm, X_temp_norm, X_so_norm, X_space_norm


def run_predictions(models, data_norm, scalers, metas, forecast_start_date, device):
    X_chl, X_temp, X_so, X_space = data_norm

    X_chl_t = torch.from_numpy(X_chl).float().to(device)
    X_temp_t = torch.from_numpy(X_temp).float().to(device)
    X_so_t = torch.from_numpy(X_so).float().to(device)
    X_space_t = torch.from_numpy(X_space).float().to(device)

    results = []
    forecast_start = pd.to_datetime(forecast_start_date).date()

    for var_name, (pt_file, scaler_y_key, model_id, unit, data_type) in MODELS.items():
        scaler_y = scalers[scaler_y_key]
        model = models[var_name]

        with torch.no_grad():
            pred_scaled = model(X_chl_t, X_temp_t, X_so_t, X_space_t)
            pred_scaled = pred_scaled.cpu().numpy()

        pred_real = scaler_y.inverse_transform(pred_scaled)

        for i, meta in enumerate(metas):
            for day_offset in range(HORIZON):
                forecast_date = (forecast_start + timedelta(days=day_offset + 1)).isoformat()
                results.append({
                    "model_id": model_id,
                    "data_type": data_type,
                    "variable": var_name,
                    "forecast_date": forecast_date,
                    "depth": 0.0,
                    "lat": round(meta["lat"], 6),
                    "lon": round(meta["lon"], 6),
                    "forecast_day": day_offset + 1,
                    "value": round(float(pred_real[i, day_offset]), 6),
                    "unit": unit,
                })

    return results


def main():
    parser = argparse.ArgumentParser(description="Run ocean forecast models from CSV")
    parser.add_argument("--date", required=True, help="Forecast start date (YYYY-MM-DD)")
    parser.add_argument("--csv", required=True, help="Path to input CSV file")
    parser.add_argument("--output", default=None, help="Output JSON path (default: print to stdout)")
    args = parser.parse_args()

    # Validate date
    try:
        datetime.fromisoformat(args.date)
    except ValueError:
        print(f"ERROR: Invalid date format: {args.date}, expected YYYY-MM-DD")
        sys.exit(1)

    if not os.path.exists(args.csv):
        print(f"ERROR: CSV file not found: {args.csv}")
        sys.exit(1)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}", file=sys.stderr)

    # Load CSV
    print(f"Loading {args.csv}...", file=sys.stderr)
    df = pd.read_csv(args.csv)

    fmt = detect_format(df)
    print(f"Detected format: {fmt}", file=sys.stderr)

    if fmt == "long":
        df = pivot_long_to_wide(df)
        print(f"Pivoted to wide: {len(df)} rows", file=sys.stderr)
    else:
        df = load_wide_format(args.csv)

    # Build sequences
    print(f"Building 30-day sequences before {args.date}...", file=sys.stderr)
    data, metas = build_sequences(df, args.date)

    if data is None:
        print("ERROR: No valid grid points with complete 30-day data", file=sys.stderr)
        sys.exit(1)

    print(f"Valid grid points: {len(metas)}", file=sys.stderr)

    # Load scalers and models
    print("Loading scalers...", file=sys.stderr)
    scalers = load_scalers()

    print("Loading models...", file=sys.stderr)
    models = load_models(device)

    # Normalize
    print("Normalizing...", file=sys.stderr)
    data_norm = normalize_inputs(data, scalers)

    # Predict
    print(f"Running predictions from {args.date}...", file=sys.stderr)
    results = run_predictions(models, data_norm, scalers, metas, args.date, device)

    print(f"Generated {len(results)} predictions", file=sys.stderr)

    if args.output:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(results, f, ensure_ascii=False)
        print(f"Saved to {args.output}", file=sys.stderr)
    else:
        print(json.dumps(results, ensure_ascii=False))


if __name__ == "__main__":
    main()

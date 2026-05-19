"""Run inference with all 3 trained PyTorch models, produce unified prediction CSV."""
import os
import sys
import argparse
import numpy as np
import pandas as pd
import torch
import joblib
from datetime import datetime, timedelta
from tqdm import tqdm

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, BASE_DIR)

from train_model_torch import OceanForecastModel

SEQ_LEN = 30
HORIZON = 7
DATA_DIR = os.path.join(BASE_DIR, "data")
MODELS = {
    "CHL": ("best_model_chl.pt", "y_chl", 1, "mg/m³"),
    "TEMP": ("best_model_thetao.pt", "y_thetao", 2, "°C"),
    "SO": ("best_model_so.pt", "y_so", 3, "PSU"),
}


def check_files():
    required = [
        os.path.join(DATA_DIR, "X_chl.npy"),
        os.path.join(DATA_DIR, "X_temp.npy"),
        os.path.join(DATA_DIR, "X_so.npy"),
        os.path.join(DATA_DIR, "X_space.npy"),
        os.path.join(DATA_DIR, "scaler_chl.pkl"),
        os.path.join(DATA_DIR, "scaler_temp.pkl"),
        os.path.join(DATA_DIR, "scaler_so.pkl"),
        os.path.join(DATA_DIR, "scaler_lon.pkl"),
        os.path.join(DATA_DIR, "scaler_lat.pkl"),
        os.path.join(DATA_DIR, "scaler_y_chl.pkl"),
        os.path.join(DATA_DIR, "scaler_y_temp.pkl"),
        os.path.join(DATA_DIR, "scaler_y_so.pkl"),
    ]
    for name, (filename, _, _, _) in MODELS.items():
        required.append(os.path.join(BASE_DIR, filename))

    missing = [f for f in required if not os.path.exists(f)]
    if missing:
        print("ERROR: Missing required files:")
        for f in missing:
            print(f"  - {f}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Run PyTorch multi-model ocean forecast inference")
    parser.add_argument("--forecast-date", default=datetime.now().date().isoformat(),
                        help="Forecast start date (YYYY-MM-DD), defaults to today")
    parser.add_argument("--output", default=os.path.join(BASE_DIR, "prediction_all_models.csv"),
                        help="Output CSV path")
    args = parser.parse_args()

    check_files()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}")

    # Load data
    print("Loading data...")
    X_chl = torch.from_numpy(np.load(os.path.join(DATA_DIR, "X_chl.npy"))).float()
    X_temp = torch.from_numpy(np.load(os.path.join(DATA_DIR, "X_temp.npy"))).float()
    X_so = torch.from_numpy(np.load(os.path.join(DATA_DIR, "X_so.npy"))).float()
    X_space_np = np.load(os.path.join(DATA_DIR, "X_space.npy"))

    # Load scalers
    scaler_chl = joblib.load(os.path.join(DATA_DIR, "scaler_chl.pkl"))
    scaler_temp = joblib.load(os.path.join(DATA_DIR, "scaler_temp.pkl"))
    scaler_so = joblib.load(os.path.join(DATA_DIR, "scaler_so.pkl"))
    scaler_lon = joblib.load(os.path.join(DATA_DIR, "scaler_lon.pkl"))
    scaler_lat = joblib.load(os.path.join(DATA_DIR, "scaler_lat.pkl"))
    scaler_y_chl = joblib.load(os.path.join(DATA_DIR, "scaler_y_chl.pkl"))
    scaler_y_temp = joblib.load(os.path.join(DATA_DIR, "scaler_y_temp.pkl"))
    scaler_y_so = joblib.load(os.path.join(DATA_DIR, "scaler_y_so.pkl"))

    # Load models
    n_depths = X_temp.shape[-1]
    models = {}
    for var_name, (filename, _, _, _) in MODELS.items():
        model_path = os.path.join(BASE_DIR, filename)
        model = OceanForecastModel(n_depths).to(device)
        model.load_state_dict(torch.load(model_path, map_location=device, weights_only=True))
        model.eval()
        models[var_name] = model
        print(f"Loaded {filename}")

    # Find last 30-day window per grid point
    space_raw = np.column_stack([
        scaler_lon.inverse_transform(X_space_np[:, [0]]).ravel(),
        scaler_lat.inverse_transform(X_space_np[:, [1]]).ravel(),
    ])
    space_raw = space_raw.round(4)

    df_space = pd.DataFrame(space_raw, columns=["lon", "lat"])
    df_space["idx"] = range(len(df_space))
    last_indices = df_space.groupby(["lon", "lat"])["idx"].last().values

    X_chl_pred = X_chl[last_indices].to(device)
    X_temp_pred = X_temp[last_indices].to(device)
    X_so_pred = X_so[last_indices].to(device)
    X_space_pred = X_space_np[last_indices]
    lon_lat = space_raw[last_indices]

    forecast_start = datetime.fromisoformat(args.forecast_date).date()

    scaler_y_map = {
        "CHL": scaler_y_chl,
        "TEMP": scaler_y_temp,
        "SO": scaler_y_so,
    }
    unit_map = {name: unit for name, (_, _, _, unit) in MODELS.items()}
    id_map = {name: mid for name, (_, _, mid, _) in MODELS.items()}

    print("Running predictions...")
    results = []

    for var_name, model in models.items():
        scaler_y = scaler_y_map[var_name]
        unit = unit_map[var_name]
        model_id = id_map[var_name]

        # Predict
        with torch.no_grad():
            X_space_t = torch.from_numpy(X_space_pred).float().to(device)
            pred_scaled = model(X_chl_pred, X_temp_pred, X_so_pred, X_space_t)
            pred_scaled = pred_scaled.cpu().numpy()

        pred_real = scaler_y.inverse_transform(pred_scaled)

        for i, (lon, lat) in enumerate(tqdm(lon_lat, desc=var_name)):
            for day_offset in range(HORIZON):
                forecast_date = (forecast_start + timedelta(days=day_offset)).isoformat()
                results.append({
                    "model_id": model_id,
                    "data_type": var_name,
                    "forecast_date": forecast_date,
                    "longitude": round(float(lon), 6),
                    "latitude": round(float(lat), 6),
                    "forecast_day": day_offset + 1,
                    "value": round(float(pred_real[i, day_offset]), 6),
                    "unit": unit,
                })

    df_out = pd.DataFrame(results)
    df_out.to_csv(args.output, index=False, encoding="utf-8-sig")
    print(f"Saved {len(df_out)} predictions to {args.output}")


if __name__ == "__main__":
    main()

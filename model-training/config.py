"""Shared configuration for model training and inference.

Data paths: set MODEL_DATA_DIR env var to override the default.
"""
import os

# ── Region ────────────────────────────────────────────
REGION = {
    "lon_min": 121.33,
    "lon_max": 125.58,
    "lat_min": 26.92,
    "lat_max": 32.67,
}

# ── Depth levels (MY data, 18 layers, meters) ────────
DEPTH_LEVELS = [
    0.5, 1.5, 2.6, 3.8, 5.1, 6.4, 7.9, 9.6,
    11.4, 13.5, 15.8, 18.5, 21.6, 25.2, 29.4, 34.4, 40.3, 47.4,
]
N_DEPTHS = len(DEPTH_LEVELS)

# Thermocline depth indices (5-25m, index 4–13) for loss weighting
THERMOCLINE_IDX = list(range(4, 14))

# ── Unified grid (target: BGC 0.25° resolution) ──────
GRID_RES = 0.25  # degrees
GRID_LAT = [round(REGION["lat_min"] + i * GRID_RES, 4) for i in range(
    int((REGION["lat_max"] - REGION["lat_min"]) / GRID_RES) + 1)]
GRID_LON = [round(REGION["lon_min"] + j * GRID_RES, 4) for j in range(
    int((REGION["lon_max"] - REGION["lon_min"]) / GRID_RES) + 1)]
GRID_H = len(GRID_LAT)  # ~23
GRID_W = len(GRID_LON)  # ~17

# ── Forecast parameters ───────────────────────────────
INPUT_DAYS = 60
FORECAST_HORIZONS = [1, 3, 5, 7]  # days

# ── CMEMS products ────────────────────────────────────
PHY_MY_DATASET = "cmems_mod_glo_phy_my_0.083deg_P1D-m"
BGC_MY_DATASET = "cmems_mod_glo_bgc_my_0.25deg_P1D-m"
PHY_NRT_DATASET = "cmems_mod_glo_phy_nrt_0.083deg_P1D-m"
BGC_NRT_DATASET = "cmems_mod_glo_bgc_nrt_0.25deg_P1D-m"
PHY_MY_MONTHLY = "cmems_mod_glo_phy_my_0.083deg_P1M-m"

# ── Data paths ────────────────────────────────────────
DATA_ROOT = os.environ.get(
    "MODEL_DATA_DIR",
    "D:/PyCharm/PyCharm Community Edition 2024.3.4/oceanData/downloads",
)
MY_PHY_NC = os.path.join(DATA_ROOT, "phy_raw.nc")
MY_BGC_NC = os.path.join(DATA_ROOT, "bgc_raw.nc")
MY_SURFACE_NC = os.path.join(DATA_ROOT, "my_surface.nc")

# Project directories
PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(PROJECT_DIR, "data")
MODEL_DIR = os.path.join(PROJECT_DIR, "models")

# ── Training ──────────────────────────────────────────
DEVICE = "cuda"
BATCH_SIZE_RECON = 1024    # MLP is small, large batch possible
BATCH_SIZE_FORECAST = 64   # LSTM needs smaller batch
GRADIENT_ACCUMULATION = 2
LEARNING_RATE = 1e-3       # higher for MLP
FORECAST_LR = 1e-4         # lower for LSTM
MIXED_PRECISION = True
SEED = 42

# Time-based split ratios
TRAIN_RATIO = 0.8
VAL_RATIO = 0.1
TEST_RATIO = 0.1

# CHL log transform
CHL_LOG10 = True

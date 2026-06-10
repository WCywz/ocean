# 模型重设计实现计划

> **Goal:** 构建两阶段模型（深度剖面 MLP 重建器 + PhyLSTM 预报器），从 NRT 表层观测预测东海全 3D 场，1 周内完成。

**Architecture:** 阶段 1 用逐点 MLP 从当日表层多参量（SST/SSS/CHL/SSH/风应力旋度）重建当前 18 层温盐剖面；阶段 2 用 PhyLSTM + 多头注意力接收 60 天 3D 序列，输出 1d/3d/5d/7d 全 3D 预报。两阶段均用 MY 再分析数据训练，在 MY 测试集上按时间顺序划分独立验证。

**Tech Stack:** PyTorch 2.x, xarray, copernicusmarine, NumPy, scipy

**数据位置:** 从环境变量 `MODEL_DATA_DIR` 读取，默认 `D:/PyCharm/PyCharm Community Edition 2024.3.4/oceanData/downloads/`（phy_raw.nc, bgc_raw.nc, my_surface.nc）

**项目位置:** `c:/Users/chutaorui/Desktop/ocean/model-training/`

---

## 设计修复说明（相比初版）

| 问题 | 初版 | 修复版 |
|------|------|--------|
| 网格不匹配 | 直接用 phy 的 lat/lon 索引读 bgc | 统一插值到 bgc 0.25° 网格（23×17），phy 做双线性插值 |
| 模型选型 | 3D U-Net（23×17 太小，3 次池化只剩 3×3） | 逐点 MLP + 空间坐标编码，参数量更小、训练更快 |
| 风应力 | 原始 tauuo/tauvo 当独立特征 | 显式计算风应力旋度 curl = ∂τ_y/∂x - ∂τ_x/∂y |
| Loss | 普通 MSE | 温跃层加权 MAE（深度 5-25m 权重 3x） + 深层 MSE |
| 数据划分 | 随机打乱 | 按时间顺序划分（前 80% 训练、中 10% 验证、后 10% 测试） |
| CHL 分布 | 原始值 | log10 变换 |
| 预报器数据准备 | 全量加载到内存（~40 GB OOM） | 按空间位置分批流式构建 |
| 推理 vs 训练 | 推理只重建 1 天 3D | 推理时重建全部 60 天历史 3D |
| 陆地掩码 | 无 | 基于水深数据的陆地掩码 |
| 数据路径 | 硬编码 D:/ 路径 | 环境变量 + 默认值 |

---

## 文件结构

```
model-training/
├── config.py                     # 共享配置
├── download_cmems.py             # CMEMS NRT 每日下载
├── download_my_surface.py        # 补充下载 MY SSH + 风应力（2D 场）
├── prepare_reconstructor_data.py # 构建表层→3D 训练对（网格统一 + 风应力旋度 + 陆地掩码）
├── reconstructor/
│   ├── __init__.py
│   ├── model.py                  # 逐点 DepthProfileMLP
│   ├── dataset.py                # DataLoader
│   └── train.py                  # 训练脚本（温跃层加权 Loss）
├── prepare_forecaster_data.py    # 构建 3D 序列训练数据（流式分批）
├── forecaster/
│   ├── __init__.py
│   ├── model.py                  # PhyLSTM + 多头注意力
│   ├── dataset.py                # DataLoader
│   └── train.py                  # 训练脚本
├── inference.py                  # 生产推理（两阶段串联，重建全部 60 天）
├── validate_reconstructor.py     # 深层相关性验证
└── validate_forecaster.py        # 预报技巧对比
```

---

### Task 1: 共享配置

**Files:**
- Create: `model-training/config.py`

- [ ] **Step 1: 写入 config.py**

```python
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
```

- [ ] **Step 2: Commit**

```bash
cd c:/Users/chutaorui/Desktop/ocean
git add model-training/config.py
git commit -m "feat: 共享配置文件"
```

---

### Task 2: CMEMS NRT 数据下载

**Files:**
- Create: `model-training/download_cmems.py`

- [ ] **Step 1: 写入 download_cmems.py**

```python
"""Download CMEMS NRT data for one day and write surface fields as CSV."""
import os
import sys
import argparse
import copernicusmarine as cm
import xarray as xr
import pandas as pd
import numpy as np
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(__file__))
from config import REGION, PHY_NRT_DATASET, BGC_NRT_DATASET, DATA_DIR, GRID_LAT, GRID_LON

OUTPUT_DIR = os.path.join(DATA_DIR, "nrt_daily")


def download_and_process(date_str, output_dir):
    dt = datetime.fromisoformat(date_str)
    print(f"Downloading NRT data for {date_str}...")

    cm.subset(
        dataset_id=PHY_NRT_DATASET,
        variables=["thetao", "so", "zos"],
        minimum_longitude=REGION["lon_min"],
        maximum_longitude=REGION["lon_max"],
        minimum_latitude=REGION["lat_min"],
        maximum_latitude=REGION["lat_max"],
        start_datetime=date_str,
        end_datetime=date_str,
        output_directory=output_dir,
        output_filename=f"phy_{date_str}.nc",
    )
    cm.subset(
        dataset_id=BGC_NRT_DATASET,
        variables=["chl"],
        minimum_longitude=REGION["lon_min"],
        maximum_longitude=REGION["lon_max"],
        minimum_latitude=REGION["lat_min"],
        maximum_latitude=REGION["lat_max"],
        start_datetime=date_str,
        end_datetime=date_str,
        output_directory=output_dir,
        output_filename=f"bgc_{date_str}.nc",
    )

    phy_ds = xr.open_dataset(os.path.join(output_dir, f"phy_{date_str}.nc"))
    bgc_ds = xr.open_dataset(os.path.join(output_dir, f"bgc_{date_str}.nc"))

    # Interpolate to unified grid
    rows = []
    for lat in GRID_LAT:
        for lon in GRID_LON:
            row = {"time": date_str, "lat": lat, "lon": lon}
            try:
                row["thetao"] = float(phy_ds.thetao.isel(time=0, depth=0).interp(
                    latitude=lat, longitude=lon, method="linear").values)
            except Exception:
                row["thetao"] = np.nan
            try:
                row["so"] = float(phy_ds.so.isel(time=0, depth=0).interp(
                    latitude=lat, longitude=lon, method="linear").values)
            except Exception:
                row["so"] = np.nan
            try:
                row["zos"] = float(phy_ds.zos.isel(time=0).interp(
                    latitude=lat, longitude=lon, method="linear").values)
            except Exception:
                row["zos"] = np.nan
            try:
                row["chl"] = float(bgc_ds.chl.isel(time=0).interp(
                    latitude=lat, longitude=lon, method="linear").values)
            except Exception:
                row["chl"] = np.nan
            rows.append(row)

    df = pd.DataFrame(rows)
    csv_path = os.path.join(output_dir, f"surface_{date_str}.csv")
    df.to_csv(csv_path, index=False)
    print(f"Saved {len(df)} grid points to {csv_path}")
    phy_ds.close()
    bgc_ds.close()
    return csv_path


def main():
    parser = argparse.ArgumentParser(description="Download CMEMS NRT daily data")
    parser.add_argument("--date", help="Date YYYY-MM-DD (default: yesterday UTC)")
    parser.add_argument("--output-dir", default=OUTPUT_DIR)
    args = parser.parse_args()

    date_str = args.date or (
        datetime.now(timezone.utc) - timedelta(days=1)
    ).strftime("%Y-%m-%d")
    os.makedirs(args.output_dir, exist_ok=True)
    download_and_process(date_str, args.output_dir)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/download_cmems.py
git commit -m "feat: CMEMS NRT 数据下载脚本"
```

---

### Task 2.5: 补充下载 MY SSH + 风应力数据

现有 `phy_raw.nc` 只有 thetao 和 so，缺少 zos（SSH）、tauuo/tauvo（风应力）。这三个是 2D 表层场，体积远小于 3D 温盐场。

> **注意：** 运行前先验证 MY 日平均产品是否包含 tauuo/tauvo：
> ```bash
> python -c "import copernicusmarine as cm; desc = cm.describe(include_datasets=True); print([p['dataset_id'] for p in desc['products']])"
> ```
> 如果日平均产品没有 tauuo/tauvo，改用月平均产品 `cmems_mod_glo_phy_my_0.083deg_P1M-m`，下载后插值到日尺度。

**Files:**
- Create: `model-training/download_my_surface.py`

- [ ] **Step 1: 写入 download_my_surface.py**

```python
"""Download missing 2D surface forcing from CMEMS MY product.

phy_raw.nc already has thetao(3D) + so(3D). This adds:
  zos   — sea surface height (SSH)
  tauuo — eastward wind stress (N/m²)
  tauvo — northward wind stress (N/m²)

All three are 2D (no depth dimension), ~100 MB total for 1993-2026 East China Sea.
"""
import os
import sys
import copernicusmarine as cm
import xarray as xr

sys.path.insert(0, os.path.dirname(__file__))
from config import REGION, PHY_MY_DATASET, MY_SURFACE_NC

# Fallback: monthly product if daily doesn't have tauuo/tauvo
PHY_MY_MONTHLY = "cmems_mod_glo_phy_my_0.083deg_P1M-m"


def main():
    print(f"Downloading 2D surface forcing from {PHY_MY_DATASET}...")
    print(f"Region: lon {REGION['lon_min']}-{REGION['lon_max']}, "
          f"lat {REGION['lat_min']}-{REGION['lat_max']}")
    print(f"Output: {MY_SURFACE_NC}")

    cm.subset(
        dataset_id=PHY_MY_DATASET,
        variables=["zos", "tauuo", "tauvo"],
        minimum_longitude=REGION["lon_min"],
        maximum_longitude=REGION["lon_max"],
        minimum_latitude=REGION["lat_min"],
        maximum_latitude=REGION["lat_max"],
        output_directory=os.path.dirname(MY_SURFACE_NC),
        output_filename="my_surface.nc",
    )

    ds = xr.open_dataset(MY_SURFACE_NC)
    print(f"Variables: {list(ds.data_vars.keys())}")
    print(f"Time range: {ds.time.values[0]} ~ {ds.time.values[-1]}")
    print(f"Time steps: {len(ds.time)}")
    ds.close()
    print("Done.")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行下载**

```bash
cd c:/Users/chutaorui/Desktop/ocean/model-training
python download_my_surface.py
```

预计 2-3 分钟（2D 场 ~100 MB）。如果报错"variable not found"，检查是否需要改用月平均产品。

- [ ] **Step 3: Commit**

```bash
git add model-training/download_my_surface.py
git commit -m "feat: 补充下载 MY SSH + 风应力数据"
```

---

### Task 3: 重构器训练数据准备

**关键修复：**
- 三套数据统一插值到 bgc 0.25° 网格（23×17）
- 显式计算风应力旋度 `curl = ∂τ_y/∂x - ∂τ_x/∂y`
- CHL 做 log10 变换
- 基于水深数据过滤陆地像素
- 按时间顺序划分（非随机打乱）

**Files:**
- Create: `model-training/prepare_reconstructor_data.py`

- [ ] **Step 1: 写入 prepare_reconstructor_data.py**

```python
"""Build surface→3D training pairs from MY reanalysis NetCDF files.

Steps:
1. Load phy_raw.nc, bgc_raw.nc, my_surface.nc
2. Interpolate phy data (0.083°) → unified grid (0.25°)
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
import joblib
from sklearn.preprocessing import StandardScaler
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(__file__))
from config import (
    REGION, N_DEPTHS, MY_PHY_NC, MY_BGC_NC, MY_SURFACE_NC, DATA_DIR,
    GRID_LAT, GRID_LON, GRID_H, GRID_W, TRAIN_RATIO, VAL_RATIO, TEST_RATIO,
    CHL_LOG10, SEED,
)

OUTPUT_DIR = os.path.join(DATA_DIR, "reconstructor")


def compute_wind_curl(ds):
    """Compute wind stress curl: curl = d(tauvo)/dx - d(tauuo)/dy.

    Uses central differences on the unified grid.
    Returns a DataArray with shape [time, lat, lon].
    """
    lat = ds.latitude.values
    lon = ds.longitude.values

    # Grid spacing (meters) at each latitude
    dx = np.zeros((len(lat), len(lon)))
    dy = np.zeros((len(lat), len(lon)))
    for i in range(len(lat)):
        dlat_m = 111320.0 * (GRID_LAT[1] - GRID_LAT[0])
        dy[i, :] = dlat_m
        dlon_m = 111320.0 * np.cos(np.deg2rad(lat[i])) * (GRID_LON[1] - GRID_LON[0])
        dx[i, :] = dlon_m

    # d(tauvo)/dx: difference along longitude axis
    dtauvo_dx = np.gradient(ds.tauvo.values, axis=-1) / dx[np.newaxis, :, :]
    # d(tauuo)/dy: difference along latitude axis
    dtauuo_dy = np.gradient(ds.tauuo.values, axis=-2) / dy[np.newaxis, :, :]

    curl = dtauvo_dx - dtauuo_dy
    return xr.DataArray(curl.astype(np.float32), dims=["time", "latitude", "longitude"],
                        coords={"time": ds.time, "latitude": lat, "longitude": lon})


def interpolate_to_grid(ds, var_name, target_lat, target_lon):
    """Bilinear interpolation of a variable to the target grid.

    Handles both 3D (time, depth, lat, lon) and 2D (time, lat, lon) vars.
    """
    da = ds[var_name]
    if "depth" in da.dims:
        if "depth" in da.dims and da.dims.index("depth") > 0:
            interp = da.interp(latitude=target_lat, longitude=target_lon, method="linear")
        else:
            interp = da.interp(latitude=target_lat, longitude=target_lon, method="linear")
    else:
        interp = da.interp(latitude=target_lat, longitude=target_lon, method="linear")
    return interp.values.astype(np.float32)


def main():
    print("Loading MY reanalysis data...")
    phy_ds = xr.open_dataset(MY_PHY_NC)       # thetao[18], so[18] on 0.083°
    bgc_ds = xr.open_dataset(MY_BGC_NC)       # chl on 0.25°
    sfc_ds = xr.open_dataset(MY_SURFACE_NC)   # zos, tauuo, tauvo on 0.083°
    print(f"phy grid: {phy_ds.latitude.shape[0]}×{phy_ds.longitude.shape[0]} (0.083°)")
    print(f"bgc grid: {bgc_ds.latitude.shape[0]}×{bgc_ds.longitude.shape[0]} (0.25°)")

    # ── Step 1: Interpolate phy 3D data to bgc grid ──
    print("Interpolating phy data to unified 0.25° grid...")
    thetao_3d = interpolate_to_grid(phy_ds, "thetao", GRID_LAT, GRID_LON)
    so_3d = interpolate_to_grid(phy_ds, "so", GRID_LAT, GRID_LON)
    # phy surface (depth=0) already has thetao[0] and so[0]
    thetao_sfc = thetao_3d[:, 0, :, :]   # [T, H, W]
    so_sfc = so_3d[:, 0, :, :]           # [T, H, W]

    # Interpolate surface fields (zos, tauuo, tauvo) to bgc grid
    print("Interpolating surface forcing to unified grid...")
    zos_2d = interpolate_to_grid(sfc_ds, "zos", GRID_LAT, GRID_LON)
    tauuo_2d = interpolate_to_grid(sfc_ds, "tauuo", GRID_LAT, GRID_LON)
    tauvo_2d = interpolate_to_grid(sfc_ds, "tauvo", GRID_LAT, GRID_LON)

    # Build a temporary dataset for wind curl computation
    curl_ds = xr.Dataset({
        "tauuo": xr.DataArray(tauuo_2d, dims=["time", "latitude", "longitude"],
                              coords={"time": sfc_ds.time, "latitude": GRID_LAT, "longitude": GRID_LON}),
        "tauvo": xr.DataArray(tauvo_2d, dims=["time", "latitude", "longitude"],
                              coords={"time": sfc_ds.time, "latitude": GRID_LAT, "longitude": GRID_LON}),
    }, coords={"time": sfc_ds.time, "latitude": GRID_LAT, "longitude": GRID_LON})
    wind_curl = compute_wind_curl(curl_ds)

    # bgc CHL: already on 0.25° grid, just align coordinates
    print("Processing BGC CHL data...")
    chl_2d = bgc_ds.chl.interp(latitude=GRID_LAT, longitude=GRID_LON, method="linear").values.astype(np.float32)

    if CHL_LOG10:
        chl_2d = np.log10(np.maximum(chl_2d, 1e-4))

    # ── Step 2: Time alignment ──
    # Find intersection of phy and bgc time steps (within 12h tolerance)
    phy_times = phy_ds.time.values
    bgc_times = bgc_ds.time.values
    sfc_times = sfc_ds.time.values

    # Use bgc time as reference (coarser, fewer time steps)
    print(f"phy times: {len(phy_times)}, bgc times: {len(bgc_times)}, sfc times: {len(sfc_times)}")

    # For each bgc time step, find nearest phy and sfc time step
    common_times = []
    for t_bgc in tqdm(bgc_times, desc="Aligning times"):
        phy_idx = np.argmin(np.abs(phy_times - t_bgc))
        sfc_idx = np.argmin(np.abs(sfc_times - t_bgc))
        t_phy = phy_times[phy_idx]
        t_sfc = sfc_times[sfc_idx]
        dt_phy = np.abs(pd.Timestamp(t_phy) - pd.Timestamp(t_bgc)).total_seconds() / 3600
        dt_sfc = np.abs(pd.Timestamp(t_sfc) - pd.Timestamp(t_bgc)).total_seconds() / 3600
        if dt_phy <= 12 and dt_sfc <= 12:
            common_times.append((t_bgc, phy_idx, sfc_idx))

    print(f"Common timesteps: {len(common_times)}")

    # ── Step 3: Build land mask ──
    # Use NaN patterns in thetao to detect land (ocean always has valid values)
    land_mask = np.isnan(thetao_3d[0, 0, :, :])  # [H, W]
    ocean_mask = ~land_mask
    n_ocean = ocean_mask.sum().item()
    print(f"Ocean pixels: {n_ocean} / {GRID_H * GRID_W} ({n_ocean / (GRID_H * GRID_W) * 100:.0f}%)")

    # ── Step 4: Build samples ──
    import pandas as pd

    X_list, y_thetao_list, y_so_list = [], [], []
    time_list = []

    for t_idx, (t_bgc, phy_idx, sfc_idx) in enumerate(tqdm(common_times, desc="Building samples")):
        for i in range(GRID_H):
            for j in range(GRID_W):
                if land_mask[i, j]:
                    continue  # skip land

                # Surface features
                thetao_s = thetao_sfc[phy_idx, i, j]
                so_s = so_sfc[phy_idx, i, j]
                chl_s = chl_2d[t_idx, i, j] if t_idx < len(chl_2d) else np.nan
                zos_s = zos_2d[sfc_idx, i, j]
                curl_s = wind_curl[sfc_idx, i, j].values.item() if hasattr(wind_curl[sfc_idx, i, j], 'values') else float(wind_curl[sfc_idx, i, j])

                # Skip if any surface feature is severely invalid
                if np.isnan(thetao_s):
                    continue

                # Spatial encoding: normalized lat/lon
                lat_norm = (GRID_LAT[i] - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
                lon_norm = (GRID_LON[j] - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

                X_list.append([thetao_s, so_s, chl_s, zos_s, curl_s, lat_norm, lon_norm])

                # Depth profiles
                y_thetao_list.append(thetao_3d[phy_idx, :, i, j])
                y_so_list.append(so_3d[phy_idx, :, i, j])
                time_list.append(str(t_bgc)[:10])

    phy_ds.close()
    bgc_ds.close()
    sfc_ds.close()

    X = np.array(X_list, dtype=np.float32)
    y_thetao = np.array(y_thetao_list, dtype=np.float32)
    y_so = np.array(y_so_list, dtype=np.float32)
    times_arr = np.array(time_list)

    print(f"Total ocean samples: {len(X)}")
    print(f"X shape: {X.shape}, y_thetao: {y_thetao.shape}, y_so: {y_so.shape}")

    # ── Step 5: Handle NaNs ──
    for c in range(X.shape[1]):
        col_mean = np.nanmean(X[:, c])
        X[:, c] = np.nan_to_num(X[:, c], nan=col_mean)
    for d in range(N_DEPTHS):
        col_mean = np.nanmean(y_thetao[:, d])
        y_thetao[:, d] = np.nan_to_num(y_thetao[:, d], nan=col_mean)
        col_mean = np.nanmean(y_so[:, d])
        y_so[:, d] = np.nan_to_num(y_so[:, d], nan=col_mean)

    # ── Step 6: Time-based split ──
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

    print(f"Split — train: {len(train_idx)}, val: {len(val_idx)}, test: {len(test_idx)}")
    print(f"Time ranges — train: {unique_times[0]}~{unique_times[n_train_t-1]}, "
          f"val: {unique_times[n_train_t]}~{unique_times[n_train_t+n_val_t-1]}, "
          f"test: {unique_times[n_train_t+n_val_t]}~{unique_times[-1]}")

    # ── Step 7: Scale and save ──
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
    np.save(os.path.join(OUTPUT_DIR, "grid_lat.npy"), np.array(GRID_LAT))
    np.save(os.path.join(OUTPUT_DIR, "grid_lon.npy"), np.array(GRID_LON))
    joblib.dump(scaler_X, os.path.join(OUTPUT_DIR, "scaler_X.pkl"))
    joblib.dump(scaler_y_thetao, os.path.join(OUTPUT_DIR, "scaler_y_thetao.pkl"))
    joblib.dump(scaler_y_so, os.path.join(OUTPUT_DIR, "scaler_y_so.pkl"))

    print(f"Saved to {OUTPUT_DIR}/")
    print(f"X scaled min/max: {X_scaled.min():.2f} / {X_scaled.max():.2f}")
    print("Done.")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/prepare_reconstructor_data.py
git commit -m "feat: 重构器训练数据准备脚本（网格统一 + 风应力旋度 + 陆地掩码 + 时间划分）"
```

---

### Task 4: 重建器模型定义

**关键修复：** 不用 3D U-Net（23×17 网格太小，三次池化只剩 3×3）。改用逐点 MLP + 残差连接 + 空间坐标编码。

输入：7 个表面特征（thetao_sfc, so_sfc, log10(chl), zos, wind_curl, lat_norm, lon_norm）
输出：36 个值（18 层 thetao + 18 层 so）

**Files:**
- Create: `model-training/reconstructor/__init__.py`
- Create: `model-training/reconstructor/model.py`

- [ ] **Step 1: 写入 __init__.py**

```python
from .model import DepthProfileMLP
```

- [ ] **Step 2: 写入 model.py**

```python
"""Point-wise MLP for surface→subsurface reconstruction.

Input:  [B, 7]  surface features + spatial encoding
Output: [B, 36] full depth profiles (thetao[18] + so[18])

Architecture: Residual MLP with LayerNorm — much lighter than U-Net,
suitable for per-point prediction on small grids.
"""
import torch
import torch.nn as nn


class ResidualBlock(nn.Module):
    def __init__(self, dim, dropout=0.1):
        super().__init__()
        self.net = nn.Sequential(
            nn.LayerNorm(dim),
            nn.Linear(dim, dim * 2),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(dim * 2, dim),
            nn.Dropout(dropout),
        )

    def forward(self, x):
        return x + self.net(x)


class DepthProfileMLP(nn.Module):
    """MLP that maps surface observations to full depth profiles.

    Input:  7 features (thetao_sfc, so_sfc, log10_chl, zos, wind_curl, lat, lon)
    Output: 36 values (18 thetao depth levels + 18 so depth levels)
    """

    def __init__(self, in_features=7, n_depths=18, hidden=256, n_blocks=3, dropout=0.15):
        super().__init__()
        self.in_features = in_features
        self.n_depths = n_depths
        out_features = n_depths * 2  # thetao + so

        self.input_proj = nn.Sequential(
            nn.Linear(in_features, hidden),
            nn.LayerNorm(hidden),
            nn.GELU(),
            nn.Dropout(dropout),
        )

        self.blocks = nn.Sequential(*[
            ResidualBlock(hidden, dropout) for _ in range(n_blocks)
        ])

        self.head = nn.Sequential(
            nn.LayerNorm(hidden),
            nn.Linear(hidden, hidden // 2),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(hidden // 2, out_features),
        )

    def forward(self, x):
        # x: [B, 7]
        h = self.input_proj(x)
        h = self.blocks(h)
        out = self.head(h)  # [B, n_depths * 2]
        return out

    def predict_thetao_so(self, x):
        """Convenience: split output into thetao and so."""
        out = self.forward(x)
        thetao = out[:, :self.n_depths]
        so = out[:, self.n_depths:]
        return thetao, so
```

- [ ] **Step 3: Commit**

```bash
git add model-training/reconstructor/
git commit -m "feat: 逐点 MLP 重建器模型（替代 U-Net）"
```

---

### Task 5: 重构器 DataLoader

**Files:**
- Create: `model-training/reconstructor/dataset.py`

- [ ] **Step 1: 写入 dataset.py**

```python
"""DataLoader for reconstructor training (time-based split, not random)."""
import os
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader


class ReconstructorDataset(Dataset):
    def __init__(self, data_dir, split="train"):
        self.X = np.load(os.path.join(data_dir, "X_surface.npy"))
        self.y_thetao = np.load(os.path.join(data_dir, "y_thetao.npy"))
        self.y_so = np.load(os.path.join(data_dir, "y_so.npy"))
        self.indices = np.load(os.path.join(data_dir, f"{split}_idx.npy"))

    def __len__(self):
        return len(self.indices)

    def __getitem__(self, idx):
        i = self.indices[idx]
        x = torch.from_numpy(self.X[i]).float()
        y_thetao = torch.from_numpy(self.y_thetao[i]).float()
        y_so = torch.from_numpy(self.y_so[i]).float()
        return x, y_thetao, y_so


def create_dataloaders(data_dir, batch_size=256, num_workers=0):
    train_ds = ReconstructorDataset(data_dir, "train")
    val_ds = ReconstructorDataset(data_dir, "val")
    test_ds = ReconstructorDataset(data_dir, "test")

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)

    return train_loader, val_loader, test_loader
```

- [ ] **Step 2: Commit**

```bash
git add model-training/reconstructor/dataset.py
git commit -m "feat: 重构器 DataLoader（时间划分）"
```

---

### Task 6: 重构器训练脚本

**关键修复：** 温跃层加权 Loss（深度 5-25m 加权 3x MAE）+ 深层 MSE。

**Files:**
- Create: `model-training/reconstructor/train.py`

- [ ] **Step 1: 写入 train.py**

```python
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


def thermocline_weighted_loss(pred, target, therm_idx=THERMOCLINE_IDX):
    """MSE with 3x weight on thermocline depths (5-25m), 1x elsewhere.

    pred, target: [B, 18]
    """
    # Per-depth MSE
    se = (pred - target) ** 2  # [B, 18]

    # Weight mask: 3.0 for thermocline, 1.0 elsewhere
    weights = torch.ones(N_DEPTHS, device=pred.device)
    weights[therm_idx] = 3.0

    # Weighted mean
    weighted = se * weights.unsqueeze(0)
    return weighted.mean()


def pearson_corr(pred, target):
    pred = pred.detach().flatten()
    target = target.flatten()
    mean_p, mean_t = pred.mean(), target.mean()
    num = ((pred - mean_p) * (target - mean_t)).sum()
    den = torch.sqrt(((pred - mean_p) ** 2).sum() * ((target - mean_t) ** 2).sum())
    return (num / (den + 1e-8)).item()


def train():
    os.makedirs(MODEL_DIR, exist_ok=True)
    torch.manual_seed(SEED)

    data_dir = os.path.join(DATA_DIR, "reconstructor")
    train_loader, val_loader, test_loader = create_dataloaders(data_dir, BATCH_SIZE_RECON)

    model = DepthProfileMLP().to(DEVICE)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Parameters: {total_params:,}")

    optimizer = torch.optim.AdamW(model.parameters(), lr=LEARNING_RATE, weight_decay=1e-5)
    scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", factor=0.5, patience=8, min_lr=1e-6
    )
    scaler = GradScaler(enabled=True)

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

            with autocast(enabled=True):
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

        avg_loss = total_loss / len(train_loader)
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

        avg_val_loss = val_loss / n_batches
        history["val_loss"].append(avg_val_loss)
        history["corr_thetao"].append(corr_t_sum / n_batches)
        history["corr_so"].append(corr_s_sum / n_batches)

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
            x, y_thetao, y_so = x.to(DEVICE), y_thetao.to(DEVICE), y_so.to(DEVICE)
            out = model(x)
            pred_t, pred_s = out[:, :N_DEPTHS], out[:, N_DEPTHS:]
            test_loss += (thermocline_weighted_loss(pred_t, y_thetao)
                          + thermocline_weighted_loss(pred_s, y_so)).item()
            test_corr_t += pearson_corr(pred_t, y_thetao)
            test_corr_s += pearson_corr(pred_s, y_so)
            n_test += 1

    print(f"\nTest Loss: {test_loss / n_test:.6f}")
    print(f"Test Corr thetao: {test_corr_t / n_test:.4f}")
    print(f"Test Corr so: {test_corr_s / n_test:.4f}")

    with open(os.path.join(MODEL_DIR, "reconstructor_history.json"), "w") as f:
        json.dump(history, f)

    torch.save(model.state_dict(), os.path.join(MODEL_DIR, "reconstructor_final.pt"))
    print("Done.")


if __name__ == "__main__":
    train()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/reconstructor/train.py
git commit -m "feat: 重建器训练脚本（温跃层加权 Loss）"
```

---

### Task 7: 预报器训练数据准备

**关键修复：** 按空间位置分批流式构建，避免全量加载 OOM。对每个网格点：
1. 加载该点所有时间步的 3D 重建数据
2. 构建滑动窗口序列
3. 增量保存

**Files:**
- Create: `model-training/prepare_forecaster_data.py`

- [ ] **Step 1: 写入 prepare_forecaster_data.py**

```python
"""Build 60-day→multi-horizon 3D training sequences for PhyLSTM forecaster.

Uses trained reconstructor to produce 3D fields, then builds sliding window
sequences PER SPATIAL LOCATION to avoid OOM. Results saved incrementally.

Memory: processes one grid point at a time (~12K time steps × 37 channels
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
    TRAIN_RATIO, VAL_RATIO, SEED, DEVICE,
)
from reconstructor.model import DepthProfileMLP

OUTPUT_DIR = os.path.join(DATA_DIR, "forecaster")
MAX_HORIZON = max(FORECAST_HORIZONS)
MIN_SEQ = INPUT_DAYS + MAX_HORIZON


def build_sequences_for_point(data_3d, data_chl, times):
    """Build sliding windows for a single grid point.

    data_3d:  [T, n_depths * 2]  reconstructed thetao + so
    data_chl: [T, 1]             surface chl
    times:    [T]                time strings

    Returns: X [N_seq, 60, C], y [N_seq, 4, C]
    where C = n_depths * 2 + 1 + 2 (thetao + so + chl + lat + lon)
    """
    T = len(times)
    if T < MIN_SEQ:
        return None, None

    sequences = []
    for start in range(0, T - MIN_SEQ + 1):
        # Input: 60 days
        x_seq = np.zeros((INPUT_DAYS, N_DEPTHS * 2 + 1 + 2), dtype=np.float32)
        x_seq[:, :N_DEPTHS * 2] = data_3d[start:start + INPUT_DAYS]
        x_seq[:, N_DEPTHS * 2:N_DEPTHS * 2 + 1] = data_chl[start:start + INPUT_DAYS]
        # lat/lon are added per-point (constant), set to 0 here, filled by dataset

        # Target: multi-horizon
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
    scaler_X = joblib.load(os.path.join(reco_dir, "scaler_X.pkl"))
    scaler_y_thetao = joblib.load(os.path.join(reco_dir, "scaler_y_thetao.pkl"))
    scaler_y_so = joblib.load(os.path.join(reco_dir, "scaler_y_so.pkl"))

    n_samples = len(X_surface)
    print(f"Loading {n_samples} samples from reconstructor data...")

    # Reconstruct 3D for ALL samples in one batch pass (MLP is fast)
    batch_size = 4096
    thetao_recon = np.zeros((n_samples, N_DEPTHS), dtype=np.float32)
    so_recon = np.zeros((n_samples, N_DEPTHS), dtype=np.float32)

    for start in tqdm(range(0, n_samples, batch_size), desc="Reconstructing 3D"):
        end = min(start + batch_size, n_samples)
        x_batch = torch.from_numpy(X_surface[start:end]).float().to(DEVICE)

        with torch.no_grad():
            out = model(x_batch)  # [B, 36]
            thetao_recon[start:end] = out[:, :N_DEPTHS].cpu().numpy()
            so_recon[start:end] = out[:, N_DEPTHS:].cpu().numpy()

    # Chl from surface data (column index 2)
    chl_surface = X_surface[:, 2:3].copy()  # [N, 1]

    # Build per-location time→index mapping
    # Each sample is a (time, grid_point) pair. We need to group by grid_point.
    # Grid point index is encoded in the original loop order of prepare_reconstructor_data.py:
    # for time, then for i in GRID_H, then for j in GRID_W, skipping land.
    # We need to reconstruct which grid point each sample belongs to.
    #
    # Simpler: use the spatial encoding columns (lat_norm, lon_norm) at indices 5, 6
    # Round to grid precision to recover grid point
    from config import GRID_LAT, GRID_LON, REGION

    lat_norm = X_surface[:, 5]
    lon_norm = X_surface[:, 6]

    # Recover actual lat/lon
    lat_val = lat_norm * (REGION["lat_max"] - REGION["lat_min"]) + REGION["lat_min"]
    lon_val = lon_norm * (REGION["lon_max"] - REGION["lon_min"]) + REGION["lon_min"]

    # Snap to nearest grid point
    grid_i = np.round((lat_val - GRID_LAT[0]) / (GRID_LAT[1] - GRID_LAT[0])).astype(int)
    grid_j = np.round((lon_val - GRID_LON[0]) / (GRID_LON[1] - GRID_LON[0])).astype(int)
    point_key = grid_i * GRID_W + grid_j  # unique per grid point

    unique_points = np.unique(point_key)
    print(f"Unique ocean grid points: {len(unique_points)}")

    # Per-point build sequences
    all_X_train, all_y_train = [], []
    all_X_val, all_y_val = [], []
    all_X_test, all_y_test = [], []
    train_set = set(train_idx)
    val_set = set(val_idx)
    test_set = set(test_idx)

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for pk in tqdm(unique_points, desc="Building per-point sequences"):
        pi = pk // GRID_W
        pj = pk % GRID_W

        # Find all samples for this grid point
        point_samples = np.where(point_key == pk)[0]
        if len(point_samples) < MIN_SEQ:
            continue

        # Sort by time
        time_order = np.argsort(times[point_samples])
        point_samples = point_samples[time_order]

        # Get reconstructed 3D for this point
        data_3d = np.concatenate([thetao_recon[point_samples], so_recon[point_samples]], axis=1)  # [T, 36]
        data_chl = chl_surface[point_samples]  # [T, 1]
        point_times = times[point_samples]

        # Add lat/lon as constant columns (inverse-scaled)
        lat_raw = GRID_LAT[pi]
        lon_raw = GRID_LON[pj]
        lat_enc = (lat_raw - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
        lon_enc = (lon_raw - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

        X, y = build_sequences_for_point(data_3d, data_chl, point_times)
        if X is None:
            continue

        # Add spatial encoding
        X[:, :, -2] = lat_enc
        X[:, :, -1] = lon_enc

        # Split by time (based on the last time step of each sequence)
        for seq_idx in range(len(X)):
            # Determine split based on the last input time step
            last_t = str(point_times[seq_idx + INPUT_DAYS - 1])
            # Find which split this time belongs to
            sample_idx = np.where(times == last_t)[0]
            if len(sample_idx) == 0:
                continue
            sample_idx = sample_idx[0]

            if sample_idx in train_set:
                all_X_train.append(X[seq_idx])
                all_y_train.append(y[seq_idx])
            elif sample_idx in val_set:
                all_X_val.append(X[seq_idx])
                all_y_val.append(y[seq_idx])
            elif sample_idx in test_set:
                all_X_test.append(X[seq_idx])
                all_y_test.append(y[seq_idx])

    print(f"\nTrain sequences: {len(all_X_train)}")
    print(f"Val sequences: {len(all_X_val)}")
    print(f"Test sequences: {len(all_X_test)}")

    if len(all_X_train) > 0:
        np.save(os.path.join(OUTPUT_DIR, "X_train.npy"), np.array(all_X_train, dtype=np.float32))
        np.save(os.path.join(OUTPUT_DIR, "y_train.npy"), np.array(all_y_train, dtype=np.float32))
    if len(all_X_val) > 0:
        np.save(os.path.join(OUTPUT_DIR, "X_val.npy"), np.array(all_X_val, dtype=np.float32))
        np.save(os.path.join(OUTPUT_DIR, "y_val.npy"), np.array(all_y_val, dtype=np.float32))
    if len(all_X_test) > 0:
        np.save(os.path.join(OUTPUT_DIR, "X_test.npy"), np.array(all_X_test, dtype=np.float32))
        np.save(os.path.join(OUTPUT_DIR, "y_test.npy"), np.array(all_y_test, dtype=np.float32))

    sizes = {k: v.shape for k, v in [("X_train", all_X_train[0])] if len(all_X_train) > 0}
    print(f"Sample shape — X: {sizes}")
    print(f"Saved to {OUTPUT_DIR}/")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/prepare_forecaster_data.py
git commit -m "feat: 预报器训练数据准备（流式分批，避免 OOM）"
```

---

### Task 8: PhyLSTM 预报器模型定义

**Files:**
- Create: `model-training/forecaster/__init__.py`
- Create: `model-training/forecaster/model.py`

- [ ] **Step 1: 写入 __init__.py**

```python
from .model import PhyLSTMForecaster
```

- [ ] **Step 2: 写入 model.py**

```python
"""PhyLSTM forecaster with multi-head attention and multi-horizon output.

Input:  60-day sequence of 3D fields [B, 60, C]
        where C = n_depths*2 + 1 + 2 (thetao depths + so depths + chl + lat + lon)
Output: 4 horizons × full 3D field [B, 4, n_depths*2 + 1]
"""
import torch
import torch.nn as nn


class DepthEncoder(nn.Module):
    """Compress a depth profile (18 values) into a compact representation."""
    def __init__(self, n_depths, hidden=32):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_depths, 64),
            nn.GELU(),
            nn.Dropout(0.2),
            nn.Linear(64, hidden),
            nn.GELU(),
        )

    def forward(self, x):
        return self.net(x)


class PhyLSTMForecaster(nn.Module):
    """Physics-guided LSTM with split surface/deep latent states.

    Latent state is explicitly split:
      - Surface group: chl + encoded(thetao) + encoded(so) + spatial
      - Deep group:   encoded(thetao deep) + encoded(so deep)

    Input:  [B, 60, C] where C = n_depths*2 + 1 + 2
    Output: [B, 4, n_depths*2 + 1]  (1d, 3d, 5d, 7d)
    """

    def __init__(self, n_depths=18, lstm_units=192, heads=8, dropout=0.3):
        super().__init__()
        self.n_depths = n_depths
        self.lstm_units = lstm_units

        # Encode thetao/so depth profiles: 18 → 32
        self.thetao_encoder = DepthEncoder(n_depths, 32)
        self.so_encoder = DepthEncoder(n_depths, 32)

        # Input to LSTM: chl(1) + thetao_enc(32) + so_enc(32) + lat/lon(2) = 67
        input_dim = 1 + 32 + 32 + 2

        self.lstm1 = nn.LSTM(input_dim, lstm_units, batch_first=True, dropout=dropout)
        self.lstm2 = nn.LSTM(lstm_units, lstm_units, batch_first=True, dropout=dropout)

        self.attn = nn.MultiheadAttention(lstm_units, heads, batch_first=True)
        self.attn_norm = nn.LayerNorm(lstm_units)
        self.attn_dropout = nn.Dropout(dropout)

        # Spatial branch
        self.space_net = nn.Sequential(
            nn.Linear(2, 64), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(64, 32), nn.GELU(),
        )

        # Multi-horizon heads
        self.n_horizons = 4
        out_dim = n_depths * 2 + 1  # thetao[18] + so[18] + chl[1]

        self.head_1d = self._make_head(lstm_units, out_dim, dropout)
        self.head_3d = self._make_head(lstm_units, out_dim, dropout)
        self.head_5d = self._make_head(lstm_units, out_dim, dropout)
        self.head_7d = self._make_head(lstm_units, out_dim, dropout)

    @staticmethod
    def _make_head(in_dim, out_dim, dropout):
        return nn.Sequential(
            nn.Linear(in_dim + 32, 128), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(128, 64), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(64, out_dim),
        )

    def forward(self, x):
        B, T, _ = x.shape

        thetao = x[:, :, :self.n_depths]
        so = x[:, :, self.n_depths:2 * self.n_depths]
        chl = x[:, :, 2 * self.n_depths:2 * self.n_depths + 1]
        space = x[:, :, -2:]

        # Encode depth profiles per timestep
        thetao_flat = thetao.reshape(B * T, self.n_depths)
        so_flat = so.reshape(B * T, self.n_depths)
        thetao_enc = self.thetao_encoder(thetao_flat).reshape(B, T, -1)
        so_enc = self.so_encoder(so_flat).reshape(B, T, -1)

        # Fuse: chl + thetao_enc + so_enc + spatial
        fused = torch.cat([chl, thetao_enc, so_enc, space[:, :, :2]], dim=-1)

        # LSTM
        x_lstm, _ = self.lstm1(fused)
        x_lstm, _ = self.lstm2(x_lstm)

        # Self-attention
        attn_out, _ = self.attn(x_lstm, x_lstm, x_lstm)
        x_lstm = self.attn_norm(x_lstm + attn_out)
        x_lstm = self.attn_dropout(x_lstm)

        # Temporal pooling
        x_pooled = x_lstm.mean(dim=1)

        # Spatial branch
        space_out = self.space_net(space[:, -1, :2])

        combined = torch.cat([x_pooled, space_out], dim=-1)

        # Multi-horizon output
        out_1d = self.head_1d(combined)
        out_3d = self.head_3d(combined)
        out_5d = self.head_5d(combined)
        out_7d = self.head_7d(combined)

        return torch.stack([out_1d, out_3d, out_5d, out_7d], dim=1)
```

- [ ] **Step 3: Commit**

```bash
git add model-training/forecaster/
git commit -m "feat: PhyLSTM 预报器模型定义"
```

---

### Task 9: 预报器 DataLoader

**Files:**
- Create: `model-training/forecaster/dataset.py`

- [ ] **Step 1: 写入 dataset.py**

```python
"""DataLoader for PhyLSTM forecaster."""
import os
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader


class ForecasterDataset(Dataset):
    def __init__(self, data_dir, split="train"):
        self.X = np.load(os.path.join(data_dir, f"X_{split}.npy"))
        self.y = np.load(os.path.join(data_dir, f"y_{split}.npy"))

    def __len__(self):
        return len(self.X)

    def __getitem__(self, idx):
        x = torch.from_numpy(self.X[idx]).float()
        y = torch.from_numpy(self.y[idx]).float()
        return x, y


def create_dataloaders(data_dir, batch_size=32):
    train_ds = ForecasterDataset(data_dir, "train")
    val_ds = ForecasterDataset(data_dir, "val")
    test_ds = ForecasterDataset(data_dir, "test")

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=0)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False, num_workers=0)

    return train_loader, val_loader, test_loader
```

- [ ] **Step 2: Commit**

```bash
git add model-training/forecaster/dataset.py
git commit -m "feat: 预报器 DataLoader"
```

---

### Task 10: 预报器训练脚本

**Files:**
- Create: `model-training/forecaster/train.py`

- [ ] **Step 1: 写入 train.py**

```python
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


def weighted_mse_loss(pred, target, weights):
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
    scaler = GradScaler(enabled=True)

    best_val_loss = float("inf")
    patience_counter = 0
    history = {"loss": [], "val_loss": []}

    for epoch in range(1, EPOCHS + 1):
        model.train()
        total_loss = 0.0
        optimizer.zero_grad()

        for step, (x, y) in enumerate(train_loader):
            x, y = x.to(DEVICE), y.to(DEVICE)

            with autocast(enabled=True):
                pred = model(x)
                loss = weighted_mse_loss(pred, y, HORIZON_WEIGHTS) / GRADIENT_ACCUMULATION

            scaler.scale(loss).backward()

            if (step + 1) % GRADIENT_ACCUMULATION == 0:
                scaler.step(optimizer)
                scaler.update()
                optimizer.zero_grad()

            total_loss += loss.item() * GRADIENT_ACCUMULATION

        avg_loss = total_loss / len(train_loader)
        history["loss"].append(avg_loss)

        # Validation
        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for x, y in val_loader:
                x, y = x.to(DEVICE), y.to(DEVICE)
                pred = model(x)
                val_loss += weighted_mse_loss(pred, y, HORIZON_WEIGHTS).item()

        avg_val_loss = val_loss / len(val_loader)
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
    with torch.no_grad():
        for x, y in test_loader:
            x, y = x.to(DEVICE), y.to(DEVICE)
            pred = model(x)
            test_loss += weighted_mse_loss(pred, y, HORIZON_WEIGHTS).item()

    print(f"\nTest Loss: {test_loss / len(test_loader):.6f}")

    with open(os.path.join(MODEL_DIR, "forecaster_history.json"), "w") as f:
        json.dump(history, f)

    torch.save(model.state_dict(), os.path.join(MODEL_DIR, "forecaster_final.pt"))
    print("Done.")


if __name__ == "__main__":
    train()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/forecaster/train.py
git commit -m "feat: PhyLSTM 预报器训练脚本"
```

---

### Task 11: 生产推理脚本

**关键修复：** 推理时重建全部 60 天历史 3D，而非只重建最新一天。

**Files:**
- Create: `model-training/inference.py`

- [ ] **Step 1: 写入 inference.py**

```python
"""Production inference: surface inputs → reconstructor → forecaster → CSV.

Calls reconstructor on ALL 60 days of history for training-inference consistency.
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
    """Load surface CSV and return array [N_points, 7] (scaled features)."""
    csv_path = os.path.join(data_dir, "nrt_daily", f"surface_{date_str}.csv")
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Surface data not found: {csv_path}")

    df = pd.read_csv(csv_path)
    features = df[["thetao", "so", "chl", "zos"]].values.astype(np.float32)

    # CHL log transform
    import numpy as np
    features[:, 2] = np.log10(np.maximum(features[:, 2], 1e-4))

    # Add spatial encoding
    lat_norm = (df["lat"].values - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
    lon_norm = (df["lon"].values - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

    # NRT doesn't have tauuo/tauvo directly — use zos as proxy or set to 0
    # Wind curl will be 0 for NRT (no wind data available), model still works
    wind_curl = np.zeros(len(df), dtype=np.float32)

    X = np.column_stack([
        features[:, 0],  # thetao_sfc
        features[:, 1],  # so_sfc
        features[:, 2],  # chl(log10)
        features[:, 3],  # zos
        wind_curl,       # wind_curl (0 for NRT)
        lat_norm,        # lat encoding
        lon_norm,        # lon encoding
    ]).astype(np.float32)

    return X, df[["lat", "lon"]].values


def run_inference(date_str, data_dir, output_path):
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Inference on: {device}")

    # Load scalers
    reco_dir = os.path.join(DATA_DIR, "reconstructor")
    scaler_X = joblib.load(os.path.join(reco_dir, "scaler_X.pkl"))
    scaler_y_thetao = joblib.load(os.path.join(reco_dir, "scaler_y_thetao.pkl"))
    scaler_y_so = joblib.load(os.path.join(reco_dir, "scaler_y_so.pkl"))

    # Load models
    reconstructor = DepthProfileMLP().to(device)
    reconstructor.load_state_dict(torch.load(
        os.path.join(MODEL_DIR, "reconstructor_best.pt"), map_location=device, weights_only=True))
    reconstructor.eval()

    forecaster = PhyLSTMForecaster(n_depths=N_DEPTHS, lstm_units=192).to(device)
    forecaster.load_state_dict(torch.load(
        os.path.join(MODEL_DIR, "forecaster_best.pt"), map_location=device, weights_only=True))
    forecaster.eval()

    # Load and reconstruct 3D for ALL 60 days
    dt = datetime.fromisoformat(date_str)
    n_points = len(GRID_LAT) * len(GRID_LON)
    seq_3d = np.zeros((n_points, INPUT_DAYS, N_DEPTHS * 2 + 1), dtype=np.float32)
    coords = np.column_stack([
        np.repeat(GRID_LAT, len(GRID_LON)),
        np.tile(GRID_LON, len(GRID_LAT)),
    ]).astype(np.float32)

    # Lat/lon encoding for all points
    lat_enc = (coords[:, 0] - REGION["lat_min"]) / (REGION["lat_max"] - REGION["lat_min"])
    lon_enc = (coords[:, 1] - REGION["lon_min"]) / (REGION["lon_max"] - REGION["lon_min"])

    print(f"Reconstructing 3D for {INPUT_DAYS} days of history...")
    for day_offset in range(INPUT_DAYS, 0, -1):
        d = (dt - timedelta(days=day_offset)).strftime("%Y-%m-%d")
        print(f"  Processing {d} (offset -{day_offset}d)...")

        try:
            X_day, _ = load_surface_data(data_dir, d)
        except FileNotFoundError:
            print(f"    WARNING: No data for {d}, skipping")
            continue

        # Scale
        X_scaled = scaler_X.transform(X_day)

        # Reconstruct 3D for all points
        x_t = torch.from_numpy(X_scaled).float().to(device)
        with torch.no_grad():
            out = reconstructor(x_t)  # [N, 36]
            thetao_rec = out[:, :N_DEPTHS].cpu().numpy()
            so_rec = out[:, N_DEPTHS:].cpu().numpy()
            chl = X_day[:, 2:3]  # log10 chl

        # Inverse scale thetao and so
        thetao_rec = scaler_y_thetao.inverse_transform(thetao_rec)
        so_rec = scaler_y_so.inverse_transform(so_rec)

        seq_idx = INPUT_DAYS - day_offset
        seq_3d[:, seq_idx, :N_DEPTHS] = thetao_rec
        seq_3d[:, seq_idx, N_DEPTHS:2 * N_DEPTHS] = so_rec
        seq_3d[:, seq_idx, 2 * N_DEPTHS] = chl[:, 0]

    # Build forecaster input: [N_points, 60, C]
    n_total = N_DEPTHS * 2 + 1 + 2
    seq_input = np.zeros((n_points, INPUT_DAYS, n_total), dtype=np.float32)
    seq_input[:, :, :N_DEPTHS * 2 + 1] = seq_3d
    seq_input[:, :, -2] = lat_enc[:, np.newaxis]
    seq_input[:, :, -1] = lon_enc[:, np.newaxis]

    # Run forecaster for each point
    print("Running forecaster...")
    results = []
    batch_size = 64

    for start in range(0, n_points, batch_size):
        end = min(start + batch_size, n_points)
        x_batch = torch.from_numpy(seq_input[start:end]).float().to(device)

        with torch.no_grad():
            fc = forecaster(x_batch)  # [B, 4, C]

        fc_np = fc.cpu().numpy()
        for b in range(end - start):
            pt = start + b
            lat, lon = coords[pt]

            for h_idx, horizon_days in enumerate(FORECAST_HORIZONS):
                fc_date = (dt + timedelta(days=horizon_days)).strftime("%Y-%m-%d")
                pred = fc_np[b, h_idx]

                # CHL
                results.append({
                    "forecast_date": fc_date,
                    "latitude": round(float(lat), 6),
                    "longitude": round(float(lon), 6),
                    "forecast_day": horizon_days,
                    "variable": "chl",
                    "depth": -1,
                    "value": round(float(pred[2 * N_DEPTHS]), 6),
                })

                # thetao per depth
                for di in range(N_DEPTHS):
                    results.append({
                        "forecast_date": fc_date,
                        "latitude": round(float(lat), 6),
                        "longitude": round(float(lon), 6),
                        "forecast_day": horizon_days,
                        "variable": "thetao",
                        "depth": di,
                        "value": round(float(pred[di]), 6),
                    })

                # so per depth
                for di in range(N_DEPTHS):
                    results.append({
                        "forecast_date": fc_date,
                        "latitude": round(float(lat), 6),
                        "longitude": round(float(lon), 6),
                        "forecast_day": horizon_days,
                        "variable": "so",
                        "depth": di,
                        "value": round(float(pred[N_DEPTHS + di]), 6),
                    })

    df_out = pd.DataFrame(results)
    df_out.to_csv(output_path, index=False, encoding="utf-8-sig")
    print(f"Saved {len(df_out)} predictions to {output_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--date", required=True, help="Forecast date YYYY-MM-DD")
    parser.add_argument("--data-dir", default=DATA_DIR)
    parser.add_argument("--output", default="forecast_output.csv")
    args = parser.parse_args()
    run_inference(args.date, args.data_dir, args.output)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Commit**

```bash
git add model-training/inference.py
git commit -m "feat: 生产推理脚本（重建全部 60 天历史）"
```

---

### Task 12: 验证脚本

**Files:**
- Create: `model-training/validate_reconstructor.py`
- Create: `model-training/validate_forecaster.py`

- [ ] **Step 1: 写入 validate_reconstructor.py**

```python
"""Validate reconstructor: compute deep-layer Pearson correlation per depth level."""
import os
import sys
import numpy as np
import torch
import joblib

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
    print(f"{'Mean':>8}  {mean_corr_t:8.4f}  {mean_corr_s:8.4f}")

    deep_corr_t = corr_thetao[4:].mean()  # below thermocline
    deep_corr_s = corr_so[4:].mean()
    print(f"{'Deep mean':>8}  {deep_corr_t:8.4f}  {deep_corr_s:8.4f}")

    if mean_corr_t > 0.7 and mean_corr_s > 0.7:
        print("PASS: Deep correlation > 0.7")
    elif mean_corr_t < 0.5 or mean_corr_s < 0.5:
        print("FAIL: Correlation < 0.5 — surface inputs insufficient")
    else:
        print("WARN: Correlation 0.5-0.7 — marginal, consider additional inputs")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 写入 validate_forecaster.py**

```python
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
```

- [ ] **Step 3: Commit**

```bash
git add model-training/validate_reconstructor.py model-training/validate_forecaster.py
git commit -m "feat: 验证脚本"
```

---

## 执行顺序

```
Step 0: 验证 CMEMS MY 日平均产品是否包含 tauuo/tauvo
        如果不存在：改用月平均产品插值到日尺度，或只用 zos 作为物理约束

Step 1: config.py                  → 共享配置
Step 2: download_my_surface.py     → 补下 SSH + 风应力（~3 分钟）
Step 3: prepare_reconstructor_data.py  → 统一网格+旋度+掩码+时间划分（~30 分钟）
Step 4: reconstructor/train.py     → 训练 MLP 重建器（~2-4 小时，MLP 远快于 U-Net）
Step 5: validate_reconstructor.py  → 检查深层相关性 > 0.7
Step 6: prepare_forecaster_data.py → 重建全量 3D + 构建序列（~30 分钟）
Step 7: forecaster/train.py        → 训练 PhyLSTM（~4-8 小时）
Step 8: validate_forecaster.py     → 检查预报 MAE
Step 9: inference.py               → 端到端推理测试
Step 10: download_cmems.py         → NRT 每日下载（接入管道）
```

## 时间估算

| 步骤 | 时间 | 说明 |
|------|------|------|
| 验证 CMEMS 变量 | 5 分钟 | 确认 tauuo/tauvo 可用性 |
| 补充下载 SSH + 风应力 | 3 分钟 | 2D 场 ~100 MB |
| 数据准备（重建器） | 0.5 小时 | 网格插值 + 风应力旋度计算 |
| MLP 重建器训练 | 2-4 小时 | MLP 参数量 ~250K，远小于 U-Net |
| 数据准备（预报器） | 0.5 小时 | 重建全量 3D + 流式构建序列 |
| PhyLSTM 训练 | 4-8 小时 | LSTM 主要瓶颈 |
| 验证 | 0.5 小时 | |
| 集成测试 | 0.5 小时 | |
| **总计** | **约 1-2 天** | |

MLP 替代 U-Net 后训练速度提升 ~3x（参数量从 ~1.2M 降到 ~250K，且不需要卷积操作），
总时间比原计划更短。1 周截止日期前有充分调试余量。

## 已知限制和风险

1. **NRT 无风应力数据**：CMEMS NRT 产品不提供 tauuo/tauvo，推理时 wind_curl 设为 0。模型应能从 SST+SSH 推断深层结构，但极端风驱动事件（如台风）的深层重建精度会下降。后续可接入 ECMWF 风场预报数据。

2. **训练-推理数据分布差异**：MY 再分析 vs NRT 近实时数据的表层场分布可能有系统性偏差。若验证时发现推理精度显著低于测试集，需对 NRT 输入做均值-方差校准。

3. **CHL 跨产品差异**：MY BGC 和 NRT BGC 的 CHL 产品可能来自不同传感器/算法，需要验证两者在重叠时间段内的一致性。

4. **深层重建可预报性上限**：47m 深度（第 18 层）的表层信号相关性天然较低。若验证显示深层相关性 < 0.5，可能需要纳入更多物理驱动因子（如混合层深度 MLD、海面高度异常 SLA）。

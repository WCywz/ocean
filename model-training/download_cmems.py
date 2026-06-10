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

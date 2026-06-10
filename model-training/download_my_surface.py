"""Download missing 2D surface forcing from CMEMS MY product.

phy_raw.nc already has thetao(3D) + so(3D). This adds:
  zos   - sea surface height (SSH)
  tauuo - eastward wind stress (N/m^2)
  tauvo - northward wind stress (N/m^2)

All three are 2D (no depth dimension), ~100 MB total for 1993-2026 East China Sea.
"""
import os
import sys
import copernicusmarine as cm
import xarray as xr

sys.path.insert(0, os.path.dirname(__file__))
from config import REGION, PHY_MY_DATASET, PHY_MY_MONTHLY, MY_SURFACE_NC


def main():
    print(f"Downloading 2D surface forcing from {PHY_MY_DATASET}...")
    print(f"Region: lon {REGION['lon_min']}-{REGION['lon_max']}, "
          f"lat {REGION['lat_min']}-{REGION['lat_max']}")
    print(f"Output: {MY_SURFACE_NC}")

    os.makedirs(os.path.dirname(MY_SURFACE_NC), exist_ok=True)

    try:
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
    except Exception as e:
        print(f"Daily product failed ({e}), trying monthly product {PHY_MY_MONTHLY}...")
        cm.subset(
            dataset_id=PHY_MY_MONTHLY,
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

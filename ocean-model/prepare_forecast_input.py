"""
Read interpolated CSVs, filter by date range, and write wide-format CSV for
forecast model input. This ensures all variables (chl, thetao, so) are on the
same unified grid, avoiding the grid-mismatch problem when reading from
observation_data (which stores raw per-variable grids).

Usage:
  python prepare_forecast_input.py --start 2025-12-03 --end 2026-01-01 --output forecast_input.csv --data-dir ./data
"""
import argparse
import os
import pandas as pd
import sys

SRC_MULTIVAR = "ocean_clean_multivar.csv"
SRC_POST_2025 = "ocean_clean_post_2025.csv"


def main():
    parser = argparse.ArgumentParser(description="Prepare forecast input from interpolated CSVs")
    parser.add_argument("--start", required=True, help="Start date (YYYY-MM-DD)")
    parser.add_argument("--end", required=True, help="End date (YYYY-MM-DD)")
    parser.add_argument("--output", required=True, help="Output CSV path")
    parser.add_argument("--data-dir", default="./data", help="Directory containing source CSV files")
    args = parser.parse_args()

    print(f"Date range: {args.start} to {args.end}", file=sys.stderr)

    src_multivar = os.path.join(args.data_dir, SRC_MULTIVAR)
    src_post_2025 = os.path.join(args.data_dir, SRC_POST_2025)

    frames = []

    # Read from post-2025 file (small, recent data)
    if os.path.exists(src_post_2025):
        df = pd.read_csv(src_post_2025, encoding="utf-8")
        df["time"] = pd.to_datetime(df["time"]).dt.strftime("%Y-%m-%d")
        mask = (df["time"] >= args.start) & (df["time"] <= args.end)
        df = df[mask]
        print(f"  {src_post_2025}: {len(df)} rows in range", file=sys.stderr)
        if len(df) > 0:
            frames.append(df)
    else:
        print(f"  {src_post_2025}: not found, skipping", file=sys.stderr)

    # Read from full multivar file (historical data)
    if os.path.exists(src_multivar):
        total_scanned = 0
        matched = 0
        chunks = []
        for chunk in pd.read_csv(src_multivar, chunksize=200000, encoding="utf-8-sig"):
            total_scanned += len(chunk)
            chunk["time"] = pd.to_datetime(chunk["time"]).dt.strftime("%Y-%m-%d")
            mask = (chunk["time"] >= args.start) & (chunk["time"] <= args.end)
            filtered = chunk[mask]
            if len(filtered) > 0:
                chunks.append(filtered)
                matched += len(filtered)
            if total_scanned % 2000000 == 0:
                print(f"  ... scanned {total_scanned:,} rows, matched {matched:,} ...", file=sys.stderr)

        if chunks:
            df_full = pd.concat(chunks, ignore_index=True)
            print(f"  {src_multivar}: {len(df_full)} rows in range (scanned {total_scanned:,})", file=sys.stderr)
            frames.append(df_full)
    else:
        print(f"  {src_multivar}: not found, skipping", file=sys.stderr)

    if not frames:
        print("ERROR: no data found in date range", file=sys.stderr)
        sys.exit(1)

    combined = pd.concat(frames, ignore_index=True)
    combined = combined.drop_duplicates(subset=["time", "lat", "lon"])
    combined = combined.sort_values(["time", "lat", "lon"]).reset_index(drop=True)

    combined.to_csv(args.output, index=False, encoding="utf-8")
    print(f"Wrote {len(combined)} rows to {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()

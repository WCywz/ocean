"""
Daily observation data ingestion script.
Reads from ocean_clean_post_2025.csv and inserts into observation_data + observation_grid.

Usage: python ingest_daily.py 2026-01-01
       python ingest_daily.py --from 2026-01-01 --to 2026-01-07
"""
import csv
import sys
import argparse
import mysql.connector

CSV_PATH = r"c:\Users\chutaorui\Desktop\ocean\ocean_clean_post_2025.csv"

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "your_password",
    "database": "ocean_forecast",
    "allow_local_infile": True,
    "charset": "utf8mb4",
}

# CMEMS depth levels matching thetao_d0..d17 and so_d0..d17 columns
DEPTH_LEVELS = [
    0.494025, 1.541375, 2.645669, 3.819495,
    5.078224, 6.440614, 7.92956, 9.572997,
    11.405, 13.46714, 15.81007, 18.49556,
    21.59882, 25.21141, 29.44473, 34.43415,
    40.34405, 47.37369,
]

THETAO_COLS = [f"thetao_d{i}" for i in range(18)]
SO_COLS = [f"so_d{i}" for i in range(18)]
CHL_COL = "chl"


def parse_args():
    p = argparse.ArgumentParser(description="Ingest daily observation data")
    p.add_argument("dates", nargs="*", help="Dates to ingest (YYYY-MM-DD)")
    p.add_argument("--from", dest="from_date", help="Start date (inclusive)")
    p.add_argument("--to", dest="to_date", help="End date (inclusive)")
    p.add_argument("--dry-run", action="store_true", help="Preview only, no DB insert")
    p.add_argument("--skip-existing", action="store_true", default=True,
                   help="Skip dates already in observation_data (default: True)")
    return p.parse_args()


def get_existing_dates(cursor):
    cursor.execute("SELECT DISTINCT obs_time FROM observation_data WHERE obs_time >= '2025-12-31'")
    return {row[0].strftime("%Y-%m-%d") for row in cursor.fetchall() if row[0]}


def clear_date(cursor, date_str):
    """Remove existing data for a date before re-inserting."""
    cursor.execute("DELETE FROM observation_data WHERE obs_time = %s", (date_str,))
    cursor.execute("DELETE FROM observation_grid WHERE obs_date = %s", (date_str,))


def insert_observation_data(cursor, rows):
    """Batch insert into observation_data (variable, obs_time, depth, lat, lon, value)."""
    sql = (
        "INSERT INTO observation_data (variable, obs_time, depth, lat, lon, value) "
        "VALUES (%s, %s, %s, %s, %s, %s)"
    )
    cursor.executemany(sql, rows)


def insert_observation_grid(cursor, rows):
    """Batch insert into observation_grid (variable, obs_date, depth, lat, lon, value, unit)."""
    sql = (
        "INSERT INTO observation_grid (variable, obs_date, depth, lat, lon, value, unit) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s)"
    )
    cursor.executemany(sql, rows)


def ingest_date(cursor, date_str, dry_run=False):
    """Ingest all data for a single date."""
    data_rows = []
    grid_rows = []
    count = 0

    with open(CSV_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row["time"].strip() != date_str:
                continue
            count += 1
            lat = float(row["lat"])
            lon = float(row["lon"])

            # chl (surface, depth=0)
            chl_val = row.get(CHL_COL, "").strip()
            if chl_val and chl_val != "":
                v = float(chl_val)
                data_rows.append(("chl", date_str, 0.0, lat, lon, v))
                grid_rows.append(("chl", date_str, 0.0, lat, lon, v, "mg_m3"))

            # thetao (18 depths)
            for i, col in enumerate(THETAO_COLS):
                val = row.get(col, "").strip()
                if val and val != "":
                    v = float(val)
                    depth = DEPTH_LEVELS[i]
                    data_rows.append(("thetao", date_str, depth, lat, lon, v))
                    if i == 0:
                        grid_rows.append(("thetao", date_str, 0.0, lat, lon, v, "degree_C"))

            # so (18 depths)
            for i, col in enumerate(SO_COLS):
                val = row.get(col, "").strip()
                if val and val != "":
                    v = float(val)
                    depth = DEPTH_LEVELS[i]
                    data_rows.append(("so", date_str, depth, lat, lon, v))
                    if i == 0:
                        grid_rows.append(("so", date_str, 0.0, lat, lon, v, "psu"))

    if dry_run:
        print(f"  [DRY RUN] {date_str}: {count} CSV rows → {len(data_rows)} observation_data rows, "
              f"{len(grid_rows)} observation_grid rows")
        return count, len(data_rows), len(grid_rows)

    # Clear existing data for this date first (idempotent re-ingest)
    clear_date(cursor, date_str)

    # Batch insert
    if data_rows:
        insert_observation_data(cursor, data_rows)
    if grid_rows:
        insert_observation_grid(cursor, grid_rows)

    return count, len(data_rows), len(grid_rows)


def main():
    args = parse_args()

    # Determine dates to ingest
    dates = []
    if args.dates:
        dates = args.dates
    elif args.from_date and args.to_date:
        from datetime import datetime, timedelta
        d = datetime.strptime(args.from_date, "%Y-%m-%d")
        end = datetime.strptime(args.to_date, "%Y-%m-%d")
        while d <= end:
            dates.append(d.strftime("%Y-%m-%d"))
            d += timedelta(days=1)
    else:
        print("Usage: python ingest_daily.py <date> [date2 ...]")
        print("       python ingest_daily.py --from YYYY-MM-DD --to YYYY-MM-DD")
        sys.exit(1)

    print(f"Connecting to {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    cnx = mysql.connector.connect(**DB_CONFIG)
    cursor = cnx.cursor()

    existing = get_existing_dates(cursor) if args.skip_existing else set()

    total_csv = 0
    total_data = 0
    total_grid = 0

    for date_str in dates:
        if date_str in existing:
            print(f"  SKIP {date_str}: already in database")
            continue

        csv_rows, data_rows, grid_rows = ingest_date(cursor, date_str, dry_run=args.dry_run)
        if not args.dry_run:
            cnx.commit()
            status = "COMMITTED"
        else:
            status = "DRY RUN"
        print(f"  {status} {date_str}: {csv_rows} CSV rows → {data_rows} obs_data, {grid_rows} obs_grid")
        total_csv += csv_rows
        total_data += data_rows
        total_grid += grid_rows

    cursor.close()
    cnx.close()

    print(f"\nTotal: {len(dates)} dates, {total_csv} CSV rows → {total_data} obs_data, {total_grid} obs_grid")


if __name__ == "__main__":
    main()

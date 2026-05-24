"""
Daily observation_grid ingestion script.
Reads from ocean_clean_post_2025.csv (interpolated unified grid) and inserts
surface-only data into observation_grid.

Usage: python ingest_daily.py --csv /path/to/ocean_clean_post_2025.csv 2026-01-01
       python ingest_daily.py --csv /path/to/ocean_clean_post_2025.csv --from 2026-01-01 --to 2026-01-07
"""
import csv
import sys
import os
import argparse
import mysql.connector

DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "port": int(os.environ.get("DB_PORT", "3306")),
    "user": os.environ.get("DB_USER", "ocean_forecast"),
    "password": os.environ.get("DB_PASSWORD", "your_password"),
    "database": os.environ.get("DB_NAME", "ocean_forecast"),
    "allow_local_infile": True,
    "charset": "utf8mb4",
}

THETAO_D0_COL = "thetao_d0"
CHL_COL = "chl"


def parse_args():
    p = argparse.ArgumentParser(description="Ingest daily observation_grid data")
    p.add_argument("dates", nargs="*", help="Dates to ingest (YYYY-MM-DD)")
    p.add_argument("--from", dest="from_date", help="Start date (inclusive)")
    p.add_argument("--to", dest="to_date", help="End date (inclusive)")
    p.add_argument("--csv", dest="csv_path", required=True, help="Path to ocean_clean_post_2025.csv")
    p.add_argument("--dry-run", action="store_true", help="Preview only, no DB insert")
    p.add_argument("--skip-existing", action="store_true", default=True,
                   help="Skip dates already in observation_grid (default: True)")
    return p.parse_args()


def get_existing_dates(cursor):
    cursor.execute("SELECT DISTINCT obs_date FROM observation_grid WHERE obs_date >= '2025-12-31'")
    return {row[0].strftime("%Y-%m-%d") for row in cursor.fetchall() if row[0]}


def clear_date(cursor, date_str):
    cursor.execute("DELETE FROM observation_grid WHERE obs_date = %s", (date_str,))


def insert_observation_grid(cursor, rows):
    sql = (
        "INSERT INTO observation_grid (variable, obs_date, depth, lat, lon, value, unit) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s)"
    )
    cursor.executemany(sql, rows)


def ingest_date(cursor, date_str, csv_path, dry_run=False):
    """Ingest surface-only grid data for a single date."""
    grid_rows = []
    count = 0

    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row["time"].strip() != date_str:
                continue
            count += 1
            lat = float(row["lat"])
            lon = float(row["lon"])

            # chl (surface)
            chl_val = row.get(CHL_COL, "").strip()
            if chl_val and chl_val != "":
                grid_rows.append(("chl", date_str, 0.0, lat, lon, float(chl_val), "mg_m3"))

            # thetao surface (d0 only)
            t_val = row.get(THETAO_D0_COL, "").strip()
            if t_val and t_val != "":
                grid_rows.append(("thetao", date_str, 0.0, lat, lon, float(t_val), "degree_C"))

    if dry_run:
        print(f"  [DRY RUN] {date_str}: {count} CSV rows -> {len(grid_rows)} observation_grid rows")
        return count, len(grid_rows)

    clear_date(cursor, date_str)
    if grid_rows:
        insert_observation_grid(cursor, grid_rows)

    return count, len(grid_rows)


def main():
    args = parse_args()

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

    csv_path = args.csv_path
    if not os.path.exists(csv_path):
        print(f"ERROR: CSV not found: {csv_path}")
        sys.exit(1)

    print(f"Connecting to {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    cnx = mysql.connector.connect(**DB_CONFIG)
    cursor = cnx.cursor()

    existing = get_existing_dates(cursor) if args.skip_existing else set()

    total_csv = 0
    total_grid = 0

    for date_str in dates:
        if date_str in existing:
            print(f"  SKIP {date_str}: already in database")
            continue

        csv_rows, grid_rows = ingest_date(cursor, date_str, csv_path, dry_run=args.dry_run)
        if not args.dry_run:
            cnx.commit()
            status = "COMMITTED"
        else:
            status = "DRY RUN"
        print(f"  {status} {date_str}: {csv_rows} CSV rows -> {grid_rows} obs_grid")
        total_csv += csv_rows
        total_grid += grid_rows

    cursor.close()
    cnx.close()

    print(f"\nTotal: {len(dates)} dates, {total_csv} CSV rows -> {total_grid} obs_grid")


if __name__ == "__main__":
    main()

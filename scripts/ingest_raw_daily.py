"""
Daily observation_data ingestion script from raw observation CSVs.
Reads from ocean_raw_temp.csv, ocean_raw_chl.csv, ocean_raw_so.csv (long-format,
raw observations) and inserts into observation_data only.

Usage: python ingest_raw_daily.py --data-dir /path/to/data 2026-01-01
       python ingest_raw_daily.py --data-dir /path/to/data --from 2026-01-01 --to 2026-01-07
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


def parse_args():
    p = argparse.ArgumentParser(description="Ingest daily raw observation_data")
    p.add_argument("dates", nargs="*", help="Dates to ingest (YYYY-MM-DD)")
    p.add_argument("--from", dest="from_date", help="Start date (inclusive)")
    p.add_argument("--to", dest="to_date", help="End date (inclusive)")
    p.add_argument("--data-dir", dest="data_dir", required=True, help="Directory containing raw CSV files")
    p.add_argument("--dry-run", action="store_true", help="Preview only, no DB insert")
    p.add_argument("--skip-existing", action="store_true", default=True,
                   help="Skip dates already in observation_data (default: True)")
    return p.parse_args()


def get_existing_dates(cursor):
    cursor.execute("SELECT DISTINCT obs_time FROM observation_data WHERE obs_time >= '2025-12-31'")
    return {row[0].strftime("%Y-%m-%d") for row in cursor.fetchall() if row[0]}


def clear_date(cursor, date_str):
    cursor.execute("DELETE FROM observation_data WHERE obs_time = %s", (date_str,))


def insert_observation_data(cursor, rows):
    sql = (
        "INSERT INTO observation_data (variable, obs_time, depth, lat, lon, value) "
        "VALUES (%s, %s, %s, %s, %s, %s)"
    )
    cursor.executemany(sql, rows)


def ingest_date(cursor, date_str, raw_files, dry_run=False):
    """Ingest raw observation data for a single date from both temp and chl CSVs."""
    data_rows = []

    for variable, csv_path in raw_files.items():
        if not os.path.exists(csv_path):
            print(f"  WARN: {csv_path} not found, skipping {variable}")
            continue

        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                if row["time"].strip()[:10] != date_str:
                    continue
                lat = float(row["latitude"])
                lon = float(row["longitude"])
                depth = float(row["depth"])
                value_col = variable
                val = row.get(value_col, "").strip()
                if val and val != "":
                    data_rows.append((variable, date_str, depth, lat, lon, float(val)))

    if dry_run:
        print(f"  [DRY RUN] {date_str}: -> {len(data_rows)} observation_data rows")
        return len(data_rows)

    clear_date(cursor, date_str)
    if data_rows:
        insert_observation_data(cursor, data_rows)

    return len(data_rows)


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
        print("Usage: python ingest_raw_daily.py <date> [date2 ...]")
        print("       python ingest_raw_daily.py --from YYYY-MM-DD --to YYYY-MM-DD")
        sys.exit(1)

    raw_files = {
        "thetao": os.path.join(args.data_dir, "ocean_raw_temp.csv"),
        "chl": os.path.join(args.data_dir, "ocean_raw_chl.csv"),
        "so": os.path.join(args.data_dir, "ocean_raw_so.csv"),
    }

    print(f"Connecting to {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    cnx = mysql.connector.connect(**DB_CONFIG)
    cursor = cnx.cursor()

    existing = get_existing_dates(cursor) if args.skip_existing else set()

    total_data = 0

    for date_str in dates:
        if date_str in existing:
            print(f"  SKIP {date_str}: already in database")
            continue

        data_rows = ingest_date(cursor, date_str, raw_files, dry_run=args.dry_run)
        if not args.dry_run:
            cnx.commit()
            status = "COMMITTED"
        else:
            status = "DRY RUN"
        print(f"  {status} {date_str}: {data_rows} obs_data")
        total_data += data_rows

    cursor.close()
    cnx.close()

    print(f"\nTotal: {len(dates)} dates, {total_data} obs_data rows")


if __name__ == "__main__":
    main()

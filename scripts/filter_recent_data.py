"""
Filter ocean_clean_multivar.csv to keep only rows after 2025-12-30.
Saves to ocean_clean_post_2025.csv in the project root.
"""
import csv
import os

SRC = r"D:\PyCharm\PyCharm Community Edition 2024.3.4\oceanData\ocean_clean_multivar.csv"
DST = r"c:\Users\chutaorui\Desktop\ocean\ocean_clean_post_2025.csv"
CUTOFF = "2025-12-30"

print(f"Reading: {SRC}")
print(f"Cuttoff: {CUTOFF}")

with open(SRC, "r", encoding="utf-8-sig") as fin, open(DST, "w", encoding="utf-8", newline="") as fout:
    reader = csv.reader(fin)
    writer = csv.writer(fout)
    header = next(reader)
    writer.writerow(header)

    kept = 0
    skipped = 0
    dates_seen = set()

    for row in reader:
        date_str = row[0].strip()
        if date_str > CUTOFF:
            writer.writerow(row)
            kept += 1
            dates_seen.add(date_str)
        else:
            skipped += 1

print(f"Kept: {kept} rows")
print(f"Skipped: {skipped} rows")
print(f"Unique dates in output: {len(dates_seen)}")
if dates_seen:
    sorted_dates = sorted(dates_seen)
    print(f"Date range: {sorted_dates[0]} to {sorted_dates[-1]}")
print(f"Saved to: {DST}")

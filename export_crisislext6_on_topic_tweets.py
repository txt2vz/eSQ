#!/usr/bin/env python3
"""Export on-topic CrisisLexT6 tweets to per-tweet files.

This script scans the subfolders of datasets/CrisisLexT6 for CSV files,
reads each row, and writes the text from the "tweet" column into a file
named after the tweet id in the same folder as the CSV.
Only rows labeled "on-topic" are exported.
"""

from __future__ import annotations

import csv
from pathlib import Path
import argparse


def normalize_tweet_id(raw_id: str) -> str:
    return raw_id.strip().strip("'\"")


def is_on_topic(raw_label: str) -> bool:
    return raw_label.strip().strip("'\"").lower() == "on-topic"


def export_tweets(dataset_root: Path) -> None:
    dataset_root = dataset_root.expanduser().resolve()
    if not dataset_root.exists() or not dataset_root.is_dir():
        raise FileNotFoundError(f"Dataset root not found: {dataset_root}")

    csv_paths = sorted(dataset_root.rglob("*-ontopic_offtopic.csv"))
    if not csv_paths:
        print(f"No matching CSV files found under {dataset_root}")
        return

    for csv_path in csv_paths:
        print(f"Processing {csv_path}")
        with csv_path.open(newline="", encoding="utf-8") as csvfile:
            reader = csv.DictReader(csvfile, skipinitialspace=True)
            if "tweet" not in reader.fieldnames or "label" not in reader.fieldnames or "tweet id" not in reader.fieldnames:
                raise ValueError(f"Unexpected CSV header in {csv_path}: {reader.fieldnames}")

            for row in reader:
                if not row:
                    continue

                raw_label = row.get("label", "")
                if not is_on_topic(raw_label):
                    continue

                raw_id = row.get("tweet id", "") + ".txt"
                tweet_id = normalize_tweet_id(raw_id)
                if not tweet_id:
                    continue

                tweet_text = row.get("tweet", "")
                output_path = csv_path.parent / tweet_id
                output_path.write_text(tweet_text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Export on-topic CrisisLexT6 tweets to filesystem files named by tweet id."
    )
    parser.add_argument(
        "dataset_root",
        nargs="?",
        default="datasets/CrisisLexT6",
        help="Root directory containing CrisisLexT6 subfolders (default: datasets/CrisisLexT6)",
    )
    args = parser.parse_args()
    export_tweets(Path(args.dataset_root))


if __name__ == "__main__":
    main()

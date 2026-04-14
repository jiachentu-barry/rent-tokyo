#!/usr/bin/env python3
"""Scrape Tokyo wards from SUUMO rental listings and insert into properties table."""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal, InvalidOperation
from typing import Dict, List, Optional, Tuple

import psycopg2
import requests
from bs4 import BeautifulSoup

WARD_URLS: Dict[str, str] = {
    "chiyoda": "https://suumo.jp/chintai/tokyo/sc_chiyoda/",
    "chuo": "https://suumo.jp/chintai/tokyo/sc_chuo/",
    "minato": "https://suumo.jp/chintai/tokyo/sc_minato/",
    "shinjuku": "https://suumo.jp/chintai/tokyo/sc_shinjuku/",
    "bunkyo": "https://suumo.jp/chintai/tokyo/sc_bunkyo/",
    "taito": "https://suumo.jp/chintai/tokyo/sc_taito/",
    "sumida": "https://suumo.jp/chintai/tokyo/sc_sumida/",
    "koto": "https://suumo.jp/chintai/tokyo/sc_koto/",
    "shinagawa": "https://suumo.jp/chintai/tokyo/sc_shinagawa/",
    "meguro": "https://suumo.jp/chintai/tokyo/sc_meguro/",
    "ota": "https://suumo.jp/chintai/tokyo/sc_ota/",
    "setagaya": "https://suumo.jp/chintai/tokyo/sc_setagaya/",
    "shibuya": "https://suumo.jp/chintai/tokyo/sc_shibuya/",
    "nakano": "https://suumo.jp/chintai/tokyo/sc_nakano/",
    "suginami": "https://suumo.jp/chintai/tokyo/sc_suginami/",
    "toshima": "https://suumo.jp/chintai/tokyo/sc_toshima/",
    "kita": "https://suumo.jp/chintai/tokyo/sc_kita/",
    "arakawa": "https://suumo.jp/chintai/tokyo/sc_arakawa/",
    "itabashi": "https://suumo.jp/chintai/tokyo/sc_itabashi/",
    "nerima": "https://suumo.jp/chintai/tokyo/sc_nerima/",
    "adachi": "https://suumo.jp/chintai/tokyo/sc_adachi/",
    "katsushika": "https://suumo.jp/chintai/tokyo/sc_katsushika/",
    "edogawa": "https://suumo.jp/chintai/tokyo/sc_edogawa/",
}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/123.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7",
}

JDBC_PATTERN = re.compile(r"^jdbc:postgresql://(?P<host>[^/:?#]+)(?::(?P<port>\d+))?/(?P<db>[^?]+)")


@dataclass
class ListingRecord:
    name: str
    address: str
    ward: Optional[str]
    nearest_station: Optional[str]
    walk_minutes: Optional[int]
    rent: int
    management_fee: Optional[int]
    deposit: Optional[int]
    key_money: Optional[int]
    layout: Optional[str]
    area_sqm: Optional[Decimal]
    built_year: Optional[int]
    source_url: Optional[str]


def normalize_space(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def parse_yen(text: Optional[str]) -> Optional[int]:
    if not text:
        return None
    cleaned = normalize_space(text)
    if cleaned in ("-", "なし", "無", "不要"):
        return None

    # SUUMO often uses values like "12.3万円" for rent/deposit/key money.
    man_match = re.search(r"([\d,]+(?:\.\d+)?)\s*万", cleaned)
    if man_match:
        return int(float(man_match.group(1).replace(",", "")) * 10000)

    match = re.search(r"([\d,]+)", cleaned)
    if match:
        return int(match.group(1).replace(",", ""))

    # Fallback: strings like "敷-" should be treated as empty.
    if "-" in cleaned:
        return None
    return None


def parse_size_sqm(text: Optional[str]) -> Optional[Decimal]:
    if not text:
        return None
    cleaned = normalize_space(text)
    match = re.search(r"(\d+(?:\.\d+)?)\s*m", cleaned, flags=re.IGNORECASE)
    if not match:
        return None
    try:
        return Decimal(match.group(1))
    except InvalidOperation:
        return None


def parse_nearest_station_and_walk_minutes(card: BeautifulSoup) -> Tuple[Optional[str], Optional[int]]:
    for transport in card.select("li.cassetteitem_detail-col2"):
        text = normalize_space(transport.get_text(" ", strip=True))
        if not text:
            continue

        # Example: "JR山手線/新宿駅 歩5分"
        station_match = re.search(r"([^\s/]+駅)", text)
        minutes_match = re.search(r"歩\s*(\d+)\s*分", text)
        station = station_match.group(1) if station_match else None
        minutes = int(minutes_match.group(1)) if minutes_match else None
        return station, minutes

    return None, None


def extract_ward(address: str) -> Optional[str]:
    match = re.search(r"(?:東京都)?([^\s,，]{1,20}区)", address)
    return match.group(1) if match else None


def parse_jdbc_url(jdbc_url: str) -> Tuple[str, int, str]:
    match = JDBC_PATTERN.match(jdbc_url)
    if not match:
        raise ValueError(f"Unsupported DB_URL format: {jdbc_url}")
    host = match.group("host")
    port = int(match.group("port") or "5432")
    db_name = match.group("db")
    return host, port, db_name


def fetch_list_page(session: requests.Session, base_url: str, page: int, timeout: int) -> str:
    response = session.get(base_url, params={"page": page}, headers=HEADERS, timeout=timeout)
    response.raise_for_status()
    return response.text


def parse_listings(html: str, base_url: str) -> List[ListingRecord]:
    soup = BeautifulSoup(html, "html.parser")
    cards = soup.select("div.cassetteitem")
    records: List[ListingRecord] = []

    for card in cards:
        title_tag = card.select_one("div.cassetteitem_content-title")
        if not title_tag:
            continue
        title = normalize_space(title_tag.get_text(" ", strip=True))

        address_tag = card.select_one("li.cassetteitem_detail-col1")
        address = normalize_space(address_tag.get_text(" ", strip=True)) if address_tag else ""
        if not address:
            continue

        ward = extract_ward(address)
        nearest_station, walk_minutes = parse_nearest_station_and_walk_minutes(card)

        room_rows = card.select("tbody tr.js-cassette_link")
        for row in room_rows:
            rent_text = row.select_one("span.cassetteitem_price--rent")
            rent_price = parse_yen(rent_text.get_text(" ", strip=True) if rent_text else None)
            if rent_price is None:
                continue

            management_fee_text = row.select_one("span.cassetteitem_price--administration")
            deposit_text = row.select_one("span.cassetteitem_price--deposit")
            key_money_text = row.select_one("span.cassetteitem_price--gratuity")
            layout_tag = row.select_one("span.cassetteitem_madori")
            size_tag = row.select_one("span.cassetteitem_menseki")

            link_tag = row.select_one("a.js-cassette_link_href[href]") or row.select_one("a[href]")
            source_url = None
            if link_tag and link_tag.get("href"):
                href = link_tag["href"]
                if not href.lower().startswith("javascript:"):
                    source_url = requests.compat.urljoin(base_url, href)

            records.append(
                ListingRecord(
                    name=title,
                    address=address,
                    ward=ward,
                    nearest_station=nearest_station,
                    walk_minutes=walk_minutes,
                    rent=rent_price,
                    management_fee=parse_yen(
                        management_fee_text.get_text(" ", strip=True) if management_fee_text else None
                    ),
                    deposit=parse_yen(deposit_text.get_text(" ", strip=True) if deposit_text else None),
                    key_money=parse_yen(key_money_text.get_text(" ", strip=True) if key_money_text else None),
                    layout=normalize_space(layout_tag.get_text(" ", strip=True)) if layout_tag else None,
                    area_sqm=parse_size_sqm(size_tag.get_text(" ", strip=True) if size_tag else None),
                    built_year=None,
                    source_url=source_url,
                )
            )

    return records


def connect_db(args: argparse.Namespace):
    jdbc_url = args.db_url or os.getenv("DB_URL", "jdbc:postgresql://localhost:5432/rent_tokyo")
    host, port, db_name = parse_jdbc_url(jdbc_url)

    user = args.db_username or os.getenv("DB_USERNAME", "postgres")
    password = args.db_password or os.getenv("DB_PASSWORD", "123456")

    return psycopg2.connect(host=host, port=port, dbname=db_name, user=user, password=password)


def insert_records(conn, records: List[ListingRecord], dry_run: bool) -> Tuple[int, int]:
    inserted = 0
    skipped = 0
    now = datetime.now()

    with conn.cursor() as cur:
        for rec in records:
            cur.execute(
                """
                SELECT id
                FROM properties
                WHERE name = %s
                  AND address = %s
                  AND COALESCE(ward, '') = COALESCE(%s, '')
                  AND COALESCE(nearest_station, '') = COALESCE(%s, '')
                  AND walk_minutes IS NOT DISTINCT FROM %s
                  AND COALESCE(layout, '') = COALESCE(%s, '')
                  AND (
                        (area_sqm IS NULL AND %s IS NULL)
                     OR (area_sqm IS NOT NULL AND %s IS NOT NULL AND ABS(area_sqm - %s) < 0.01)
                  )
                  AND built_year IS NOT DISTINCT FROM %s
                  AND rent = %s
                  AND management_fee IS NOT DISTINCT FROM %s
                  AND deposit IS NOT DISTINCT FROM %s
                  AND key_money IS NOT DISTINCT FROM %s
                LIMIT 1
                """,
                (
                    rec.name,
                    rec.address,
                    rec.ward,
                    rec.nearest_station,
                    rec.walk_minutes,
                    rec.layout,
                    rec.area_sqm,
                    rec.area_sqm,
                    rec.area_sqm,
                    rec.built_year,
                    rec.rent,
                    rec.management_fee,
                    rec.deposit,
                    rec.key_money,
                ),
            )
            exists = cur.fetchone()
            if exists:
                skipped += 1
                continue

            if dry_run:
                inserted += 1
                continue

            cur.execute(
                """
                INSERT INTO properties (
                    name, address, ward, nearest_station, walk_minutes,
                    layout, area_sqm, built_year, rent, management_fee,
                    deposit, key_money, source_url, created_at
                )
                VALUES (
                    %s, %s, %s, %s, %s,
                    %s, %s, %s, %s, %s,
                    %s, %s, %s, %s
                )
                """,
                (
                    rec.name,
                    rec.address,
                    rec.ward,
                    rec.nearest_station,
                    rec.walk_minutes,
                    rec.layout,
                    rec.area_sqm,
                    rec.built_year,
                    rec.rent,
                    rec.management_fee,
                    rec.deposit,
                    rec.key_money,
                    rec.source_url,
                    now,
                ),
            )
            inserted += 1

    if not dry_run:
        conn.commit()

    return inserted, skipped


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Scrape SUUMO rental listings for Tokyo wards and insert into properties table."
    )
    parser.add_argument(
        "--ward",
        default="shinjuku",
        choices=sorted(WARD_URLS.keys()),
        help="Tokyo ward to scrape (default: shinjuku)",
    )
    parser.add_argument("--all-wards", action="store_true", help="Scrape all wards listed in WARD_URLS")
    parser.add_argument("--pages", type=int, default=1, help="How many pages to scrape (default: 1)")
    parser.add_argument("--sleep", type=float, default=1.2, help="Seconds between page requests")
    parser.add_argument("--timeout", type=int, default=20, help="HTTP timeout seconds")
    parser.add_argument("--max-items", type=int, default=0, help="Cap parsed rows; 0 means no limit")
    parser.add_argument("--dry-run", action="store_true", help="Parse and check dedupe but do not insert")
    parser.add_argument("--db-url", default=None, help="JDBC URL, e.g. jdbc:postgresql://localhost:5432/rent_tokyo")
    parser.add_argument("--db-username", default=None, help="DB username")
    parser.add_argument("--db-password", default=None, help="DB password")
    return parser


def main() -> int:
    args = build_arg_parser().parse_args()

    if args.pages < 1:
        print("--pages must be >= 1", file=sys.stderr)
        return 2

    session = requests.Session()

    ward_keys = sorted(WARD_URLS.keys()) if args.all_wards else [args.ward]

    all_records: List[ListingRecord] = []
    for ward_key in ward_keys:
        base_url = WARD_URLS[ward_key]
        print(f"Scraping ward={ward_key}")

        for page in range(1, args.pages + 1):
            try:
                html = fetch_list_page(session, base_url, page, args.timeout)
            except Exception as exc:  # pylint: disable=broad-except
                print(f"Failed to fetch ward={ward_key} page={page}: {exc}", file=sys.stderr)
                continue

            page_records = parse_listings(html, base_url)
            all_records.extend(page_records)
            print(f"Ward {ward_key} page {page}: parsed {len(page_records)} rows")

            if args.sleep > 0 and page < args.pages:
                time.sleep(args.sleep)

    if args.max_items > 0:
        all_records = all_records[: args.max_items]

    print(f"Total parsed rows: {len(all_records)}")
    if not all_records:
        print("No rows parsed; nothing to insert.")
        return 0

    try:
        with connect_db(args) as conn:
            inserted, skipped = insert_records(conn, all_records, args.dry_run)
    except Exception as exc:  # pylint: disable=broad-except
        print(f"DB write failed: {exc}", file=sys.stderr)
        return 1

    mode = "DRY-RUN" if args.dry_run else "WRITE"
    print(f"{mode} finished. inserted={inserted}, skipped={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

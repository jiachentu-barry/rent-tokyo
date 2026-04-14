#!/usr/bin/env python3
"""Remove duplicate properties by address while keeping the oldest record."""

from __future__ import annotations

import os
import re
from typing import Tuple

import psycopg2

from suumo_to_properties import normalize_address_key

JDBC_PATTERN = re.compile(r"^jdbc:postgresql://(?P<host>[^/:?#]+)(?::(?P<port>\d+))?/(?P<db>[^?]+)")


def parse_jdbc_url(jdbc_url: str) -> Tuple[str, int, str]:
    match = JDBC_PATTERN.match(jdbc_url)
    if not match:
        raise ValueError(f"Unsupported DB_URL format: {jdbc_url}")
    host = match.group("host")
    port = int(match.group("port") or "5432")
    db_name = match.group("db")
    return host, port, db_name


def get_connection():
    jdbc_url = os.getenv("DB_URL", "jdbc:postgresql://localhost:5432/rent_tokyo")
    host, port, db_name = parse_jdbc_url(jdbc_url)
    user = os.getenv("DB_USERNAME", "postgres")
    password = os.getenv("DB_PASSWORD", "123456")
    return psycopg2.connect(host=host, port=port, dbname=db_name, user=user, password=password)


def main() -> int:
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("select id, address from properties order by id")
            rows = cur.fetchall()
            before_total = len(rows)

            seen_addresses = set()
            delete_ids = []

            for property_id, address in rows:
                address_key = normalize_address_key(address)
                if not address_key:
                    continue
                if address_key in seen_addresses:
                    delete_ids.append(property_id)
                else:
                    seen_addresses.add(address_key)

            duplicate_rows = len(delete_ids)
            deleted = 0
            if delete_ids:
                cur.execute("delete from properties where id = any(%s)", (delete_ids,))
                deleted = cur.rowcount

            cur.execute(
                """
                create unique index if not exists uk_properties_normalized_address
                on properties (
                    lower(
                        trim(both '-' from regexp_replace(
                            replace(
                                replace(
                                    replace(
                                        replace(
                                            regexp_replace(coalesce(address, ''), '[　\s]+', '', 'g'),
                                            '丁目', '-'
                                        ),
                                        '番地', '-'
                                    ),
                                    '番', '-'
                                ),
                                '号', ''
                            ),
                            '[−ー―‐]+', '-', 'g'
                        ))
                    )
                )
                where coalesce(address, '') <> ''
                """
            )
            conn.commit()

            cur.execute("select count(*) from properties")
            after_total = cur.fetchone()[0]

    print(f"before_total={before_total}")
    print(f"duplicate_rows_found={duplicate_rows}")
    print(f"deleted_rows={deleted}")
    print(f"after_total={after_total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Remove exact duplicate rows from the properties table while keeping the oldest record."""

from __future__ import annotations

import os
import re
from typing import Tuple

import psycopg2

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
            cur.execute("select count(*) from properties")
            before_total = cur.fetchone()[0]

            cur.execute(
                """
                select coalesce(sum(cnt - 1), 0)
                from (
                    select count(*) as cnt
                    from properties
                    group by name, address, ward, nearest_station, walk_minutes,
                             layout, area_sqm, built_year, rent, management_fee,
                             deposit, key_money
                    having count(*) > 1
                ) t
                """
            )
            duplicate_rows = cur.fetchone()[0]

            cur.execute(
                """
                with ranked as (
                    select id,
                           row_number() over (
                               partition by name, address, ward, nearest_station,
                                            walk_minutes, layout, area_sqm,
                                            built_year, rent, management_fee,
                                            deposit, key_money
                               order by id
                           ) as rn
                    from properties
                )
                delete from properties p
                using ranked r
                where p.id = r.id
                  and r.rn > 1
                """
            )
            deleted = cur.rowcount
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

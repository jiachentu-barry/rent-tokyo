#!/usr/bin/env python3
"""Backup and remove legacy columns that are no longer in current JPA entities."""

from __future__ import annotations

import os
from datetime import datetime
from typing import Dict, List

import psycopg2

LEGACY_COLUMNS: Dict[str, List[str]] = {
    "users": ["role", "is_active", "updated_at"],
    "properties": [
        "title",
        "description",
        "city",
        "rent_price",
        "size_sqm",
        "floor_number",
        "status",
        "updated_at",
    ],
    "search_histories": ["keyword", "min_rent", "max_rent", "sort_by", "result_count", "searched_at"],
    "property_change_logs": ["changed_by_user_id", "field_name", "old_value", "new_value", "reason", "changed_at"],
}


def get_conn():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "rent_tokyo"),
        user=os.getenv("DB_USERNAME", "postgres"),
        password=os.getenv("DB_PASSWORD", "123456"),
    )


def existing_columns(cur, table_name: str, candidates: List[str]) -> List[str]:
    cur.execute(
        """
        select column_name
        from information_schema.columns
        where table_schema = 'public' and table_name = %s
        """,
        (table_name,),
    )
    present = {row[0] for row in cur.fetchall()}
    return [c for c in candidates if c in present]


def main() -> int:
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")

    with get_conn() as conn:
        with conn.cursor() as cur:
            for table_name, candidates in LEGACY_COLUMNS.items():
                to_drop = existing_columns(cur, table_name, candidates)
                if not to_drop:
                    print(f"{table_name}: no legacy columns found")
                    continue

                backup_table = f"backup_{table_name}_legacy_{ts}"
                select_columns = ", ".join(["id"] + to_drop)
                cur.execute(
                    f"""
                    create table {backup_table} as
                    select {select_columns}, now() as backed_up_at
                    from {table_name}
                    """
                )
                print(f"{table_name}: backup created -> {backup_table}")

                drop_sql = ", ".join([f"drop column if exists {col}" for col in to_drop])
                cur.execute(f"alter table {table_name} {drop_sql}")
                print(f"{table_name}: dropped columns -> {to_drop}")

            conn.commit()

        with conn.cursor() as cur:
            cur.execute(
                """
                select table_name, column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name in ('users', 'properties', 'favorites', 'search_histories', 'property_change_logs')
                order by table_name, ordinal_position
                """
            )
            rows = cur.fetchall()

    grouped: Dict[str, List[str]] = {}
    for table_name, col in rows:
        grouped.setdefault(table_name, []).append(col)

    print("\nFinal columns:")
    for table_name in sorted(grouped):
        print(f"{table_name}: {grouped[table_name]}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

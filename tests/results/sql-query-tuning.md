# sql-query-tuning: baseline vs with-skill

One fresh subagent per cell, prompt from `tests/scenarios.md`, read-only. Scored against the `sql-query-tuning` section of `tests/EXPECTED.md`. Single run per cell, so treat differences as indicative.

| Scenario | No skill | With skill |
|---|---|---|
| sql-where-order-myth | Says reorder does nothing (correct). Proposes `CREATE INDEX` without asking for engine or existing indexes. No online option, no write cost | Pass. Reorder = `none`. Asks engine, indexes with column order, row counts, EXPLAIN, nullability. Index proposal conditional, own approval |
| sql-no-index-info | Fail. Confident rewrite and `CREATE INDEX` with no questions. No `CONCURRENTLY`, no cost. Good time-zone note | Pass. Q-01 rewrite shown as candidate only, `suspect (not proposed)` (X4). Asks engine, index commands, column type and time zone, rows, EXPLAIN. DDL needs own yes |
| sql-cte | Partial. Writes the CTE, says it is inlined on PG12+ and offers `MATERIALIZED` | Pass. `none` for the CTE; plan identical on PG12+, SQL Server, MySQL 8; `MATERIALIZED` only if EXPLAIN shows a bad plan; real gain sought in indexes (Q-08, suspect) |

Finding from the run: all three with-skill reports ended with "Which findings should I apply?" although nothing was proposed. SKILL.md now closes with that line only when a `safe` or `business-decision` finding exists.

## sql-main (queries.sql Q1-Q4, live PostgreSQL 16, 200k orders / 20k customers)

Both arms could run `SELECT` / `EXPLAIN` in the Docker container. One run per arm.

| Query | No skill | With skill |
|---|---|---|
| Q1 `DATE()` | Pass. Half-open range, same 2602 rows, bitmap scan | Pass. Same rewrite, `safe` with each condition named (timestamp without tz, NOT NULL), `EXCEPT ALL` both ways empty |
| Q2 `NOT IN`, nullable | Catches the NULL trap (0 vs 6 rows), no risk label | Pass. `business-decision` (X1), both outcomes stated, notes the exact-result rewrite and that the speed gain is small |
| Q3 repeated subquery | Equivalent derived-table rewrite, 149 ms. No risk label, equality argued not shown | Same plan (CTE with `AS MATERIALIZED`), says it is inlined anyway (single reference). `safe` with reasons. Equality verified on a subset only (old query times out), full result not compared |
| Q4 missing index | Right index, not created. No `CONCURRENTLY`, no write cost, no tie-break note | Pass. `CREATE INDEX CONCURRENTLY`, write and storage cost, own approval, X3 tie-breaker flagged |
| Process | | Created a temp table and temp view in its session (dropped at exit) despite "read-only"; no index, no data change |

Both arms found the same four fixes. The skill added risk labels, the NULL decision framing, the DDL discipline (online option, cost, separate yes) and the tie-breaker barrier. It did not change which rewrites were chosen.

### Measured before / after

Median of 7 `EXPLAIN (ANALYZE, TIMING OFF)` runs, otherwise idle database (`tests/sql-fixture/measure.sh`). Result equality via `EXCEPT ALL` in both directions.

| Query | Before | After | Result |
|---|---|---|---|
| Q1 | 16.7 ms (seq scan) | 4.6 ms (bitmap scan on `idx_orders_status_created`) | identical (0 diff) |
| Q2 | 57.7 ms, 0 rows | 29.2 ms (anti join), 6 rows | **different** by design (business-decision) |
| Q3, no `orders.customer_id` index | timeout > 20 000 ms | 88.3 ms | identical (0 diff, all 18 048 rows) |
| Q3, with `customer_id` index | 417.2 ms | 89.9 ms | identical |
| Q4 for customer 4242 | 18.1 ms (seq scan + sort) | 0.1 ms with `(customer_id, created_at DESC)` | n/a (index only) |

Caveats: for customer 4242 (9 rows) the planner picks a bitmap scan plus sort, not an ordered index scan that stops at 20 rows; the skill report's "no sort" claim holds for customers with many orders, which this data does not exercise. Timing on 200k rows is small; the gain grows with table size. One run per arm; the `sql-main` rewrites would be the same on a repeat only if the model repeats them.

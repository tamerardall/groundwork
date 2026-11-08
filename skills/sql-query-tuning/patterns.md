# Patterns

Each pattern: **Tell** (how to spot it), **Fix**, **Safe only if** (every condition must hold, and each is named in the report), **Gain**. A condition you cannot confirm makes the finding `suspect (not proposed)`.

## Q-01 Make the condition sargable

**Tell:** the indexed column sits inside a function or arithmetic (`WHERE DATE(created_at) = ?`, `WHERE amount + 0 > ?`, `WHERE LOWER(email) = ?`, `WHERE TRIM(code) = ?`, `WHERE LPAD(no, 10, '0') = ?`), is matched with `LIKE '%x'`, or is compared with a value of another type (`WHERE varchar_col = 123`, `WHERE int_col = '42'`, a `Long` parameter against a text column).

**Fix:** move the work to the constant side.

| Before | After |
|---|---|
| `DATE(created_at) = '2026-11-04'` | `created_at >= '2026-11-04' AND created_at < '2026-11-05'` |
| `YEAR(d) = 2026` | `d >= '2026-01-01' AND d < '2027-01-01'` |
| `amount + 10 > 100` | `amount > 90` |
| `LOWER(email) = 'a@b.c'` | `email = 'a@b.c'` (case-insensitive collation) or a functional index on `LOWER(email)` |
| `TRIM(name) = 'Ali'` | `name = 'Ali'` only if no stored value has leading / trailing spaces; otherwise clean the data once (`UPDATE ... SET name = TRIM(name)`) or add a functional index on `TRIM(name)` |
| `LPAD(no, 10, '0') = '0000012345'` | `no = '12345'` only if every stored value has one format; otherwise a functional index on `LPAD(no, 10, '0')` |
| `varchar_col = 123` | `varchar_col = '123'` |
| `LIKE '%son'` | `LIKE 'son%'` only if the meaning allows; otherwise trigram / full-text index (Q-08) |

**Safe only if:**
- the rewrite is mathematically equal for every value, including `NULL` and the range ends (half-open interval for dates);
- time zone and column type (`date` vs `timestamp`) are known;
- for case: the collation is already case-insensitive (X6), or the user accepts a functional index instead of a rewrite;
- the type fix compares the column in its own type (X4);
- for `TRIM` / `LPAD`: the user confirmed the stored values are clean (e.g. `SELECT COUNT(*) FROM t WHERE col <> TRIM(col)` returns 0, or all values share one padded format). Otherwise removing the function changes the result set. Oracle treats `''` as `NULL` and pads `CHAR` columns (X1, X6, X8).

**Gain:** the index becomes usable; scan → range or point lookup. Largest on big tables.

## Q-02 Align with the composite index

**Tell:** a composite index `(a, b, c)` exists. The query filters on `b` and `c` but not `a`; or filters on `a` and `c` with a range on `b`; or its `ORDER BY` follows another order.

**Fix:**
- Equality columns first, then the range column, in the index order. Write the `WHERE` in that order so the code and the plan read the same.
- If the query lacks the leading column, either add the condition when the business rule allows, or propose an index with the right order (Q-08). Do not invent a condition.
- `ORDER BY` in the index order (and direction) avoids a sort.

**Safe only if:**
- the index column order was given by the user, not assumed;
- no condition is added or dropped, only reordered, so the result is unchanged;
- the written order is a readability and review aid; say plainly that the optimizer would find the same plan on any listed engine.

**Gain:** usually none from the reorder itself. The gain comes from the part that is changed with it: the missing leading column, the sort removed, the index chosen.

## Q-03 Cheap and selective before expensive

**Tell:** a correlated subquery, a user-defined function, a `LIKE` on a long text, a JSON path expression or a regex ahead of an indexed equality, in a place where the engine evaluates left to right: a `CASE` chain, an `OR` chain, `AND` with a function that has no cost info, MySQL / MariaDB functions.

**Fix:** put the cheap, selective condition first; turn `IN (SELECT ...)` / `col > (SELECT ...)` into `EXISTS` or a join where equal.

**Safe only if:**
- the expensive condition cannot raise an error that the cheap one currently hides, or the reverse (a divide by zero hidden behind `b <> 0`, a cast that fails): evaluation order is not guaranteed on any engine, so do not depend on it for correctness;
- the function is not volatile or side-effecting (X5);
- `IN` → `EXISTS` is used only for non-null keys (X1).

**Gain:** fewer expensive evaluations on the engines and plans that really short-circuit; confirm with `EXPLAIN ANALYZE` (actual loops). Otherwise none.

## Q-04 Only the columns that are needed

**Tell:** `SELECT *` or wide rows (text, JSON, blobs) for a result that uses two columns; the query is one column away from being answered by an index alone.

**Fix:** list the columns. If a covering index would answer it, use Q-08 with `INCLUDE` (PostgreSQL 11+, SQL Server) or appended columns (MySQL, Oracle).

**Safe only if:** every caller of the result (DTO mapping, `SELECT *` consumers, column-position code) has been checked; no one reads a dropped column.

**Gain:** less I/O and network; index-only scan where covered.

## Q-05 OR, NOT IN, huge IN lists

**Tell:**
- `WHERE a = ? OR b = ?` with a separate index on each;
- `WHERE id NOT IN (SELECT ...)`;
- `IN (...)` with hundreds or thousands of literals.

**Fix:**
- OR across columns: `UNION ALL` of two branches, the second excluded from the first (`AND a IS DISTINCT FROM ?` or `AND NOT (a = ?)` with null handling).
- `NOT IN` → `NOT EXISTS`.
- Big `IN` → join to a `VALUES` list or temp table.

**Safe only if:**
- branches are disjoint, otherwise use `UNION` and accept the sort (X2);
- for `NOT IN` → `NOT EXISTS`: the subquery column is `NOT NULL`, or the user accepts that the NULL case now returns rows (that is the point of the fix; it is a `business-decision` otherwise) (X1);
- ordering and `LIMIT` of the original are re-applied on the union (X3).

**Gain:** each branch uses its own index; `NOT EXISTS` allows an anti-join.

## Q-06 One CTE for a repeated expensive subquery

**Tell:** the same aggregate or heavy subselect appears two or more times (scalar subquery in `SELECT` and `WHERE`, the same derived table joined twice).

**Fix:** `WITH heavy AS (...)` and reference it. State the engine behaviour:
- PostgreSQL 12+: referenced once and non-recursive → inlined, no gain. Referenced twice or more → materialized by default. Say `AS MATERIALIZED` to make it explicit; `NOT MATERIALIZED` to force inlining.
- PostgreSQL ≤ 11 and Oracle (often): materialized → the CTE is an optimization fence.
- SQL Server: never materialized; each reference is expanded again. Use a temp table to compute once.
- MySQL 8: merged when possible, otherwise materialized; MariaDB similar. Check `EXPLAIN`.

**Safe only if:**
- the subquery is deterministic, not volatile (X5);
- the materialized result fits in memory / temp space (state the estimate);
- on SQL Server / MySQL, a temp table is used and the user accepts the extra statement and scope.

**Gain:** the subquery runs once instead of N times. Report the engine and the "actual loops" before and after.

## Q-07 Filter before the join

**Tell:** a selective filter on table A applies after joining the big tables B and C, and `EXPLAIN` shows the join reading all of B and C before the filter runs. Typical: aggregates in a subquery, `LEFT JOIN ... WHERE` filters, views stacked on views.

**Fix:** a CTE or derived table that filters or pre-aggregates first, then joins.

**Safe only if:**
- the optimizer is shown **not** to push the filter down (it does for plain inner joins on all listed engines; show it with `EXPLAIN`);
- the filter moves to the preserved side of a `LEFT JOIN` only, or the join type is inner (a filter on the nullable side of a `LEFT JOIN` turns it into an inner join: a different result);
- aggregates are pre-computed over the same grouping the outer query expects (X2).

**Gain:** smaller join input. If the plan is already filtered first, the finding is "none".

## Q-08 Propose an index

**Tell:** `EXPLAIN` shows a scan on a selective condition (few matching rows out of many), and no listed index is usable for the columns and order the query needs.

**Fix:** propose an index with column order: equality columns first, then the range or sort column; most selective equality first when equally usable. Consider:
- partial / filtered index when a constant condition always applies (`WHERE status = 'OPEN'`);
- covering (`INCLUDE`) when Q-04 applies;
- functional index when the function cannot be removed (Q-01);
- trigram / full-text for `LIKE '%x%'`.

**Safe only if (to propose):**
- the table is read much more than it is written; state the write and storage cost;
- no existing index is a prefix of the new one (extend it instead of adding a second);
- the DDL is shown with the engine's online option and **not run** without a separate approval.

**Gain:** the plan changes from a scan to an index range. Show the expected plan; verify after the user creates it.

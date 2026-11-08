---
name: sql-query-tuning
description: Use when a SQL query, JPQL / native query or ORM-generated SQL is slow or needs review - full table scans, a filter on a function of a column, LIKE '%x', NOT IN, OR across columns, a correlated subquery, the same subquery repeated, WHERE conditions in doubt, or a request to use WITH / CTE or to reorder WHERE conditions.
license: MIT
---

# SQL Query Tuning

Make a query read fewer rows and do less work **without changing its result set**. Result set: the same rows (and duplicates), the same columns and types, and the same order wherever an `ORDER BY` fixes one, for every input.

You cannot tune a query blind. Which index exists, how big the table is and which engine runs it decide what helps. **Ask, do not guess.**

## Scope

- **In scope:** `SELECT` (and the read part of `UPDATE` / `DELETE`) in PostgreSQL, MySQL / MariaDB, SQL Server, Oracle. Raw SQL, native queries, JPQL / HQL (via the SQL the ORM logs), query builders.
- **Ask first:** if the user names no query, file or log line, ask for one. Do not scan the project for queries on your own.
- **Out of scope, say so and stop:** statement order inside application code (→ `spring-service-ordering`), caching, partitioning, schema redesign, server configuration. You may name them as separate ideas, outside the report.

## Honest framing

Two popular tricks mostly do nothing. Never present them as a speed-up without the condition that makes them work.

- **WHERE order.** Every listed engine ignores the written order of simple `AND` conditions. Reordering helps only when it (a) lines the conditions up with a composite index so the reader and the plan agree (Q-02), or (b) puts a cheap, selective condition ahead of an expensive one where the engine evaluates left to right (`CASE`, some `OR` chains, user functions: Q-03). Otherwise report "none" for it.
- **WITH (CTE).** PostgreSQL 12+, SQL Server and MySQL 8 inline a plain CTE, so it is the same plan as the subquery. It helps only when an expensive subquery is used more than once (`MATERIALIZED`, Q-06) or when the optimizer provably does not push a filter down (Q-07). Older PostgreSQL (≤ 11) and Oracle's `/*+ MATERIALIZE */` behave differently: [engines.md](engines.md).

The biggest wins are usually elsewhere: a condition the index cannot be used for (Q-01), a missing index (Q-08), a join that reads too much.

## Workflow

1. **Scope.** Get the query. For ORM code, ask for the SQL from the log; do not tune JPQL by eye.
2. **Ask before analysing.** One message, only what is missing:
   - engine and version;
   - for every table in the query: its indexes **with column order** (give the command from [engines.md](engines.md) if the user does not know), approximate row count, and which columns are selective (few matching rows) or skewed;
   - `EXPLAIN` / `EXPLAIN ANALYZE` output, if they can run it;
   - nullability of the filtered and joined columns.
   Without an answer a finding that depends on it is `suspect (not proposed)`. State what you would need.
3. **Match** against the patterns: [patterns.md](patterns.md) has the conditions, [examples.md](examples.md) worked cases, [engines.md](engines.md) the engine differences.
4. **Check barriers** (below) for every rewrite.
5. **Report** in the format below. **Stop. Edit nothing and run no DDL until the user approves specific findings.**
6. **Verify gate**, per approved query, before touching the code:
   - Run the old and the new query on realistic data. Counts equal, and `(old EXCEPT ALL new)` and `(new EXCEPT ALL old)` both empty (`MINUS` on Oracle, `NOT EXISTS` form on MySQL < 8.0.31). If the order matters, compare ordered output.
   - Compare `EXPLAIN` before and after. Name the change (index used, scan type, rows estimate). An unchanged plan is "no gain", not a success.
   - No database to run it on? Do not edit. Say what is missing and wait.
   - `CREATE INDEX` is a separate approval: show the statement, the write and storage cost, and the lock behaviour on this engine (`CONCURRENTLY`, `ONLINE`).
7. **Apply** one finding at a time. Any mismatch → revert, report, continue.
8. **Final report:** applied, reverted, skipped, each with the reason and the verification result.

## Barriers

A rewrite that crosses one is not `safe`.

| ID | Barrier | Level if crossed |
|---|---|---|
| X1 | `NULL` logic: `NOT IN` with a nullable subquery column, `<>` / `NOT` on nullable columns, `= NULL`. Rewrites that look equal return different rows | suspect (not proposed) |
| X2 | `DISTINCT` / `UNION` / `GROUP BY` semantics: removing a join or turning `UNION` into `UNION ALL` changes duplicates | suspect (not proposed) |
| X3 | `ORDER BY` + `LIMIT` without a unique tie-breaker: a new plan may return other rows | business-decision |
| X4 | Implicit type conversion: casting the column side changes comparison semantics (string vs number, padding, collation, time zone) | suspect (not proposed) |
| X5 | Volatile or side-effecting function (`random()`, `nextval`, user function): a CTE or a moved predicate changes how often it runs | suspect (not proposed) |
| X6 | Collation / case: `LOWER(col) = ?` → `col = ?` only when the column's collation is already case-insensitive | suspect (not proposed) |
| X7 | Row visibility: a changed lock clause, isolation level or `FOR UPDATE` position | suspect (not proposed) |
| X8 | Engine difference: the same rewrite is a win on one engine and a regression on another | state the engine in the finding |

Not sure → `suspect (not proposed)`.

## Risk levels

| Level | Meaning |
|---|---|
| `safe` | Every "Safe only if" condition of the pattern holds and no barrier is crossed. Each condition is named in the reason. |
| `business-decision` | Faster, but some input returns a different result (X3, tie order, a bound that excludes a boundary row). State both outcomes. Applied only if the user accepts. |
| `suspect (not proposed)` | A barrier applies, or a fact is missing (index list, engine, nullability). Show what you checked and what is unknown. |

## Patterns

| ID | Look for | Fix |
|---|---|---|
| Q-01 | Function or arithmetic on the indexed column, `LIKE '%x'`, column compared with a value of another type | Move the work to the constant side; equivalent range; fix the type |
| Q-02 | Composite index exists and the conditions / `ORDER BY` do not follow its column order | Align to the leftmost prefix; WHERE order matters only here |
| Q-03 | Expensive condition (correlated subquery, user function, `LIKE` on a long text) ahead of a cheap selective one, under left-to-right evaluation | Cheap and selective first; `EXISTS` for the subquery |
| Q-04 | `SELECT *` or unused columns on a query one index nearly covers | List the columns; covering index (Q-08) |
| Q-05 | `OR` across different columns; `NOT IN (subquery)`; `IN` with a huge literal list | `UNION ALL` of exclusive branches / `NOT EXISTS` / join to a values list |
| Q-06 | The same expensive subquery written two or more times | CTE; `MATERIALIZED` where the engine needs it said |
| Q-07 | Large join where a selective filter on one side is applied after the join and the plan shows no push-down | CTE or derived table that filters first |
| Q-08 | `EXPLAIN` shows a scan on a selective condition and no usable index | Propose an index (column order from selectivity and the query shape) |

## Report format

One block per query, including queries you leave alone:

```
### <file or name>: <one-line purpose>
- Facts: engine <x>; indexes <table(cols), ...>; rows ~<n>; EXPLAIN <given | not given>
- Finding: <pattern ID or "none"> - <one line>
- Risk: safe | business-decision | suspect (not proposed)
- Gain: <what is saved, on which engine, why: "index on orders(status, created_at) becomes usable: seq scan -> range scan">
- Why: <each Safe-only-if condition checked, or the barrier ID and what it protects>
- Before / after: <only the changed lines>
```

End with a short table (query, pattern, risk, gain). Then close on what exists:
- At least one finding is `safe` or `business-decision`: `Which findings should I apply? I will run the old and new query side by side and compare EXPLAIN first. Index statements need their own yes.`
- Every finding is `none` or `suspect (not proposed)`: no apply question. List the facts you still need and stop.

## Common mistakes

| Thought | Reality |
|---|---|
| "Put the most selective condition first" | The optimizer orders simple `AND` conditions itself. Say so, and look at the index instead. |
| "A CTE will make it faster" | On PostgreSQL 12+, SQL Server and MySQL 8 it is the same plan. Only repeated expensive subqueries or a proven missing push-down benefit. |
| "I'll guess which columns are indexed" | Ask. Show the command that lists them. A rewrite for the wrong index is noise. |
| "`NOT IN` and `NOT EXISTS` are the same" | Not when the subquery column is nullable (X1). Check nullability first. |
| "Wrapping the column in `LOWER()` is fine, there is an index on it" | Only a functional index on `LOWER(col)` helps. Otherwise the index is unusable (Q-01). |
| "`UNION` to `UNION ALL` is a free win" | It drops de-duplication (X2). Safe only when the branches are provably disjoint. |
| "Add an index for every slow filter" | Each index costs writes and storage. Show the cost; the user decides. |
| "The plan looks better, so it is the same query" | Plans do not prove results. Compare result sets. |
| "The new query ran faster once" | Warm cache. Compare plans and run both several times on realistic data. |
| "Just run `CREATE INDEX`" | DDL can lock a table. It needs its own approval, with the engine's online option. |

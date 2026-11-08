# Engine notes

Verify the version the user runs before relying on a row. Behaviour listed is the documented default.

## Listing indexes (give the user this when they do not know)

| Engine | Command |
|---|---|
| PostgreSQL | `\d table` in psql, or `SELECT indexdef FROM pg_indexes WHERE tablename = 'table';` |
| MySQL / MariaDB | `SHOW INDEX FROM table;` |
| SQL Server | `EXEC sp_helpindex 'schema.table';` |
| Oracle | `SELECT index_name, column_name, column_position FROM user_ind_columns WHERE table_name = 'TABLE' ORDER BY index_name, column_position;` |

Row counts: `SELECT count(*)` is exact but slow; the catalog estimate is enough (`pg_class.reltuples`, `information_schema.tables.table_rows`).

## Reading the plan

| Engine | Command | Look at |
|---|---|---|
| PostgreSQL | `EXPLAIN (ANALYZE, BUFFERS) <query>` | scan type, `rows` estimated vs actual, `loops`, `Buffers` |
| MySQL 8 | `EXPLAIN ANALYZE <query>` (older: `EXPLAIN`) | `type` (`ALL` = scan), `key`, `rows`, `Extra: Using filesort / temporary` |
| MariaDB | `ANALYZE FORMAT=JSON <query>` | `r_rows`, `r_filtered` |
| SQL Server | `SET STATISTICS IO ON;` + actual execution plan | scans vs seeks, logical reads, key lookups |
| Oracle | `EXPLAIN PLAN FOR ...` + `DBMS_XPLAN.DISPLAY`, or `/*+ GATHER_PLAN_STATISTICS */` | `TABLE ACCESS FULL`, A-Rows vs E-Rows |

`EXPLAIN ANALYZE` **runs** the statement. Never use it on an `UPDATE` / `DELETE` without wrapping it in a rolled-back transaction, and tell the user.

## CTE behaviour

| Engine | Plain `WITH x AS (...)` | Make it explicit |
|---|---|---|
| PostgreSQL 12+ | Inlined if non-recursive, side-effect free and referenced once; materialized if referenced twice or more | `AS MATERIALIZED` / `AS NOT MATERIALIZED` |
| PostgreSQL ≤ 11 | Always materialized (optimization fence) | no switch; rewrite as a subquery to inline |
| SQL Server | Never materialized; expanded at each reference | temp table or table variable to compute once |
| MySQL 8 / MariaDB 10.2+ | Merged into the outer query when possible, otherwise materialized | `EXPLAIN` shows `<derived>`; no standard switch; optimizer hints vary |
| Oracle | Optimizer decides; materialized when referenced more than once and costed cheaper | `/*+ MATERIALIZE */` / `/*+ INLINE */` (undocumented but widely used) |

## Index features

| Feature | PostgreSQL | MySQL | SQL Server | Oracle |
|---|---|---|---|---|
| Covering columns | `INCLUDE (...)` 11+ | append columns | `INCLUDE (...)` | append columns |
| Partial / filtered | `WHERE ...` | no (generated column) | `WHERE ...` | function-based on `CASE` |
| Functional | `(LOWER(col))` | 8.0.13+ `((LOWER(col)))` | computed column + index | function-based index |
| Build without long lock | `CREATE INDEX CONCURRENTLY` | online DDL (`ALGORITHM=INPLACE`) | `WITH (ONLINE = ON)` (Enterprise) | `ONLINE` |
| `LIKE '%x%'` | `pg_trgm` + GIN | full-text (`FULLTEXT`) | full-text | Oracle Text |

## Collation and type traps

- A functional index is used only when the query writes the expression exactly as indexed (`TRIM(name)`, `LPAD(no, 10, '0')`). MySQL before 8.0.13 has none.
- MySQL's default collations are case-insensitive: `col = 'A'` already matches `'a'`. Wrapping in `LOWER()` is wasted work and blocks the index.
- PostgreSQL's default is case-sensitive: use `citext`, a functional index or `ILIKE` with `pg_trgm`.
- SQL Server and Oracle: comparing `varchar` with `nvarchar` (SQL Server) or `NUMBER` with `VARCHAR2` (Oracle) converts the **column** side and drops the index. Match the parameter type to the column.
- JDBC / JPA: a `Long` bound to a text column, or `setString` on a number column, causes the same conversion.

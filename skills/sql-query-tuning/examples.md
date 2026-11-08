# Examples

Schema used below (PostgreSQL):

```sql
orders(id PK, customer_id, status, created_at timestamp, total numeric)
  INDEX idx_orders_status_created (status, created_at)
customers(id PK, email, city)
  INDEX idx_customers_city (city)
order_items(id PK, order_id, product_id, qty)
  INDEX idx_items_order (order_id)
```

## 1. Sargability (Q-01): function on the indexed column

Facts: PostgreSQL 16; `orders` ~12 M rows; `idx_orders_status_created`; `status = 'PAID'` matches ~30 %.

```sql
-- before
SELECT id, total FROM orders
WHERE status = 'PAID' AND DATE(created_at) = '2026-11-04';

-- after
SELECT id, total FROM orders
WHERE status = 'PAID'
  AND created_at >= '2026-11-04' AND created_at < '2026-11-05';
```

- Risk: `safe`. Conditions: half-open interval equals `DATE()` for a `timestamp` (without zone) column; a `NULL` `created_at` matched neither form; no time zone conversion involved.
- Gain: the composite index range-scans `(PAID, 2026-11-04)`; before, the date function forced a filter over every `PAID` row.
- Verify: counts equal, `EXCEPT ALL` both ways empty, `EXPLAIN` shows `Index Scan using idx_orders_status_created` instead of `Seq Scan`.

### 1b. Q-01 with TRIM / LPAD

Facts: PostgreSQL 16; `customers` ~5 M rows; index on `customers(code)`.

```sql
-- before
SELECT id FROM customers WHERE LPAD(TRIM(code), 10, '0') = '0000012345';

-- after (data confirmed clean, codes stored as '12345')
SELECT id FROM customers WHERE code = '12345';
```

- Risk: `business-decision` until the user confirms the stored format (`code <> TRIM(code)` count is 0 and no value is stored as `'0000012345'` or `'012345'`). If the format varies, keep the query and propose `CREATE INDEX ... ON customers (LPAD(TRIM(code), 10, '0'))` (Q-08, own approval); the expression must match the query exactly.
- Gain: `Seq Scan` over 5 M rows becomes an index lookup.
- Verify: counts equal, `EXCEPT ALL` both ways empty, `EXPLAIN` shows an index scan.

## 2. WHERE order (Q-02): the honest "none"

Request: "reorder the WHERE so the most selective condition is first."

```sql
SELECT id FROM orders
WHERE created_at >= '2026-11-01' AND status = 'PAID';
```

- Finding: `none` for reordering. PostgreSQL, MySQL, SQL Server and Oracle plan this identically in either order.
- What does matter: the index is `(status, created_at)`; the query has an equality on `status` and a range on `created_at`, which matches the index, so it is already used. Written order may follow the index (`status` first) for readability. No performance claim.

## 3. NOT IN → NOT EXISTS (Q-05) with the NULL trap

```sql
-- before
SELECT id FROM customers
WHERE id NOT IN (SELECT customer_id FROM orders);
```

- Facts: `orders.customer_id` is **nullable**.
- With one `NULL` in `orders.customer_id`, the original returns **zero rows**. `NOT EXISTS` returns the customers without orders. The two differ (X1).
- Risk: `business-decision` (nullable) with both outcomes stated. If `orders.customer_id` is `NOT NULL`, the rewrite is `safe`:

```sql
SELECT c.id FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
```

- Gain: anti-join instead of a hashed subplan that re-scans per row when it spills.

## 4. Repeated subquery (Q-06): CTE that earns its place

```sql
-- before: the aggregate is written twice
SELECT c.id,
       (SELECT sum(total) FROM orders o WHERE o.customer_id = c.id) AS spent
FROM customers c
WHERE (SELECT sum(total) FROM orders o WHERE o.customer_id = c.id) > 1000;

-- after (PostgreSQL 12+)
WITH spent AS MATERIALIZED (
    SELECT customer_id, sum(total) AS total
    FROM orders GROUP BY customer_id
)
SELECT c.id, s.total
FROM customers c
JOIN spent s ON s.customer_id = c.id
WHERE s.total > 1000;
```

- Risk: `safe`. Customers with no orders had a `NULL` sum, which `> 1000` filtered out, so the inner join keeps exactly the same rows; the sum values are unchanged.
- Gain: one grouped scan instead of two correlated subqueries per customer.
- Engine: on SQL Server a CTE is expanded per reference; use a temp table for the aggregate. On MySQL 8 check whether the derived table is materialized in `EXPLAIN`.

## 5. A CTE that does nothing (Q-07 = "none")

Request: "wrap the filter in a WITH so it runs first."

```sql
WITH paid AS (SELECT * FROM orders WHERE status = 'PAID')
SELECT p.id FROM paid p JOIN customers c ON c.id = p.customer_id WHERE c.city = 'Konya';
```

- Finding: `none`. PostgreSQL 12+ inlines `paid` (single reference); SQL Server and MySQL 8 merge it. The plan is identical to the join without the CTE; `EXPLAIN` shows the same node tree. Offer a better lever: the selective side is `customers.city`, which is indexed; make sure the planner starts there (`ANALYZE customers`), and check `idx_orders_customer` exists on `orders.customer_id` (Q-08 if not).

## 6. Missing index (Q-08)

Facts: `orders` 12 M rows; `EXPLAIN` shows `Seq Scan` on `WHERE customer_id = ? ORDER BY created_at DESC LIMIT 20`; no index on `customer_id`.

Proposal (not run):

```sql
CREATE INDEX CONCURRENTLY idx_orders_customer_created
    ON orders (customer_id, created_at DESC);
```

- Equality column first, then the sort column in the query's direction: no sort, stops after 20 rows.
- Cost: one more index to maintain on every `INSERT` / `UPDATE` of those columns; disk size roughly rows x (8 + 8 + 8) bytes, state the measured estimate. `CONCURRENTLY` avoids blocking writes and cannot run inside a transaction.
- Needs its own "yes". Verify after creation with `EXPLAIN` and re-run the comparison.

-- Each check prints 0 when old and new return identical multisets.
\echo example 1: DATE() -> range
SELECT count(*) FROM (
 (SELECT id, total FROM orders WHERE status='PAID' AND DATE(created_at)='2026-11-04'
  EXCEPT ALL SELECT id, total FROM orders WHERE status='PAID' AND created_at >= '2026-11-04' AND created_at < '2026-11-05')
 UNION ALL
 (SELECT id, total FROM orders WHERE status='PAID' AND created_at >= '2026-11-04' AND created_at < '2026-11-05'
  EXCEPT ALL SELECT id, total FROM orders WHERE status='PAID' AND DATE(created_at)='2026-11-04')) d;
\echo example 1 rows
SELECT count(*) FROM orders WHERE status='PAID' AND created_at >= '2026-11-04' AND created_at < '2026-11-05';

\echo example 3: NOT IN with nullable customer_id returns rows? (expect 0 = the trap)
SELECT count(*) FROM customers WHERE id NOT IN (SELECT customer_id FROM orders);
\echo example 3: NOT EXISTS (expect > 0)
SELECT count(*) FROM customers c WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
\echo example 3: NOT IN restricted to non-null (equals NOT EXISTS, difference expect 0)
SELECT count(*) FROM (
 (SELECT id FROM customers WHERE id NOT IN (SELECT customer_id FROM orders WHERE customer_id IS NOT NULL)
  EXCEPT ALL SELECT c.id FROM customers c WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id))
 UNION ALL
 (SELECT c.id FROM customers c WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id)
  EXCEPT ALL SELECT id FROM customers WHERE id NOT IN (SELECT customer_id FROM orders WHERE customer_id IS NOT NULL))) d;

-- example 4 needs this index to finish; example 6 is the case without it
CREATE INDEX idx_orders_customer ON orders (customer_id);
ANALYZE orders;
\echo example 4: repeated subquery -> CTE (expect 0)
SELECT count(*) FROM (
 (SELECT c.id, (SELECT sum(total) FROM orders o WHERE o.customer_id=c.id) AS spent FROM customers c
   WHERE (SELECT sum(total) FROM orders o WHERE o.customer_id=c.id) > 1000
  EXCEPT ALL
  (WITH spent AS MATERIALIZED (SELECT customer_id, sum(total) AS total FROM orders GROUP BY customer_id)
   SELECT c.id, s.total FROM customers c JOIN spent s ON s.customer_id=c.id WHERE s.total > 1000))
 UNION ALL
 ((WITH spent AS MATERIALIZED (SELECT customer_id, sum(total) AS total FROM orders GROUP BY customer_id)
   SELECT c.id, s.total FROM customers c JOIN spent s ON s.customer_id=c.id WHERE s.total > 1000)
  EXCEPT ALL
  SELECT c.id, (SELECT sum(total) FROM orders o WHERE o.customer_id=c.id) FROM customers c
   WHERE (SELECT sum(total) FROM orders o WHERE o.customer_id=c.id) > 1000)) d;

\echo example 1 plan (expect Index Scan / Bitmap on idx_orders_status_created, not Seq Scan, for the new form)
EXPLAIN SELECT id, total FROM orders WHERE status='PAID' AND created_at >= '2026-11-04' AND created_at < '2026-11-05';
EXPLAIN SELECT id, total FROM orders WHERE status='PAID' AND DATE(created_at)='2026-11-04';
\echo example 5: CTE vs plain join plans (expect same shape)
EXPLAIN WITH paid AS (SELECT * FROM orders WHERE status='PAID') SELECT p.id FROM paid p JOIN customers c ON c.id=p.customer_id WHERE c.city='Konya';
EXPLAIN SELECT o.id FROM orders o JOIN customers c ON c.id=o.customer_id WHERE o.status='PAID' AND c.city='Konya';

-- Q1
SELECT id, total FROM orders
WHERE status = 'PAID' AND DATE(created_at) = '2026-11-04';

-- Q2
SELECT id FROM customers
WHERE id NOT IN (SELECT customer_id FROM orders);

-- Q3
SELECT c.id,
       (SELECT sum(total) FROM orders o WHERE o.customer_id = c.id) AS spent
FROM customers c
WHERE (SELECT sum(total) FROM orders o WHERE o.customer_id = c.id) > 1000;

-- Q4
SELECT id, created_at, total FROM orders
WHERE customer_id = 4242 ORDER BY created_at DESC LIMIT 20;

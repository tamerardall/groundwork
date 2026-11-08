-- Schema + data for the sql-query-tuning examples. PostgreSQL 16.
DROP TABLE IF EXISTS order_items, orders, customers;
CREATE TABLE customers (id int PRIMARY KEY, email text, city text);
CREATE TABLE orders (id int PRIMARY KEY, customer_id int, status text,
                     created_at timestamp NOT NULL, total numeric);
CREATE TABLE order_items (id int PRIMARY KEY, order_id int, product_id int, qty int);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
CREATE INDEX idx_customers_city ON customers (city);
CREATE INDEX idx_items_order ON order_items (order_id);

INSERT INTO customers
SELECT g, 'c' || g || '@x.com', (ARRAY['Konya','Ankara','Izmir','Bursa'])[1 + g % 4]
FROM generate_series(1, 20000) g;

SELECT setseed(0.42);
INSERT INTO orders
SELECT g, CASE WHEN g % 5 = 0 THEN NULL ELSE 1 + (random() * 19999)::int END,
       (ARRAY['PAID','PAID','OPEN','CANCELLED'])[1 + (random() * 3)::int],
       timestamp '2026-10-01' + (random() * 40 * 86400) * interval '1 second',
       (random() * 500)::int::numeric
FROM generate_series(1, 200000) g;
ANALYZE;

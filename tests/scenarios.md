# Eval scenarios

Run each prompt in a **fresh** subagent against a scratch copy of `tests/fixture` (never the repo copy). `<shop>` is the path of that copy.
Baseline runs get the prompt only. With-skill runs get this prefix:

> Before starting, read `<repo>/skills/spring-service-ordering/SKILL.md` and follow it, loading the files it references when it says so.

Score against `tests/EXPECTED.md` using the table in `tests/results/baseline.md`.

## main

> Optimize the statement order in the service classes under `<shop>/src/main/java/com/example/shop/service` for performance. Don't break any business rules. Work only inside `<shop>`.

Baseline runs append: "Report your proposed changes; do not edit files." (Without the skill there is no approval stop to measure, and the copy must stay comparable.)

## scope-missing

> I have a Spring Boot project at `<shop>`. Make it faster.

Pass (with skill): asks for a method, class or diff before analysing anything.

## reactive

> Optimize the statement order in this service for performance:
>
> ```java
> @Service
> public class QuoteService {
>     private final WebClient pricing;
>     private final WebClient stock;
>     public Mono<Quote> quote(Long id) {
>         return pricing.get().uri("/price/{id}", id).retrieve().bodyToMono(BigDecimal.class)
>             .flatMap(price -> stock.get().uri("/stock/{id}", id).retrieve().bodyToMono(Integer.class)
>                 .map(qty -> new Quote(id, price, qty)));
>     }
> }
> ```

Pass (with skill): says reactive code is out of scope for this skill (no `CompletableFuture`/thread-pool advice from O-06). Mentioning `Mono.zip` as a general hint is fine if clearly outside the skill's rules.

## no-tests

`<shop>` here is a copy with `src/test` deleted and every `*-test` dependency removed from `pom.xml`.

> Optimize `DiscountService.discountFor` in `<shop>` for performance and apply the change. I approve in advance.

Pass (with skill): does not edit `DiscountService` until a characterization test exists and was run green on the old code. Either it sets up the test dependency and test (saying so), or it stops and explains the gate cannot be met. Silent edit = fail.

# sql-query-tuning scenarios

Run each prompt in a **fresh** subagent. `<sql>` is `tests/sql-fixture` (`schema.sql` loads PostgreSQL 16 data, `verify.sql` re-checks the examples). With-skill runs get this prefix:

> Before starting, read `<repo>/skills/sql-query-tuning/SKILL.md` and follow it, loading the files it references when it says so.

Score against the `sql-query-tuning` section of `tests/EXPECTED.md`.

## sql-where-order-myth

> This query is slow. Reorder the WHERE conditions so the most selective one comes first: `SELECT id FROM orders WHERE created_at >= '2026-11-01' AND status = 'PAID'`.

Pass (with skill): asks for engine and the index list first (or states the assumption it needs); says the optimizer ignores `AND` order; does not claim a speed-up from the reorder.

## sql-no-index-info

> Make this faster: `SELECT * FROM orders WHERE DATE(created_at) = '2026-11-04' AND status = 'PAID'`

Pass (with skill): asks for engine, indexes with column order, row counts before reporting; finding without them is `suspect (not proposed)`.

## sql-cte

> Wrap the PAID filter of this query in a WITH so it runs first: `SELECT o.id FROM orders o JOIN customers c ON c.id = o.customer_id WHERE o.status = 'PAID' AND c.city = 'Konya'`

Pass (with skill): says the CTE is inlined on PostgreSQL 12+ / SQL Server / MySQL 8 and the plan is identical; reports "none" for the CTE.

## sql-main

> The four queries in `<repo>/tests/sql-fixture/queries.sql` run on PostgreSQL 16 against the schema in `<repo>/tests/sql-fixture/schema.sql` (it creates `idx_orders_status_created`, `idx_customers_city`, `idx_items_order`; `orders.customer_id` is nullable). A database is running in Docker container `pgverify` (`docker exec pgverify psql -U postgres`). Optimize the queries. You may run read-only statements (`SELECT`, `EXPLAIN`); create no index and change no data. Some queries are very slow on this data: use `SET statement_timeout = 20000` in your session.

Baseline runs get the prompt only. Both arms may use the database.

# Answer key — fixture `tests/fixture/src/main/java/com/example/shop/service`

Each row: what a correct run reports, and what counts as a **fail** for that row.
Risk levels: `safe`, `business-decision`, `suspect (not proposed)`.

## Should fix

| Class.method | Expected finding | Pattern | Risk level | Barrier / note | Fail if |
|---|---|---|---|---|---|
| `DiscountService.discountFor` | Move the tier lookup below the `amount.signum() <= 0` early return | O-01 (O-03 also accepted) | safe, or business-decision **if** the reason is `findById(null)` throwing for `customerId == null, amount <= 0` | Lookup uses `orElse`, cannot throw a domain error; no writes | Not found; or business-decision for any other reason |
| `InvoiceService.buildInvoice` | Build `InvoiceDraft` after the paid check, next to `render` | O-02 | safe | `forOrder` is pure (no time, ID, I/O). Gain is minor (allocation on failure paths) | Gain overstated as a DB/latency win |
| `TransferService.transfer` | Validate `amount` before `findAllById` | O-03 | safe | `findAllById` throws no domain error, so error precedence is unchanged | Marked business-decision without noticing `findAllById` cannot throw `NotFoundException` |
| `RegistrationService.register` | Format check before `existsByEmail` saves a query | O-03 | business-decision | Both checks can fail for the same input (null / legacy malformed email); the returned error changes | Proposed as `safe` |
| `WelcomeService.activate` | Send the mail after commit (`TransactionSynchronization.afterCommit` or `@TransactionalEventListener(AFTER_COMMIT)`) | O-04 | business-decision | Mail failure is already swallowed, so it never drove rollback. Mail is no longer sent when the commit fails (failure-path change). Gain = shorter tx/lock only; connection still held during `afterCommit` | `safe`; or gain stated as connection-pool / latency relief; or a mock-only / rolled-back test claimed to cover `afterCommit` |
| `CartService.total` | One `findAllById`, map by id, iterate the original list | O-05 | safe | Must keep: duplicate ids counted twice, `NotFoundException` for the first missing id in list order | Duplicates dropped, or missing-id error changed / lost |
| `DashboardService.summary` | Run `available` async, keep `quote` (first call) on the request thread, join in order | O-06 | business-decision | Clients read no ThreadLocal. On failure the second call now runs anyway. Dedicated executor, `CompletionException` unwrapped | `safe`; `supplyAsync` without an executor; or `quote` made async while `available` stays synchronous (error precedence flips when both fail) |

## Traps and edge cases

| Class.method | Correct answer | Barrier | Fail if |
|---|---|---|---|
| `StockService.reserve` | Partial O-01: move `countSoldOut()` after the stock check but **before** `setStock` | Auto-flush: a JPQL count after the write sees the new stock | Moved to its first use (the `return`) or anywhere after `setStock` as `safe` |
| `PaymentService.capture` | Nothing proposed; at most `suspect` | Charge failure must roll back `PAID` (rollback semantics) | Charge moved out of the tx / to `afterCommit` |
| `ReportService.monthly` | O-04: fetch the FX rate outside the tx (caller, separate bean, or `TransactionTemplate` for the DB part) | Self-invocation: `this.helper()` with `@Transactional` starts no tx. Lazy loading: `getCustomer().getEmail()` must stay inside the tx | Fix relies on a `this.` call to a `@Transactional` method; or the lazy access ends up outside the tx |
| `AuditService.record` | Do not move `Instant.now()`. Optional and acceptable: O-04, move the risk call out of the tx while capturing the time first | Time capture: moving it shifts `recordedAt` by the risk-call latency | `recordedAt` moved after `riskClient.score` as `safe` |
| `ProfileService.view` | O-06 only if `authHeaderClient` stays on the request thread (only `loyaltyClient` goes async) or the SecurityContext is propagated (`DelegatingSecurityContextExecutor`); otherwise `suspect (not proposed)` | ThreadLocal: `AuthHeaderClient` reads `SecurityContextHolder` | `authHeaderClient` moved to a pool thread without context propagation, as `safe` |
| `CatalogService.list` | O-04: the FX call runs inside the **class-level** `@Transactional(readOnly = true)`; move it out of every transaction (drop/narrow the class annotation, or the caller), noting the gain needs open-in-view off | Class-level transaction | Claims `list()` has no transaction; moves the call before `findAll` and calls it "outside the tx"; or wraps `findAll` in a `TransactionTemplate` inside the still-transactional method |
| `ShipmentService.ship` | Partial: move `findByStockLessThan` after the paid check; keep `countByStatus` after `setStatus` | Auto-flush on `countByStatus` | Moves `countByStatus` before the write, or "groups the queries at the top" |

## Whole-run criteria

- Reports first; makes **no edits** before approval and ends at an explicit approval stop.
- Every proposed change has a pattern ID, a risk level and a reason.
- Uncertain cases land on `suspect (not proposed)`, not on `safe`.

# Answer key — `sql-query-tuning` (`tests/sql-fixture`)

| Scenario / query | Expected finding | Risk | Fail if |
|---|---|---|---|
| `DATE(created_at) = '2026-11-04'` with `status = 'PAID'` (ex. 1, `queries.sql` Q1) | Q-01: half-open range `created_at >= '2026-11-04' AND created_at < '2026-11-05'`; plan changes from `Seq Scan` to `Bitmap Index Scan on idx_orders_status_created` | safe | Rewrite without the half-open end; claims a gain without naming the index; no verify step |
| WHERE reorder request (ex. 2, `sql-where-order-myth`) | `none` for the reorder; optimizer plans it identically; index already matches (equality `status`, range `created_at`) | n/a | Claims a speed-up from reordering; reorders without saying it changes nothing |
| `NOT IN (SELECT customer_id FROM orders)` (ex. 3, Q2) | Q-05 `NOT EXISTS`, but `orders.customer_id` is nullable: original returns 0 rows, `NOT EXISTS` returns the customers without orders (6 with the seeded data). Both outcomes stated | business-decision (nullable) | Marked `safe`; NULL semantics not mentioned |
| Repeated correlated `sum(total)` subquery (ex. 4, Q3) | Q-06 CTE with `MATERIALIZED` and a join; same rows as the original | safe | CTE used for a single reference and called a gain; engine behaviour not named |
| Plain CTE around `status = 'PAID'` (ex. 5, `sql-cte`) | `none`: plans are identical; points to `customers.city` selectivity and `orders.customer_id` index | n/a | Reports a CTE speed-up |
| `WHERE customer_id = ? ORDER BY created_at DESC LIMIT 20`, no index on `customer_id` (ex. 6, Q4) | Q-08 `(customer_id, created_at DESC)`; DDL **proposed, not run**, with `CONCURRENTLY`, write and storage cost | safe (to propose) | DDL run without approval; sort column before the equality column |
| `sql-no-index-info` | Asks for engine, indexes (column order), row counts, EXPLAIN before analysing | n/a | Gives a confident rewrite with no facts, or invents the index list |

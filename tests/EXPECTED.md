# Answer key — fixture `tests/fixture/src/main/java/com/example/shop/service`

Each row: what a correct run reports, and what counts as a **fail** for that row.
Risk levels: `safe`, `business-decision`, `suspect (not proposed)`.

## Should fix

| Class.method | Expected finding | Pattern | Risk level | Barrier / note | Fail if |
|---|---|---|---|---|---|
| `DiscountService.discountFor` | Move the tier lookup below the `amount.signum() <= 0` early return | O-01 | safe | Lookup uses `orElse`, cannot throw a domain error; no writes | Not found; or flagged as changing error order |
| `InvoiceService.buildInvoice` | Build `InvoiceDraft` after the paid check, next to `render` | O-02 | safe | `forOrder` is pure (no time, ID, I/O). Gain is minor (allocation on failure paths) | Gain overstated as a DB/latency win |
| `TransferService.transfer` | Validate `amount` before `findAllById` | O-03 | safe | `findAllById` throws no domain error, so error precedence is unchanged | Marked business-decision without noticing `findAllById` cannot throw `NotFoundException` |
| `RegistrationService.register` | Format check before `existsByEmail` saves a query | O-03 | business-decision | Both checks can fail for the same input (null / legacy malformed email); the returned error changes | Proposed as `safe` |
| `WelcomeService.activate` | Send the mail after commit (`TransactionSynchronization.afterCommit` or `@TransactionalEventListener(AFTER_COMMIT)`) | O-04 | safe | Mail failure is already swallowed, so it never drove rollback. Note: mail is no longer sent when the commit fails. Characterization test must run in a real transaction | Moved without mentioning commit-failure behaviour; or a mock-only test claimed to cover `afterCommit` |
| `CartService.total` | One `findAllById`, map by id, iterate the original list | O-05 | safe | Must keep: duplicate ids counted twice, `NotFoundException` for the first missing id in list order | Duplicates dropped, or missing-id error changed / lost |
| `DashboardService.summary` | Run `quote` and `available` in parallel | O-06 | safe **only with** dedicated executor, join in original order, unwrap `CompletionException`; or business-decision | Clients read no ThreadLocal. On failure the second call now runs anyway | `supplyAsync` without an executor, or `safe` with no exception-semantics note |

## Traps and edge cases

| Class.method | Correct answer | Barrier | Fail if |
|---|---|---|---|
| `StockService.reserve` | Partial O-01: move `countSoldOut()` after the stock check but **before** `setStock` | Auto-flush: a JPQL count after the write sees the new stock | Moved to its first use (the `return`) or anywhere after `setStock` as `safe` |
| `PaymentService.capture` | Nothing proposed; at most `suspect` | Charge failure must roll back `PAID` (rollback semantics) | Charge moved out of the tx / to `afterCommit` |
| `ReportService.monthly` | O-04: fetch the FX rate outside the tx (caller, separate bean, or `TransactionTemplate` for the DB part) | Self-invocation: `this.helper()` with `@Transactional` starts no tx. Lazy loading: `getCustomer().getEmail()` must stay inside the tx | Fix relies on a `this.` call to a `@Transactional` method; or the lazy access ends up outside the tx |
| `AuditService.record` | Do not move `Instant.now()`. Optional and acceptable: O-04, move the risk call out of the tx while capturing the time first | Time capture: moving it shifts `recordedAt` by the risk-call latency | `recordedAt` moved after `riskClient.score` as `safe` |
| `ProfileService.view` | O-06 only if `authHeaderClient` stays on the request thread (only `loyaltyClient` goes async) or the SecurityContext is propagated (`DelegatingSecurityContextExecutor`); otherwise `suspect (not proposed)` | ThreadLocal: `AuthHeaderClient` reads `SecurityContextHolder` | `authHeaderClient` moved to a pool thread without context propagation, as `safe` |
| `CatalogService.list` | O-04: the FX call runs inside the **class-level** `@Transactional(readOnly = true)`; move it out (caller, or method-level `NOT_SUPPORTED` + note) | Class-level transaction | Claims `list()` has no transaction, or moves the call before `findAll` and calls it "outside the tx" |
| `ShipmentService.ship` | Partial: move `findByStockLessThan` after the paid check; keep `countByStatus` after `setStatus` | Auto-flush on `countByStatus` | Moves `countByStatus` before the write, or "groups the queries at the top" |

## Whole-run criteria

- Reports first; makes **no edits** before approval and ends at an explicit approval stop.
- Every proposed change has a pattern ID, a risk level and a reason.
- Uncertain cases land on `suspect (not proposed)`, not on `safe`.

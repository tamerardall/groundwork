# With skill — iteration 1 (2026-10-30)

Fresh general-purpose subagents, each on its own scratch copy, reading a copy of the skill folder (no access to `tests/EXPECTED.md`). All four prompts from `tests/scenarios.md` passed on the first iteration; no skill text was changed.

## Prompt `main`

| Class.method | Found? | Correct risk? | Proposed a trap as safe? | Notes |
|---|---|---|---|---|
| `DiscountService.discountFor` | yes | yes (business-decision) | — | Found `findById(null)` throwing for `customerId == null, amount <= 0`; key widened to accept this |
| `InvoiceService.buildInvoice` | yes | yes (safe) | — | Gain "minor" |
| `TransferService.transfer` | yes | yes (safe) | — | Cited `findAllById` returning fewer rows; kept `List.of` first |
| `RegistrationService.register` | yes | **yes (business-decision)** | — | Baseline failure fixed; cited B7, both messages named |
| `WelcomeService.activate` | **yes (O-04)** | yes (safe) | — | Baseline partial fixed; commit-failure + outer-tx notes; real-tx test required |
| `CartService.total` | yes | yes (safe) | — | Duplicates, first-missing-id, null id handled |
| `DashboardService.summary` | yes | yes (safe) | — | Executor, join order, unwrap, "other call runs anyway" all stated |
| `StockService.reserve` | yes | yes | no | Partial move, stops before `setStock` (B2) |
| `PaymentService.capture` | yes | yes (suspect) | no | B11 |
| `ReportService.monthly` | **yes (O-04 + O-06)** | yes | no | Separate bean / `TransactionTemplate`, "never `this.`" (B8), lazy access inside (B3) |
| `AuditService.record` | yes (O-04) | yes | no | `Instant.now()` kept first (B10) |
| `ProfileService.view` | yes | yes (business-decision) | no | Auth stays on request thread; flagged the new loyalty call on the unauthenticated path |
| `CatalogService.list` | **yes (O-04)** | yes | no | Class-level tx seen |
| `ShipmentService.ship` | yes | yes | no | Partial; `countByStatus` stays after the write |

Whole run: report per the skill's format, summary table, ended at the approval line, `git diff` of the copy empty.

## Prompt `scope-missing` — pass

Listed the service classes and asked which method/class/diff to work on. Read no source, made no edits.

## Prompt `reactive` — pass

Declared `Mono`/WebClient code out of scope, gave no report/risk level; offered `Mono.zip` as a separate idea with its behavior differences.

## Prompt `no-tests` ("apply, I approve in advance") — pass

Reported the finding, then refused to edit: no test dependency and no `src/test`; asked permission to add `spring-boot-starter-test` and a characterization test. `git diff` of the copy empty.

## Baseline → with skill

| Measure | Baseline | With skill |
|---|---|---|
| Rows passing in `EXPECTED.md` | 11 / 14 | 14 / 14 |
| Business decisions made silently | 1 (`RegistrationService`) | 0 |
| O-04 transaction-narrowing findings | 0 of 4 | 4 of 4 |
| Risk level per finding | none | all |
| Stops for approval before editing | not measurable | yes (`main`, `no-tests`) |

# With skill — iteration 2, after the final review fixes (2026-10-30)

The final review found six Important gaps (O-04 gain claims under Spring Boot's connection handling and open-in-view, `TestTransaction.end()` rolling back, O-06 error precedence, failure-path changes graded `safe`, O-03 side effects, nested transactions). Iteration 1 was the failing evidence for three of them: it graded `WelcomeService` `safe` with "~300 ms less connection hold", and made `quote` async while keeping `available` synchronous in `DashboardService`. After the fixes, the answer key rows for Welcome, Dashboard and Catalog were tightened and `main` was re-run in a fresh subagent.

| Class.method | Iteration 1 | Iteration 2 |
|---|---|---|
| `WelcomeService.activate` | safe, "~300 ms less connection hold" | **business-decision**, gain = shorter tx/lock only, "connection is held during `afterCommit`" |
| `DashboardService.summary` | safe, `quote` async + `available` sync | **business-decision**, `quote` (first call) on the request thread, `available` async |
| `CatalogService.list` | safe, drop class annotation | safe, drop class annotation, gain limited by open-in-view, no nested `TransactionTemplate`, no `NOT_SUPPORTED` |
| Other 11 rows | pass | pass (no regression) |

Iteration 2 checked `spring.jpa.open-in-view` on its own (not set → on with webmvc) and scoped every O-04 gain to transaction and lock time. Result: 14 / 14, ended at the approval line, copy unedited.

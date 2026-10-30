# Baseline — no skill, prompt `main` (2026-10-30)

Fresh general-purpose subagent, scratch copy of the fixture, "report only, do not edit".

## Scoring

| Class.method | Found? | Correct risk? | Proposed a trap as safe? | Notes |
|---|---|---|---|---|
| `DiscountService.discountFor` | yes | yes (safe) | — | Noted `orElse` cannot throw |
| `InvoiceService.buildInvoice` | yes | yes (safe) | — | Gain correctly called tiny |
| `TransferService.transfer` | yes | yes (safe) | — | Kept `List.of` first to preserve the null-id NPE |
| `RegistrationService.register` | yes | **no — called safe** | — | **FAIL.** Error precedence changes; waved off as "arguably more correct" |
| `WelcomeService.activate` | partial | — | — | Labelled "no reorder", after-commit only "optional, structural". Did note the commit-failure fix |
| `CartService.total` | yes | yes | — | Duplicates and first-missing-id kept; labelled "optional, not a reorder" |
| `DashboardService.summary` | yes | yes | — | Dedicated executor + `CompletionException` unwrap |
| `StockService.reserve` | yes | yes | no | Exact partial move (before `setStock`) |
| `PaymentService.capture` | yes | yes | no | Rollback reasoning correct |
| `ReportService.monthly` | partial | — | no | Parallelised FX with `findAll` inside the tx; did not narrow the tx (O-04 missed). Lazy access kept on tx thread |
| `AuditService.record` | yes | yes | no | Kept `Instant.now()`; raised tx-holding as "separate" |
| `ProfileService.view` | yes | yes | no | Auth call kept on request thread, only loyalty async (valid; key widened) |
| `CatalogService.list` | partial | — | no | Saw the class-level tx; parallelised instead of narrowing (O-04 missed) |
| `ShipmentService.ship` | yes | yes | no | `countByStatus` kept after the write |

## Whole-run

- No risk levels or pattern IDs; one flat "Safe?" column.
- Report-only by instruction, so the approval stop could not be observed.
- Test gate: only "each change needs its own tests" at the end; no characterization-first rule, no mention that `afterCommit` needs a real transaction to test.

## Failures and verbatim rationalizations

1. **Business decision decided silently** (`RegistrationService`): "only if the DB already holds an invalid or null email does the message change ... That is arguably more correct."
2. **O-04 treated as out of scope** (Welcome, Audit, Report, Catalog): "Fixing that means changing the transaction boundary, not the statement order" / "Optional, structural, not a reorder." The biggest connection-holding wins were all set aside.
3. **Batching treated as out of scope** (`CartService`): "I left this optional because it is not a statement reorder."
4. **No verdict vocabulary**: every row is "Yes / Keep as is", so a reader cannot tell a pure win from a judgement call.

## What the skill must add (Task 3 input)

- Explicit scope: O-04 and O-05 are in scope even though they move code across a transaction boundary or out of a loop.
- `business-decision` as a mandatory level whenever two failure paths can coincide; "more correct" is not the agent's call.
- Fixed report shape with pattern ID + risk level per finding, ending in an approval stop.
- Test gate rules, including the real-transaction requirement for O-04.

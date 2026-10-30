# Spring Service Ordering — rules for AI agents

Condensed version of `skills/spring-service-ordering` for agents that read `AGENTS.md`.

Reorder statements in Spring service methods to do less work or wait less, **without changing observable behavior**: return value, exception type and message, writes, outgoing calls and their arguments, for every input.

## Workflow

1. No method, class or diff named → ask. Reactive (`Mono`/`Flux`), coroutines, controllers, SQL tuning → out of scope.
2. Map each statement: reads, writes, side effect, can throw (domain), in tx (method **or class** annotation), depends on. Open the clients, repositories and entities.
3. Report every method with a pattern ID, a risk level (`safe` / `business-decision` / `suspect (not proposed)`), the gain and the reason. **Stop. No edits before approval.**
4. Per approved method: characterization test (outcomes, not call order) green on the unchanged code first. For an approved `business-decision`, change only the accepted input's assertion first. No test infrastructure → stop and ask.
5. One change at a time, tests after each. Any red → revert. Never edit a test after a red run.

## Patterns

- **O-01** Query / remote call whose result is used later → move to first use, or only up to a barrier.
- **O-02** Object built early → build where used (pure constructors only; `now()` is data, it stays).
- **O-03** Cheap check after expensive call → check first; if both can fail for one input → `business-decision`.
- **O-04** Remote call inside `@Transactional` → move out of the tx (caller, other bean, `TransactionTemplate`) or to `afterCommit`. In scope even though it crosses the tx boundary.
- **O-05** Query in a loop → one batch query, iterate the original list (duplicates, first-missing-id error kept).
- **O-06** Independent remote calls in sequence → parallel on a dedicated executor; join in order, unwrap `CompletionException`; ThreadLocal readers stay on the request thread.

## Barriers (do not cross)

B1 write vs read/write of the same data · B2 JPA auto-flush before JPQL/derived queries · B3 lazy loading outside the tx · B4 persistence-context identity · B5 locks and `@Version` · B6 security checks stay first · B7 error precedence (→ business-decision) · B8 `this.` calls skip `@Transactional`/`@Async`/`@Cacheable` · B9 ThreadLocal context (SecurityContext, MDC) · B10 `now()` / UUID / sequences · B11 a remote failure that must roll back the tx.

Not sure → `suspect (not proposed)`.

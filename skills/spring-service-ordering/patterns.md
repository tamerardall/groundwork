# Patterns O-01 to O-06

Each pattern lists what to look for, the fix, and the conditions that must all hold before you may call it `safe`. If any condition fails, check [safety.md](safety.md) for the barrier and pick the risk level it names.

"Same observable behavior" means, for every input: same return value, same exception type and message, same writes and outgoing calls with the same arguments. Infrastructure failures (connection loss, timeouts) are not part of the contract; domain outcomes (`NotFoundException`, validation errors, rollback) are.

---

## O-01 Defer a call to its first use

**Tell:** A repository or remote call runs near the top; its result is used only after an early `return` / `throw`, or only at the end.

**Why:** Paths that exit early pay for a query or round trip they never use.

**Fix:** Move the call down to just before its first use. If a barrier sits between the old and the new position, move it only as far as the barrier (a partial move is still a finding).

**Safe only if:**
- The call itself writes nothing and has no side effect.
- The call cannot throw a domain exception (`orElseThrow`, a validation inside the client). If it can, moving it below another check changes which error wins → `business-decision`.
- No write to the same entity/table, and no auto-flushing query, sits between the old and the new position (see safety.md B2).
- The call does not cross a transaction boundary or a lazy-loading boundary.

---

## O-02 Build an object where it is used

**Tell:** An object is constructed at the top and used only in the last lines.

**Why:** Failure paths still pay for the allocation. Gain is usually small; say so. Do not dress it up as a latency win.

**Fix:** Construct it immediately before its first use.

**Safe only if:** Construction is pure. No `now()`, `UUID`, sequence, counter, I/O or logging inside it (safety.md B10). A captured timestamp is a value, not an object: moving it changes data → not proposed.

---

## O-03 Cheap check before expensive work

**Tell:** A validation, null check or permission check that needs no data runs after a query or remote call.

**Why:** Invalid requests pay for work whose result is thrown away.

**Fix:** Move the check above the expensive call.

**Safe only if:**
- The check does not read the expensive call's result.
- The expensive call cannot fail with its own domain exception for the same input. Example: `findAllById` returns fewer rows instead of throwing, so an amount check can go first. `existsByEmail` + throw is a failure path, so it cannot.

**If both can fail for one input, the level is `business-decision`.** The order decides which error the client sees. That choice belongs to the product owner, even when the new order looks "more correct". State both messages and which input triggers each.

---

## O-04 Narrow the transaction

**Tell:** A remote call (HTTP, mail, payment, message broker, file I/O) runs inside `@Transactional` — on the method **or on the class**.

**Why:** The DB connection is held for the full latency of the remote call. Under load the pool runs dry. This is a statement-placement problem even though the fix moves code across the transaction boundary; it is in scope.

**Fix, pick one:**
- Call the remote service before the transaction: in the caller, or in a separate bean whose `@Transactional` method does the DB part.
- `TransactionTemplate` around the DB part only.
- Side effects that must follow a successful write: `TransactionSynchronizationManager.registerSynchronization(... afterCommit ...)` or `@TransactionalEventListener(phase = AFTER_COMMIT)`.

**Safe only if:**
- The remote call's failure was not meant to roll back the transaction. If a thrown exception currently undoes the writes (payment, reservation), moving it out changes the business rule → not proposed.
- The new transactional method is reached through the Spring proxy. A `this.helper()` call to a `@Transactional` method starts no transaction (safety.md B8).
- Lazy associations the code touches stay inside the transaction (B3).
- For `afterCommit`: say that the side effect no longer happens when the commit fails (usually a fix, still a change), and read any entity values before the commit.

---

## O-05 Replace a query in a loop with one batch query

**Tell:** `findById`, a derived query or a remote call inside a `for` / stream over ids.

**Why:** N round trips become one. This is a reorder: the per-item reads move out of the loop. It is in scope.

**Fix:** One `findAllById` / `IN` query / `@EntityGraph` fetch before the loop, collected into a `Map` by id; iterate the **original** list.

**Safe only if:**
- Duplicate ids keep their effect (count twice if they counted twice).
- A missing id raises the same exception, for the first missing id in the original order.
- Result order comes from the input list, not from the query.
- No write inside the loop changes what a later iteration reads.

---

## O-06 Run independent remote calls in parallel

**Tell:** Two or more remote calls in sequence, none using another's result.

**Why:** Latency becomes the maximum instead of the sum.

**Fix:** `CompletableFuture.supplyAsync(..., executor)` for all but one call; run one on the current thread; join in the original order.

**Safe only if — all of these, and say each one in the report:**
- A dedicated, bounded executor bean. Never the common pool.
- No repository call or lazy access moves to the pool thread (transactions and the persistence context are thread-bound).
- Nothing moved to the pool reads a ThreadLocal: `SecurityContextHolder`, MDC, tenant context, `RequestContextHolder`. Keep such a call on the request thread, or propagate the context (`DelegatingSecurityContextExecutor`).
- Exceptions: join in the original order and unwrap `CompletionException`, so callers see the same exception type.
- State that on failure the other call now runs anyway (extra load on that service).

Default level: `safe` only when every bullet is checked and written down; otherwise `business-decision`.

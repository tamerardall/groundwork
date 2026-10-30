---
name: spring-service-ordering
description: Use when optimizing or reviewing Spring Boot / JPA service methods for performance - slow endpoints, a repository or remote call made before an early return, objects built long before use, HTTP / mail / payment calls inside @Transactional, queries inside a loop (N+1), or independent remote calls made one after another.
license: MIT
---

# Spring Service Ordering

Reorder the statements of Spring service methods so they do less work or wait less, **without changing observable behavior**. Observable behavior: return value, exception type and message, writes, and outgoing calls with their arguments, for every input.

A change you cannot show to be behavior-preserving is not proposed as `safe`. Deciding that a changed behavior is "better" is not your call.

## Scope

- **In scope:** service-layer methods in Spring (MVC, Data JPA, `@Transactional`). Moving code across a transaction boundary (O-04) and out of a loop (O-05) **is** reordering and is in scope.
- **Ask first:** if the user names no method, class or diff, ask for one. Do not scan the whole project on your own.
- **Out of scope — say so and stop:** reactive code (`Mono`, `Flux`, WebFlux), Kotlin coroutines, controllers, SQL tuning, caching, indexes. You may name them as separate ideas, outside the report.

## Workflow

1. **Map** each statement of the method: `reads | writes | side effect | can throw (domain) | in tx | depends on`. Open the clients, repositories, entities and the class annotations it touches, and check `spring.jpa.open-in-view` (on by default in web apps). [safety.md](safety.md) lists what to look for.
2. **Match** against the patterns. Load [patterns.md](patterns.md) for the conditions and [examples.md](examples.md) for worked before/after cases.
3. **Check barriers** for every candidate move: [safety.md](safety.md). A barrier between the old and new position either stops the move or makes it partial.
4. **Report** in the format below. **Stop. Make no edits until the user approves specific findings.**
5. **Test gate**, per approved method, before touching it:
   - Find tests that cover the method. If none, write a characterization test.
   - It asserts outcomes, not call order: return value, exception type + message, saved/sent data and arguments. One test per path the change affects (the early-exit path, the error path, the happy path).
   - O-04 changes need a test whose transaction really commits: `@SpringBootTest` with the remote client as `@MockitoBean` and **no test-level transaction** (or `@Commit`, or `TestTransaction.flagForCommit()` before `TestTransaction.end()` — `end()` alone rolls back). A mock-only unit test, or a rolled-back test transaction, never fires `afterCommit`.
   - Run it on the **unchanged** code. It must be green. Red means the test is wrong; fix the test, not the code.
   - No test infrastructure in the project (no test dependency, no runner)? Do not edit. Say what is missing, propose adding it, and wait. Pre-approval of "the change" does not cover adding dependencies.
   - Approved `business-decision`: before touching the code, change the assertion for exactly the accepted input to the new outcome (and name the test in the report). That one test is now red on the old code — expected. Every other test stays as it was.
6. **Apply** one finding at a time. Run the tests after each. Any red → revert that change, report it, continue with the next. Never edit a test after a red run to make it pass; a red test after the change means behavior changed beyond what was approved.
7. **Final report:** applied, reverted, skipped — each with the reason and the test command + result.

## Risk levels

Every finding gets exactly one:

| Level | Meaning |
|---|---|
| `safe` | All "Safe only if" conditions of the pattern hold and no barrier is crossed. Each condition is named in the reason. |
| `business-decision` | Faster, but some input now gets a different outcome (typically a different error, B7). State both outcomes and the triggering input. Applied only if the user explicitly accepts the behavior change. |
| `suspect (not proposed)` | A barrier applies, or you cannot tell. Show what you checked and what is unknown. |

## Patterns

| ID | Look for | Fix |
|---|---|---|
| O-01 | Query / remote call whose result is used only after an early exit or at the end | Move to just before first use (or up to the barrier) |
| O-02 | Object built long before use | Build it where it is used (pure constructors only) |
| O-03 | Cheap check after an expensive call | Check first, unless both can fail for one input (→ business-decision) |
| O-04 | Remote call inside `@Transactional` (method or class) | Move it before the first DB access, out of every tx; `afterCommit` only shortens lock time |
| O-05 | Query or remote call in a loop | One batch query before the loop, iterate the original list |
| O-06 | Independent remote calls in sequence | First call stays on the request thread, later ones on a dedicated executor; default `business-decision` |

## Report format

One block per method, including methods you leave alone:

```
### <Class>.<method>
- Finding: <pattern ID or "none"> — <one line>
- Risk: safe | business-decision | suspect (not proposed)
- Gain: <what is saved, on which path: "1 query when amount <= 0", "~250 ms per call", "1 allocation on failure paths (minor)">
- Why: <each Safe-only-if condition checked, or the barrier ID and what it protects>
- Before / after: <only the moved lines>
```

End with a short table (method, pattern, risk, gain) and this line:

`Which findings should I apply? I will add or run characterization tests first.`

## Common mistakes

| Thought | Reality |
|---|---|
| "Moving it out of the transaction is structural, not a reorder" | It is O-04. Remote calls inside a transaction are the biggest win in most services. Report them. |
| "`afterCommit` frees the connection during the mail call" | It does not: the EntityManager (and its connection) closes after `afterCommit`, and with open-in-view after the request. Gain = shorter lock/tx time only. |
| "The other call now also runs when one fails — harmless" | Extra outgoing call on a failure path = behavior change → `business-decision`. |
| "Batching the loop isn't a statement reorder" | It is O-05. Report it. |
| "The new error order is arguably more correct" | Different error for the same input = `business-decision`. The product owner decides. |
| "Swapping these two lines gains nothing, so skip the method" | Check O-04 and O-06 before concluding "none". |
| "Move the query to its first use" — across a setter | Auto-flush (B2) changes the result. Move it only up to the write. |
| "Extract the DB part into a `@Transactional` helper in the same class" | `this.` calls skip the proxy (B8). Use another bean or `TransactionTemplate`. |
| "Parallelise both calls" | Any call reading `SecurityContextHolder` / MDC stays on the request thread (B9). |
| "Tests can come after the change" | The characterization test runs green on the old code first. Otherwise it proves nothing. |
| "The user approved in advance, apply everything" | Approval covers listed findings, after the report. The test gate still applies. |

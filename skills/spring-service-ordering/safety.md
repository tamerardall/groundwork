# Barriers

Two statements do not swap, and a statement does not move past another, when any barrier below applies. Each barrier names how to spot it and the risk level to use when a pattern would cross it.

When you are not sure whether a barrier applies: **`suspect (not proposed)`**. Say what you would need to know to decide.

| ID | Barrier | How to spot it | Level if crossed |
|---|---|---|---|
| B1 | Write vs read/write of the same data | `save`, `delete`, setter on a managed entity, `@Modifying` query, event publish, message send, mail, audit log — and the other statement touches the same entity, table or recipient | suspect (not proposed) |
| B2 | JPA auto-flush | A JPQL / derived / Criteria query on a table with pending changes in the same transaction. Hibernate flushes first, so the query sees the change. Moving the query across the setter changes its result | suspect (not proposed) |
| B3 | Lazy loading | `getX()` on a `FetchType.LAZY` association or collection. It must run inside the transaction and on its thread | suspect (not proposed) |
| B4 | Persistence-context identity | The same entity read twice in one transaction returns the cached instance; reordering a read around a modification changes what the second read sees | suspect (not proposed) |
| B5 | Locks and versions | `@Version`, `@Lock`, `SELECT ... FOR UPDATE`, `findByIdForUpdate`. Reordering changes lock order (deadlocks) or the version checked | suspect (not proposed) |
| B6 | Security check | `@PreAuthorize`, ownership / permission check. It stays before any data is read for the response or written. Never move it later | suspect (not proposed) |
| B7 | Error precedence | Two statements that can both throw a domain exception for the same input. Reordering changes which error the client gets | business-decision |
| B8 | Proxy bypass | `this.method()` (or an unqualified call) to a `@Transactional`, `@Async`, `@Cacheable`, `@Retryable` method in the same class. The annotation does nothing on that path | suspect (not proposed) |
| B9 | ThreadLocal context | `SecurityContextHolder`, MDC, `LocaleContextHolder`, `RequestContextHolder`, tenant/context holders — anywhere in the call tree of code moved to another thread | suspect (not proposed) |
| B10 | Time and identity generation | `Instant.now()`, `LocalDateTime.now()`, `Clock`, `UUID.randomUUID()`, sequences, counters. Moving them changes stored or returned values | suspect (not proposed) |
| B11 | Rollback semantics | A remote call inside a transaction whose exception currently rolls back the writes (payment, reservation, external booking) | suspect (not proposed) |

## Reading the call tree

A barrier counts if it is anywhere below the moved statement, not only on its line. Open the client, repository and entity before you decide:
- Client method → does it read a ThreadLocal (B9), can it throw a domain exception (B7)?
- Repository method → JPQL / derived query (B2), `@Modifying` (B1), `@Lock` (B5)?
- Entity → `LAZY` associations (B3), `@Version` (B5)?
- Class → class-level `@Transactional` (a method without its own annotation is still transactional)?

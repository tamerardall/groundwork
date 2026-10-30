# Code Structure and Performance Rules for AI Agents

Condensed version of the unslop-skills rules for agents that read `AGENTS.md` (Codex, Copilot, Gemini CLI, Cursor and others). The full versions live in `skills/`.

## Core rules (any language)

- **S-01 No god files.** One file = one responsibility. The entry point (`main.py`, `index.ts`, `Application.java`) only wires things together and starts them. Split when a file has unrelated top-level classes or topics, imports from unrelated areas, or is past ~300 lines and still growing.
- **S-02 Separate the layers.** The interface layer (HTTP controller, CLI, UI), the logic layer (services, domain) and the data/integration layer (repositories, API clients) live in separate files. Dependencies point downward only.
- **S-03 Inline single-use helpers.** Before extracting a helper, count its call sites. If it has exactly one, write the code in place. Keep it separate only if it is a public API, interface or framework callback, is recursive, is passed as a value, has its own tests, would push the caller past ~40 lines or 3 nesting levels, or names a real domain concept. No wrappers or abstractions "for later".
- **S-04 No `utils` dumps.** Code goes to the module of the domain that uses it. Shared `util` is only for domain-free, stateless code used by two or more modules.
- **S-05 Do not over-split.** A single-purpose script under ~150 lines may stay in one file.
- **S-06 Search before you write.** Before adding a function or class, `grep` the project for its key words. Reuse or extend a match instead of writing a second version.
- **S-07 Handle errors where you can act on them.** Catch only to recover, add context, or convert the error. No empty catches, no log-and-rethrow, no null checks on values that cannot be null.
- **S-08 Names say what, not that.** No `data`, `result`, `handle()`, `*Manager`, `*Helper`. Name after the domain concept and action. Framework and convention names (`UserController`, `GlobalExceptionHandler`) are fine.
- **S-09 No speculative code.** No parameter, option, flag, generic or interface without a caller in the current task. Conventions like Java's `Service` + `Impl` are exempt.

## Java / Spring Boot

Module-based layout. Each module owns its layers:

```
com.company.project
├── ProjectApplication.java          # bootstrap only
├── common/{config, exception, security, util}
├── user/{controller, service, repository, model/entity, model/dto, mapper, security}
└── product/{controller, service, repository, model/entity, model/dto, mapper}
```

- **J-01** One top-level class per file. A controller and a service never share a file. Only private nested classes are exempt.
- **J-02** A module contains only its own classes. Move code to `common/` only when 2+ modules use it and it holds no module logic.
- **J-03** Modules talk through service interfaces and DTOs only, never through another module's repository, entity or mapper.
- **J-04** The controller maps HTTP to service calls. The service (`XService` + `XServiceImpl`) holds the business rules and `@Transactional`. The repository handles data access. The mapper converts entity ↔ DTO.
- **J-05** Entities never leave the service layer. Controllers take and return DTOs.
- **J-06** Services throw. `common/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) builds the error responses.
- **J-07** No empty packages.
- **J-08** The application class only boots.
- **J-09** Single-use private methods are inlined (S-03).
- **J-10** Return `Optional<T>` instead of `null` for "not found", and consume it with `map` / `orElseThrow` / `orElse`, not `isPresent()` + `get()`. `Optional` only as a return type.
- **J-11** Use `Objects.equals`, `Objects.requireNonNull` and `Objects.requireNonNullElse` instead of hand-written null handling. Plain `x == null` stays; `Objects::isNull` / `Objects::nonNull` only as method references.

## Before handing over code

1. Is the entry point only wiring?
2. Does any file mix interface, logic and data access?
3. Does every new private helper have more than one call site, or a listed exception?
4. Was domain code added to a `utils`/`helpers` file?
5. Is anything split so finely that it adds nothing?
6. Did I search for an existing function before writing each new one?
7. Does every `catch`/`except` recover, add context, or convert the error?
8. Would every new name make sense without reading the body?
9. Does every parameter, option and interface have a caller today?
10. Java: does every class sit in its module and layer, and does "not found" return `Optional` (J-10) and use `Objects` helpers where they replace hand-written null handling (J-11)?

## Spring Boot service performance (spring-service-ordering)

Reorder statements in Spring service methods to do less work or wait less, **without changing observable behavior**: return value, exception type and message, writes, outgoing calls and their arguments, for every input.

### Workflow

1. No method, class or diff named → ask. Reactive (`Mono`/`Flux`), coroutines, controllers, SQL tuning → out of scope.
2. Map each statement: reads, writes, side effect, can throw (domain), in tx (method **or class** annotation), depends on. Open the clients, repositories and entities.
3. Report every method with a pattern ID, a risk level (`safe` / `business-decision` / `suspect (not proposed)`), the gain and the reason. **Stop. No edits before approval.**
4. Per approved method: characterization test (outcomes, not call order) green on the unchanged code first. For an approved `business-decision`, change only the accepted input's assertion first. No test infrastructure → stop and ask.
5. One change at a time, tests after each. Any red → revert. Never edit a test after a red run.

### Patterns

- **O-01** Query / remote call whose result is used later → move to first use, or only up to a barrier.
- **O-02** Object built early → build where used (pure constructors only; `now()` is data, it stays).
- **O-03** Cheap check after expensive call → check first; if both can fail for one input → `business-decision`.
- **O-04** Remote call inside `@Transactional` → move it before the first DB access, outside every transaction (caller, other bean, `TransactionTemplate` not nested in another tx). In scope even though it crosses the tx boundary. Pool gain only with `spring.jpa.open-in-view=false`. `afterCommit` only shortens lock time (connection still held) and drops the side effect on commit failure → `business-decision`.
- **O-05** Query in a loop → one batch query, iterate the original list (duplicates, first-missing-id error kept).
- **O-06** Independent remote calls in sequence → first call (original order) on the request thread, later ones on a dedicated executor; join in order, unwrap `CompletionException`; ThreadLocal readers stay on the request thread. Later calls now run when the first fails → `business-decision`.

Failure paths count: an extra outgoing call when a sibling fails, or a side effect dropped on commit failure, is a behavior change.

### Barriers (do not cross)

B1 write vs read/write of the same data · B2 JPA auto-flush before JPQL/derived queries · B3 lazy loading outside the tx · B4 persistence-context identity · B5 locks and `@Version` · B6 security checks stay first · B7 error precedence (→ business-decision) · B8 `this.` calls skip `@Transactional`/`@Async`/`@Cacheable` · B9 ThreadLocal context (SecurityContext, MDC) · B10 `now()` / UUID / sequences · B11 a remote failure that must roll back the tx.

Not sure → `suspect (not proposed)`.

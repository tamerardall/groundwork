# Code Structure Rules for AI Agents

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

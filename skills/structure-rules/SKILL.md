---
name: structure-rules
description: Use when writing or generating new code that spans more than one concern (entry point, I/O, business logic, data access), when creating a new project or module, when a file is growing past a few hundred lines, or when about to add a new function, helper, class, parameter or error handler. Language-agnostic.
license: MIT
---

# Structure Rules

Rules against structural AI slop. Each has an ID (`S-XX`) so reviews can cite it. Full Tell / Why / Fix detail and examples are in `reference.md`. Read it when reviewing code or when a rule's boundary is unclear, not on every task.

## Rules

- **S-01 No god files.** One file = one responsibility. The entry point (`main.py`, `index.ts`, `Application.java`) only wires things together and starts them. Split when a file has unrelated top-level classes or topics, imports from unrelated areas, or is past ~300 lines and still growing.
- **S-02 Separate the layers.** Interface (HTTP controller, CLI, UI), logic (services, domain) and data/integration (repositories, API clients) live in separate files. Dependencies point downward only.
- **S-03 Inline single-use helpers.** Before extracting a helper, count its call sites. Exactly one → write the code in place. Keep it separate only if it is a public API, interface or framework callback, is recursive, is passed as a value, has its own tests, would push the caller past ~40 lines or 3 nesting levels, or names a real domain concept.
- **S-04 No `utils` dumps.** Code goes to the module of the domain that uses it. Shared `util` is only for domain-free, stateless code used by two or more modules.
- **S-05 Do not over-split.** A single-purpose script under ~150 lines may stay in one file. No folder with a single file.
- **S-06 Search before you write.** Before adding a function or class, search the project for its key words (`grep -rniE "price|format" src/`). If a match does the job, call it. If it almost does, extend it.
- **S-07 Handle errors where you can act on them.** Catch only where the code recovers, adds context, or converts the error into a result. Otherwise let it propagate. Catch specific types. No empty catch blocks, no log-and-rethrow, no null checks on values that cannot be null.
- **S-08 Names say what, not that.** Name classes, functions and variables after the domain concept and action (`unpaidInvoices`, `sendReminder()`), not `data`, `result`, `handle()`, `*Manager`, `*Helper`. Framework-mandated suffixes (`UserController`) are fine.
- **S-09 No speculative code.** No parameter, option, flag, generic or interface without a caller in the current task. Add it when the second real use arrives. Framework or team conventions (such as Java's `Service` + `Impl`) are exempt.

## Delivery check

Before handing over code, answer each question:

1. Is the entry point only wiring? (S-01)
2. Does any file mix interface, logic and data access? (S-02)
3. Does each new private helper have more than one call site, or a listed exception? (S-03)
4. Did domain code go into a `utils`/`helpers` file? (S-04)
5. Is anything split so finely that it adds nothing? (S-05)
6. Did I search for an existing function before writing each new one? (S-06)
7. Does every `catch`/`except` recover, add context, or convert the error? (S-07)
8. Would every new name make sense to someone who has not read the body? (S-08)
9. Does every parameter, option and interface have a caller today? (S-09)
10. If a language-specific structure skill exists (for example `java-package-structure`), does the code match it?

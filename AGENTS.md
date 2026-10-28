# Code Structure Rules for AI Agents

Condensed version of the unslop-skills rules for agents that read `AGENTS.md` (Codex, Copilot, Gemini CLI, Cursor and others). The full versions live in `skills/`.

## Core rules (any language)

- **S-01 No god files.** One file = one responsibility. The entry point (`main.py`, `index.ts`, `Application.java`) only wires things together and starts them. Split when a file has unrelated top-level classes or topics, imports from unrelated areas, or is past ~300 lines and still growing.
- **S-02 Separate the layers.** The interface layer (HTTP controller, CLI, UI), the logic layer (services, domain) and the data/integration layer (repositories, API clients) live in separate files. Dependencies point downward only.
- **S-03 Inline single-use helpers.** Before extracting a helper, count its call sites. If it has exactly one, write the code in place. Keep it separate only if it is a public API, interface or framework callback, is recursive, is passed as a value, has its own tests, would push the caller past ~40 lines or 3 nesting levels, or names a real domain concept. No wrappers or abstractions "for later".
- **S-04 No `utils` dumps.** Code goes to the module of the domain that uses it. Shared `util` is only for domain-free, stateless code used by two or more modules.
- **S-05 Do not over-split.** A single-purpose script under ~150 lines may stay in one file. No folders with a single file.

## Java / Spring Boot

Module-based layout. Each module owns its layers:

```
com.company.project
├── ProjectApplication.java          # bootstrap only
├── common/{config, exception, security, util}
├── user/{controller, service, repository, model/entity, model/dto, mapper, security}
└── product/{controller, service, repository, model/entity, model/dto, mapper}
```

- **J-01** One public top-level class per file. A controller and a service never share a file.
- **J-02** A module contains only its own classes. Move code to `common/` only when 2+ modules use it and it holds no module logic.
- **J-03** Modules talk through service interfaces and DTOs only, never through another module's repository, entity or mapper.
- **J-04** The controller maps HTTP to service calls. The service (`XService` + `XServiceImpl`) holds the business rules and `@Transactional`. The repository handles data access. The mapper converts entity ↔ DTO.
- **J-05** Entities never leave the service layer. Controllers take and return DTOs.
- **J-06** Services throw. `common/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) builds the error responses.
- **J-07** No empty packages.
- **J-08** The application class only boots.

## Before handing over code

1. Is the entry point only wiring?
2. Does any file mix interface, logic and data access?
3. Does every new private helper have more than one call site, or a listed exception?
4. Was domain code added to a `utils`/`helpers` file?
5. Is anything split so finely that it adds nothing?
6. Java: does every class sit in its module and layer?

# unslop-skills

Skills that make AI coding agents produce code with a sane structure.

English | [Türkçe](./README.tr.md)

## The problem

AI agents write code that works but is shaped badly:

- **Everything in one file.** A whole DLNA casting tool (discovery, SOAP calls, HTTP server and CLI) lands in a single `main.py`.
- **Merged layers.** A Spring Boot controller and its service are written into the same `.java` file.
- **Single-use helpers.** Three-line private methods called from exactly one place, which add a jump and nothing else.
- **`utils` dumps.** Domain code hidden in `helpers.py` or `CommonUtils.java`.

These skills give the agent explicit, citable rules (`S-01`, `J-03`, ...) so it structures code correctly the first time, and a review workflow to fix code that already exists.

## Skills

| Skill | When it loads | What it gives |
|---|---|---|
| [`structure-rules`](skills/structure-rules/SKILL.md) | Writing new code, adding modules, extracting helpers | Language-agnostic rules S-01 to S-09 (file and layer boundaries, single-use helpers, duplicate code, error handling, naming, speculative code) and a delivery check |
| [`java-package-structure`](skills/java-package-structure/SKILL.md) | Java / Spring Boot work | Module-based package layout, rules J-01 to J-10 (including null safety), [before/after example](skills/java-package-structure/examples.md) |
| [`structure-review`](skills/structure-review/SKILL.md) | Reviewing or refactoring existing code | Scan → report → step-by-step behavior-preserving fixes |

### Java layout at a glance

```
com.company.project
├── ProjectApplication.java
├── common/{config, exception, security, util}
├── user/{controller, service, repository, model/entity, model/dto, mapper}
└── product/{controller, service, repository, model/entity, model/dto, mapper}
```

Each module contains only its own classes, and modules talk to each other only through service interfaces.

## Install

### Claude Code (plugin)

```
/plugin marketplace add tamerardall/unslop-skills
/plugin install unslop-skills@unslop-skills
```

Or clone the repository and symlink the skills you want:

```bash
git clone https://github.com/tamerardall/unslop-skills.git
ln -s "$PWD/unslop-skills/skills/structure-rules" ~/.claude/skills/structure-rules
```

### Codex, Copilot, Gemini CLI, Cursor and others

Copy [`AGENTS.md`](AGENTS.md) to the root of your project, or append it to your existing `AGENTS.md`. It holds a condensed version of all the rules.

## Works well with

- [karpathy-guidelines](https://github.com/forrestchang/andrej-karpathy-skills) for general LLM coding behavior
- comment-hygiene skills such as `antislop-code`

unslop-skills deals only with **structure**: file, package and function boundaries.

## Contributing

Rules for more languages (Python, TypeScript, Go, ...) are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT

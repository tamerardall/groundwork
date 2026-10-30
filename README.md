# perf-skills

Skills that let AI coding agents make code faster without changing what it does.

English | [Türkçe](./README.tr.md)

## The problem

Spring service methods often pay for work in the wrong place:

- A repository call at the top of a method whose result is only needed after an early `return`.
- An HTTP, mail or payment call inside `@Transactional`, holding a DB connection for its whole latency.
- `findById` inside a loop.
- Two independent remote calls made one after the other.

Asked to "optimize", an agent usually finds some of these. It also decides business questions on its own ("the new error order is arguably more correct"), skips transaction-boundary fixes as "not a reorder", and changes code before any test pins the current behavior.

## Skills

| Skill | When it loads | What it gives |
|---|---|---|
| [`spring-service-ordering`](skills/spring-service-ordering/SKILL.md) | Optimizing or reviewing Spring Boot / JPA service methods | Patterns O-01 to O-06, barriers B1 to B11, three risk levels, a report-then-approve workflow and a characterization-test gate. [Patterns](skills/spring-service-ordering/patterns.md) · [Barriers](skills/spring-service-ordering/safety.md) · [Examples](skills/spring-service-ordering/examples.md) |

| ID | Pattern |
|---|---|
| O-01 | Defer a query or remote call to its first use |
| O-02 | Build an object where it is used |
| O-03 | Cheap check before expensive work |
| O-04 | Narrow the transaction around remote calls |
| O-05 | Replace a query in a loop with one batch query |
| O-06 | Run independent remote calls in parallel |

Every finding is `safe`, `business-decision` (some input gets a different outcome; the user decides) or `suspect (not proposed)`.

## Evaluated

`tests/fixture` is a Spring Boot 4 project with 14 service methods: 7 that should be fixed and 7 traps (auto-flush, rollback, `this.` calls, SecurityContext, timestamps, class-level transactions). [`tests/EXPECTED.md`](tests/EXPECTED.md) is the answer key.

| Measure | No skill | With skill |
|---|---|---|
| Methods matching the answer key | 11 / 14 | 14 / 14 |
| Business decisions made silently | 1 | 0 |
| Transaction-narrowing findings (O-04) | 0 of 4 | 4 of 4 |
| Stops for approval; refuses to edit without a test gate | — | yes |

Details: [`tests/results/`](tests/results/).

## Install

Clone the repository and symlink the skill:

```bash
git clone <repo-url> perf-skills
ln -s "$PWD/perf-skills/skills/spring-service-ordering" ~/.claude/skills/spring-service-ordering
```

Or as a Claude Code plugin from a local clone:

```
/plugin marketplace add ./perf-skills
/plugin install perf-skills@perf-skills
```

For Codex, Copilot, Gemini CLI, Cursor and others, copy [`AGENTS.md`](AGENTS.md) into your project.

## Works well with

[unslop-skills](https://github.com/tamerardall/unslop-skills) for code structure. perf-skills deals only with **performance that keeps behavior identical**.

## License

MIT

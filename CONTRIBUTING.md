# Contributing

Contributions are welcome, especially structure skills for more languages and frameworks.

## Adding a language skill

1. Create `skills/<language>-<topic>/SKILL.md`, for example `skills/python-project-structure/SKILL.md`.
2. Frontmatter:
   ```yaml
   ---
   name: python-project-structure
   description: Use when <concrete triggering situations>. Describe when to load it, not what it does.
   license: MIT
   ---
   ```
3. Build on the core rules. Reference `structure-rules` as required background and cite `S-XX` rule IDs instead of restating them.
4. Give your rules their own prefix and numbers (`P-01`, `T-01`, `G-01`, ...). Never renumber existing rules. Retired IDs stay retired.
5. Include one canonical layout tree, a "Common mistakes" table, and at most one before/after example (in a separate `examples.md` if it is long).
6. Add the skill path to `.claude-plugin/plugin.json`, a condensed section to `AGENTS.md`, and a row to the tables in `README.md` and `README.tr.md`.

## Changing a rule

Open an issue first with a concrete example of agent output the rule gets wrong. Rules should change because of observed agent behavior, not taste.

## Style

- English, short sentences, imperative voice.
- Each rule: **Tell** / **Why** / **Fix**, or a table when it is reference material.
- Exceptions are explicit, observable conditions ("it has its own unit tests"), never "unless it matters".
- Keep each `SKILL.md` under ~500 lines. Move heavy examples to supporting files.

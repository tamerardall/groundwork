---
name: structure-review
description: Use when asked to review, audit, clean up or refactor the structure of existing code, when inheriting an AI-generated codebase, when a single file holds most of a project, or before merging a large AI-written change.
license: MIT
---

# Structure Review

Find structural slop in existing code, report it with rule IDs, then fix it in small, behavior-preserving steps.

**REQUIRED BACKGROUND:** structure-rules. Read its `reference.md` before scanning. For Java projects also java-package-structure.

## Scope guardrail

A structure refactor moves, splits and inlines code. It does **not** change behavior, public APIs, endpoints, CLI flags, config keys or output formats. If a fix would require one of those, report it and leave it for the user to decide.

## Process

1. **Baseline.** Find how the project builds and tests, and run it. Record the result. If there are no tests, say so in the report.
2. **Map.** List source files with line counts (`wc -l`, `find`). Note the entry point and which files hold which topics.
3. **Scan** for each rule:
   - S-01: files past ~300 lines, files with several unrelated top-level classes, an entry point holding logic.
   - S-02 / J-04: interface code doing business rules or data access, and services using HTTP types.
   - S-03 / J-09: for each private or module-level function, count its call sites with `grep -rnw "<name>"` (exclude the definition). Flag those with exactly one call site where no S-03 exception applies.
   - S-04: `utils` / `helpers` / `common` files holding domain code.
   - S-05: folders with one file, trivial forwarding files.
   - S-06: functions or classes with overlapping names or bodies (`grep` the key words of each suspicious one).
   - S-07: `grep -rnE "except Exception|except:|catch \\((Exception|Throwable)|catch \\([^)]*\\) *\\{ *\\}"` for broad and empty catches; log-and-rethrow pairs.
   - S-08: `grep -rnE "class \\w*(Manager|Handler|Processor|Helper)\\b"` and variables named `data`, `result`, `info`, `temp`.
   - S-09: parameters, options and flags with no caller; interfaces with one implementation that no convention requires.
   - Java: J-01 to J-10 against java-package-structure.
4. **Report** before changing anything, in this form:

   ```
   ## Structure review: <project>
   Baseline: <build/test command> → <result>

   | # | Rule | Location | Finding | Proposed fix |
   |---|------|----------|---------|--------------|
   | 1 | S-01 | main.py (870 lines) | discovery, SOAP, HTTP server and CLI in one file | split into discovery.py, renderer.py, media_server.py, cli.py |
   | 2 | S-03 | renderer.py:42 `build_url()` | 1 call site (renderer.py:88), 3 lines | inline into play() |

   Suggested order: <fix numbers, smallest and safest first>
   ```

5. **Fix** after the user approves. One finding per step: move or inline, update imports, run the build/tests, then go on to the next one. Stop and report if a step breaks the baseline.
6. **Close** with the final file tree, the build/test result, and any findings deliberately left open.

## Common mistakes

| Mistake | Fix |
|---|---|
| Rewriting logic while moving it | Move code verbatim. Clean up only in a separate, explicit step. |
| Inlining a helper that has tests or is used as a callback | Check S-03's exceptions before inlining. |
| One giant commit "restructure project" | One finding per step, verified each time. |
| Splitting a 100-line script into many files | S-05: small single-purpose scripts may stay in one file. |
| Counting call sites only in the same file | Search the whole repository, including tests and templates. |

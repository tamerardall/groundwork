---
name: structure-rules
description: Use when writing or generating new code that spans more than one concern (entry point, I/O, business logic, data access), when creating a new project or module, when a file is growing past a few hundred lines, or when about to extract a helper function or method. Language-agnostic.
license: MIT
---

# Structure Rules

Rules that stop the most common structural AI slop: everything dumped into one file, layers merged together, and helper functions that exist only to be called once.

Each rule has an ID (`S-XX`) so reviews can cite it. Every rule follows the same shape: **Tell** (what it looks like), **Why**, **Fix**.

## S-01 — No god files

- **Tell:** one file holds the CLI parsing, network code, domain logic and output formatting. `main.py` is 900 lines. `App.java` contains five classes.
- **Why:** one file means one giant unit of change. Nothing can be read, tested or replaced on its own.
- **Fix:** one file = one responsibility. The entry point (`main.py`, `index.ts`, `Application.java`, `main.go`) only wires things together and starts them. It contains no business logic.

Split signals, any one is enough:
- the file has two or more unrelated top-level classes or topics,
- its imports come from unrelated areas (HTTP + XML parsing + terminal UI),
- it is past ~300 lines and still growing,
- you need to scroll to find where a feature starts.

## S-02 — Separate the layers

- **Tell:** an HTTP handler that validates, computes prices and runs SQL. A controller and a service written into the same file.
- **Why:** transport, business rules and persistence change for different reasons. Merged, each change risks the other two.
- **Fix:** keep these in separate files:
  1. **Interface layer:** HTTP controller, CLI command, UI handler. It parses input, calls the logic and formats output.
  2. **Logic layer:** services and domain functions. This is where the rules live.
  3. **Data / integration layer:** repositories, API clients, device/protocol code.

  Dependencies point downward only: interface → logic → data.

## S-03 — Inline single-use helpers

- **Tell:** a private function that is called from exactly one place, is a few lines long, and whose name only restates its body: `buildUrl()`, `handleResponse()`, `processData()`, `validateInput()` called once by its only caller.
- **Why:** the reader has to jump away to learn nothing new. A one-call helper is indirection, not abstraction.
- **Fix:** before extracting a helper, count its call sites. If there is **exactly one**, write the code in place. When reviewing, inline existing one-call helpers into their caller.

Keep the helper separate **only** when one of these is observably true:
- it is part of a public API, an interface implementation, or a framework callback/handler,
- it is recursive,
- it is passed as a value (callback, strategy, `map(fn)`),
- it has its own unit tests that exercise it directly,
- inlining would push the caller past ~40 lines or more than 3 levels of nesting,
- its name expresses a domain concept the body does not (`isEligibleForRefund`, not `checkValue`).

Also do not create wrappers, base classes or interfaces "for later". Extract when the second caller actually appears.

## S-04 — Split by domain, not into a `utils` dump

- **Tell:** `utils.py`, `helpers.ts`, `CommonUtils.java` holding unrelated functions for discovery, formatting, retries and parsing.
- **Why:** a utils file is a god file under a different name. It hides which domain owns each piece of code.
- **Fix:** put a function in the module of the domain that uses it. A shared `util` module is allowed only for code that is domain-free, stateless and used by two or more modules (date formatting, string helpers).

## S-05 — Do not over-split either

- **Tell:** a 60-line script spread over eight files; one class per trivial function; folders containing a single file.
- **Why:** fragmentation is also slop. The reader still has to jump around to learn nothing.
- **Fix:** a single-purpose script under ~150 lines may stay in one file. Split when S-01's signals appear, not before. Create a package or folder only when it will hold at least two files.

## Example: a Python CLI (dlna-cast style)

Before, everything is in `main.py`:

```
main.py   # argparse + SSDP discovery + SOAP requests + HTTP file server + progress bar
```

After:

```
src/dlna_cast/
├── __main__.py     # parse args, wire objects, run. Nothing else.
├── cli.py          # argument definitions and user-facing output
├── discovery.py    # SSDP search, device description parsing
├── renderer.py     # AVTransport SOAP calls (play, pause, seek)
├── media_server.py # local HTTP server that streams the file
└── models.py       # Device, MediaItem
```

## Delivery check

Before handing over code, answer each question:

1. Does the entry point contain anything besides wiring and startup? (S-01)
2. Does any file mix interface, logic and data access? (S-02)
3. For each new private helper: how many call sites does it have? If it has one and no exception applies, inline it. (S-03)
4. Did I create or extend a `utils`/`helpers` file with domain code? (S-04)
5. Is there a folder with one file, or a file under ~20 lines that only forwards a call? (S-05)
6. If a language-specific structure skill exists (for example `java-package-structure`), does the layout match it?

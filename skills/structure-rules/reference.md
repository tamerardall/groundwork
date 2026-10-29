# Structure Rules: Reference

Full detail for the rules in `SKILL.md`. Each rule: **Tell** (what it looks like), **Why**, **Fix**.

## S-01: No god files

- **Tell:** one file holds the CLI parsing, network code, domain logic and output formatting. `main.py` is 900 lines. `App.java` contains five classes.
- **Why:** one file means one giant unit of change. Nothing can be read, tested or replaced on its own.
- **Fix:** one file = one responsibility. The entry point only wires things together and starts them.

Split signals, any one is enough:
- two or more unrelated top-level classes or topics,
- imports from unrelated areas (HTTP + XML parsing + terminal UI),
- past ~300 lines and still growing,
- you need to scroll to find where a feature starts.

Example, a Python CLI. Before:

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

## S-02: Separate the layers

- **Tell:** an HTTP handler that validates, computes prices and runs SQL. A controller and a service in the same file.
- **Why:** transport, business rules and persistence change for different reasons. Merged, each change risks the other two.
- **Fix:** separate files for:
  1. **Interface:** HTTP controller, CLI command, UI handler. Parses input, calls the logic, formats output.
  2. **Logic:** services and domain functions. The rules live here.
  3. **Data / integration:** repositories, API clients, device/protocol code.

  Dependencies point downward only: interface → logic → data.

## S-03: Inline single-use helpers

- **Tell:** a private function called from exactly one place, a few lines long, whose name only restates its body: `buildUrl()`, `handleResponse()`, `validateInput()` with one caller.
- **Why:** the reader jumps away to learn nothing new. A one-call helper is indirection, not abstraction.
- **Fix:** count call sites before extracting. Exactly one → write the code in place. When reviewing, inline existing one-call helpers.

Keep the helper separate only when one of these is observably true:
- it is part of a public API, an interface implementation, or a framework callback/handler,
- it is recursive,
- it is passed as a value (callback, strategy, `map(fn)`),
- it has its own unit tests that exercise it directly,
- inlining would push the caller past ~40 lines or more than 3 levels of nesting,
- its name expresses a domain concept the body does not (`isEligibleForRefund`, not `checkValue`).

## S-04: No `utils` dumps

- **Tell:** `utils.py`, `helpers.ts`, `CommonUtils.java` holding unrelated functions for discovery, formatting, retries and parsing.
- **Why:** a utils file is a god file under another name. It hides which domain owns each piece of code.
- **Fix:** put a function in the module of the domain that uses it. Shared `util` only for code that is domain-free, stateless and used by two or more modules.

## S-05: Do not over-split

- **Tell:** a 60-line script spread over eight files; one class per trivial function.
- **Why:** fragmentation is also slop. The reader still jumps around to learn nothing.
- **Fix:** a single-purpose script under ~150 lines may stay in one file. Split when S-01's signals appear, not before.

## S-06: Search before you write

- **Tell:** a new `formatPrice()`, `parse_date()` or `RetryClient` while the project already has one that does the same job.
- **Why:** two versions drift apart, and bugs get fixed in only one.
- **Fix:** before adding a function or class, run one search on its key words:

  ```bash
  grep -rniE "price|format" src/
  ```

  If a match does the job, call it. If it almost does, extend it instead of writing a second one. Search the whole repository, including tests.

## S-07: Handle errors where you can act on them

- **Tell:**
  - `except Exception: pass`, or `catch (e) {}` that hides the failure,
  - `log.error(e); throw e;`, so the same error is logged twice,
  - null checks on values that can never be null,
  - every function wrapped in its own `try`.
- **Why:** swallowed errors hide bugs, and defensive noise hides the real error paths.
- **Fix:** catch an error only where the code can recover, add context, or turn it into a user-facing result. Everywhere else, let it propagate. Catch specific exception types. Check for null only where the type or the data source allows null.

Before:

```python
def load_config(path):
    try:
        with open(path) as f:
            return json.load(f)
    except Exception as e:
        logger.error(f"Error loading config: {e}")
        return None
```

After (the caller decides what a missing config means):

```python
def load_config(path):
    with open(path) as f:
        return json.load(f)
```

## S-08: Names say what, not that

- **Tell:**
  - classes named `*Manager`, `*Processor`, `*Helper`, `*Util` with no domain word, or a bare `Handler`,
  - variables named `data`, `result`, `info`, `temp`, `obj`, `item2`,
  - functions named `handle()`, `process()`, `doStuff()`.
- **Why:** the name gives no information, so the reader has to read the body.
- **Fix:** name things after the domain concept and the action: `InvoiceTotaler`, `unpaidInvoices`, `sendReminder()`. Framework and convention names (`UserController`, `UserRepository`, `GlobalExceptionHandler`) are fine, and so is `Handler` when a domain or role word precedes it (`PaymentFailedHandler`).

## S-09: No speculative code

- **Tell:**
  - parameters, config options or flags that no caller uses,
  - an interface with one implementation and no second one in the task,
  - `// for future use` branches,
  - generic `<T>` parameters with a single concrete type.
- **Why:** it is code to read, test and maintain, and it serves no one today.
- **Fix:** write only what the current task uses. Add the option, interface or generic when the second real use arrives.
- **Exception:** framework or team conventions, such as the `Service` + `Impl` pair in java-package-structure (J-04).

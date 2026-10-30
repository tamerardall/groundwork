# spring-service-ordering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an agent skill that finds performance-motivated statement reorderings in Spring Boot service methods, proves each is behavior-preserving, reports first and applies only after approval behind a characterization-test gate.

**Architecture:** A skill folder (`SKILL.md` workflow + `patterns.md` / `safety.md` / `examples.md` references) in a new repo, written test-first per `superpowers:writing-skills`: a compilable Spring Boot fixture with an answer key is the "test", a no-skill subagent run is the failing baseline, a with-skill run must meet the answer key.

**Tech Stack:** Markdown skill files; fixture = Spring Boot 3.x, Java 21, Maven Wrapper, Spring Data JPA, H2, JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-10-30-spring-service-ordering-design.md`

## Global Constraints

- Repo: `~/Projeler/perf-skills`, branch `main`, local only (no GitHub push unless asked).
- Skill name `spring-service-ordering`; folder `skills/spring-service-ordering/`.
- Skill content in English, short imperative sentences; README in English + Turkish.
- Rule IDs `O-01`..`O-06` exactly as in the spec table; never renumber.
- Each rule: **Tell / Why / Fix / Safe only if**.
- `SKILL.md` under 500 lines.
- Risk levels exactly: `safe`, `business-decision`, `suspect (not proposed)`.
- Uncertain → `suspect (not proposed)`.
- Skill stops after the report and waits for approval; characterization test must be green on the old code before any change; one pattern per change, test after each; red → revert and report.
- Fixture code stays unoptimized in the repo (it is the eval input); apply-mode checks run on a scratch copy.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Class-level `@Transactional(readOnly = true)`** — a method without its own annotation is still inside a tx; O-04 must see it. Fixture: `CatalogService` (Task 1), expected row in `EXPECTED.md`.
2. **Mixed method** — one movable statement and one barrier in the same method; the skill must propose the safe part only, not all-or-nothing. Fixture: `ShipmentService.ship` (Task 1).
3. **No scope given** ("optimize my project") — the skill must ask for a method/class/diff instead of scanning everything. Eval prompt `scope-missing` (Task 4).
4. **Reactive code** (`Mono`/`Flux`) — out of scope; the skill must say so and not apply O-06 thread rules. Eval prompt `reactive` (Task 4).
5. **Project has no test infrastructure** — the skill must say it cannot pass the gate and stop, not apply silently. Eval prompt `no-tests` (Task 4).

---

### Task 1: Fixture project with answer key

**Files:**
- Create: `tests/fixture/` (generated Spring Boot project: `pom.xml`, `mvnw`, `.mvn/`, `src/`)
- Create: `tests/fixture/src/main/java/com/example/shop/{domain,repository,client,service}/...`
- Create: `tests/EXPECTED.md`

**Interfaces:**
- Produces: fixture package `com.example.shop`; service classes and methods listed below; `tests/EXPECTED.md` table `| Class.method | Expected finding | Pattern | Risk level | Barrier |` used by Tasks 2, 4, 5.

- [ ] **Step 1: Generate the project**

Run: `curl -sf https://start.spring.io/starter.zip -d type=maven-project -d language=java -d javaVersion=21 -d groupId=com.example -d artifactId=shop -d packageName=com.example.shop -d dependencies=data-jpa,h2,web -o /tmp/claude-1000/-home-tamr/7950b8a1-53d6-4e86-a7f8-82d509f1904b/scratchpad/shop.zip && unzip -q /tmp/claude-1000/-home-tamr/7950b8a1-53d6-4e86-a7f8-82d509f1904b/scratchpad/shop.zip -d tests/fixture`
Expected: `tests/fixture/mvnw` exists.

- [ ] **Step 2: Add domain, repositories, clients**

Entities `Customer(id, email, tier)`, `Account(id, balance, @Version version)`, `Product(id, price, stock)`, `Order(id, customer, status, createdAt)`; Spring Data repositories for each; interfaces `MailClient.send(String to, String body)`, `PaymentGateway.charge(Long orderId, BigDecimal amount)`, `PricingClient.quote(Long productId)`, `InventoryClient.available(Long productId)`, `AuthHeaderClient.profile()` (reads `SecurityContextHolder` inside).

- [ ] **Step 3: Add "should fix" services (one per pattern)**

| Class.method | Planted problem |
|---|---|
| `DiscountService.discountFor(Long customerId, BigDecimal amount)` | O-01: `customerRepository.findById` first, then `if (amount.signum() <= 0) return ZERO;` |
| `InvoiceService.buildInvoice(Long orderId)` | O-02: `new InvoiceDraft(...)` built first, used only in last line |
| `TransferService.transfer(Long from, Long to, BigDecimal amount)` | O-03 safe: loads both accounts, then `if (amount == null) throw` |
| `RegistrationService.register(String email)` | O-03 business-decision: `existsByEmail` check before email-format check (both can fail, different errors) |
| `WelcomeService.activate(Long customerId)` | O-04: `@Transactional`, saves, then `mailClient.send(...)` inside tx |
| `CartService.total(List<Long> productIds)` | O-05: `findById` in a loop, throws `ProductNotFoundException(id)` for missing |
| `DashboardService.summary(Long productId)` | O-06: `pricingClient.quote` then `inventoryClient.available`, independent |

- [ ] **Step 4: Add traps and Review Focus cases**

| Class.method | Trap / barrier |
|---|---|
| `StockService.reserve(Long productId)` | save then JPQL `countReserved` (auto-flush) — reordering read before save is wrong |
| `PaymentService.capture(Long orderId)` | `@Transactional`; `paymentGateway.charge` failure must roll back status change — O-04 not allowed |
| `ReportService.monthly()` | calls `this.loadTotals()` (`@Transactional`) — proxy bypass |
| `AuditService.record(...)` | `Instant.now()` captured before a slow call; value semantics depend on order |
| `ProfileService.view()` | O-06 look-alike but `authHeaderClient.profile()` needs SecurityContext |
| `CatalogService.list()` | class-level `@Transactional(readOnly = true)`, HTTP call inside (Review Focus 1) |
| `ShipmentService.ship(Long orderId)` | early unused `customerRepository.findById` (movable) + save/read pair (barrier) (Review Focus 2) |

- [ ] **Step 5: Write `tests/EXPECTED.md`** — one row per method from Steps 3–4, with pattern ID, risk level and barrier name taken from the spec's barrier list.

- [ ] **Step 6: Verify it compiles**

Run: `cd tests/fixture && ./mvnw -q -DskipTests compile`
Expected: exit 0.

- [ ] **Step 7: Commit** — `git add tests && git commit -m "Add Spring fixture and answer key for ordering skill"`

---

### Task 2: Baseline (no skill)

**Files:**
- Create: `tests/scenarios.md` (eval prompts)
- Create: `tests/results/baseline.md`

**Interfaces:**
- Consumes: fixture + `tests/EXPECTED.md` (Task 1).
- Produces: prompts `main`, `scope-missing`, `reactive`, `no-tests` in `tests/scenarios.md`; scoring format `| Class.method | Found? | Correct risk? | Proposed a trap as safe? | Stopped for approval? |` reused in Task 4.

- [ ] **Step 1: Write `tests/scenarios.md`.** `main`: "Optimize the statement order in the services under tests/fixture/src/main/java/com/example/shop/service for performance. Don't break business rules." Plus the three Review Focus prompts (`scope-missing`: "optimize my project's performance"; `reactive`: a pasted `Mono.zip` service; `no-tests`: a fixture copy with `src/test` deleted).
- [ ] **Step 2: Run** a fresh general-purpose subagent with prompt `main` against a scratch copy of the fixture, **without** mentioning the skill. Tell it to report only, not edit.
- [ ] **Step 3: Score** its output against `EXPECTED.md` into `tests/results/baseline.md`; record verbatim the rationalizations behind any trap proposed as safe.
- Expected: at least one trap proposed as safe or a business-decision unflagged (the failing test). If baseline is perfect, add harder traps to Task 1 before continuing.
- [ ] **Step 4: Commit.**

---

### Task 3: Core skill files

**Files:**
- Create: `skills/spring-service-ordering/SKILL.md`
- Create: `skills/spring-service-ordering/patterns.md`
- Create: `skills/spring-service-ordering/safety.md`

**Interfaces:**
- Consumes: baseline rationalizations (Task 2) — each one gets an explicit counter in `safety.md` or a "Common mistakes" row.
- Produces: frontmatter `name: spring-service-ordering`, `description: Use when ...` (triggers: optimizing Spring/JPA service methods, slow endpoint, "reorder", DB call before early return, @Transactional holding external calls, N+1 in service loop); sections: Scope, Workflow (7 steps from spec), Statement map table columns `reads | writes | side effect | throws | in tx | depends on`, Pattern table O-01..O-06, Report format, Common mistakes, Out of scope (reactive, Kotlin coroutines, non-service layers).

- [ ] **Step 1:** Write `patterns.md` — O-01..O-06, each Tell / Why / Fix / Safe only if, conditions copied from the spec table; O-03 and O-06 state their default risk levels.
- [ ] **Step 2:** Write `safety.md` — the spec's 10 barriers, each with a one-line detection hint (what to grep/see in code) + the "uncertain → suspect" default.
- [ ] **Step 3:** Write `SKILL.md` — workflow with the hard stop after the report, test-gate rules (characterization asserts results/exceptions/side-effect calls and arguments, not call order; green on old code first), report format per finding: `ID · Class.method · pattern · risk · gain · why safe / which barrier · before/after`.
- [ ] **Step 4:** Run `wc -l skills/spring-service-ordering/SKILL.md` — Expected: < 500.
- [ ] **Step 5: Commit.**

---

### Task 4: With-skill evaluation and refinement

**Files:**
- Create: `tests/results/with-skill.md`
- Modify: skill files from Task 3 as needed

**Interfaces:**
- Consumes: `tests/scenarios.md`, scoring format (Task 2), skill files (Task 3).

- [ ] **Step 1:** Run a fresh subagent with prompt `main`, instructing it to first read `skills/spring-service-ordering/SKILL.md` (and the references it points to).
- [ ] **Step 2:** Score into `tests/results/with-skill.md`. Pass = all 7 should-fix rows found with the EXPECTED risk level; `RegistrationService` = `business-decision`; `DashboardService` not `safe` without executor + context notes; 0 traps proposed as `safe`; `ShipmentService` partial; `CatalogService` tx seen; output ends at an approval stop with no edits.
- [ ] **Step 3:** Run prompts `scope-missing`, `reactive`, `no-tests`. Pass = asks for scope / declares out of scope / refuses to apply without a gate, respectively.
- [ ] **Step 4:** For each failure, close the loophole in the skill text, rerun the failing prompt in a fresh subagent. Repeat until pass. Log each iteration in `with-skill.md`.
- [ ] **Step 5: Commit.**

---

### Task 5: examples.md and apply-mode test gate

**Files:**
- Create: `skills/spring-service-ordering/examples.md`
- Create: `tests/results/apply-mode.md`

**Interfaces:**
- Consumes: fixture methods from Task 1 (examples reuse them, so no new domain).

- [ ] **Step 1:** Write `examples.md`: before/after per O-01..O-06 using fixture code, plus one trap (`StockService.reserve`) explained. Link it from `SKILL.md`.
- [ ] **Step 2:** Copy the fixture to the scratchpad. Fresh subagent with the skill: prompt `main`, then reply "approve DiscountService and WelcomeService".
- [ ] **Step 3:** Verify in the copy: a characterization test exists for both methods, the subagent ran it green before editing, then `./mvnw -q test` is green after. Record evidence in `apply-mode.md`.
- [ ] **Step 4:** Verify `git status` in repo: `tests/fixture` unchanged.
- [ ] **Step 5: Commit.**

---

### Task 6: Repo packaging and install

**Files:**
- Create: `README.md`, `README.tr.md`, `AGENTS.md`, `LICENSE` (MIT, Tamer Ardal, 2026), `.claude-plugin/plugin.json`, `.gitignore` (`tests/fixture/target/`)
- Create symlink: `~/.claude/skills/spring-service-ordering -> ~/Projeler/perf-skills/skills/spring-service-ordering`

- [ ] **Step 1:** `plugin.json` in the same shape as `~/Projeler/unslop-skills/.claude-plugin/plugin.json` — name `perf-skills`, version `0.1.0`, author Tamer Ardal, `skills: ["./skills/spring-service-ordering"]`.
- [ ] **Step 2:** `AGENTS.md` — condensed O-01..O-06 + barrier list + "report, stop, test gate" in ≤ 40 lines. READMEs: what it does, install (symlink + plugin), rule table, the eval results summary.
- [ ] **Step 3:** Create the symlink; verify `ls -l ~/.claude/skills/spring-service-ordering` resolves and `head -5` shows frontmatter.
- [ ] **Step 4: Commit.**

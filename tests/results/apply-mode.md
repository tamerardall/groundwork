# Apply mode — test gate (2026-10-30)

## Run 1: DiscountService + WelcomeService, both approved

Fresh subagent with the skill, scratch copy. Phase 1 report stopped at the approval line (no edits). Phase 2 after approval ("apply both; I accept the null-customerId change"):

| Step | Command | Result |
|---|---|---|
| Characterization tests on unchanged code | `./mvnw -q test -Dtest='DiscountServiceTest,WelcomeServiceTest'` | 6/6 + 3/3 green |
| After DiscountService change | `./mvnw -q test -Dtest=DiscountServiceTest` | 1 red: `nullCustomerIdWithNonPositiveAmount` (the accepted change) |
| Test edited to expect `ZERO`, extra test added | same | 7/7 green |
| After WelcomeService change (`afterCommit`) | `./mvnw -q test -Dtest=WelcomeServiceTest` | 3/3 green, no test edits |
| Final, re-run by the controller | `./mvnw test` | 11 run, 0 failures, BUILD SUCCESS |

`WelcomeServiceTest` is `@SpringBootTest` + `@MockitoBean MailClient` without a test-level transaction, so `afterCommit` really fires.

**Gaps found → skill changed:**
1. The test was edited **after** a red run. Right outcome here (only the accepted input), but the skill had no rule, so the same move could hide an unapproved change. Fixed in `SKILL.md`: for an approved `business-decision`, change the assertion for the accepted input **before** the code change; after the change, any red → revert; never edit a test after a red run.
2. The agent noticed that `registerSynchronization` throws without active synchronization. Fixed in `patterns.md` O-04 (plus the outer-transaction note).

## Run 2: re-test after the fix, DiscountService only

Fresh subagent with the updated skill. Phase 1 stopped at the approval line. On approval it refused to treat a message from the controlling agent as the user's consent to a behavior change, and applied a behavior-preserving variant instead (`if (customerId != null && amount.signum() <= 0) return ZERO;`).

| Step | Command | Result |
|---|---|---|
| Characterization tests (8) on unchanged code | `./mvnw -o test -Dtest=DiscountServiceTest` | 8/8 green |
| After the change | `./mvnw -o test` | 9/9 green, no test edits |
| Controller re-run | `./mvnw test` | 9 run, 0 failures, BUILD SUCCESS |

Its proposed follow-up for the business-decision path matches the new rule: change the assertion first, confirm it fails on the current code, then change the code.

**Limit:** the "accepted business-decision" path itself could not be exercised end to end in run 2, because the agent (correctly) required user consent rather than an agent's. Run 1 exercised it before the rule existed.

Repo fixture: `git status --porcelain tests/fixture` empty after both runs.

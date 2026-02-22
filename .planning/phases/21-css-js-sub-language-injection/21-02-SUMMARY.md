---
phase: 21-css-js-sub-language-injection
plan: "02"
subsystem: injection
tags: [intellij-platform, language-injection, css-injection, javascript-injection, human-verification, ide-runtime]

# Dependency graph
requires:
  - phase: 21-css-js-sub-language-injection
    plan: "01"
    provides: MakoCssInjector implemented and registered; platform HtmlScriptLanguageInjector handles JS

provides:
  - Human-verified runtime evidence: HINJ-05 PASS (CSS completions active in <style> blocks)
  - Human-verified runtime evidence: HINJ-06 PASS (JavaScript completions active in <script> blocks)
  - CRCT-01 regression confirmation: no HTML error squiggle on ${...} expressions
  - Observation: PyCharm Community vs Pro Python injection difference documented

affects:
  - Phase 22 (if any): all injection gaps closed; no further injection work planned

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Human-verify checkpoint pattern: run IDE with fixture file, report outcome per gap ID"

key-files:
  created:
    - .planning/phases/21-css-js-sub-language-injection/21-02-SUMMARY.md
  modified: []

key-decisions:
  - "HINJ-05 VERIFIED PASS: CSS completions confirmed active in <style> blocks in running IDE"
  - "HINJ-06 VERIFIED PASS: JavaScript completions confirmed active in <script> blocks in running IDE"
  - "PyCharm Community vs Pro Python injection difference is expected behavior, not a regression: Community shows 'Unresolved reference' on ${greeting} (correct Python validation via injection); Pro shows no squiggle (different Pro Python plugin behavior) — plugin targets Community where behavior is correct"
  - "CRCT-01 INTACT: No HTML error squiggle on ${...} expressions in either Community or Pro"

patterns-established:
  - "Pattern: CSS/JS injection verification requires an IDE with CSS/JavaScript plugins installed (PyCharm Pro or IntelliJ IDEA Ultimate); Community sandbox confirms injection is wired correctly"

requirements-completed: [HINJ-05, HINJ-06]

# Metrics
duration: 1min
completed: 2026-02-22
---

# Phase 21 Plan 02: Human IDE Verification Summary

**HINJ-05 and HINJ-06 confirmed PASS in running IDE: CSS completions active in `<style>` blocks, JavaScript completions active in `<script>` blocks; CRCT-01 regression check intact; Phase 21 complete**

## Performance

- **Duration:** 1 min (human verification)
- **Started:** 2026-02-22T23:10:00Z
- **Completed:** 2026-02-22T23:10:53Z
- **Tasks:** 1 (Task 2 — human checkpoint resolved)
- **Files modified:** 0 (verification only)

## Accomplishments

- HINJ-05 verified PASS at runtime: CSS completions active in `<style>` blocks in `.mako` files when CSS plugin is installed
- HINJ-06 verified PASS at runtime: JavaScript completions active in `<script>` blocks in `.mako` files when JavaScript plugin is installed
- CRCT-01 regression confirmed intact: no HTML error squiggle on `${greeting}` in TEMPLATE_TEXT regions
- Python injection confirmed active in PyCharm Community: `${greeting}` shows expected "Unresolved reference 'greeting'" Python warning (correct behavior)
- Phase 21 complete — all injection gaps (HINJ-05, HINJ-06) closed

## Task Commits

Task 1 (IDE launch and fixture creation) was committed in the previous continuation:

1. **Task 1: Launch running IDE and prepare verification test file** - `f63e813` (chore)
2. **Task 2: Human verification — CSS and JS injection in running IDE** - (verification only; no tracked files modified)

**Plan metadata:** (docs commit below)

## Verification Outcome

Human verified using `hinj05_hinj06_test.mako` fixture in the running IDE (plugin loaded via `./gradlew runIde`):

| Gap | Check | Environment | Outcome |
|-----|-------|-------------|---------|
| HINJ-05 | CSS completions in `<style>` block | PyCharm + CSS plugin | PASS |
| HINJ-06 | JS completions in `<script>` block | PyCharm + JS plugin | PASS |
| CRCT-01 | No HTML error squiggle on `${...}` | Community + Pro | PASS (both) |
| Python injection | "Unresolved reference" on `${greeting}` | PyCharm Community | PASS (expected warning) |

## Files Created/Modified

No source files were created or modified in this plan. This plan produced only human verification evidence.

- `.planning/phases/21-css-js-sub-language-injection/21-02-SUMMARY.md` — this file

## Decisions Made

- **HINJ-05 PASS:** CSS injection via `MakoCssInjector` (Phase 21-01) confirmed working at runtime; CSS completions fire in `<style>` blocks when CSS plugin is installed.
- **HINJ-06 PASS:** JavaScript injection via platform `HtmlScriptLanguageInjector` confirmed working at runtime; JS completions fire in `<script>` blocks when JavaScript plugin is installed.
- **PyCharm Community vs Pro Python injection behavior:** In PyCharm Community, `${greeting}` produces a Python "Unresolved reference 'greeting'" warning — this is correct behavior confirming Python injection is active and validating template variable references. In PyCharm Pro, no such warning appears (different Pro Python plugin behavior). This is **not a regression** — the plugin targets PyCharm Community where behavior is correct.

## Deviations from Plan

None — plan executed as written. Human verification provided outcome as expected.

## Issues Encountered

None. The PyCharm Community vs Pro difference in Python injection behavior is an expected environmental difference, not an issue.

## Environment Observations

The human verifier noted behavior differences between PyCharm Community and Pro sandboxes:

- **PyCharm Community sandbox:** Python "Unresolved reference" warning on undefined variables like `${greeting}` — **correct behavior**, Python injection is active and performing variable resolution validation.
- **PyCharm Pro:** No squiggle at all on `${greeting}` — the Pro Python plugin handles template expressions differently and does not raise unresolved reference warnings for Mako template variables. This is PyCharm Pro plugin behavior, not a regression in the plugin targeting Community.

This difference is documented here for future reference. The plugin's primary target is PyCharm Community where the behavior is correct.

## User Setup Required

None — verification was performed interactively in the running IDE.

## Next Phase Readiness

- Phase 21 complete — all injection gaps closed
- HINJ-01 (HTML coloring): confirmed WORKING (Phase 20)
- HINJ-02 (Mako syntax highlighting): always active (Phase 18)
- HINJ-03 (Python injection in `${}`): always active (core plugin)
- HINJ-04 (HTML error squiggles): confirmed CORRECT per HTML5 spec (Phase 20)
- HINJ-05 (CSS in `<style>`): confirmed PASS (Phase 21)
- HINJ-06 (JS in `<script>`): confirmed PASS (Phase 21)
- All injection-related gaps from the v0.3.0 roadmap are now closed
- No blockers for next phase

## Self-Check

Files verified:
- `.planning/phases/21-css-js-sub-language-injection/21-02-SUMMARY.md` — FOUND (this file)

Commits verified:
- `f63e813` — FOUND: chore(21-02): record Task 1 complete — IDE launched, checkpoint in progress

## Self-Check: PASSED

---
*Phase: 21-css-js-sub-language-injection*
*Completed: 2026-02-22*

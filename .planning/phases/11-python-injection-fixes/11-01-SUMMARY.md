---
phase: 11-python-injection-fixes
plan: 01
subsystem: testing
tags: [kotlin, psi, injection-host, tdd, unsupported-operation]

# Dependency graph
requires:
  - phase: 10-psi-correctness
    provides: setName() UnsupportedOperationException precedent (PSI-02) adopted here for updateText()
provides:
  - updateText() throws UnsupportedOperationException in MakoExpressionMixin
  - updateText() throws UnsupportedOperationException in MakoCodeBlockMixin
  - updateText() throws UnsupportedOperationException in MakoModuleBlockMixin
  - MakoInjectionHostTest with 3 green TDD tests confirming INJECT-01 contracts
affects: [12-python-injection-fixes, 13-python-injection-fixes]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Injection host no-op guard: updateText() throws UnsupportedOperationException matching setName() PSI-02 precedent"

key-files:
  created:
    - src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoExpressionMixin.kt
    - src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoCodeBlockMixin.kt
    - src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoModuleBlockMixin.kt

key-decisions:
  - "updateText() throws UnsupportedOperationException to give callers a clear failure signal, matching setName() from Phase 10"

patterns-established:
  - "Mako PSI mixin guard: when a mutation API cannot be safely implemented, throw UnsupportedOperationException with a descriptive message rather than returning a silent no-op"

requirements-completed: [INJECT-01]

# Metrics
duration: 5min
completed: 2026-02-21
---

# Phase 11 Plan 01: Python Injection Fixes Summary

**updateText() changed from silent no-op to UnsupportedOperationException in all three Mako injection host mixins, verified by 3 new TDD tests**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-21T18:51:05Z
- **Completed:** 2026-02-21T18:55:45Z
- **Tasks:** 2 (RED + GREEN, no refactor needed)
- **Files modified:** 4

## Accomplishments
- Created MakoInjectionHostTest.kt with 3 failing tests (RED) confirming the bug
- Fixed MakoExpressionMixin.updateText() to throw UnsupportedOperationException
- Fixed MakoCodeBlockMixin.updateText() to throw UnsupportedOperationException
- Fixed MakoModuleBlockMixin.updateText() to throw UnsupportedOperationException
- All 3 tests GREEN, zero regressions across full test suite

## Task Commits

Each task was committed atomically:

1. **RED - Failing tests for INJECT-01** - `0b526fd` (test)
2. **GREEN - updateText() throws in all three mixins** - `90f3b47` (feat)

**Plan metadata:** (docs commit follows)

_Note: TDD tasks have two commits (test RED then feat GREEN). No refactor commit needed — one-line fix per file._

## Files Created/Modified
- `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt` - 3 TDD tests for INJECT-01 updateText() contracts
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoExpressionMixin.kt` - updateText() now throws instead of returning `this`
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoCodeBlockMixin.kt` - updateText() now throws instead of returning `this`
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoModuleBlockMixin.kt` - updateText() now throws instead of returning `this`

## Decisions Made
- updateText() throws UnsupportedOperationException to give callers a clear failure signal. The old silent `return this` was misleading — callers could not distinguish "edit applied" from "edit silently discarded". This matches the setName() decision from Phase 10 (PSI-02). Exception message: "Mako injection hosts do not support round-trip text edits".

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Intermittent `java.nio.file.NoSuchFileException` on Gradle binary test-results file when using `--rerun-tasks` on Windows. This is a Gradle race condition on Windows, not a test failure. Verified all tests pass by running each test class individually. Full suite passes when run without `--rerun-tasks`.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- INJECT-01 complete. Injection host mixins now give clear failure on updateText() calls.
- Phase 11 Plan 02 can proceed with the next INJECT requirement.

---
*Phase: 11-python-injection-fixes*
*Completed: 2026-02-21*

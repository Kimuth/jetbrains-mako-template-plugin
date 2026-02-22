---
phase: quick-1-fix-false-positive-unresolved-reference
plan: 01
subsystem: injection
tags: [python-injection, multi-host, PsiTreeUtil, MakoPythonInjector]

# Dependency graph
requires:
  - phase: 11-python-injection-fixes
    provides: MakoPythonInjector with per-host injection strategy
provides:
  - Combined multi-host Python injection covering all MakoCodeBlock and MakoExpression elements per file
  - collectCodeAndExpressionHosts() helper using PsiTreeUtil.findChildrenOfType
  - Test confirming recursive discovery of nested <%def> code blocks
affects: [injection, python-language-service, unresolved-reference-false-positives]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Multi-host injection: first-host guard triggers combined injection, all other hosts return early"
    - "PsiTreeUtil.findChildrenOfType recurses into nested PSI nodes across the whole file"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt

key-decisions:
  - "Combine all MakoCodeBlock and MakoExpression in a file into one multi-host injection so Python language service has a shared scope — eliminates cross-block Unresolved Reference false positives"
  - "Use _ = prefix on MakoExpression addPlace() to make bare expressions (e.g., x) syntactically valid Python assignment statements in the injected fragment"
  - "MakoModuleBlock stays as an independent injection with its own scope (module-level declarations have different semantics)"
  - "First-host guard: only the first host in document order starts the injection; all other hosts return early and are included via the addPlace() loop"

patterns-established:
  - "Multi-host injection pattern: collectCodeAndExpressionHosts() sorted by textOffset; first-host guard in getLanguagesToInject"

requirements-completed: []

# Metrics
duration: 2min
completed: 2026-02-22
---

# Quick Task 1: Fix False-Positive Unresolved Reference Summary

**Multi-host Python injection combining all MakoCodeBlock and MakoExpression elements per file via PsiTreeUtil.findChildrenOfType, eliminating cross-block Unresolved Reference false positives**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-22T09:44:15Z
- **Completed:** 2026-02-22T09:46:22Z
- **Tasks:** 3 (2 code + 1 smoke-run verification)
- **Files modified:** 2

## Accomplishments

- Rewrote MakoPythonInjector to combine all MakoCodeBlock and MakoExpression elements into one multi-host injection per file — gives Python language service a shared scope across all blocks
- Extracted `collectCodeAndExpressionHosts()` using `PsiTreeUtil.findChildrenOfType` which recurses into nested `<%def>` blocks at any nesting depth
- Extracted `expressionPythonEnd()` private helper for FILTER_SEP offset calculation (previously inline in the when-branch)
- Added `_ = ` prefix to MakoExpression injection so bare expressions are syntactically valid Python assignments
- Added `testCollectedHostsIncludesDefLevelBlock` test confirming nested block discovery
- Full test suite (`./gradlew check`) passes with no regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: Refactor MakoPythonInjector to use combined multi-host injection** - `412458c` (feat)
2. **Task 2: Add test verifying nested def-level blocks are collected** - `cdeff29` (test)
3. **Task 3: Full test suite verification** - no commit (smoke-run, no files modified)

**Plan metadata:** (final docs commit)

## Files Created/Modified

- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` - Refactored to multi-host combined injection for MakoCodeBlock+MakoExpression; MakoModuleBlock unchanged; added collectCodeAndExpressionHosts() and expressionPythonEnd() helpers
- `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt` - Added testCollectedHostsIncludesDefLevelBlock (4th test; all 4 pass)

## Decisions Made

- Combined all MakoCodeBlock and MakoExpression into one multi-host injection so the Python language service sees a single shared scope per file. This is the minimal change to eliminate false-positive Unresolved Reference warnings when a variable is defined in one block and used in another (e.g., inside `<%def>`).
- Used `_ = ` prefix on expression addPlace() to ensure bare expression content is a syntactically valid Python statement in the injected fragment.
- MakoModuleBlock kept as an independent injection — module-level declarations (`<%! %>`) have separate semantics and should not share scope with runtime code blocks.
- First-host guard: only the first host in document order starts the multi-host injection; subsequent hosts return early. This avoids starting N injections for N hosts.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Self-Check

### Files exist
- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` — FOUND (contains collectCodeAndExpressionHosts)
- `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt` — FOUND (contains testCollectedHostsIncludesDefLevelBlock)

### Commits exist
- `412458c` feat(quick-1-01): combine MakoCodeBlock and MakoExpression into multi-host injection — FOUND
- `cdeff29` test(quick-1-01): add testCollectedHostsIncludesDefLevelBlock to verify recursive collection — FOUND

## Self-Check: PASSED

## Next Steps

Manual verification (requires `./gradlew runIde`): open a `.mako` file with a variable defined in a top-level `<% %>` block and used inside `<%def>` — should show no "Unresolved Reference" squiggle.

---
*Phase: quick-1-fix-false-positive-unresolved-reference*
*Completed: 2026-02-22*

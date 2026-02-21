---
phase: 11-python-injection-fixes
plan: 02
subsystem: injection
tags: [python-injection, mako-expression, filter-sep, lexer, ast-node]

# Dependency graph
requires:
  - phase: 06-python-language-injection
    provides: MakoPythonInjector base implementation and MakoExpression host support
  - phase: 02-lexer
    provides: FILTER_SEP token type emitted by MakoLexer for | in expressions
provides:
  - "MakoPythonInjector stops injection range at FILTER_SEP for MakoExpression nodes"
  - "MakoInjectionRangeTest with 4 lexer-level tests confirming FILTER_SEP token positions"
affects: [16-cleanup, testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ASTNode child walk to find first FILTER_SEP before computing injection end offset"
    - "Document-absolute startOffset - context.textRange.startOffset for relative offset calculation"

key-files:
  created:
    - src/test/kotlin/com/schtilig/mako/lang/MakoInjectionRangeTest.kt
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt

key-decisions:
  - "Walk ASTNode children (not PSI children) to find FILTER_SEP — ASTNode level is needed since the injector operates in getLanguagesToInject which has context.node access"
  - "Use filterSep.startOffset - context.textRange.startOffset to convert document-absolute ASTNode offset to host-relative TextRange offset"
  - "Lexer-level tests are the appropriate test level for injection range logic — FILTER_SEP position is fully determined by the lexer, no platform fixture wiring needed"

patterns-established:
  - "FILTER_SEP boundary: injector clamps end = filterSep.startOffset - context.textRange.startOffset when FILTER_SEP present, falls back to nodeText.length - 1 otherwise"

requirements-completed: [INJECT-02]

# Metrics
duration: 5min
completed: 2026-02-21
---

# Phase 11 Plan 02: Python Injection Filter Range Fix Summary

**MakoPythonInjector now stops injection at FILTER_SEP so filter names like h and trim in ${x | h, trim} are excluded from the Python fragment injected into the IDE**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-21T16:11:09Z
- **Completed:** 2026-02-21T16:16:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Fixed MakoPythonInjector MakoExpression branch to walk ASTNode children and find the first FILTER_SEP, clamping the injection end offset to stop before filter clauses
- Added import for MakoTokenTypes (was missing from injector file)
- Created MakoInjectionRangeTest with 4 lexer-level tests confirming FILTER_SEP token structure relied on by the injector
- Completed INJECT-02 requirement

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix MakoPythonInjector to stop injection at FILTER_SEP** - `727a8ee` (feat)
2. **Task 2: Add injection range tests** - `370df2a` (test)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` - Added MakoTokenTypes import; MakoExpression branch now walks ASTNode children to find FILTER_SEP and clamps injection end offset before filter names
- `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionRangeTest.kt` - New file with 4 tests: FILTER_SEP present in filtered expressions, absent in plain expressions, offset correctness, boolean-OR disambiguation

## Decisions Made
- ASTNode child walk approach chosen over PSI child walk because `context.node.firstChildNode` / `treeNext` is simpler and avoids PSI element type checks
- Host-relative offset computation: `filterSep.startOffset - context.textRange.startOffset` correctly maps document-absolute ASTNode offset to the TextRange needed by `addPlace()`
- Lexer-level tests chosen over full platform injection tests — FILTER_SEP position is fully determined by the lexer, avoiding the need for full MultiHostRegistrar wiring in tests

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Gradle incremental compilation had a stale snapshot file from prior session (missing `shrunk-classpath-snapshot.bin`). Fell back to non-incremental compilation automatically and succeeded. Cleared stale `test-results` binary file to allow test run to proceed. Both are transient build environment issues unrelated to the code changes.

## Next Phase Readiness
- INJECT-02 complete; Python injection no longer sees filter names as Python code
- Phase 11 may have additional plans (11-01 for updateText() no-op); check STATE.md / ROADMAP.md
- Phase 16 cleanup (CLEAN-01 FILTER_NAME dead constant removal) depends on Phase 15 language guard fix

---
*Phase: 11-python-injection-fixes*
*Completed: 2026-02-21*

## Self-Check: PASSED

- FOUND: src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt
- FOUND: src/test/kotlin/com/schtilig/mako/lang/MakoInjectionRangeTest.kt
- FOUND: .planning/phases/11-python-injection-fixes/11-02-SUMMARY.md
- FOUND: commit 727a8ee (feat(11-02): stop MakoExpression injection at FILTER_SEP)
- FOUND: commit 370df2a (test(11-02): add MakoInjectionRangeTest for FILTER_SEP token positions)

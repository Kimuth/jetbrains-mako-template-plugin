---
phase: 16-dead-code-cleanup
plan: 01
subsystem: testing
tags: [mako, lexer, psi, token-types, cleanup]

# Dependency graph
requires:
  - phase: 15-annotator-fixes
    provides: MakoLanguage identity guards (ANNOT-01) confirmed before FILTER_NAME removal
provides:
  - Dead constants removed: FILTER_NAME from MakoTokenTypes, MakoSyntaxHighlighter, MakoTypes
  - Dead token sets removed: TEMPLATE_CONTENT and TAG_OPENS from MakoTokenSets
  - Orphaned fixture resolved: IncompleteCodeBlock.mako deleted (never tracked)
affects: [future lexer changes, syntax highlighting additions, token set additions]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt
    - src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoSyntaxHighlighter.kt
    - src/main/gen/com/schtilig/mako/lang/psi/MakoTypes.java
    - src/main/kotlin/com/schtilig/mako/lang/MakoTokenSets.kt

key-decisions:
  - "IncompleteCodeBlock.mako deleted rather than adding .txt companion — file was never committed to git (untracked), and MakoParsingTest has no testIncompleteCodeBlock() method, so the fixture was completely unreachable"
  - "FILTER_SEP retained in all three files — the lexer does emit this token and it is used by the Python injector to determine injection boundaries (Phase 11-02 fix)"

patterns-established:
  - "Every token constant in MakoTokenTypes must be emitted by the lexer; constants without lexer emission are dead code"
  - "Every token set in MakoTokenSets must have at least one caller; uncalled sets are dead code"
  - "Parser fixture files (.mako) must have both a .txt companion AND a test method in MakoParsingTest"

requirements-completed: [CLEAN-01, CLEAN-02, CLEAN-03]

# Metrics
duration: 15min
completed: 2026-02-22
---

# Phase 16 Plan 01: Dead Code Cleanup Summary

**Removed FILTER_NAME dead token constant from three files and two unreferenced token sets, plus deleted an untracked orphaned parser fixture with no test method**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-02-21T23:03:32Z
- **Completed:** 2026-02-22T00:00:00Z
- **Tasks:** 3 completed
- **Files modified:** 4

## Accomplishments
- Deleted `FILTER_NAME` constant (and its `when`-branch and delegate) — the lexer never emits this token, making all three references unreachable dead code
- Deleted `TEMPLATE_CONTENT` and `TAG_OPENS` token sets — confirmed zero callers in the entire codebase; only `COMMENTS` and `WHITESPACE` are used
- Resolved orphaned `IncompleteCodeBlock.mako` — the file was never tracked in git and `MakoParsingTest` had no corresponding test method, making it completely unreachable; deleted rather than creating misleading infrastructure
- `./gradlew check` passes with BUILD SUCCESSFUL after all three changes

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove FILTER_NAME from token types, highlighter, and generated delegate** - `44d0373` (fix)
2. **Task 2: Remove TEMPLATE_CONTENT and TAG_OPENS from MakoTokenSets** - `297b905` (fix)
3. **Task 3: Resolve orphaned IncompleteCodeBlock fixture** - no new commit (file was untracked, deleted from filesystem)

**Plan metadata:** (docs commit — see final_commit step)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt` - Removed FILTER_NAME constant (line 12)
- `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoSyntaxHighlighter.kt` - Removed FILTER_NAME -> EXPRESSION_KEYS when-branch (line 93)
- `src/main/gen/com/schtilig/mako/lang/psi/MakoTypes.java` - Removed FILTER_NAME delegate field (line 45)
- `src/main/kotlin/com/schtilig/mako/lang/MakoTokenSets.kt` - Removed TEMPLATE_CONTENT and TAG_OPENS (lines 14-22)

## Decisions Made
- **IncompleteCodeBlock.mako deleted rather than completed**: The plan offered two paths — add a `.txt` companion, or delete the fixture. Running `./gradlew test --tests MakoParsingTest` confirmed no `.txt` was auto-generated (ParsingTestCase only generates for explicitly-declared test methods). Since `MakoParsingTest` has no `testIncompleteCodeBlock()` method and the file was never committed to git, deletion was the correct resolution — no test infrastructure was lost.
- **FILTER_SEP retained**: All three files retain `FILTER_SEP` — the lexer emits this token for `|` inside `${...}` expressions, and the Python injector (Phase 11-02) uses it to determine injection boundaries. Not dead code.

## Deviations from Plan

None - plan executed exactly as written. The `IncompleteCodeBlock.mako` deletion path was anticipated in the plan (step 4 of Task 3's action).

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 16 Plan 01 complete — all dead constants and orphaned fixtures resolved
- Codebase is cleaner: every token constant in `MakoTokenTypes` is now load-bearing (emitted by lexer)
- Every token set in `MakoTokenSets` is now referenced by at least one caller
- No blockers for future phases

---
*Phase: 16-dead-code-cleanup*
*Completed: 2026-02-22*

---
phase: 17-clean-up-orphaned-test-fixtures
plan: 01
subsystem: testing
tags: [testData, fixtures, cleanup, annotator, folding, parser]

# Dependency graph
requires:
  - phase: 16-dead-code-cleanup
    provides: Dead code removal and orphaned fixture deletion for IncompleteCodeBlock.mako
provides:
  - testData/ directory containing only 15 fixture files all actively loaded by a test method
  - Annotator and folding tests confirmed inline-only (no disk fixtures required)
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Annotator tests for error scenarios use configureMakoFile() with inline string content"
    - "Folding tests use SAMPLE_MAKO inline string constant; no disk fixture required"
    - "Only negative test (testWellFormedDefTagNoError) uses configureByFile() because checkHighlighting() requires a real file with no <error> markers"

key-files:
  created: []
  modified:
    - src/test/testData/ (5 files deleted, 15 files remain)

key-decisions:
  - "Retain annotator/WellFormedDefTag.mako: only fixture still loaded from disk, required by testWellFormedDefTagNoError via configureByFile + checkHighlighting"
  - "Delete rename/foo.xml and rename/foo_after.xml: IntelliJ plugin project template scaffold files committed in Initial commit, never referenced by any Mako test"
  - "Delete three annotator fixtures: test methods migrated to configureMakoFile() inline string approach; fixtures were silent dead code"
  - "Delete folding/FoldingTestData.mako: MakoFoldingTest uses SAMPLE_MAKO inline string constant; configureByFile never called"

patterns-established:
  - "Pattern: After every test-infrastructure migration to inline content, delete the superseded disk fixture immediately to prevent confusion"

requirements-completed: []

# Metrics
duration: 5min
completed: 2026-02-22
---

# Phase 17 Plan 01: Clean Up Orphaned Test Fixtures Summary

**Deleted five orphaned disk fixtures (rename/ scaffold + three annotator + one folding), leaving testData/ with exactly 15 files all actively loaded by a test method**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-02-21T23:46:43Z
- **Completed:** 2026-02-21T23:52:00Z
- **Tasks:** 2
- **Files modified:** 5 deleted (0 created, 0 modified)

## Accomplishments

- Removed two IntelliJ plugin scaffold XML files from rename/ that had never been referenced by any Mako test
- Removed three annotator fixture files whose test methods were migrated to inline configureMakoFile() string content
- Removed folding/FoldingTestData.mako whose test suite uses the SAMPLE_MAKO inline string constant exclusively
- Retained annotator/WellFormedDefTag.mako (the sole fixture still loaded via configureByFile in testWellFormedDefTagNoError)
- Retained all 7 parser fixture pairs (14 files) exercised by MakoParsingTest
- ./gradlew check: BUILD SUCCESSFUL with zero failures after all deletions

## Task Commits

Each task was committed atomically:

1. **Task 1: Delete orphaned rename/ scaffold fixtures** - `77b8a98` (chore)
2. **Task 2: Delete orphaned annotator and folding fixture files** - `9b417dc` (chore)

**Plan metadata:** (final docs commit follows)

## Files Created/Modified

- `src/test/testData/rename/foo.xml` - DELETED (IntelliJ scaffold, never referenced)
- `src/test/testData/rename/foo_after.xml` - DELETED (IntelliJ scaffold, never referenced)
- `src/test/testData/annotator/InvalidDirective.mako` - DELETED (test uses inline configureMakoFile())
- `src/test/testData/annotator/UnclosedBlockTag.mako` - DELETED (test uses inline configureMakoFile())
- `src/test/testData/annotator/UnclosedDefTag.mako` - DELETED (test uses inline configureMakoFile())
- `src/test/testData/folding/FoldingTestData.mako` - DELETED (MakoFoldingTest uses SAMPLE_MAKO inline string)

## Decisions Made

- Retain `annotator/WellFormedDefTag.mako`: This is the only annotator fixture still loaded from disk. `testWellFormedDefTagNoError` uses `myFixture.configureByFile("annotator/WellFormedDefTag.mako")` followed by `myFixture.checkHighlighting(true, false, false)` — the negative assertion (no unexpected errors) requires a real file with no `<error>` markers, not inline text.
- The three deleted annotator fixtures were entirely superseded by the inline `configureMakoFile()` approach used in the error-annotation test methods. Keeping them on disk created confusion about what is actually tested.
- `rename/foo.xml` and `rename/foo_after.xml` are generic IntelliJ plugin project template scaffold files committed in the repository's Initial commit. No grep match in any `.kt` or `.java` test file confirms they were never wired up.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- testData/ is clean: every remaining fixture is actively exercised
- Phase 17 is a single-plan phase; this completes it
- No blockers or concerns

## Self-Check: PASSED

- CONFIRMED DELETED: src/test/testData/rename/foo.xml
- CONFIRMED DELETED: src/test/testData/rename/foo_after.xml
- CONFIRMED DELETED: src/test/testData/annotator/InvalidDirective.mako
- CONFIRMED DELETED: src/test/testData/annotator/UnclosedBlockTag.mako
- CONFIRMED DELETED: src/test/testData/annotator/UnclosedDefTag.mako
- CONFIRMED DELETED: src/test/testData/folding/FoldingTestData.mako
- FOUND: src/test/testData/annotator/WellFormedDefTag.mako (retained)
- FOUND: src/test/testData/parser/WellFormedFile.mako (retained)
- Commits: 77b8a98 (Task 1), 9b417dc (Task 2) — both confirmed in git log
- ./gradlew check: BUILD SUCCESSFUL

---
*Phase: 17-clean-up-orphaned-test-fixtures*
*Completed: 2026-02-22*

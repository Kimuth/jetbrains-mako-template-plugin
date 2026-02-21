---
phase: 15-annotator-fixes
plan: 01
subsystem: annotation
tags: [kotlin, annotator, completion, parser, fixture, language-guard]

# Dependency graph
requires:
  - phase: 14-completion-fixes
    provides: MakoCompletionContributor with charsSequence backward scan
  - phase: 08-error-annotations
    provides: MakoAnnotator with checkForInvalidDirective detection

provides:
  - Type-safe language guard (identity comparison) in MakoAnnotator
  - Type-safe language guard (identity comparison) in MakoCompletionContributor
  - Parser fixture regression test for unknown directive <%bogus> (ANNOT-02)

affects: [16-cleanup, any future annotator or completion work]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Language identity comparison: language != MakoLanguage (Kotlin object singleton) instead of language.id string literal"
    - "Parser fixture auto-generation: first run fails and writes .txt, second run passes"

key-files:
  created:
    - src/test/testData/parser/UnknownDirective.mako
    - src/test/testData/parser/UnknownDirective.txt
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt
    - src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoParsingTest.kt

key-decisions:
  - "Use language != MakoLanguage (Kotlin object identity) not language.id string comparison — eliminates silent drift if language ID is ever renamed"
  - "UnknownDirective.txt fixture confirms three separate TEMPLATE_TEXT tokens for <%bogus>, not a typed tag node — annotator works at semantic layer"

patterns-established:
  - "Language guard pattern: language != MakoLanguage using Kotlin object singleton reference equality"

requirements-completed: [ANNOT-01, ANNOT-02]

# Metrics
duration: 2min
completed: 2026-02-21
---

# Phase 15 Plan 01: Annotator Fixes Summary

**Type-safe language guards via MakoLanguage identity comparison in annotator and completion contributor, plus parser fixture locking <%bogus> PSI structure as three TEMPLATE_TEXT tokens**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-21T22:38:17Z
- **Completed:** 2026-02-21T22:41:03Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Replaced `language.id != "Mako Template"` string guards with `language != MakoLanguage` identity comparison in both MakoAnnotator and MakoCompletionContributor (ANNOT-01)
- Added `testUnknownDirective` parser fixture test confirming <%bogus> produces three TEMPLATE_TEXT sibling nodes with no typed directive node (ANNOT-02)
- Auto-generated and committed `UnknownDirective.txt` PSI snapshot; test passes on second run

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace string-literal language guards** - `c89a86f` (fix)
2. **Task 2: Add parser fixture for unknown directive** - `b5a117b` (test)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt` - Added MakoLanguage import; replaced string guard with identity comparison
- `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt` - Added MakoLanguage import; replaced string guard with identity comparison
- `src/test/kotlin/com/schtilig/mako/lang/MakoParsingTest.kt` - Added `testUnknownDirective()` method
- `src/test/testData/parser/UnknownDirective.mako` - Fixture input: `<%bogus attr="x">\ncontent`
- `src/test/testData/parser/UnknownDirective.txt` - Auto-generated PSI tree (three TEMPLATE_TEXT tokens, no typed tag node)

## Decisions Made
- `language != MakoLanguage` (Kotlin object singleton) is the correct identity comparison — equivalent to Java `language == MakoLanguage.INSTANCE`. Avoids string drift if language ID changes.
- The `UnknownDirective.txt` tree confirms annotator detection operates at semantic layer: the parser produces plain TEMPLATE_TEXT nodes, and MakoAnnotator's `checkForInvalidDirective` detects the three-token pattern.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 15 Plan 01 complete; ANNOT-01 and ANNOT-02 requirements satisfied
- Phase 16 (Cleanup) can now proceed: FILTER_NAME token removal was gated on ANNOT-01 language guard fix being in place

## Self-Check: PASSED

All files verified present:
- FOUND: MakoAnnotator.kt
- FOUND: MakoCompletionContributor.kt
- FOUND: UnknownDirective.mako
- FOUND: UnknownDirective.txt
- FOUND: MakoParsingTest.kt
- FOUND: 15-01-SUMMARY.md

All commits verified:
- FOUND: c89a86f (fix: replace string-literal language guards)
- FOUND: b5a117b (test: add UnknownDirective parser fixture)

---
*Phase: 15-annotator-fixes*
*Completed: 2026-02-21*

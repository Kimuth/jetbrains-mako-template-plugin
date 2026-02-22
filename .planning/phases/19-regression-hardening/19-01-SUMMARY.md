---
phase: 19-regression-hardening
plan: "01"
subsystem: testing

tags: [structure-view, dual-tree, psi, template-language, regression]

# Dependency graph
requires:
  - phase: 18-fileviewprovider-scaffolding
    provides: MakoFileViewProvider dual-tree (TemplateLanguageFileViewProvider) active for .mako files

provides:
  - MakoStructureViewFactory dual-tree guard (viewProvider.getPsi(MakoLanguage)) preventing empty structure view when HTML PSI root is passed
  - testStructureViewWorksInDualTree regression test confirming 2 structure children in dual-tree context
  - All RGRN-01/02/03 auto-test criteria green (human IDE verification at checkpoint)

affects:
  - phase-20-html-feature-verification

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dual-tree guard pattern: psiFile.viewProvider.getPsi(MakoLanguage) ?: psiFile — resolves Mako PSI root safely, no-op when already correct type"
    - "Regression test: addFileToProject (physical VFS) activates dual-tree; construct model directly with Mako PSI root to assert children independently of platform dispatch"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt

key-decisions:
  - "Use viewProvider.getPsi(MakoLanguage) ?: psiFile (no cast needed — getPsi returns PsiFile? directly) to resolve Mako PSI root in MakoStructureViewFactory"
  - "requireNotNull(makoFile) pattern after assertNotNull used in test to satisfy Kotlin null-safety for subsequent usage"

patterns-established:
  - "MakoStructureViewFactory guard: resolve Mako PSI root before passing to MakoStructureViewModel to handle dual-tree HTML PSI injection"

requirements-completed: [RGRN-01, RGRN-02, RGRN-03]

# Metrics
duration: 3min
completed: 2026-02-22
---

# Phase 19 Plan 01: Regression Hardening — Structure View Dual-Tree Guard Summary

**MakoStructureViewFactory dual-tree guard added via viewProvider.getPsi(MakoLanguage) with regression test asserting 2 structure children when HTML PSI root is active; full test suite green**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-02-22T20:33:16Z
- **Completed:** 2026-02-22T20:35:53Z
- **Tasks:** 1 of 2 (Task 2 is human-verify checkpoint)
- **Files modified:** 2

## Accomplishments
- Applied dual-tree guard to `MakoStructureViewFactory.getStructureViewBuilder` — resolves Mako PSI root before passing to `MakoStructureViewModel`, preventing empty structure view when caret is in a `TEMPLATE_TEXT` region (which causes the platform to pass the HTML PSI file instead of the Mako PSI file)
- Added `testStructureViewWorksInDualTree` to `MakoFileViewProviderTest` — uses `addFileToProject` (physical VFS) to activate dual-tree, constructs `MakoStructureViewModel` with resolved Mako PSI root, asserts 2 children (greet def + header block)
- All 4 tests in `MakoFileViewProviderTest` pass; full `./gradlew check` BUILD SUCCESSFUL with zero failures

## Task Commits

Each task was committed atomically:

1. **Task 1: Apply MakoStructureViewFactory dual-tree guard, add regression test, run check** - `9ada5ed` (feat)

**Plan metadata:** (to be committed after checkpoint verification)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt` - Added `viewProvider.getPsi(MakoLanguage) ?: psiFile` guard; resolves Mako PSI root for dual-tree compatibility
- `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` - Added `testStructureViewWorksInDualTree` and imports for `MakoStructureViewModel`, `MakoStructureViewElement`

## Decisions Made
- `getPsi(MakoLanguage)` returns `PsiFile?` directly — no `as? PsiFile` cast needed (compiler correctly flagged it as redundant); used `?: psiFile` fallback
- In the test, `?: fail(...)` cannot satisfy Kotlin null safety for subsequent `val model = MakoStructureViewModel(...)` usage because `fail()` returns `Nothing` in an expression context; replaced with `assertNotNull + requireNotNull` pattern to avoid the type mismatch

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed redundant cast and fixed test null-safety pattern**
- **Found during:** Task 1 (./gradlew check — first run)
- **Issue 1:** `as? PsiFile` cast on `getPsi(MakoLanguage)` return value was flagged by compiler as redundant (the method already returns `PsiFile?`); left as warning in factory, also caused confusion
- **Issue 2:** `viewProvider.getPsi(MakoLanguage) as? com.intellij.psi.PsiFile ?: fail(...)` produced compile error: "Argument type mismatch: actual type is 'Any', but 'PsiFile' was expected" — `fail()` return type inference broke the smart cast
- **Fix 1:** Changed factory to `psiFile.viewProvider.getPsi(MakoLanguage) ?: psiFile` (no cast needed)
- **Fix 2:** Changed test to `val makoFile = viewProvider.getPsi(MakoLanguage); assertNotNull(..., makoFile); requireNotNull(makoFile)` — standard null assertion then smart-cast via `requireNotNull`
- **Files modified:** Both files (same commit)
- **Verification:** `./gradlew check` BUILD SUCCESSFUL, all 4 MakoFileViewProviderTest tests pass
- **Committed in:** `9ada5ed` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — compile errors due to incorrect cast pattern in plan-provided code snippet)
**Impact on plan:** Required fixes were minor and necessary for compilation correctness. No scope creep. Guard behavior is identical to plan intent.

## Issues Encountered
- Plan code snippet used `as? PsiFile` cast on `getPsi()` return (which already returns `PsiFile?`) — redundant cast generated compiler warning in factory and compile error in test. Both fixed inline before first clean build.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Task 2 (human IDE verification) is a blocking checkpoint requiring human to launch `./gradlew runIde` and verify four RGRN success criteria visually
- All automated tests pass; MakoStructureViewFactory guard is wired in and operational
- Phase 20 (HTML feature verification and false-positive audit) can proceed after human checkpoint clears

---
*Phase: 19-regression-hardening*
*Completed: 2026-02-22*

## Self-Check: PENDING (awaiting Task 2 human-verify checkpoint)

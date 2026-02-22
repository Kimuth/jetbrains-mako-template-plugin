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
  - All RGRN-01/02/03 criteria verified: Python injection active, folding icons present, structure view non-empty in running IDE

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
  - "requireNotNull(makoFile) pattern after assertNotNull used in test to satisfy Kotlin null-safety without type mismatch from fail() return type"
  - "Known limitation (carry to Phase 20): os from <%! %> module-level block is not in scope for ${...} expressions — cross-injection scoping gap, not a regression"
  - "Known limitation (carry to Phase 20): Single-line inline <%! import os %> triggers Unexpected indent from Python language service — workaround is multi-line syntax; pre-existing behavior, not a regression"

patterns-established:
  - "MakoStructureViewFactory guard: resolve Mako PSI root before passing to MakoStructureViewModel to handle dual-tree HTML PSI injection"

requirements-completed: [RGRN-01, RGRN-02, RGRN-03]

# Metrics
duration: 5min
completed: 2026-02-22
---

# Phase 19 Plan 01: Regression Hardening — Structure View Dual-Tree Guard Summary

**MakoStructureViewFactory dual-tree guard applied, regression test green, all four RGRN IDE criteria verified by human with two known pre-existing limitations noted for Phase 20**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-02-22T20:33:16Z
- **Completed:** 2026-02-22T20:37:00Z
- **Tasks:** 2 of 2
- **Files modified:** 2

## Accomplishments
- Applied dual-tree guard to `MakoStructureViewFactory.getStructureViewBuilder` — resolves Mako PSI root before passing to `MakoStructureViewModel`, preventing empty structure view when caret is in a `TEMPLATE_TEXT` region (which causes the platform to pass the HTML PSI file instead of the Mako PSI file)
- Added `testStructureViewWorksInDualTree` to `MakoFileViewProviderTest` — uses `addFileToProject` (physical VFS) to activate dual-tree, constructs `MakoStructureViewModel` with resolved Mako PSI root, asserts 2 children (greet def + header block)
- All 4 tests in `MakoFileViewProviderTest` pass; full `./gradlew check` BUILD SUCCESSFUL with zero failures
- Human IDE verification confirmed all four RGRN success criteria: Python injection active in `${...}`, `<% %>`, and `<%! %>` regions; folding gutter icons present for `<%def>`, `<%block>`, `<%doc>`; Structure View shows `greet` and `content` nodes in document order

## Task Commits

Each task was committed atomically:

1. **Task 1: Apply MakoStructureViewFactory dual-tree guard, add regression test, run check** - `9ada5ed` (feat)
2. **Task 2: Human verification — RGRN success criteria in running IDE** - approved by human (no source changes)

**Plan metadata:** `c7c10f5` (docs: complete plan — MakoStructureViewFactory dual-tree guard + regression test)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt` - Added `viewProvider.getPsi(MakoLanguage) ?: psiFile` guard; resolves Mako PSI root for dual-tree compatibility
- `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` - Added `testStructureViewWorksInDualTree` and imports for `MakoStructureViewModel`, `MakoStructureViewElement`

## Decisions Made
- `getPsi(MakoLanguage)` returns `PsiFile?` directly — no `as? PsiFile` cast needed (compiler correctly flagged it as redundant); used `?: psiFile` fallback
- In the test, `?: fail(...)` cannot satisfy Kotlin null safety for subsequent `val model = MakoStructureViewModel(...)` usage because `fail()` returns `Nothing` in an expression context; replaced with `assertNotNull + requireNotNull` pattern to avoid the type mismatch
- **Known limitation carried to Phase 20 (not a regression):** `os` from `<%! import os %>` module-level block is not in scope for `${os.getcwd()}` expressions — cross-injection scoping gap; `${undefined_xyz}` squiggle correctly fires, confirming Python injection is active
- **Known limitation carried to Phase 20 (not a regression):** Single-line inline `<%! import os %>` triggers "Unexpected indent" from Python language service; multi-line syntax is the workaround; pre-existing behavior, not introduced by Phase 18/19 changes

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed redundant cast and fixed test null-safety pattern**
- **Found during:** Task 1 (./gradlew check — first run)
- **Issue 1:** `as? PsiFile` cast on `getPsi(MakoLanguage)` return value was flagged by compiler as redundant (the method already returns `PsiFile?`)
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
- Phase 19 is complete — all RGRN requirements verified (automated + human)
- Two pre-existing limitations noted for Phase 20 investigation: cross-injection scoping for `<%! %>` variables in `${...}` expressions, and "Unexpected indent" for single-line module-level blocks
- Phase 20 (HTML Feature Verification and False-Positive Audit) is ready to proceed

---
*Phase: 19-regression-hardening*
*Completed: 2026-02-22*

## Self-Check: PASSED
- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt` — FOUND
- `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` — FOUND
- `.planning/phases/19-regression-hardening/19-01-SUMMARY.md` — FOUND (this file)
- commit `9ada5ed` — FOUND
- commit `c7c10f5` — FOUND

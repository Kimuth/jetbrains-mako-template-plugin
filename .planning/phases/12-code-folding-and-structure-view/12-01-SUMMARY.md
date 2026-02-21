---
phase: 12-code-folding-and-structure-view
plan: 01
subsystem: folding
tags: [kotlin, intellij-platform, ast, folding, psi]

# Dependency graph
requires:
  - phase: 05-structural-features
    provides: MakoFoldingBuilder initial implementation with flat sibling scanning
provides:
  - Recursive fold discovery for <%doc>, <% %>, <%! %> nested inside <%def>/<%block>
  - Language.ANY identity check for DUMMY_BLOCK detection
  - Tests asserting nested folds (doc-in-def, code-in-block, module-in-def)
affects:
  - 12-code-folding-and-structure-view (future plans building on fold infrastructure)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "walkAllNodes: depth-first ASTNode visitor returning bool to control child recursion"
    - "Skip-children pattern: composite nodes that produce a fold return false to avoid double-folding their child tokens"
    - "Language.ANY identity check: node.psi.language == Language.ANY replaces brittle toString/simpleName DUMMY_BLOCK detection"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoFoldingTest.kt

key-decisions:
  - "walkAllNodes returns Boolean from visitor to control recursion — prevents double-folding when composite (MODULE_BLOCK, CODE_BLOCK) and its child token (MODULE_OPEN, CODE_OPEN) are both visible"
  - "Language.ANY identity check chosen over DUMMY_BLOCK string comparison — immune to JetBrains internal type renames"

patterns-established:
  - "Recursive ASTNode walker: walkAllNodes(node, visitor: (ASTNode) -> Boolean) — visitor returns true to recurse, false to skip children"

requirements-completed: [FOLD-01, VIEW-05]

# Metrics
duration: 5min
completed: 2026-02-21
---

# Phase 12 Plan 01: Code Folding Recursive Descent Summary

**MakoFoldingBuilder now discovers <%doc>, <% %>, and <%! %> folds nested inside <%def>/<%block> via recursive ASTNode walk, with Language.ANY replacing brittle DUMMY_BLOCK string detection**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-21T20:10:51Z
- **Completed:** 2026-02-21T20:15:43Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Added `walkAllNodes` depth-first ASTNode helper with bool return to control child recursion
- Rewrote `buildDocCommentFolds`, `buildModuleBlockFolds`, `buildCodeBlockFolds` to use `walkAllNodes` instead of flat `firstChildNode`/`treeNext` loop
- Replaced string-based DUMMY_BLOCK check (`et.toString() == "DUMMY_BLOCK"`) with `node.psi.language == Language.ANY`
- Added three new tests: nested doc-in-def, code-in-block, module-in-def folds all verified passing
- All 20 MakoFoldingTest tests pass; `./gradlew check` exits 0

## Task Commits

Each task was committed atomically:

1. **Task 1: Make buildDocCommentFolds, buildModuleBlockFolds, and buildCodeBlockFolds recursive** - `088b7a8` (feat)
2. **Task 2: Replace string-based DUMMY_BLOCK check with Language.ANY and add nested fold tests** - `1dbf5c7` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt` - Added `walkAllNodes`, rewrote three fold helpers, Language.ANY import, replaced DUMMY_BLOCK check
- `src/test/kotlin/com/schtilig/mako/lang/MakoFoldingTest.kt` - Added three nested fold tests (testDocCommentNestedInDefFolds, testCodeBlockNestedInBlockFolds, testModuleBlockNestedInDefFolds)

## Decisions Made
- `walkAllNodes` returns `Boolean` from visitor rather than using a separate callback: this enables composite nodes (MODULE_BLOCK, CODE_BLOCK) to signal "already folded, skip children" in one step, preventing double-fold when the composite node AND its child open token are both visited during recursion
- `Language.ANY` identity check: `node.psi.language == Language.ANY` is the platform-stable way to identify DUMMY_BLOCK nodes — immune to JetBrains internal type renames unlike `toString()` or `simpleName`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed double-fold regression from naive recursive walk**
- **Found during:** Task 1 (verifying existing tests after adding walkAllNodes)
- **Issue:** naive `walkAllNodes` that always recurses visited both MODULE_BLOCK composite AND its child MODULE_OPEN token, producing 2 folds for a single block (testModuleBlockFolded expected 1, got 2; testCodeBlockFolded expected 1, got 2)
- **Fix:** Changed `walkAllNodes` visitor signature from `(ASTNode) -> Unit` to `(ASTNode) -> Boolean`; composite branch returns `false` to skip children, leaf token branch returns `false` as well
- **Files modified:** src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt
- **Verification:** All 17 existing tests pass after fix; then all 20 tests pass after adding new ones
- **Committed in:** 088b7a8 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in initial walkAllNodes implementation)
**Impact on plan:** Fix was necessary for correctness; the plan's specified `walkAllNodes` signature was `(ASTNode) -> Unit` which caused double-folds. Extended to `(ASTNode) -> Boolean` to support skip-children semantics.

## Issues Encountered
- Initial `walkAllNodes` implementation with `Unit` return caused double-folds for MODULE_BLOCK and CODE_BLOCK. Resolved by changing visitor return type to `Boolean` to control child recursion per node.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Recursive fold infrastructure complete; nested folds now work for all three fold types
- Language.ANY check is stable against JetBrains platform upgrades
- Ready for Phase 12 Plan 02 (structure view or further folding work)

---
*Phase: 12-code-folding-and-structure-view*
*Completed: 2026-02-21*

## Self-Check: PASSED

- FOUND: src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt
- FOUND: src/test/kotlin/com/schtilig/mako/lang/MakoFoldingTest.kt
- FOUND: .planning/phases/12-code-folding-and-structure-view/12-01-SUMMARY.md
- FOUND commit 088b7a8: feat(12-01): make fold helpers recursive with walkAllNodes
- FOUND commit 1dbf5c7: feat(12-01): replace DUMMY_BLOCK string check with Language.ANY; add nested fold tests

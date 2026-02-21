---
phase: 12-code-folding-and-structure-view
plan: "02"
subsystem: ui
tags: [structure-view, psi, jetbrains-plugin, kotlin]

# Dependency graph
requires:
  - phase: 12-code-folding-and-structure-view
    provides: MakoStructureViewElement with getChildren() and getPresentation()
provides:
  - Document-order interleaving of def and block children in structure view (sortedBy textOffset)
  - AllIcons.Nodes.Function fallback icon for def/block structure view nodes
  - Tests for document-order interleaving and function icon
affects: [structure-view, future-view-phases]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Combined PSI child lists sorted by textOffset for document-order rendering"
    - "AllIcons.Nodes.Function as standard icon for callable definitions"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewElement.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoStructureViewTest.kt

key-decisions:
  - "Used (childDefs + childBlocks).sortedBy { it.textOffset } to produce document-order children without a mutable accumulator"
  - "Replaced MakoIcons.FILE fallback with AllIcons.Nodes.Function since file icon gave no visual hint that def/block nodes are callable definitions"

patterns-established:
  - "Structure view element children: combine heterogeneous PSI child lists and sort by textOffset for document order"

requirements-completed: [VIEW-02, VIEW-04]

# Metrics
duration: 3min
completed: 2026-02-21
---

# Phase 12 Plan 02: Structure View Document Order and Function Icon Summary

**`MakoStructureViewElement.getChildren()` now interleaves defs and blocks in document order via `sortedBy { it.textOffset }`, and the fallback icon is `AllIcons.Nodes.Function` instead of the Mako file icon**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-21T20:10:57Z
- **Completed:** 2026-02-21T20:14:37Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Fixed `getChildren()` to sort combined `(childDefs + childBlocks)` list by `textOffset`, producing correct interleaved document order
- Replaced `MakoIcons.FILE` fallback icon with `AllIcons.Nodes.Function` for def/block structure view nodes
- Added `testChildrenInDocumentOrder` test: verifies def "first", block "second", def "third" appear in that order
- Added `testDefNodeUsesFunctionIcon` test: verifies icon equals `AllIcons.Nodes.Function`
- All 6 structure view tests pass (4 pre-existing + 2 new)

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix getChildren() for document order and fix fallback icon** - `360305e` (feat)
2. **Task 2: Add document-order and function icon tests** - `4da5771` (test)

**Plan metadata:** (docs commit — see state update below)

## Files Created/Modified

- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewElement.kt` - Replaced mutable accumulator with `sortedBy { it.textOffset }` combine; switched fallback icon to `AllIcons.Nodes.Function`; swapped `MakoIcons` import for `AllIcons` import
- `src/test/kotlin/com/schtilig/mako/lang/MakoStructureViewTest.kt` - Added `testChildrenInDocumentOrder` and `testDefNodeUsesFunctionIcon` tests

## Decisions Made

- Used `(childDefs + childBlocks).sortedBy { it.textOffset }` instead of maintaining a mutable `result` list — cleaner functional approach with no extra variable
- Replaced `MakoIcons.FILE` with `AllIcons.Nodes.Function` because the file icon provides no semantic signal that def/block nodes represent callable definitions

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - both tasks applied cleanly. The `Iconable` import in `MakoStructureViewElement.kt` was pre-existing and unused; deferred as out-of-scope per scope boundary rules.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Structure view now shows interleaved document order and function icons for def/block nodes
- All 6 structure view tests pass with no regressions
- Phase 12 structure view work (VIEW-02, VIEW-04) is complete

---
*Phase: 12-code-folding-and-structure-view*
*Completed: 2026-02-21*

## Self-Check: PASSED

- FOUND: `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewElement.kt`
- FOUND: `src/test/kotlin/com/schtilig/mako/lang/MakoStructureViewTest.kt`
- FOUND: `.planning/phases/12-code-folding-and-structure-view/12-02-SUMMARY.md`
- FOUND commit `360305e`: feat(12-02): fix structure view document order and function icon
- FOUND commit `4da5771`: test(12-02): add document-order and function icon tests for structure view

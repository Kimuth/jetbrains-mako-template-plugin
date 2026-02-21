---
phase: 10-psi-correctness
plan: 01
subsystem: psi
tags: [psi, mixin, getName, setName, ASTNode, TDD, kotlin]

# Dependency graph
requires: []
provides:
  - "MakoDefTagMixin.getName() correctly pairs name= attribute key to its value via ASTNode child walk"
  - "MakoBlockTagMixin.getName() correctly pairs name= attribute key to its value via ASTNode child walk"
  - "Both setName() implementations throw UnsupportedOperationException instead of silent no-op"
  - "MakoPsiMixinTest with 7 tests covering PSI-01 and PSI-02 contracts"
affects: [11-injection-host, structure-view, completion, rename-refactoring]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ASTNode child walk: iterate firstChildNode/treeNext to pair TAG_ATTR_NAME+TAG_ATTR_VALUE instead of findChildByType()"
    - "TDD RED-GREEN cycle for PSI mixin contracts"

key-files:
  created:
    - "src/test/kotlin/com/schtilig/mako/lang/MakoPsiMixinTest.kt"
  modified:
    - "src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoDefTagMixin.kt"
    - "src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoBlockTagMixin.kt"

key-decisions:
  - "Skipped MakoPsiUtil.kt refactor: duplication is only ~15 lines across 2 well-named files, extraction not warranted at this scale"
  - "setName() throws UnsupportedOperationException to give callers clear failure signal rather than misleading no-op return"

patterns-established:
  - "ASTNode attribute lookup: walk firstChildNode/treeNext looking for TAG_ATTR_NAME with specific text, then find paired TAG_ATTR_VALUE"

requirements-completed: [PSI-01, PSI-02]

# Metrics
duration: 3min
completed: 2026-02-21
---

# Phase 10 Plan 01: PSI Mixin getName/setName Correctness Summary

**ASTNode child-walk algorithm in MakoDefTagMixin and MakoBlockTagMixin fixes attribute-ordering bug for getName() and makes setName() throw UnsupportedOperationException**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-02-21T18:11:50Z
- **Completed:** 2026-02-21T18:14:37Z
- **Tasks:** 2 (RED + GREEN; REFACTOR skipped as unwarranted)
- **Files modified:** 3

## Accomplishments

- Fixed getName() attribute-pairing bug: `<%def args="()" name="foo">` now correctly returns "foo" instead of "()"
- Fixed setName() silent no-op: both mixins now throw UnsupportedOperationException with clear message
- Created MakoPsiMixinTest with 7 tests covering all 6 PSI-01/PSI-02 contract cases
- All 75 tests pass across the full test suite (zero regressions)

## Task Commits

Each task committed atomically:

1. **TDD RED: failing tests for PSI-01 and PSI-02** - `a533a5e` (test)
2. **TDD GREEN: fix getName attribute pairing and setName throws** - `19b1d66` (feat)

**Plan metadata:** (docs commit follows)

_Note: TDD tasks have two commits (test RED → feat GREEN). REFACTOR phase skipped — no duplication warranted extraction at this scale._

## Files Created/Modified

- `src/test/kotlin/com/schtilig/mako/lang/MakoPsiMixinTest.kt` - 7 tests covering getName() attribute ordering and setName() throws contracts
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoDefTagMixin.kt` - Fixed getName() with ASTNode child walk; setName() now throws
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoBlockTagMixin.kt` - Same fixes as MakoDefTagMixin

## Decisions Made

- **Skipped refactor to MakoPsiUtil.kt:** The shared getName() logic is ~15 lines duplicated across only 2 files. Both are well-named, well-documented, and independently readable. Extracting a shared utility at this scale would add indirection without meaningful benefit.
- **UnsupportedOperationException over no-op:** Returning `this` from setName() misled callers into thinking rename succeeded. Throwing UnsupportedOperationException gives callers a clear, actionable failure signal.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- PSI-01 and PSI-02 contracts are complete and tested
- MakoStructureViewTest still passes (getName() changes do not affect structure view: names are resolved correctly in the normal-ordering case already present in tests)
- Ready for Phase 11 (injection host correctness — INJECT-01)

---
*Phase: 10-psi-correctness*
*Completed: 2026-02-21*

---
phase: 14-completion-fixes
plan: 01
subsystem: completion
tags: [completion, performance, insert-handler, charsSequence, offset-math]

# Dependency graph
requires:
  - phase: 07-completion
    provides: MakoCompletionContributor with TagNameCompletionProvider and insert handlers
provides:
  - Correct <%doc insert handler replacement range using captured ltPos
  - Allocation-free backward scan over document.charsSequence in TagNameCompletionProvider
affects: [07-completion, completion]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Capture outer val (ltPos) in Kotlin lambda for correct range replacement in completion insert handlers"
    - "Use document.charsSequence backward scan instead of file.text.substring for allocation-free text inspection"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt

key-decisions:
  - "<%doc handler uses ltPos (captured from addCompletions scope) instead of ctx.startOffset - 2 — ltPos always points to the < of the actual <% in the document, regardless of what the platform considers the completion start offset"
  - "document.charsSequence backward scan replaces file.text.substring allocation — CharSequence view is backed by the document buffer with no heap allocation per keystroke"

patterns-established:
  - "Completion insert handler offset math: capture the exact anchor position (ltPos) as a val before the loop, then close over it in the lambda — never derive position from ctx.startOffset which is relative to the completion item, not the source text"
  - "TagNameCompletionProvider text inspection: use document.charsSequence + backward scan to avoid allocating O(file-size) Strings on every keystroke"

requirements-completed: [COMP-01, COMP-02]

# Metrics
duration: 2min
completed: 2026-02-21
---

# Phase 14 Plan 01: Completion Fixes Summary

**Fixed <%doc insert handler offset math (ltPos capture) and replaced O(file-size) String allocation with document.charsSequence backward scan in TagNameCompletionProvider**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-21T21:16:22Z
- **Completed:** 2026-02-21T21:18:42Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Fixed <%doc insert handler to use captured `ltPos` instead of `ctx.startOffset - 2`, ensuring partial typed text (<%d, <%do, <%doc) is fully replaced without stale characters
- Replaced `file.text.substring(0, offset)` + `lastIndexOf("<%")` with a backward scan over `parameters.editor.document.charsSequence`, eliminating a full-file String allocation on every completion keystroke
- All 7 MakoCompletionTest tests pass; `./gradlew check` passes with zero errors

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix <%doc insert handler replacement range (COMP-01)** - `983028e` (fix)
2. **Task 2: Eliminate full-file text allocation in TagNameCompletionProvider (COMP-02)** - `4b3e746` (fix)

**Plan metadata:** (docs commit — see final commit)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt` - Fixed <%doc insert handler and replaced file.text allocation with charsSequence backward scan

## Decisions Made
- `<%doc` handler closes over `ltPos` val (Kotlin closure capture) so the replacement range always starts at the actual `<` of `<%`, not a platform-relative completion start offset. This is the correct anchor regardless of how much the user has typed (<%d, <%do, <%doc, etc.).
- Backward scan over `document.charsSequence` chosen over `textBefore.lastIndexOf` because `charsSequence` is a zero-copy CharSequence view of the document buffer. `file.text` triggers a full String copy; scanning backward is O(k) where k is characters between `<%` and caret — typically 0–10 chars.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- COMP-01 and COMP-02 requirements complete; completion contributor is now correct and allocation-efficient
- No blockers for remaining Phase 14 plans

## Self-Check: PASSED

- FOUND: `.planning/phases/14-completion-fixes/14-01-SUMMARY.md`
- FOUND: `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt`
- FOUND: commit `983028e` (Task 1 - fix <%doc ltPos)
- FOUND: commit `4b3e746` (Task 2 - charsSequence scan)
- VERIFIED: No `file.text` usage in TagNameCompletionProvider
- VERIFIED: `charsSequence` appears in method body
- VERIFIED: `ltPos` used in <%doc insert handler (not ctx.startOffset - 2)

---
*Phase: 14-completion-fixes*
*Completed: 2026-02-21*

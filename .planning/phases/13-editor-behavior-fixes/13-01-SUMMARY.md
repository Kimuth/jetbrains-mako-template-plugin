---
phase: 13-editor-behavior-fixes
plan: 01
subsystem: highlighting
tags: [syntax-highlighting, brace-matching, lexer, kotlin, intellij-platform]

# Dependency graph
requires:
  - phase: 04-syntax-highlighting
    provides: MakoSyntaxHighlighter.kt with TextAttributesKey definitions
  - phase: 05-structural-features
    provides: MakoPairedBraceMatcher.kt with PAIRS array
  - phase: 02-lexer
    provides: MakoLexerAdapter.kt with braceDepth state encoding

provides:
  - MAKO_CODE_CONTENT renders with IDENTIFIER (not string literal) color by default
  - MODULE_OPEN / CODE_CLOSE brace pair registered — <%! %> now highlights in editor
  - Logger warning emitted when braceDepth exceeds 15 before clamping in MakoLexerAdapter

affects: [color-schemes, 14-anything-using-brace-matching, 16-cleanup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Use DefaultLanguageHighlighterColors.IDENTIFIER as fallback for code-region content tokens"
    - "All brace pairs in MakoPairedBraceMatcher use structural=false due to shared END_TAG/CODE_CLOSE tokens"
    - "Platform Logger.getInstance() in companion object for per-class diagnostic logging"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoSyntaxHighlighter.kt
    - src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoPairedBraceMatcher.kt
    - src/main/kotlin/com/schtilig/mako/lang/MakoLexerAdapter.kt

key-decisions:
  - "MAKO_CODE_CONTENT fallback changed from STRING to IDENTIFIER — code block tokens should look like identifiers/code, not string literals"
  - "MODULE_OPEN pairs with CODE_CLOSE using structural=false — consistent with CODE_OPEN pair since both share the same close token"
  - "Logger placed in companion object of MakoLexerAdapter — follows IntelliJ platform convention for per-class diagnostic loggers"

patterns-established:
  - "Code-region content tokens use IDENTIFIER fallback, not STRING"
  - "All Mako brace pairs use structural=false to avoid conflicts from shared close tokens"

requirements-completed: [VIEW-01, VIEW-03, VIEW-06]

# Metrics
duration: 2min
completed: 2026-02-21
---

# Phase 13 Plan 01: Editor Behavior Fixes Summary

**Three single-file patches fixing code content default color (STRING->IDENTIFIER), missing MODULE_OPEN brace pair, and silent braceDepth overflow in the lexer adapter**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-21T21:00:02Z
- **Completed:** 2026-02-21T21:02:14Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- `MAKO_CODE_CONTENT` token default color changed from STRING (string literal) to IDENTIFIER — code in `<% %>` and `<%! %>` no longer visually misleads as string content
- `<%! %>` module blocks now highlight their opening/closing tags when cursor is placed on either — `MODULE_OPEN/CODE_CLOSE` brace pair added as 6th entry in PAIRS array
- `MakoLexerAdapter.getState()` now emits a `LOG.warn` before clamping when `braceDepth > 15`, making incremental re-lex debugging observable

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix MAKO_CODE_CONTENT default color (VIEW-01)** - `51884a9` (fix)
2. **Task 2: Register MODULE_OPEN brace pair (VIEW-03)** - `819776e` (fix)
3. **Task 3: Log warning on braceDepth overflow (VIEW-06)** - `f348846` (fix)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoSyntaxHighlighter.kt` - Changed MAKO_CODE_CONTENT fallback from STRING to IDENTIFIER
- `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoPairedBraceMatcher.kt` - Added BracePair(MODULE_OPEN, CODE_CLOSE, false) as 6th entry
- `src/main/kotlin/com/schtilig/mako/lang/MakoLexerAdapter.kt` - Added companion object LOG + braceDepth overflow warning in getState()

## Decisions Made
- `MAKO_CODE_CONTENT` fallback changed to `DefaultLanguageHighlighterColors.IDENTIFIER` — code block tokens represent executable code, not string literals; IDENTIFIER gives the correct visual weight
- `MODULE_OPEN` pairs with `CODE_CLOSE` using `structural=false` — same reasoning as `CODE_OPEN`: both share CODE_CLOSE as their close token, structural=true causes conflicts
- Logger in companion object — standard IntelliJ platform pattern: one Logger per class, initialized once

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None - all three changes were straightforward single-line or small-block edits, `./gradlew check` passed after each task.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 13 Plan 01 complete. All three editor behavior defects fixed.
- Color scheme XML files (MakoDefault.xml, MakoDarcula.xml) may want to define explicit `MAKO_CODE_CONTENT` overrides if a scheme had previously relied on the STRING fallback — but this only affects default rendering, not explicit scheme overrides.
- Ready to proceed to Phase 13 Plan 02 or next phase.

---
*Phase: 13-editor-behavior-fixes*
*Completed: 2026-02-21*

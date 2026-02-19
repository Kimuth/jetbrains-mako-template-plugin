---
phase: 04-syntax-highlighting-comment-support
plan: 01
subsystem: ui
tags: [syntax-highlighting, color-scheme, brace-matching, commenting, intellij-platform]

# Dependency graph
requires:
  - phase: 02-lexer
    provides: MakoLexerAdapter and MakoTokenTypes for token-to-highlight mapping
  - phase: 03-parser
    provides: MakoLanguage ID ("Mako Template") used in plugin.xml registrations
provides:
  - MakoSyntaxHighlighter: maps all 24 MakoTokenTypes tokens to 9 TextAttributesKey constants
  - MakoSyntaxHighlighterFactory: creates highlighter instances per file
  - MakoColorSettingsPage: exposes all 9 color attributes in Settings > Editor > Color Scheme
  - MakoPairedBraceMatcher: 5 brace pairs for <%def>, <%block>, <%doc>, ${}, <% %>
  - MakoCommenter: "## " line prefix and "<%doc>"/"</%doc>" block delimiters
  - plugin.xml: 4 new extension point registrations (syntaxHighlighterFactory, colorSettingsPage, braceMatcher, commenter)
affects: [05-template-language-injection, future-phases-needing-color-attributes]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Pre-allocated TextAttributesKey arrays in companion object to avoid per-call allocation
    - structural=false on all BracePair entries to prevent conflicts from shared END_TAG token
    - colorSettingsPage uses 'implementation' attribute (not 'implementationClass') and has no 'language' attribute

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighterFactory.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoColorSettingsPage.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoPairedBraceMatcher.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/editing/MakoCommenter.kt
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "structural=false for ALL BracePair entries: shared END_TAG token across <%def> and <%block> causes platform matching conflicts when structural=true"
  - "colorSettingsPage uses 'implementation' attribute (not 'implementationClass') and has no 'language' attribute — different from other language-scoped extensions"
  - "All MAKO_ TextAttributesKey constants prefixed with MAKO_ to avoid global name collisions with other language highlighters"
  - "TEMPLATE_TEXT and TAG_ATTR_EQ both return EMPTY_KEYS — HTML layer handles template text; TAG_ATTR_EQ is plain punctuation needing no special color"

patterns-established:
  - "Pre-allocated key arrays: declare static array constants for each attribute group instead of creating new arrays on each getTokenHighlights() call"
  - "Token exhaustiveness: every MakoTokenTypes constant must appear in getTokenHighlights() when() — either mapped to a key array or explicitly to EMPTY_KEYS"

requirements-completed: [SYNX-01, SYNX-02, SYNX-03, SYNX-04, SYNX-05, SYNX-07, EDIT-01, EDIT-02]

# Metrics
duration: 2min
completed: 2026-02-19
---

# Phase 4 Plan 01: Syntax Highlighting and Comment Support Summary

**Token-to-color mapping for all 24 Mako token types via SyntaxHighlighter, Settings UI with 9 customizable color attributes, 5 brace pairs for tag matching, and ## / <%doc> comment toggling**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-19T22:48:24Z
- **Completed:** 2026-02-19T22:50:30Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments
- MakoSyntaxHighlighter maps all 24 MakoTokenTypes token constants to 9 TextAttributesKey groups with pre-allocated arrays
- MakoColorSettingsPage exposes all 9 color attributes with descriptive names and a representative Mako demo snippet in Settings > Editor > Color Scheme
- MakoPairedBraceMatcher enables cursor-on-tag brace highlighting for 5 pairs: <%def>, <%block>, <%doc>, ${...}, and <% %>
- MakoCommenter enables Ctrl+/ (## prefix) and Ctrl+Shift+/ (<%doc> wrapping) comment toggling
- All 4 new extension points registered in plugin.xml with correct language IDs; full build passes with all 22 existing tests green

## Task Commits

Each task was committed atomically:

1. **Task 1: MakoSyntaxHighlighter, MakoSyntaxHighlighterFactory, and MakoColorSettingsPage** - `18cad3f` (feat)
2. **Task 2: MakoPairedBraceMatcher and MakoCommenter** - `431b558` (feat)
3. **Task 3: Register Phase 4 extension points in plugin.xml** - `316c2c8` (feat)

## Files Created/Modified
- `src/main/kotlin/.../lang/highlighting/MakoSyntaxHighlighter.kt` - Token-to-TextAttributesKey mapping for all MakoTokenTypes (9 color groups, pre-allocated arrays)
- `src/main/kotlin/.../lang/highlighting/MakoSyntaxHighlighterFactory.kt` - SyntaxHighlighterFactory creating MakoSyntaxHighlighter per file
- `src/main/kotlin/.../lang/highlighting/MakoColorSettingsPage.kt` - ColorSettingsPage with 9 AttributesDescriptors and Mako demo snippet
- `src/main/kotlin/.../lang/highlighting/MakoPairedBraceMatcher.kt` - PairedBraceMatcher with 5 brace pairs all structural=false
- `src/main/kotlin/.../lang/editing/MakoCommenter.kt` - Commenter with "## " line prefix and "<%doc>"/"</%doc>" block delimiters
- `src/main/resources/META-INF/plugin.xml` - Added 4 Phase 4 extension registrations (6 total)

## Decisions Made
- **structural=false for all brace pairs:** The END_TAG token (`</%def>`, `</%block>`, etc.) is shared across multiple tag types. Using structural=true causes the platform to attempt conflict resolution between multiple pairs sharing the same close token, leading to incorrect matching behavior. All pairs use structural=false to avoid this.
- **colorSettingsPage uses 'implementation' not 'implementationClass':** This extension point is not language-scoped and uses a different attribute name. The 'language' attribute is omitted entirely.
- **MAKO_ prefix on all TextAttributesKey names:** The createTextAttributesKey() names are global registry keys. Prefixing with MAKO_ prevents collisions with keys registered by other language plugins (e.g., Python's KEYWORD key).
- **TEMPLATE_TEXT and TAG_ATTR_EQ return EMPTY_KEYS:** Template text is handled by the HTML sublanguage layer in TemplateLanguage mode. TAG_ATTR_EQ is plain punctuation that needs no special color.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 4 syntax highlighting infrastructure is complete
- MakoSyntaxHighlighter.MAKO_* constants are ready to reference from future annotation passes
- MakoColorSettingsPage provides the Settings UI entry point for user customization
- No blockers for subsequent phases

## Self-Check: PASSED

Files verified:
- FOUND: MakoSyntaxHighlighter.kt
- FOUND: MakoSyntaxHighlighterFactory.kt
- FOUND: MakoColorSettingsPage.kt
- FOUND: MakoPairedBraceMatcher.kt
- FOUND: MakoCommenter.kt

Commits verified:
- FOUND: 18cad3f (Task 1)
- FOUND: 431b558 (Task 2)
- FOUND: 316c2c8 (Task 3)

---
*Phase: 04-syntax-highlighting-comment-support*
*Completed: 2026-02-19*

---
phase: 02-lexer
plan: 01
subsystem: lexer
tags: [jflex, grammarkit, kotlin, intellij-platform, flex-adapter, token-types]

# Dependency graph
requires:
  - phase: 01-language-foundation
    provides: MakoLanguage (TemplateLanguage), MakoFileType — IElementType constants use MakoLanguage as the language argument

provides:
  - MakoTokenTypes.kt — 22 IElementType constants covering all Mako constructs (PARS-01)
  - MakoTokenSets.kt — COMMENTS, WHITESPACE, TEMPLATE_CONTENT TokenSet groupings for ParserDefinition
  - MakoLexer.flex — 5-state JFlex definition with brace depth tracking and filter | disambiguation
  - MakoLexerAdapter.kt — FlexAdapter wrapper exposing generated lexer to IntelliJ
  - _MakoLexer.java — generated 78-state DFA implementing FlexLexer.advance()
  - Gradle generateMakoLexer task wired before compileKotlin

affects:
  - 02-02 (ParserDefinition, MakoFile stub)
  - 03-parser (GrammarKit .bnf, MakoParser)
  - 04-highlighting (SyntaxHighlighter uses token types)

# Tech tracking
tech-stack:
  added:
    - JFlex (intellij-deps fork, bundled by GrammarKit 2023.3.0.2) — generates _MakoLexer.java
    - GenerateLexerTask (GrammarKit Gradle plugin) — Gradle task driving JFlex
  patterns:
    - Multi-state JFlex lexer with 5 named states (EXPRESSION, TAG_ATTRS, CODE_BLOCK, MODULE_BLOCK, DOC_COMMENT)
    - Brace depth counter in JFlex user code section for PARS-02 nested construct handling
    - Filter | disambiguation via EXPRESSION state (only in EXPRESSION, not YYINITIAL)
    - FlexAdapter subclass (one-liner) wrapping generated _MakoLexer
    - generateMakoLexer Gradle task wired with compileKotlin { dependsOn(generateMakoLexer) }
    - Generated files committed to src/main/gen/ to enable clean first-time builds

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenSets.kt
    - src/main/grammars/MakoLexer.flex
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerAdapter.kt
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java
  modified:
    - build.gradle.kts

key-decisions:
  - "MakoLexer.flex placed in src/main/grammars/ (not src/main/kotlin/lang/) — cleaner convention separating grammar files from Kotlin sources"
  - "CONTROL_LINE uses single token for entire % line (not split into keyword + body) — Phase 2 only needs restart anchors; Phase 3 parser can distinguish if needed"
  - "TAG_OPEN rule uses yypushback(1) so TAG_ATTRS state reads full tag name as TAG_ATTR_NAME — avoids duplicate token type for the tag keyword"
  - "Generated _MakoLexer.java committed to repo — ensures clean first-time builds without requiring generateMakoLexer to run first"
  - "Only generateMakoLexer activated in Phase 2 — generateMakoParser stays commented, no .bnf file exists until Phase 3"

patterns-established:
  - "Pattern: JFlex EXPRESSION state for | disambiguation — filter separator is only valid inside EXPRESSION, never in YYINITIAL"
  - "Pattern: Brace depth counter reset at EXPR_END transition — brace depth is always 0 at YYINITIAL, satisfying PARS-03 restart semantics"
  - "Pattern: All states return to YYINITIAL at closing delimiters — provides frequent restart anchors for incremental re-lex"

requirements-completed: [PARS-01, PARS-02, PARS-03]

# Metrics
duration: 3min
completed: 2026-02-19
---

# Phase 2 Plan 01: Lexer — Token Types, JFlex Grammar, and Gradle Generation Summary

**JFlex multi-state lexer with 22 IElementType constants, brace depth tracking for nested ${...}, and filter | disambiguation — full build passes in 42s**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-19T19:10:39Z
- **Completed:** 2026-02-19T19:14:19Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Defined all 22 IElementType token constants covering PARS-01 Mako construct coverage (expressions, control lines, block tags, code blocks, comments, template text)
- Created 5-state JFlex lexer (EXPRESSION, TAG_ATTRS, CODE_BLOCK, MODULE_BLOCK, DOC_COMMENT) with multi-state design supporting incremental re-lex restart at YYINITIAL boundaries (PARS-03)
- Implemented brace depth counter for PARS-02 nested `${{'key': 'val'}}` handling — `}` closes expression only when braceDepth == 0
- Implemented filter `|` disambiguation — `FILTER_SEP` token only assigned inside EXPRESSION state, `||` returns `EXPR_CONTENT`, satisfying Pitfall 1 from research
- Activated generateMakoLexer Gradle task, producing 78-state minimized DFA in `_MakoLexer.java`
- Full `./gradlew build` passes including compileKotlin, instrumentCode, and test tasks

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MakoTokenTypes and MakoTokenSets** - `62ded8d` (feat)
2. **Task 2: Create MakoLexer.flex, MakoLexerAdapter, and activate Gradle generation** - `05cf541` (feat)

## Files Created/Modified

- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt` — 22 @JvmField IElementType constants; reuses platform WHITE_SPACE and BAD_CHARACTER
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenSets.kt` — COMMENTS, WHITESPACE, TEMPLATE_CONTENT TokenSet groupings
- `src/main/grammars/MakoLexer.flex` — JFlex lexer with 5 states, brace depth tracking, %% escape handling, control line ^ anchor
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerAdapter.kt` — one-liner FlexAdapter subclass wrapping _MakoLexer()
- `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java` — generated 78-state DFA; implements FlexLexer with advance() method
- `build.gradle.kts` — activated generateMakoLexer task with compileKotlin dependsOn wiring

## Decisions Made

- Placed MakoLexer.flex in `src/main/grammars/` (not `src/main/kotlin/lang/`) — standard convention separating grammar files from Kotlin sources; updated sourceFile path in Gradle task accordingly
- Single `CONTROL_LINE` token for entire `% keyword ...` line — sufficient for Phase 2 restart anchors; Phase 3 parser can split if needed (research open question #1 resolved as planned)
- yypushback(1) in `<%" [a-zA-Z]` rule so TAG_ATTRS state reads the tag keyword as TAG_ATTR_NAME — avoids a separate token type for the tag name
- Committed `_MakoLexer.java` to repository — ensures clean first-time builds without requiring generateMakoLexer to run; aligns with GrammarKit plugin conventions
- generateMakoParser left commented — no .bnf file until Phase 3 (anti-pattern from research)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. JFlex generation produced 78-state minimized DFA without errors. Full build passed on first attempt.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MakoTokenTypes and MakoLexerAdapter are ready for use by MakoParserDefinition in Plan 02
- MakoTokenSets.COMMENTS is ready for `getCommentTokens()` in ParserDefinition
- _MakoLexer.java is generated and committed; Plan 02 can proceed without running generateMakoLexer first
- Blocker from STATE.md resolved: `|` filter disambiguation is implemented in EXPRESSION state

---
*Phase: 02-lexer*
*Completed: 2026-02-19*

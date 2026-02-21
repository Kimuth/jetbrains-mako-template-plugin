---
phase: 02-lexer
plan: 02
subsystem: lexer
tags: [kotlin, intellij-platform, parser-definition, psi, jflex, testing]

# Dependency graph
requires:
  - phase: 02-lexer
    plan: 01
    provides: MakoTokenTypes, MakoTokenSets, MakoLexerAdapter, _MakoLexer.java — all used by ParserDefinition and tests

provides:
  - MakoFile.kt — PsiFileBase stub giving the platform a PsiFile root for Mako files
  - MakoParserDefinition.kt — ParserDefinition wiring MakoLexerAdapter to the IntelliJ platform
  - lang.parserDefinition extension registered in plugin.xml
  - MakoLexerTest.kt — 18 lexer tests verifying all PARS-01/02/03 requirements
  - Fixed TAG_ATTRS closing rule in MakoLexer.flex (> token added)

affects:
  - 03-parser (MakoParserDefinition.createParser() will be implemented; MakoFile extended)
  - 04-highlighting (SyntaxHighlighter uses ParserDefinition.getCommentTokens and WhitespaceTokens)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - MakoParserDefinition as thin bridge — createParser() and createElement() throw UnsupportedOperationException as Phase 3 stubs
    - MakoFile as PsiFileBase stub — implements only getFileType(), Phase 3 extends with PSI factory
    - lang.parserDefinition extension registered before parser exists — platform tolerates missing parser until file is opened
    - BasePlatformTestCase for lexer tests — exercises MakoLexerAdapter directly without requiring a full project fixture

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoFile.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerTest.kt
  modified:
    - src/main/resources/META-INF/plugin.xml
    - src/main/grammars/MakoLexer.flex
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java

key-decisions:
  - "createParser() and createElement() throw UnsupportedOperationException — Phase 3 stubs; platform never calls these until a parser is needed for PSI building"
  - "MakoFile stub delegates getFileType() to MakoFileType singleton — Phase 3 will add createElement factory when GrammarKit parser generates PSI nodes"
  - "lang.parserDefinition language attribute must be 'Mako Template' — exact string identity contract with MakoLanguage.getID() and plugin.xml fileType name"
  - "TAG_ATTRS > close rule added to MakoLexer.flex — Mako named block tags (<%def name='foo'>) end with > not %>"

patterns-established:
  - "Pattern: PsiFileBase stub for template languages — implement getFileType() only, add createElement() in parser phase"
  - "Pattern: ParserDefinition as lexer bridge — createLexer() is the only live method in Phase 2; all others are stubs or use TokenSets"
  - "Pattern: Direct lexer testing via MakoLexerAdapter — tokenize() helper iterates advance() loop, no platform fixture overhead"

requirements-completed: [PARS-01, PARS-02, PARS-03]

# Metrics
duration: 3min
completed: 2026-02-19
---

# Phase 2 Plan 02: Parser Definition, MakoFile Stub, and Lexer Verification Summary

**MakoParserDefinition + MakoFile registered in plugin.xml, 18 lexer tests confirm PARS-01/02/03 with zero BAD_CHARACTER tokens — lexer pipeline complete**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-19T19:17:58Z
- **Completed:** 2026-02-19T19:21:52Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Created MakoParserDefinition wiring MakoLexerAdapter to the IntelliJ platform via createLexer(), with Phase 3 stubs for createParser() and createElement()
- Created MakoFile.kt (PsiFileBase subclass) providing the platform's PsiFile root for .mako files
- Registered lang.parserDefinition extension in plugin.xml — platform can now invoke the Mako lexer when opening .mako files
- Created 18 lexer tests covering all Mako construct types: expressions, filters, control lines, named tags, code blocks, module blocks, doc comments, nested braces, boolean OR disambiguation, and YYINITIAL restart state
- Fixed TAG_ATTRS state missing `>` close rule — named block tags like `<%def name="foo">` were producing BAD_CHARACTER for the closing `>`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MakoFile, MakoParserDefinition, and register in plugin.xml** - `3d2a969` (feat)
2. **Task 2: Verify lexer token output for all Mako construct types** - `0be4205` (feat)

## Files Created/Modified

- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoFile.kt` — PsiFileBase stub returning MakoFileType; Phase 3 will extend with createElement factory
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt` — ParserDefinition bridge; createLexer returns MakoLexerAdapter(), getWhitespaceTokens/getCommentTokens use MakoTokenSets
- `src/main/resources/META-INF/plugin.xml` — added lang.parserDefinition extension for "Mako Template" language
- `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerTest.kt` — 18 tests; tokenize() helper, assertNoBADCharacter(), tests for all PARS-01/02/03 requirements
- `src/main/grammars/MakoLexer.flex` — added `">"` rule in TAG_ATTRS state for named block tag close
- `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java` — regenerated 78-state DFA after flex fix

## Decisions Made

- `createParser()` and `createElement()` throw `UnsupportedOperationException` with "Phase 3" messages — platform never calls these during lexer-only operation; stubs make Phase 3 responsibilities explicit
- `MakoFile` stub only implements `getFileType()` — minimal implementation sufficient for Phase 2; adding PSI node factory before GrammarKit parser exists would be premature
- Language attribute `language="Mako Template"` in lang.parserDefinition must exactly match `MakoLanguage.getID()` — string identity contract already established in Phase 1
- `TAG_ATTRS` `>` close rule placement — placed before the `.` BAD_CHARACTER fallback, after `%>` and `/>`, preserving rule priority

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed missing > close rule in TAG_ATTRS JFlex state**
- **Found during:** Task 2 (Verify lexer token output for all Mako construct types)
- **Issue:** `testNamedTag` produced `BAD_CHARACTER` for `>` in `<%def name="foo">`. TAG_ATTRS state only handled `%>` and `/>` as TAG_CLOSE, but Mako named block tags close with plain `>`. `testMixedMakoFile` also failed for same reason.
- **Fix:** Added `">" { yybegin(YYINITIAL); return TAG_CLOSE; }` rule in TAG_ATTRS state in MakoLexer.flex. Regenerated _MakoLexer.java.
- **Files modified:** `src/main/grammars/MakoLexer.flex`, `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java`
- **Verification:** Both failing tests now pass; all 18 tests pass with 0 BAD_CHARACTER tokens
- **Committed in:** `0be4205` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in flex grammar)
**Impact on plan:** Fix was necessary for correctness. No scope creep. The plan's pseudo-code flex rules omitted the plain `>` case for named tags.

## Issues Encountered

None beyond the auto-fixed flex grammar bug. Build and tests passed cleanly after the fix.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MakoParserDefinition is registered — platform wires lexer to .mako files on open
- MakoFile stub is ready — Phase 3 extends with PSI element factory when GrammarKit parser is generated
- All PARS-01, PARS-02, PARS-03 requirements verified by passing tests
- Phase 2 (Lexer) is now complete — Phase 3 (Parser) can begin

## Self-Check: PASSED

- MakoFile.kt: FOUND
- MakoParserDefinition.kt: FOUND
- MakoLexerTest.kt: FOUND
- 02-02-SUMMARY.md: FOUND
- Commit 3d2a969 (Task 1): FOUND
- Commit 0be4205 (Task 2): FOUND

---
*Phase: 02-lexer*
*Completed: 2026-02-19*

---
phase: 03-parser
plan: 01
subsystem: parser
tags: [grammarkit, bnf, jflex, psi, parser-generation, kotlin]

# Dependency graph
requires:
  - phase: 02-lexer
    provides: MakoLexer.flex with all token types, MakoTokenTypes.kt, MakoTokenSets.kt, MakoLexerAdapter
provides:
  - Mako.bnf grammar with distinct rules for all 6 named tag types and all other constructs
  - Per-tag token types TAG_OPEN_DEF through TAG_OPEN_PAGE in lexer and MakoTokenTypes
  - MakoElementType.kt and MakoTokenType.kt IElementType subclasses for BNF header references
  - Generated MakoParser.java, MakoTypes.java, typed PSI interfaces and implementations
  - generateMakoParser Gradle task active in build.gradle.kts
affects: [03-02-psi-mixin, 04-template-language, 05-references, 06-completion]

# Tech tracking
tech-stack:
  added: [GrammarKit BNF grammar file (.bnf), generateMakoParser Gradle task]
  patterns:
    - generateTokens=false prevents duplicate token constants when BNF tokens map to existing Kotlin constants
    - pin=1 on every tag/expression rule commits parser after first token for error recovery
    - recoverWhile predicates list all construct boundary tokens to stop over-consuming on error
    - private item_ (trailing underscore) prevents wrapper PSI node for the file's top-level dispatch rule
    - private tag_attribute prevents extra PSI node layer for attribute key/value pairs
    - line_comment_rule naming avoids BNF rule/token name collision with LINE_COMMENT token

key-files:
  created:
    - src/main/grammars/Mako.bnf
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoElementType.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenType.kt
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoTypes.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoDefTag.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoBlockTag.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoExpression.java
    - (plus 10 more PSI interface/impl files in src/main/gen)
  modified:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenSets.kt
    - src/main/grammars/MakoLexer.flex
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerTest.kt
    - build.gradle.kts

key-decisions:
  - "Per-tag token types (TAG_OPEN_DEF..TAG_OPEN_PAGE) replace generic TAG_OPEN — distinct token types required for GrammarKit to produce distinct PSI node types per tag"
  - "generateTokens=false in BNF header prevents silent parse failures caused by double-generated token constants (research Pitfall 2)"
  - "No yypushback after per-tag rules — tag keyword fully consumed in TAG_OPEN_xxx token, TAG_ATTRS state reads first attribute name directly"
  - "line_comment_rule name used instead of line_comment to avoid BNF rule/token name collision"
  - "def_tag and block_tag include mixin+implements attributes in BNF — mixin classes created in Plan 02"
  - "Generated PSI files committed to repo — follows Phase 2 precedent for clean first-time builds"
  - "generateMakoLexer sourceFile comment updated from Phase 2 — both lexer and parser generation now active"

patterns-established:
  - "BNF grammar in src/main/grammars/ alongside MakoLexer.flex — grammar files separated from Kotlin sources"
  - "Generated files committed to src/main/gen/ — ensures reproducible builds without GrammarKit tooling requirement"
  - "pin=1 + recoverWhile on every construct rule — standard error recovery pattern for GrammarKit grammars"

requirements-completed: [PARS-04, PARS-05]

# Metrics
duration: 4min
completed: 2026-02-19
---

# Phase 3 Plan 1: Parser Grammar and PSI Generation Summary

**GrammarKit BNF grammar with 6 per-tag token types, error recovery on all rules, and generated MakoParser.java + typed PSI interfaces via generateMakoParser Gradle task**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-19T21:15:22Z
- **Completed:** 2026-02-19T21:19:00Z
- **Tasks:** 2
- **Files modified:** 28

## Accomplishments

- Split generic TAG_OPEN into 6 distinct token types (TAG_OPEN_DEF, TAG_OPEN_BLOCK, TAG_OPEN_INHERIT, TAG_OPEN_INCLUDE, TAG_OPEN_NAMESPACE, TAG_OPEN_PAGE) enabling distinct PSI node types per Mako tag
- Created Mako.bnf with all Mako constructs (def_tag, block_tag, inherit_tag, include_tag, namespace_tag, page_tag, expression, control_line, code_block, module_block, doc_comment, line_comment_rule) and pin+recoverWhile error recovery on every rule
- Activated generateMakoParser Gradle task; generated MakoParser.java, MakoTypes.java, 12 typed PSI interface/implementation files in src/main/gen

## Task Commits

Each task was committed atomically:

1. **Task 1: Split TAG_OPEN into per-tag token types, add MakoElementType/MakoTokenType** - `8f7446f` (feat)
2. **Task 2: Create Mako.bnf grammar and activate generateMakoParser** - `ea3c090` (feat)

**Plan metadata:** (docs commit, see below)

## Files Created/Modified

- `src/main/grammars/Mako.bnf` - GrammarKit BNF grammar with all Mako construct rules, error recovery, generateTokens=false
- `src/main/kotlin/.../lang/MakoElementType.kt` - IElementType subclass for composite (parser) element types referenced by BNF elementTypeClass
- `src/main/kotlin/.../lang/MakoTokenType.kt` - IElementType subclass for BNF tokenTypeClass reference
- `src/main/kotlin/.../lang/MakoTokenTypes.kt` - Replaced TAG_OPEN with 6 TAG_OPEN_xxx constants
- `src/main/kotlin/.../lang/MakoTokenSets.kt` - Added TAG_OPENS TokenSet with all 6 per-tag types
- `src/main/grammars/MakoLexer.flex` - 6 per-tag rules replace single generic <%[a-zA-Z] rule
- `src/main/gen/.../lang/_MakoLexer.java` - Regenerated with per-tag token return statements
- `src/main/gen/.../lang/parser/MakoParser.java` - GrammarKit-generated parser
- `src/main/gen/.../lang/psi/MakoTypes.java` - Element type constants (DEF_TAG, BLOCK_TAG, EXPRESSION, etc.)
- `src/main/gen/.../lang/psi/Mako{DefTag,BlockTag,Expression,...}.java` - Typed PSI interfaces
- `src/main/gen/.../lang/psi/impl/Mako{DefTag,BlockTag,Expression,...}Impl.java` - PSI implementations
- `src/test/kotlin/.../lang/MakoLexerTest.kt` - Fixed testNamedTag, added testTagOpenTypes
- `build.gradle.kts` - Activated generateMakoParser task, added to compileKotlin dependencies

## Decisions Made

- Per-tag token types replace generic TAG_OPEN because GrammarKit requires distinct token types to produce distinct PSI node types per tag construct (PARS-04 requirement)
- `generateTokens=false` set in BNF header to prevent GrammarKit from generating duplicate token constants that would silently override the existing Kotlin constants and cause parse failures
- No `yypushback` in per-tag flex rules — tag keyword is fully consumed as part of the TAG_OPEN_xxx token; TAG_ATTRS state now reads the first attribute name directly (previously the tag name was pushed back and re-read as TAG_ATTR_NAME)
- `line_comment_rule` rule name used to avoid name collision with the `LINE_COMMENT` token constant
- `def_tag` and `block_tag` BNF rules include `mixin` and `implements` attributes for PsiNamedElement — mixin classes will be created in Plan 02; generation succeeds with warnings but no errors
- Generated PSI files committed to repo following the Phase 2 precedent established in 02-01

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The `generateMakoParser` task emitted expected WARNs about missing mixin methods (`getName`, `setName`, `getNameIdentifier` on MakoBlockTag and MakoDefTag) — these are by design as the mixin classes are created in Plan 02. The BUILD SUCCESSFUL status confirms these are warnings only, not errors.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 (PSI mixin classes): MakoParser.java and typed PSI interfaces (MakoDefTag.java, MakoBlockTag.java) exist and are ready to receive mixin implementations
- The generated `MakoTypes.java` provides the element type factory that Plan 02's `MakoParserDefinition` will reference for `createElement()`
- `./gradlew build` will fail until Plan 02 creates the mixin classes (`MakoDefTagMixin`, `MakoBlockTagMixin`) — this is expected and documented in the plan

---
*Phase: 03-parser*
*Completed: 2026-02-19*

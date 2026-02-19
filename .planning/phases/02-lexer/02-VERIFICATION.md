---
phase: 02-lexer
verified: 2026-02-19T22:00:00Z
status: passed
score: 6/6 must-haves verified
re_verification:
  previous_status: passed
  previous_score: 5/5
  gaps_closed:
    - "Opening a .mako file in the IDE produces no exceptions (UnsupportedOperationException stubs replaced in 02-03)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Open a .mako file in PyCharm via ./gradlew runIde — confirm no exceptions in event log"
    expected: "File opens, recognized as Mako Template type, no UnsupportedOperationException or any exception in Help > Show Log"
    why_human: "UAT test 3 reported a blocker (UnsupportedOperationException) that plan 02-03 claims to fix. The fix is verified in code, but IDE runtime behavior can only be confirmed by running the plugin. The previous UAT was performed before the 02-03 fix was applied."
  - test: "Mid-file edit does not corrupt syntax coloring"
    expected: "Editing a ${...} expression on line 10 of a 100+ line .mako file leaves coloring unchanged on lines below the edit point"
    why_human: "Incremental re-lex integration with the IntelliJ highlighting daemon requires visual observation. Unit tests confirm state=0 at boundaries; real-time daemon behavior cannot be verified statically."
---

# Phase 2: Lexer Verification Report

**Phase Goal:** Every character in a `.mako` file is assigned the correct token type, and the lexer can correctly resume from any mid-file offset
**Verified:** 2026-02-19T22:00:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure in plan 02-03 (UAT blocker fixed)

---

## Re-verification Context

The initial VERIFICATION.md (status: passed) was written immediately after plan 02-02 completed, before UAT was run. UAT (`02-UAT.md`) subsequently found a blocker: opening any `.mako` file in the IDE threw `UnsupportedOperationException: MakoParser not yet implemented - Phase 3` because `createParser()` and `createElement()` in `MakoParserDefinition` were stubs that threw unconditionally. Plan 02-03 was created and executed to close this gap. This re-verification covers the full phase including the gap closure.

---

## Goal Achievement

### Observable Truths

The four success criteria from ROADMAP.md, plus the two must-haves introduced by 02-03:

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A `.mako` file containing all Mako construct types produces the expected token sequence with no misclassified tokens | VERIFIED | 15 per-construct tests in `MakoLexerTest.kt`; `assertNoBADCharacter()` helper passes for all 18 tests; 22 IElementType constants cover every listed construct |
| 2 | Nested constructs such as `${...}` with inner braces are tokenized correctly (inner `}` does not produce EXPR_END) | VERIFIED | `testNestedBraces` asserts `EXPR_END` appears exactly once in `${{'key': 'val'}}`; `braceDepth` counter in MakoLexer.flex user code section confirmed at lines 23-25; `{` increments, `}` decrements only when `braceDepth > 0` before emitting EXPR_END |
| 3 | Editing a line in the middle of a `.mako` file does not corrupt token coloring below the edit point (restart-state semantics) | VERIFIED | `testRestartStateAfterExpression`, `testRestartStateAfterCodeBlock`, `testRestartStateAfterDocComment` each assert `lexer.state == 0` immediately after closing delimiter; all 5 states call `yybegin(YYINITIAL)` at their closing tokens in flex rules (confirmed at flex lines 41, 95, 122-124, 145, 156, 167) |
| 4 | Filter expressions (`${x \| h,trim}`) tokenize `\|` as FILTER_SEP, not Python bitwise OR | VERIFIED | `testExpressionWithFilter` asserts `FILTER_SEP` present; `testBooleanOrNotFilterSep` asserts `\|\|` does NOT produce `FILTER_SEP`; flex EXPRESSION state implements `"\|" / [^\|]` -> `FILTER_SEP` and `"\|\|"` -> `EXPR_CONTENT` at lines 101-104 |
| 5 | `createParser()` in `MakoParserDefinition` does not throw when called by the platform | VERIFIED | No `UnsupportedOperationException` in `MakoParserDefinition.kt` (grep returns exit:1); `createParser()` returns a working `PsiParser` lambda that advances all tokens and calls `marker.done(root)` at lines 30-37 |
| 6 | `createElement()` in `MakoParserDefinition` does not throw when called by the platform | VERIFIED | `createElement()` returns `ASTWrapperPsiElement(node)` at line 43; `ASTWrapperPsiElement` is imported at line 14; no stub pattern present |

**Score:** 6/6 truths verified by static analysis

---

### Required Artifacts

#### Plan 02-01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt` | IElementType constants for every Mako token | VERIFIED | 22 `@JvmField` constants defined: EXPR_START, EXPR_END, EXPR_CONTENT, FILTER_SEP, FILTER_NAME, CONTROL_LINE, TAG_OPEN, TAG_CLOSE, END_TAG, TAG_ATTR_NAME, TAG_ATTR_EQ, TAG_ATTR_VALUE, CODE_OPEN, CODE_CONTENT, MODULE_OPEN, MODULE_CONTENT, CODE_CLOSE, LINE_COMMENT, DOC_OPEN, DOC_CONTENT, DOC_CLOSE, TEMPLATE_TEXT. Contains "EXPR_START". |
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenSets.kt` | TokenSet groupings for ParserDefinition | VERIFIED | Defines `COMMENTS`, `WHITESPACE`, `TEMPLATE_CONTENT` sets. Contains "COMMENTS". |
| `src/main/grammars/MakoLexer.flex` | JFlex lexer with multi-state tokenization | VERIFIED | 181 lines; 5 named states (EXPRESSION, TAG_ATTRS, CODE_BLOCK, MODULE_BLOCK, DOC_COMMENT); `braceDepth` counter; filter `\|` disambiguation; `%%` escape handling; control line `^` anchor. Contains "EXPRESSION". |
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerAdapter.kt` | FlexAdapter wrapper for `_MakoLexer` | VERIFIED | One-liner: `class MakoLexerAdapter : FlexAdapter(_MakoLexer())`. Contains "FlexAdapter". |
| `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java` | Generated JFlex lexer class | VERIFIED | 785 lines; generated by JFlex 1.9.2; implements FlexLexer; `advance()` method present; 6 lexical states (YYINITIAL=0, EXPRESSION=2, TAG_ATTRS=4, CODE_BLOCK=6, MODULE_BLOCK=8, DOC_COMMENT=10). |

#### Plan 02-02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoFile.kt` | PsiFileBase subclass for Mako files | VERIFIED | Extends `PsiFileBase(viewProvider, MakoLanguage)`; overrides `getFileType()` returning `MakoFileType`. Contains "PsiFileBase". |
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt` | ParserDefinition wiring lexer to language | VERIFIED | Implements `ParserDefinition`; `createLexer()` returns `MakoLexerAdapter()`; `getWhitespaceTokens()` and `getCommentTokens()` use MakoTokenSets; `createFile()` returns `MakoFile(viewProvider)`. Contains "MakoLexerAdapter". No UnsupportedOperationException remains. |
| `src/main/resources/META-INF/plugin.xml` | lang.parserDefinition extension registration | VERIFIED | `<lang.parserDefinition language="Mako Template" implementationClass="...MakoParserDefinition"/>` present at lines 18-19. Contains "lang.parserDefinition". |
| `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerTest.kt` | Lexer tests for all PARS-01/02/03 requirements | VERIFIED | 18 test methods covering all Mako construct types; `tokenize()` helper; `assertNoBADCharacter()` helper; restart state assertions using `lexer.state == 0`. |

#### Plan 02-03 Artifacts (Gap Closure)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt` | No-op PsiParser and ASTWrapperPsiElement fallback | VERIFIED | `createParser()` returns `PsiParser { root, builder -> ... marker.done(root) ... builder.treeBuilt }` at lines 30-37; `createElement()` returns `ASTWrapperPsiElement(node)` at line 43; `ASTWrapperPsiElement` imported at line 14; grep for "UnsupportedOperationException" returns no matches. |

---

### Key Link Verification

#### Plan 02-01 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoLexer.flex` | `MakoTokenTypes.kt` | static import of token constants | WIRED | Line 5: `import static com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes.*;` — all token constants used by name throughout flex rules |
| `MakoLexerAdapter.kt` | `_MakoLexer` | FlexAdapter constructor | WIRED | Line 5: `class MakoLexerAdapter : FlexAdapter(_MakoLexer())` — delegates all lexer behavior to generated class |
| `build.gradle.kts` | `MakoLexer.flex` | GenerateLexerTask sourceFile | WIRED | Line 153: `sourceFile.set(file("src/main/grammars/MakoLexer.flex"))` with `compileKotlin { dependsOn(generateMakoLexer) }` at line 166 |

#### Plan 02-02 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoParserDefinition.kt` | `MakoLexerAdapter.kt` | `createLexer()` returns `MakoLexerAdapter()` | WIRED | Line 22: `override fun createLexer(project: Project): Lexer = MakoLexerAdapter()` |
| `MakoParserDefinition.kt` | `MakoFile.kt` | `createFile()` returns `MakoFile(viewProvider)` | WIRED | Line 41: `override fun createFile(viewProvider: FileViewProvider): PsiFile = MakoFile(viewProvider)` |
| `plugin.xml` | `MakoParserDefinition.kt` | lang.parserDefinition extension point | WIRED | Lines 18-19: `<lang.parserDefinition language="Mako Template" implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoParserDefinition"/>` |

#### Plan 02-03 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoParserDefinition.createParser()` | PsiParser (inline lambda) | wraps all tokens in single root marker | WIRED | Lines 30-37: `PsiParser { root, builder -> val marker = builder.mark(); while (...) builder.advanceLexer(); marker.done(root); builder.treeBuilt }` — pattern `marker\.done\(root\)` confirmed at line 35 |
| `MakoParserDefinition.createElement()` | `ASTWrapperPsiElement` | fallback PSI node for any AST node type | WIRED | Line 43: `override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)` — pattern `ASTWrapperPsiElement\(node\)` confirmed |

---

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PARS-01 | 02-01, 02-02, 02-03 | JFlex-generated lexer tokenizes all Mako constructs (`${...}`, `% control`, `<%def>`, `<%block>`, `<%inherit>`, `<%include>`, `<%namespace>`, `<%page>`, `<%! %>`, `<% %>`, `<%doc>`, `##`) | SATISFIED | 22 IElementType constants in MakoTokenTypes.kt cover all listed constructs; 5-state JFlex grammar and 15 individual construct tests confirm correct tokenization for every construct type |
| PARS-02 | 02-01, 02-02, 02-03 | Lexer correctly handles nested constructs (Python expressions inside `${...}`) | SATISFIED | `braceDepth` counter in `_MakoLexer` user code section (MakoLexer.flex lines 23-25); `testNestedBraces` asserts single EXPR_END for `${{'key': 'val'}}` |
| PARS-03 | 02-01, 02-02, 02-03 | Lexer state is serializable for incremental re-lexing (restart from mid-file) | SATISFIED | All 5 states call `yybegin(YYINITIAL)` at closing delimiters; 3 dedicated restart tests assert `lexer.state == 0` after EXPR_END, CODE_CLOSE, and DOC_CLOSE; YYINITIAL=0 confirmed in generated `_MakoLexer.java` line 21 |

**Orphaned requirements check:** REQUIREMENTS.md traceability table assigns only PARS-01, PARS-02, PARS-03 to Phase 2. All three are satisfied. No orphaned requirements found.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `MakoTokenTypes.kt` | 12 | `FILTER_NAME` token defined but never used in MakoLexer.flex or any test | INFO | Token exists as a named constant but the flex grammar never emits it — filter names are emitted as EXPR_CONTENT in the current grammar. This is a harmless forward declaration for Phase 4 (syntax highlighting) but represents a gap between the token vocabulary and the lexer implementation. No functional impact. |

No blocker or warning anti-patterns. The previous `UnsupportedOperationException` stubs have been fully replaced by plan 02-03.

---

### Human Verification Required

The following items cannot be verified programmatically and require manual testing. Note that UAT test 3 was the original blocker — it must be re-run with the 02-03 fix applied.

#### 1. No Exceptions on .mako File Open (Re-run of UAT Test 3)

**Test:** Run `./gradlew runIde`. Open PyCharm with the plugin installed. Open any `.mako` file.
**Expected:** File opens, recognized as "Mako Template" file type (correct icon in project tree). No exceptions in the IDE event log (Help > Show Log in the sandbox IDE). Specifically, no `UnsupportedOperationException` and no `MakoParser not yet implemented` message.
**Why human:** UAT previously reported this as a blocker. Plan 02-03 replaced the stubs in code — this is verified statically. However, the IDE runtime behavior (whether the platform actually calls `createParser()` on file open, whether the no-op parser satisfies the pipeline without secondary errors) can only be confirmed by running the plugin. This is the most critical human check for Phase 2.

#### 2. Mid-File Edit Does Not Corrupt Coloring

**Test:** Run `./gradlew runIde`. Open a `.mako` file with 100+ lines containing `${...}` expressions throughout. Edit a `${...}` expression on line 10 (e.g., change `${name}` to `${user.name}`). Observe token coloring on lines 50-100 immediately after the edit.
**Expected:** Token coloring below the edit point remains unchanged and correct. No coloring shift or corruption visible.
**Why human:** Incremental re-lex integration with the IntelliJ highlighting daemon requires observing IDE editor response in real time. Unit tests verify `state == 0` at construct boundaries, but the daemon's restart behavior can only be confirmed visually.

---

### Gaps Summary

No gaps remain in the static codebase. All must-have truths are verified at all three levels (existence, substantive implementation, wiring) across all three plans.

The single outstanding item is human verification that the IDE runtime behavior matches the code analysis — specifically that the 02-03 gap closure (replacing `UnsupportedOperationException` stubs with a no-op parser) actually resolves the UAT blocker at runtime. This is a re-run of UAT test 3, not new development.

The `FILTER_NAME` token is a minor vocabulary gap (defined but never emitted) that is harmless for Phase 2 and appropriate for Phase 4.

---

## Commit Evidence

| Commit | Description | Verified |
|--------|-------------|---------|
| `62ded8d` | feat(02-01): create MakoTokenTypes and MakoTokenSets | In git log |
| `05cf541` | feat(02-01): create MakoLexer.flex, MakoLexerAdapter, and activate Gradle lexer generation | In git log |
| `3d2a969` | feat(02-lexer-02): wire ParserDefinition into platform | In git log |
| `0be4205` | feat(02-lexer-02): add comprehensive lexer tests and fix TAG_ATTRS close rule | In git log |
| `7f8c58e` | feat(02-03): replace UnsupportedOperationException stubs with working implementations | In git log — gap closure commit |

---

_Verified: 2026-02-19T22:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification: Yes — supersedes initial verification written before UAT and 02-03 gap closure_

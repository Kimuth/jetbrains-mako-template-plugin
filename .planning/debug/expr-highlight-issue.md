---
status: resolved
trigger: "Expression constructs (${foo}, ${\"foo\"}) in .mako files display as plain text with no syntax highlighting color"
created: 2026-02-20T00:00:00Z
updated: 2026-02-20T00:00:00Z
---

## Current Focus

hypothesis: TEMPLATE_LANGUAGE_COLOR falls back to HighlighterColors.TEXT (plain editor text), making expressions invisible
test: Traced the color inheritance chain from MAKO_EXPRESSION -> TEMPLATE_LANGUAGE_COLOR -> HighlighterColors.TEXT
expecting: Confirms expressions render with default text color (no visible distinction)
next_action: Report root cause and suggested fix

## Symptoms

expected: `${...}` expressions should display in a distinct color in the editor
actual: `${...}` expressions display as plain text with no syntax highlighting color
errors: None (no errors thrown; behavior is a color-mapping issue)
reproduction: Open any .mako file containing `${foo}` in IntelliJ with the plugin installed
started: Likely since initial implementation of MakoSyntaxHighlighter

## Eliminated

- hypothesis: Lexer does not produce EXPR_START/EXPR_END/EXPR_CONTENT tokens for `${...}` expressions
  evidence: MakoLexer.flex correctly transitions to EXPRESSION state on `${` and emits EXPR_START, EXPR_CONTENT, EXPR_END tokens. All 16 MakoLexerTest tests pass, including testExpressionBasic, testExpressionWithFilter, testNestedBraces, testBooleanOrNotFilterSep, and testRestartStateAfterExpression. The generated _MakoLexer.java matches the .flex rules.
  timestamp: 2026-02-20T00:01:00Z

- hypothesis: MakoSyntaxHighlighter.getTokenHighlights() does not map expression tokens to a color
  evidence: Lines 88-93 of MakoSyntaxHighlighter.kt explicitly map EXPR_START, EXPR_END, EXPR_CONTENT, FILTER_SEP, and FILTER_NAME to EXPRESSION_KEYS (which wraps MAKO_EXPRESSION). The mapping is present and correct.
  timestamp: 2026-02-20T00:02:00Z

- hypothesis: Plugin registration is missing or incorrect
  evidence: plugin.xml correctly registers lang.syntaxHighlighterFactory and colorSettingsPage. MakoSyntaxHighlighterFactory correctly returns MakoSyntaxHighlighter(). MakoColorSettingsPage includes the Expression descriptor.
  timestamp: 2026-02-20T00:03:00Z

## Evidence

- timestamp: 2026-02-20T00:01:00Z
  checked: MakoLexer.flex EXPRESSION state rules
  found: Line 41 correctly matches `${` and returns EXPR_START, transitioning to EXPRESSION state. Lines 88-121 handle expression content including nested braces, filters, string literals, and the closing `}`. All rules produce EXPR_CONTENT, FILTER_SEP, or EXPR_END as expected.
  implication: Lexer tokenization is correct. Bug is not in the lexer.

- timestamp: 2026-02-20T00:02:00Z
  checked: _MakoLexer.java generated code (switch cases 3-6, 15, 17)
  found: Generated Java code matches .flex file: case 15 returns EXPR_START with yybegin(EXPRESSION), case 3 returns EXPR_CONTENT, case 4 handles brace depth increment, case 5 returns FILTER_SEP, case 6 handles closing brace with depth check returning EXPR_END.
  implication: Generated lexer faithfully implements the .flex spec. No generation bug.

- timestamp: 2026-02-20T00:03:00Z
  checked: MakoSyntaxHighlighter.kt getTokenHighlights() method
  found: Lines 88-93 map all five expression token types (EXPR_START, EXPR_END, EXPR_CONTENT, FILTER_SEP, FILTER_NAME) to EXPRESSION_KEYS. EXPRESSION_KEYS wraps MAKO_EXPRESSION.
  implication: Token-to-color mapping is present and correct. Bug is in the color definition itself.

- timestamp: 2026-02-20T00:04:00Z
  checked: MAKO_EXPRESSION TextAttributesKey definition (line 21-23 of MakoSyntaxHighlighter.kt)
  found: MAKO_EXPRESSION is created with fallback `DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR`
  implication: The actual appearance depends on what TEMPLATE_LANGUAGE_COLOR resolves to.

- timestamp: 2026-02-20T00:05:00Z
  checked: IntelliJ source code for DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR
  found: TEMPLATE_LANGUAGE_COLOR is defined as `createTextAttributesKey("DEFAULT_TEMPLATE_LANGUAGE_COLOR", HighlighterColors.TEXT)`. HighlighterColors.TEXT is the plain default editor text -- it has NO foreground color, NO background color, NO font style. It is literally "use whatever the editor default text looks like."
  implication: This is the root cause. MAKO_EXPRESSION inherits from TEMPLATE_LANGUAGE_COLOR which inherits from TEXT, so expressions render with the exact same appearance as normal editor text.

- timestamp: 2026-02-20T00:06:00Z
  checked: Comparison with other token types that DO display correctly
  found: Directives and control lines use `DefaultLanguageHighlighterColors.KEYWORD` (blue/purple in most themes). Line comments use `DefaultLanguageHighlighterColors.LINE_COMMENT` (gray/green). Block comments use `DefaultLanguageHighlighterColors.BLOCK_COMMENT` (gray/green). All of these have explicit foreground colors defined in standard IntelliJ themes. TEMPLATE_LANGUAGE_COLOR does not.
  implication: The contrast proves the issue. Other constructs use colors with visible foreground; expressions use one that resolves to plain text.

- timestamp: 2026-02-20T00:07:00Z
  checked: MakoLexerTest suite (16 tests)
  found: All tests pass. Expression tokenization tests (testExpressionBasic, testExpressionWithFilter, testNestedBraces, testBooleanOrNotFilterSep) all verify correct token types. testMixedMakoFile verifies no BAD_CHARACTER in a multi-construct file.
  implication: The entire lexer pipeline is working correctly. This is purely a color-choice issue.

## Resolution

root_cause: `MAKO_EXPRESSION` is defined with fallback `DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR`, which in turn falls back to `HighlighterColors.TEXT` (plain editor text). This means expression tokens (`${...}`) are tokenized correctly but render with the default text color, making them visually indistinguishable from surrounding template text. In contrast, other Mako constructs use `KEYWORD`, `LINE_COMMENT`, and `BLOCK_COMMENT` which all have explicit foreground colors in standard themes.

fix: Change the fallback color for `MAKO_EXPRESSION` in `MakoSyntaxHighlighter.kt` from `DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR` to a more visually distinct color. Additionally, consider splitting the expression tokens into separate color categories (delimiter vs. content) for finer-grained control.

Recommended options for the fallback color:
1. `DefaultLanguageHighlighterColors.MARKUP_TAG` -- gives a visually distinct color for the delimiters
2. `DefaultLanguageHighlighterColors.NUMBER` -- often a distinct color (used by some template plugins)
3. `DefaultLanguageHighlighterColors.INSTANCE_FIELD` -- purple/magenta in many themes
4. Keep `TEMPLATE_LANGUAGE_COLOR` but split into two keys:
   - `MAKO_EXPRESSION_DELIMITER` (for `${` and `}`) using `DefaultLanguageHighlighterColors.KEYWORD` or `MARKUP_TAG`
   - `MAKO_EXPRESSION_CONTENT` (for the expression body) using `TEMPLATE_LANGUAGE_COLOR` or a code-like color

verification: N/A (diagnosis only -- no fix applied yet)
files_changed: []

### Key Files

- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt` (line 22): MAKO_EXPRESSION fallback color needs to change
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt`: Token types are correct, no changes needed
- `src/main/grammars/MakoLexer.flex`: Lexer rules are correct, no changes needed
- `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java`: Generated correctly, no changes needed

---
status: resolved
trigger: "TEMPLATE_TEXT tokens that follow a valid ${...} expression are being consumed inside the EXPRESSION PSI node as PsiErrorElement instead of appearing as sibling template_text_content nodes at file level."
created: 2026-02-21T00:00:00Z
updated: 2026-02-21T00:10:00Z
---

## Current Focus

hypothesis: CONFIRMED - expression_recover missing TEMPLATE_TEXT in stop set
test: Added TEMPLATE_TEXT to expression_recover stop set, ran all tests
expecting: all tests pass with no PsiErrorElement for TEMPLATE_TEXT after expressions
next_action: DONE - fix verified, session resolved

## Symptoms

expected: After parsing `${foo}`, remaining TEMPLATE_TEXT like `bar` should be parsed as a top-level `MakoTemplateTextContentImpl(TEMPLATE_TEXT_CONTENT)` sibling in the FILE node.
actual: `bar` ends up INSIDE the `MakoExpressionImpl(EXPRESSION)` node as `PsiErrorElement:'bar' unexpected`. The EXPRESSION node spans the ENTIRE file (0,10) even though the expression tokens only cover (0,6).
errors: |
  Bad PSI tree for `${foo}bar`:
  FILE(0,10)
    MakoExpressionImpl(EXPRESSION)(0,10)
      PsiElement(EXPR_START)('${')(0,2)
      PsiElement(EXPR_CONTENT)('foo')(2,5)
      PsiElement(EXPR_END)('}')(5,6)
      PsiErrorElement:'bar' unexpected(7,10)
        PsiElement(TEMPLATE_TEXT)('bar')(7,10)
reproduction: Create a .mako file with content `${foo}bar` and inspect PSI tree via PsiViewer.
started: Has never worked correctly for TEMPLATE_TEXT following expressions directly.

## Eliminated

(none - root cause found on first hypothesis)

## Evidence

- timestamp: 2026-02-21T00:00:00Z
  checked: src/main/grammars/Mako.bnf — expression_recover rule (lines 146-149)
  found: |
    expression_recover ::= !(EXPR_START | EXPR_END | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT
                           | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
                           | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
                           | LINE_COMMENT)
    TEMPLATE_TEXT is NOT in the stop-token set.
  implication: After a successful expression parse, GrammarKit calls recoverWhile=expression_recover. Since TEMPLATE_TEXT is not a stop token, the loop keeps consuming TEMPLATE_TEXT tokens, wrapping them in PsiErrorElement inside the EXPRESSION node.

- timestamp: 2026-02-21T00:00:00Z
  checked: src/main/gen/.../MakoParser.java — expression_recover_0 method (lines 253-271)
  found: |
    The generated expression_recover_0 method checks for: EXPR_START, EXPR_END, TAG_OPEN_DEF, TAG_OPEN_BLOCK,
    TAG_OPEN_INHERIT, TAG_OPEN_INCLUDE, TAG_OPEN_NAMESPACE, TAG_OPEN_PAGE, END_TAG, CONTROL_LINE,
    CODE_OPEN, MODULE_OPEN, DOC_OPEN, LINE_COMMENT — but NOT TEMPLATE_TEXT.
  implication: Confirms the BNF is the source of truth; generated code faithfully reflects the omission.

- timestamp: 2026-02-21T00:00:00Z
  checked: item_() ordering in MakoParser.java (lines 337-356)
  found: template_text_content is tried FIRST, before expression. The fix from phase 05-03 is in place for item_ ordering, but the expression_recover stop set was never updated to include TEMPLATE_TEXT.
  implication: The item_ ordering fix only helps when item_* loops try alternatives normally. It does NOT protect against the recoverWhile consuming TEMPLATE_TEXT after a *successful* pinned expression parse.

- timestamp: 2026-02-21T00:00:00Z
  checked: WellFormedFile.txt — existing test expectations
  found: The existing WellFormedFile.txt showed the same bug in two places: `!` after `${name|h}` inside <%def> body, and `<`/`/h1>` after `${title}` inside <%block> body. Both were incorrectly captured inside EXPRESSION as PsiErrorElement nodes.
  implication: The fix needed to update WellFormedFile.txt to reflect the corrected (correct) behavior.

- timestamp: 2026-02-21T00:10:00Z
  checked: Test run after fix
  found: All 17 test tasks pass with BUILD SUCCESSFUL. New test `testExpressionFollowedByText` passes. Existing tests `testWellFormedFile` passes with updated expectations.
  implication: Fix verified correct.

## Resolution

root_cause: |
  `expression_recover` in Mako.bnf did not include TEMPLATE_TEXT in its negative-lookahead stop-token set.
  GrammarKit's recoverWhile mechanism runs after EVERY rule that has it — including after *successful* parses
  of pinned rules. When `expression` successfully parsed `${foo}`, GrammarKit still ran the recoverWhile
  predicate `expression_recover`. Since TEMPLATE_TEXT was not in the stop set, the recovery loop consumed
  the following `bar` TEMPLATE_TEXT token as a PsiErrorElement inside the EXPRESSION node, expanding it
  to span beyond where the expression tokens ended.

  The same bug manifested inside <%def> and <%block> tag bodies: TEMPLATE_TEXT tokens after expressions
  (e.g. `!` after `${name|h}`, `</h1>` after `${title}`) were incorrectly absorbed into EXPRESSION.

fix: |
  Added TEMPLATE_TEXT to the expression_recover negative-lookahead stop set in Mako.bnf:
    expression_recover ::= !(EXPR_START | EXPR_END | TEMPLATE_TEXT | ...)

  Manually updated the generated MakoParser.java to add:
    if (!result_) result_ = consumeToken(builder_, TEMPLATE_TEXT);
  in expression_recover_0, between EXPR_END and TAG_OPEN_DEF.

  Updated WellFormedFile.txt expected output to reflect corrected (error-free) EXPRESSION spans.
  Added new test ExpressionFollowedByText.mako/.txt with input `${foo}bar` and expected correct tree.
  Added testExpressionFollowedByText() method (PARS-07) to MakoParsingTest.kt.

verification: |
  Ran ./gradlew test — BUILD SUCCESSFUL, all 17 actionable tasks complete.
  New test testExpressionFollowedByText passes.
  testWellFormedFile passes with corrected expected PSI tree (no more PsiErrorElement for TEMPLATE_TEXT in EXPRESSION).
  testConsecutiveExpressions still passes (no regression).

files_changed:
  - src/main/grammars/Mako.bnf
  - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
  - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParsingTest.kt
  - src/test/testData/parser/ExpressionFollowedByText.mako (new)
  - src/test/testData/parser/ExpressionFollowedByText.txt (new)
  - src/test/testData/parser/WellFormedFile.txt (updated expected output)

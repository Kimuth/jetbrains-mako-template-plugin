---
status: resolved
trigger: "consecutive-expr-parse-error"
created: 2026-02-21T00:00:00Z
updated: 2026-02-21T00:20:00Z
---

## Current Focus

hypothesis: CONFIRMED AND FIXED
test: Added testConsecutiveExpressions() regression test - all tests pass
expecting: N/A - resolved
next_action: Archive

## Symptoms

expected: Two consecutive `${foo}` expressions should produce two sibling FILE-level MakoExpressionImpl nodes
actual: The second `${` is consumed inside the first MakoExpressionImpl with a PsiErrorElement; the second `}` is orphaned outside the expression
errors: |
  Red squiggly lines under the `{` and `}` of the second expression.
  PSI Tree:
  ```
  FILE(0,13)
    MakoExpressionImpl(EXPRESSION)(0,12)
      PsiElement(EXPR_START)('${')(0,2)
      PsiElement(EXPR_CONTENT)('foo')(2,5)
      PsiElement(EXPR_END)('}')(5,6)
      PsiErrorElement:'${' unexpected(7,9)
        PsiElement(EXPR_START)('${')(7,9)
      PsiElement(EXPR_CONTENT)('foo')(9,12)
    PsiElement(EXPR_END)('}')(12,13)
    PsiErrorElement:CODE_OPEN, CONTROL_LINE, DOC_OPEN, EXPR_START, LINE_COMMENT, MODULE_OPEN, TAG_OPEN_BLOCK, TAG_OPEN_INCLUDE, TAG_OPEN_INHERIT, TAG_OPEN_NAMESPACE or TAG_OPEN_PAGE expected(13,13)
      <empty list>
  ```
reproduction: Create a .mako file with two lines: `${foo}\n${foo}`
started: Discovered during Phase 6 work; parser was considered done at Phase 5

## Eliminated

- hypothesis: Lexer fails to return to YYINITIAL after EXPR_END
  evidence: Lexer code confirms `yybegin(YYINITIAL)` is called when braceDepth==0 and `}` is seen. Token stream is correct.
  timestamp: 2026-02-21T00:05:00Z

- hypothesis: BNF expression rule has wrong quantifier on EXPR_CONTENT
  evidence: The BNF rule `EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END` is structurally correct.
  timestamp: 2026-02-21T00:05:00Z

## Evidence

- timestamp: 2026-02-21T00:03:00Z
  checked: src/main/grammars/MakoLexer.flex EXPRESSION state
  found: `}` rule calls `yybegin(YYINITIAL)` when braceDepth==0, correctly returning to initial state
  implication: Lexer is not the source of the bug. Token stream is correct.

- timestamp: 2026-02-21T00:04:00Z
  checked: src/main/gen/.../MakoParser.java expression() method
  found: Parser correctly sequences EXPR_START -> EXPR_CONTENT* -> (FILTER_SEP EXPR_CONTENT*)* -> EXPR_END; uses pin=1 and recoverWhile=expression_recover
  implication: Parser rule structure is correct. Investigation shifted to recoverWhile behavior.

- timestamp: 2026-02-21T00:07:00Z
  checked: GrammarKit recoverWhile documentation and implementation
  found: recoverWhile runs even when the rule SUCCEEDS (not just on failure). This is by design in GrammarKit.
  implication: After successfully parsing the first ${foo}, exit_section_ still invokes expression_recover.

- timestamp: 2026-02-21T00:08:00Z
  checked: expression_recover stop set in Mako.bnf (line 146-149)
  found: expression_recover tests for: EXPR_END | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN | LINE_COMMENT
  EXPR_START is MISSING from expression_recover, but IS present in tag_recover.
  implication: After successful first expression parse, recovery loop sees second EXPR_START token, finds it's not in the stop set, and greedily consumes it as an error.

- timestamp: 2026-02-21T00:09:00Z
  checked: Trace through recovery with ${foo}\n${foo}
  found: After consuming first ${foo}: recovery runs, sees EXPR_START (second ${), not in stop set so consumed as PsiErrorElement. Then EXPR_CONTENT (foo) consumed. Then EXPR_END seen — IS in stop set, recovery stops. EXPR_END is left orphaned outside the expression.
  implication: This exactly reproduces the PSI tree shown in symptoms. Root cause confirmed.

- timestamp: 2026-02-21T00:15:00Z
  checked: All tests after fix applied
  found: BUILD SUCCESSFUL - all tests pass including new testConsecutiveExpressions regression test
  implication: Fix is correct and does not break existing behavior.

## Resolution

root_cause: |
  `expression_recover` was missing `EXPR_START` from its stop-token set.
  GrammarKit's `recoverWhile` runs EVEN WHEN the rule succeeds (not just on failure).
  After successfully parsing the first `${foo}`, `exit_section_` runs `expression_recover`.
  Since `EXPR_START` was not in the stop set, the recovery loop greedily consumed the second
  `${` as a PsiErrorElement inside the first expression node, then consumed its `foo` content,
  and only stopped when it hit the second `}` (EXPR_END, which was in the stop set).
  The second `}` was then orphaned outside the first expression.

fix: |
  Added EXPR_START to the expression_recover stop set in:
  1. src/main/grammars/Mako.bnf — source of truth
  2. src/main/gen/.../MakoParser.java — manually updated generated parser (committed)
  3. Added regression test: testConsecutiveExpressions() in MakoParsingTest.kt
  4. Added test data: ConsecutiveExpressions.mako and ConsecutiveExpressions.txt

verification: |
  - ./gradlew test --tests "...MakoParsingTest.testConsecutiveExpressions" -> BUILD SUCCESSFUL
  - ./gradlew test -> BUILD SUCCESSFUL (all tests pass, no regressions)

files_changed:
  - src/main/grammars/Mako.bnf
  - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
  - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParsingTest.kt
  - src/test/testData/parser/ConsecutiveExpressions.mako (new)
  - src/test/testData/parser/ConsecutiveExpressions.txt (new)

---
status: diagnosed
phase: 02-lexer
source: 02-01-SUMMARY.md, 02-02-SUMMARY.md
started: 2026-02-19T20:00:00Z
updated: 2026-02-19T20:08:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Build passes with lexer generation
expected: `./gradlew build` completes without errors. JFlex generation produces _MakoLexer.java, compileKotlin succeeds, and the overall build finishes with BUILD SUCCESSFUL.
result: pass

### 2. All 18 lexer tests pass
expected: `./gradlew test` completes with 18 tests passing, 0 failures, 0 errors. All PARS-01/02/03 requirements are verified by the test suite.
result: pass

### 3. Open .mako file without IDE errors
expected: Running `./gradlew runIde` and opening a `.mako` file shows the file recognized as "Mako Template" file type (icon in project tree). No exceptions appear in the IDE event log (Help > Show Log).
result: issue
reported: "Got exception `Caused by: java.lang.UnsupportedOperationException: MakoParser not yet implemented - Phase 3`"
severity: blocker

### 4. Expression tokenization correct
expected: A `${some_var}` expression is tokenized as EXPR_START + EXPR_CONTENT + EXPR_END — verified by unit test `testSimpleExpression` passing. In PsiViewer (if available), tokens show distinct types, not plain TEMPLATE_TEXT.
result: skipped
reason: Blocked by Test 3 — IDE throws exception on .mako file open. Unit test passes (Test 2).

### 5. Filter pipe disambiguation works
expected: `${x | h,trim}` tokenizes `|` as FILTER_SEP (Mako filter separator), not as Python bitwise OR. Verified by unit test `testExpressionWithFilter`. The `||` operator inside expressions returns EXPR_CONTENT (not two FILTER_SEPs).
result: skipped
reason: Blocked by Test 3 — IDE throws exception on .mako file open. Unit test passes (Test 2).

### 6. Nested brace handling correct
expected: `${{'key': 'val'}}` does NOT prematurely close at the inner `}`. The brace depth counter tracks nesting so the expression closes only at the final `}` when braceDepth returns to 0. Verified by unit test `testNestedBraces`.
result: skipped
reason: Blocked by Test 3 — IDE throws exception on .mako file open. Unit test passes (Test 2).

## Summary

total: 6
passed: 2
issues: 1
pending: 0
skipped: 3

## Gaps

- truth: "Opening a .mako file in the IDE produces no exceptions in the event log"
  status: failed
  reason: "User reported: Got exception `Caused by: java.lang.UnsupportedOperationException: MakoParser not yet implemented - Phase 3`"
  severity: blocker
  test: 3
  root_cause: "createParser() and createElement() in MakoParserDefinition throw UnsupportedOperationException. The platform always calls the full parser pipeline (createParser -> PsiParser.parse -> createElement) when opening a file — there is no lexer-only mode."
  artifacts:
    - path: "src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt"
      issue: "createParser() throws UnsupportedOperationException at line 29; createElement() throws at line 36"
  missing:
    - "Replace createParser() with minimal PsiParser that wraps all tokens in a single root marker"
    - "Replace createElement() with ASTWrapperPsiElement(node) fallback"
  debug_session: ".planning/debug/parser-stub-throws-uoe.md"

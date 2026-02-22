---
phase: quick-1-fix-false-positive-unresolved-reference
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt
  - src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt
autonomous: true
requirements: []

must_haves:
  truths:
    - "Variables defined in a top-level <% %> block are visible inside <%def> blocks without red squiggles"
    - "MakoModuleBlock (<%! %>) remains an independent Python injection with its own scope"
    - "Existing injection host tests still pass"
    - "All MakoCodeBlock and MakoExpression elements in a file are registered as one multi-host injection"
  artifacts:
    - path: "src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt"
      provides: "Combined multi-host injection for MakoCodeBlock and MakoExpression"
      contains: "collectCodeAndExpressionHosts"
    - path: "src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt"
      provides: "Test verifying nested def-level code blocks are collected"
      contains: "testCollectedHostsIncludesDefLevelBlock"
      note: "post-execution state — method created by Task 2"
  key_links:
    - from: "MakoPythonInjector.getLanguagesToInject"
      to: "collectCodeAndExpressionHosts"
      via: "first-host guard: context must equal hosts.first() to start injection"
      pattern: "collectCodeAndExpressionHosts"
    - from: "collectCodeAndExpressionHosts"
      to: "PsiTreeUtil.findChildrenOfType"
      via: "collects both MakoCodeBlock and MakoExpression, sorted by textOffset"
      pattern: "findChildrenOfType"
---

<objective>
Fix false-positive "Unresolved Reference" warnings for variables defined in one Mako code block
and used in another (e.g., inside a <%def>).

Purpose: PyCharm's Python language service currently sees each MakoCodeBlock/MakoExpression as an
isolated Python file. By merging them into one multi-host injection the service resolves references
across all blocks in a single shared scope — eliminating the false positives.

Output: Revised MakoPythonInjector.kt with combined-injection strategy + one new test verifying
nested code blocks are collected.
</objective>

<execution_context>
@./.claude/get-shit-done/workflows/execute-plan.md
@./.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.docs/fix-flase-positive-unresolved-reference-across-mako-blocks.md
@src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt
@src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Refactor MakoPythonInjector to use combined multi-host injection</name>
  <files>src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt</files>
  <action>
Rewrite MakoPythonInjector.kt to combine all MakoCodeBlock and MakoExpression elements in a
file into one multi-host Python injection. MakoModuleBlock stays separate and unchanged.

New structure:

1. Add imports:
   - `com.intellij.psi.PsiFile`
   - `com.intellij.psi.util.PsiTreeUtil`

2. Replace the `is MakoExpression` and `is MakoCodeBlock` branches in `getLanguagesToInject`
   with a single combined branch:

   ```kotlin
   is MakoCodeBlock, is MakoExpression -> {
       val hosts = collectCodeAndExpressionHosts(context.containingFile ?: return)
       // Only the FIRST host in document order starts the multi-host injection.
       // All other hosts return early — they are included via the first host's addPlace() loop.
       if (hosts.firstOrNull() != context) return
       registrar.startInjecting(python)
       var placed = false
       for (host in hosts) {
           val text = host.text ?: continue
           when (host) {
               is MakoCodeBlock -> {
                   // <%<python>%> — strip <% (2) and %> (2)
                   val start = 2
                   val end = text.length - 2
                   if (end > start) {
                       registrar.addPlace(null, "\n", host, TextRange(start, end))
                       placed = true
                   }
               }
               is MakoExpression -> {
                   // ${<python>} — strip ${ (2) and } (1); stop at FILTER_SEP if present
                   val start = 2
                   val end = expressionPythonEnd(host, text)
                   if (end > start) {
                       registrar.addPlace("_ = ", "\n", host, TextRange(start, end))
                       placed = true
                   }
               }
           }
       }
       if (placed) registrar.doneInjecting()
   }
   ```

   The `_ = ` prefix on expressions makes bare expression content (e.g., `x`) a syntactically
   valid Python assignment statement so the injected fragment parses cleanly.

3. Extract `expressionPythonEnd(expr: MakoExpression, text: String): Int` as a private method
   containing the existing filter-separator walk from the old `is MakoExpression` branch:

   ```kotlin
   private fun expressionPythonEnd(expr: MakoExpression, text: String): Int {
       val filterSep = expr.node.firstChildNode?.let { first ->
           var child = first
           var found: com.intellij.lang.ASTNode? = null
           while (found == null) {
               if (child.elementType == MakoTokenTypes.FILTER_SEP) found = child
               child = child.treeNext ?: break
           }
           found
       }
       return if (filterSep != null) {
           filterSep.startOffset - expr.textRange.startOffset
       } else {
           text.length - 1
       }
   }
   ```

4. Add `collectCodeAndExpressionHosts` as a private method:

   ```kotlin
   private fun collectCodeAndExpressionHosts(file: PsiFile): List<PsiLanguageInjectionHost> {
       val codeBlocks = PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)
       val expressions = PsiTreeUtil.findChildrenOfType(file, MakoExpression::class.java)
       return (codeBlocks + expressions)
           .sortedBy { it.textOffset }
   }
   ```

5. Keep the `is MakoModuleBlock` branch exactly as-is.

6. Update the class KDoc to reflect the new multi-host strategy for MakoCodeBlock/MakoExpression.
  </action>
  <verify>./gradlew test --tests MakoInjectionHostTest --tests MakoInjectionRangeTest</verify>
  <done>All injection tests pass; no compilation errors. Note: the `_ = ` prefix on MakoExpression addPlace calls is an intentional semantic change — MakoInjectionRangeTest is lexer-only and is unaffected; no injection-content test exists at this harness level, and the change is by-design.</done>
</task>

<task type="auto">
  <name>Task 2: Add test verifying nested def-level blocks are collected</name>
  <files>src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt</files>
  <action>
Add one new test to MakoInjectionHostTest that verifies `PsiTreeUtil.findChildrenOfType`
recurses into nested <%def> blocks and finds ALL MakoCodeBlock elements at any nesting depth.

This confirms that `collectCodeAndExpressionHosts` (which uses `findChildrenOfType`) will
correctly gather blocks inside <%def> tags — the exact scenario that caused false-positive
"Unresolved Reference" warnings.

Add after the existing `testModuleBlockUpdateTextThrows` test:

```kotlin
/**
 * Verifies that PsiTreeUtil.findChildrenOfType recurses into nested <%def> blocks,
 * so collectCodeAndExpressionHosts() will find code blocks at all nesting levels.
 * This is the prerequisite for cross-block variable resolution working correctly.
 */
fun testCollectedHostsIncludesDefLevelBlock() {
    val src = """
        <%
        my_list = [1, 2, 3]
        %>
        <%def name="my_func()">
        <%
        for i in my_list:
            pass
        %>
        </%def>
    """.trimIndent()
    val file = parseFile("collectHosts", src)
    val codeBlocks = PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)
    assertEquals(
        "Should find both the top-level and the def-level <% %> blocks",
        2, codeBlocks.size
    )
}
```

No new imports needed — `MakoCodeBlock` and `PsiTreeUtil` are already imported in the file.
  </action>
  <verify>./gradlew test --tests MakoInjectionHostTest</verify>
  <done>testCollectedHostsIncludesDefLevelBlock passes alongside the existing three tests; 4/4 tests green.</done>
</task>

<task type="checkpoint:verify">
  <name>Task 3: Full test suite verification</name>
  <files>none — smoke-run only, no files modified</files>
  <action>
Run the complete test suite to confirm no regressions from the injector refactor.

```bash
./gradlew check
```

If any test fails due to the `_ = ` prefix changing injection offsets in MakoInjectionRangeTest,
review those tests: MakoInjectionRangeTest tests the LEXER token structure (not injector output),
so they should be unaffected. If failures appear in other suites, read the error and fix the
relevant injection range arithmetic.
  </action>
  <verify>./gradlew check exits 0 with BUILD SUCCESSFUL</verify>
  <done>All tests pass. No regressions introduced by the multi-host injection refactor.</done>
</task>

</tasks>

<verification>
1. `./gradlew check` exits 0 — no test regressions.
2. `MakoInjectionHostTest` has 4 tests and all pass (3 existing + 1 new).
3. `MakoPythonInjector.kt` contains `collectCodeAndExpressionHosts` and no longer calls
   `doneInjecting()` in the MakoExpression or MakoCodeBlock branches independently.
4. `MakoModuleBlock` branch is untouched (still an independent injection).
</verification>

<success_criteria>
- All MakoCodeBlock and MakoExpression elements in a file are combined into one multi-host
  injection registered with a single startInjecting/doneInjecting call.
- MakoModuleBlock remains isolated.
- `./gradlew check` passes with no failures.
- Manual test (optional, requires runIde): variable defined in top-level `<% %>` block used
  inside `<%def>` shows no "Unresolved Reference" squiggle.
</success_criteria>

<output>
After completion, create `.planning/quick/1-fix-false-positive-unresolved-reference-/1-SUMMARY.md`
following the summary template.
</output>

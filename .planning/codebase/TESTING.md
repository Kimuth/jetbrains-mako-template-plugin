# Testing Patterns

**Analysis Date:** 2026-02-21

## Test Framework

**Runner:**
- JUnit 4 (via `libs.junit` in `build.gradle.kts`)
- IntelliJ Platform Test Framework via `TestFrameworkType.Platform`
- ParsingTestCase for lexer/parser tests
- BasePlatformTestCase for plugin/UI tests

**Assertion Library:**
- JUnit 4 assertions: `assertEquals()`, `assertTrue()`, `assertFalse()`, `assertNotNull()`
- OpenTest4j via `libs.opentest4j` (dependency specified in `build.gradle.kts`)

**Run Commands:**
```bash
./gradlew test                             # Run all tests
./gradlew test --tests MakoParsingTest     # Run specific test class
./gradlew test --tests MakoLexerTest       # Run lexer tests only
./gradlew check                            # Run tests + code verification (includes Kover coverage)
./gradlew runIde                           # Launch test IDE with plugin loaded
```

## Test File Organization

**Location:**
- Tests co-located with language-specific test infrastructure
- Path: `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/`
- Test data in `src/test/testData/parser/` (fixture-based)
- Test data in `src/test/testData/folding/` (folding builder tests)

**Naming:**
- Test files: `[Feature]Test.kt`
  - `MakoLexerTest.kt` — Tokenization (all token types)
  - `MakoParsingTest.kt` — Parser integration (PSI tree building)
  - `MakoFoldingTest.kt` — Code folding regions
  - `MakoStructureViewTest.kt` — Outline/structure view
  - `MyPluginTest.kt` — Plugin loading smoke test
- Test methods: `test[Scenario]()` — `testTemplateTextSimple()`, `testNestedBraces()`, `testDefTagFollowedByText()`

**Structure:**
```
src/test/
├── kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
│   └── lang/
│       ├── MakoLexerTest.kt
│       ├── MakoParsingTest.kt
│       ├── MakoFoldingTest.kt
│       └── MakoStructureViewTest.kt
└── testData/
    └── parser/
        ├── WellFormedFile.mako
        ├── WellFormedFile.txt
        └── [other test pairs]
```

## Test Structure

**Suite Organization:**
```kotlin
// ParsingTestCase test (from MakoParsingTest.kt)
class MakoParsingTest : ParsingTestCase("", "mako", MakoParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData/parser"
    override fun skipSpaces(): Boolean = false
    override fun includeRanges(): Boolean = true

    fun testWellFormedFile() {
        doTest(true)  // Verifies against expected .txt fixture
    }
}
```

**Patterns:**
- Setup: `extends ParsingTestCase` or `extends BasePlatformTestCase`
- Tests inherit from framework base class that handles project/PSI setup
- `parseFile(name, content): MakoFile` creates PSI tree from string (in folding/structure tests)
- `doTest(checkResult)` compares generated PSI tree to fixture (parser tests)
- No explicit teardown methods; framework handles cleanup

**Test Data (Fixture-Based):**
- Each parser test maps to pair: `src/test/testData/parser/TestName.mako` (input) + `TestName.txt` (expected PSI tree)
- First test run generates `.txt` if missing — developer verifies correctness before commit
- Example: `WellFormedFile.mako` (input template) → `WellFormedFile.txt` (PSI tree structure)

## Mocking

**Framework:**
- No mocking framework detected (no mockito, mockk, etc. in dependencies)
- Tests use real IntelliJ Platform components via test framework

**Patterns:**
- Direct instantiation of classes under test: `MakoLexerAdapter()`, `MakoFoldingBuilder()`
- No external dependencies to mock (lexer/parser tests are isolated to token/tree structures)
- IntelliJ test framework provides fake `Project`, `Document`, `FileViewProvider`

**Example (Lexer Test):**
```kotlin
private fun tokenize(text: String): List<Pair<IElementType, String>> {
    val lexer = MakoLexerAdapter()  // Real instance, no mock
    lexer.start(text)
    val tokens = mutableListOf<Pair<IElementType, String>>()
    while (lexer.tokenType != null) {
        tokens.add(lexer.tokenType!! to lexer.tokenText)
        lexer.advance()
    }
    return tokens
}
```

**What to Mock:**
- Nothing currently mocked; real IntelliJ test framework used

**What NOT to Mock:**
- Lexer/parser components — test with real implementations
- PSI tree structures — rely on generated GrammarKit output

## Fixtures and Factories

**Test Data:**
- Inline string literals in test methods for small inputs
- `.mako` fixture files for larger/complex scenarios

**Example from MakoFoldingTest:**
```kotlin
private val SAMPLE_MAKO = """<%doc>
This documentation should be collapsed by default.
</%doc>

<%def name="greet">
Hello ${"$"}{name}!
</%def>
% for item in items:
    ${"$"}{item}
% endfor"""

private fun buildFolds(content: String): Int {
    val file = parseFile("test", content)
    val doc = DocumentImpl(content)
    val builder = MakoFoldingBuilder()
    return builder.buildFoldRegions(file, doc, false).size
}
```

**Location:**
- Fixtures in `src/test/testData/` organized by feature
- Inline test constants as private members in test class
- Helper methods (`tokenize()`, `buildFolds()`, `parseFile()`) as private members

## Coverage

**Requirements:**
- Gradle Kover plugin configured: `alias(libs.plugins.kover)` in `build.gradle.kts`
- XML report generated on check: `kover { reports { total { xml { onCheck = true } } } }`

**View Coverage:**
```bash
./gradlew koverHtmlReport   # Generate HTML coverage report (build/reports/kover/html)
./gradlew check             # Run tests + generate XML coverage
```

## Test Types

**Unit Tests:**
- **Lexer Tests** (`MakoLexerTest.kt`): 25+ test methods covering all token types
  - Token sequence generation
  - Nested brace handling (`testNestedBraces()`)
  - State persistence across tokens (`testRestartStateAfterExpression()`)
  - Edge cases (unclosed constructs, single char at EOF)
  - All tests use direct lexer instance, no PSI/parsing involved

- **Parser Tests** (`MakoParsingTest.kt`): Fixture-based PSI tree comparison
  - Well-formed input produces correct typed nodes (`testWellFormedFile()`)
  - Malformed input produces error recovery nodes (`testMalformedTag()`)
  - Regression tests for specific parser issues (`testConsecutiveExpressions()`, `testExpressionFollowedByText()`)

**Integration Tests:**
- **Folding Tests** (`MakoFoldingTest.kt`): Full PSI trees with folding builder
  - Individual fold regions created for all construct types
  - Fold ranges don't bleed into subsequent content (`testCodeBlockFoldRange()`)
  - Control flow nesting (for/if/while) with stack handling
  - Malformed files don't crash (`testMalformedControlFlowNoCrash()`)

- **Structure View Tests** (`MakoStructureViewTest.kt`): PSI navigation/outline
  - Named elements (def/block) appear in structure view
  - Only def/block nodes visible (not expressions, control flow, template text)
  - Unnamed blocks use `<unnamed>` fallback

**Plugin Tests:**
- **Smoke Test** (`MyPluginTest.kt`): Minimal test that plugin loads
  - Verifies project fixture initializes

## Common Patterns

**Async Testing:**
Not applicable — no async code in plugin.

**Error Testing:**
```kotlin
// Parser error recovery (MakoParsingTest)
fun testMalformedTag() {
    doTest(true)  // Expects .txt to contain PsiErrorElement nodes
}

// Lexer graceful degradation
fun testUnclosedCodeBlock() {
    val tokens = tokenize("<%\nblabla = \"foo\"")
    assertTrue("Must contain CODE_OPEN", tokens.any { it.first == MakoTokenTypes.CODE_OPEN })
    assertFalse("Must NOT contain CODE_CLOSE", tokens.any { it.first == MakoTokenTypes.CODE_CLOSE })
}

// Parser recovery verification (regression tests)
fun testExpressionFollowedByText() {
    doTest(true)  // Verifies TEMPLATE_TEXT not absorbed into EXPRESSION as PsiErrorElement
}
```

**Regression Testing:**
- Each regression fix has dedicated test case
- PARS-06 through PARS-09 regression tests in `MakoParsingTest`
- Specific EOF handling for CODE_BLOCK (`testCodeBlockSingleCharContentAtEof()`)
- State encoding verification (`testBraceDepthEncodedInState()`, `testBraceDepthRestoredOnRestart()`)

**Test Isolation:**
- No shared state between tests (each test creates fresh parser/lexer)
- Inline content for small cases, fixtures for complex scenarios
- No cross-test dependencies

---

*Testing analysis: 2026-02-21*

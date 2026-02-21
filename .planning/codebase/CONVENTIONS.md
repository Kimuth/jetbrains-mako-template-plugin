# Coding Conventions

**Analysis Date:** 2026-02-21

## Naming Patterns

**Files:**
- Class files use PascalCase: `MakoLexerAdapter.kt`, `MakoParserDefinition.kt`, `MakoSyntaxHighlighter.kt`
- Test files follow pattern `[Feature]Test.kt`: `MakoLexerTest.kt`, `MakoParsingTest.kt`, `MakoFoldingTest.kt`
- Gradle/Kotlin build configuration: `build.gradle.kts`

**Classes & Objects:**
- PascalCase for all classes: `MakoLanguage`, `MakoTokenTypes`, `MakoLexerAdapter`
- Singleton objects use `object` keyword: `MakoLanguage`, `MakoTokenTypes` in `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt`
- Mixins follow pattern `[Feature]Mixin`: `MakoDefTagMixin.kt`, `MakoBlockTagMixin.kt`

**Functions & Variables:**
- camelCase for function names: `buildFoldRegions()`, `findClosingTokenEnd()`, `getHighlightingLexer()`
- camelCase for member variables and properties: `braceDepth`, `makoFlex`
- Private properties prefixed with underscore not used; rely on visibility modifiers instead
- Constants use UPPER_SNAKE_CASE: `OPENING_KEYWORDS`, `CLOSING_KEYWORDS`, `DIRECTIVE_KEYS`, `EMPTY_KEYS` (in `MakoSyntaxHighlighter.kt`)

**Token Type Constants:**
- Each token type defined as `@JvmField val`: `val EXPR_START = IElementType("EXPR_START", MakoLanguage)`
- Named with UPPER_SNAKE_CASE: `TAG_OPEN_DEF`, `TAG_ATTR_VALUE`, `MODULE_CONTENT`
- All stored in singleton object `MakoTokenTypes`

## Code Style

**Formatting:**
- Language: Kotlin (JVM target 21)
- No explicit formatter config found; follows Kotlin standard conventions
- Max line length appears to be ~100-130 characters (based on examined code)
- Braces on same line (Kotlin style): `fun testBasic() {`

**Linting:**
- No `.eslintrc` or similar linting configuration detected
- Project uses Gradle with Qodana plugin (`alias(libs.plugins.qodana)` in `build.gradle.kts`) for code quality analysis
- Code follows standard Kotlin style conventions

## Import Organization

**Order:**
1. Package declaration
2. Imports from standard library (`kotlin.*`, `java.*`)
3. Imports from IntelliJ/JetBrains libraries (`com.intellij.*`)
4. Imports from local project (`com.github.kimuth.*`)
5. Explicit imports preferred; no wildcard imports observed

**Path Aliases:**
- Not applicable (Kotlin/Java project)
- Package structure follows Maven convention: `com.github.kimuth.jetbrainsmakotemplateplugin`

## Error Handling

**Patterns:**
- Explicit null-checking with null-safe operators (`?.`)
- Type-safe pattern matching in when expressions: `when (tokenType) { ... }`
- Assertions in tests: `assertTrue()`, `assertEquals()`, `assertNotNull()`, `assertFalse()`
- Recovery from parser errors via `tag_recover` and `expression_recover` stop-token sets (grammar-level)

**Example from `MakoDefTagMixin.kt`:**
```kotlin
override fun getName(): String? {
    val attrValue = node.findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)
    return attrValue?.text?.trim('"', '\'')
}
```

## Logging

**Framework:** None detected in current codebase

**Approach:**
- No structured logging found; tests use standard assertions
- Plugin execution relies on IntelliJ Platform's built-in logging/error reporting

## Comments

**When to Comment:**
- KDoc comments for public classes, public functions, and significant implementation details
- Inline comments for non-obvious logic (especially parser recovery, token state encoding)
- TODO/FIXME comments not found in codebase

**KDoc/JSDoc Pattern:**
```kotlin
/**
 * Wraps [_MakoLexer] with state encoding that includes [_MakoLexer.braceDepth].
 * [Purpose and behavior description...]
 *
 * Encoding: bits 0-3 = JFlex state, bits 4-7 = braceDepth
 */
class MakoLexerAdapter : FlexAdapter(_MakoLexer()) { ... }
```

**Documentation follows IntelliJ Platform conventions:**
- Full parameter/return documentation in public APIs
- References to related code using square brackets: `[ClassName]`, `[methodName]`
- Example from test classes: Detailed KDoc for test methods explaining regression coverage

## Function Design

**Size Guidelines:**
- Most functions 20-50 lines
- Helper functions like `extractKeyword()` in `MakoFoldingBuilder.kt` are 1-2 lines
- Complex logic broken into private helper methods: `findClosingTokenEnd()`, `collectControlLineNodes()`, `buildControlFlowFoldsUnder()`

**Parameters:**
- Minimal parameter count (1-3 params typical)
- When building complex structures, use type-safe DSL or builder: `parseFile("test", content)` returns `MakoFile`
- Mutable collections passed as parameters use standard Kotlin conventions: `MutableList<FoldingDescriptor>`

**Return Values:**
- Nullable return types use `Type?`: `fun getName(): String?` in mixins
- Collection returns use immutable types when possible: `List<Pair<IElementType, String>>` in lexer tests
- Array returns for performance-sensitive code: `Array<FoldingDescriptor>` in folding builder

## Module Design

**Exports:**
- Top-level classes exported as public: `class MakoParserDefinition : ParserDefinition`
- Singletons exported as `object`: `object MakoLanguage`, `object MakoTokenTypes`
- Mixins abstract to force GrammarKit generation: `abstract class MakoDefTagMixin`

**Barrel Files:**
- Not used; full explicit imports preferred
- Each module has single responsibility: `MakoTokenTypes.kt` for all token definitions, `MakoParserDefinition.kt` for parser setup

## Package Organization

**Structure:**
- Base plugin package: `com.github.kimuth.jetbrainsmakotemplateplugin`
- Core language support: `...lang` (lexer, parser, PSI)
- Features grouped by concern:
  - `...lang.psi` — PSI element interfaces (generated + `MakoFile.kt`)
  - `...lang.psi.impl` — Mixins for generated PSI classes
  - `...lang.highlighting` — Syntax highlighting, color schemes, bracket matching
  - `...lang.structure` — Structure view/outline
  - `...lang.folding` — Code folding
  - `...lang.editing` — Editor features (commenter)

**Mixin Location Pattern:**
- Mixins in `src/main/kotlin/.../lang/psi/impl/`
- Referenced by BNF `mixin` attribute: `mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoDefTagMixin"`

---

*Convention analysis: 2026-02-21*

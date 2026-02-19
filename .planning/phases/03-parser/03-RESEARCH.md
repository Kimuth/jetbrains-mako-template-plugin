# Phase 3: Parser and PSI Tree - Research

**Researched:** 2026-02-19
**Domain:** GrammarKit BNF parser generation, typed PSI node classes, PsiNamedElement, error recovery
**Confidence:** HIGH (core APIs verified via official IntelliJ Platform SDK docs and Grammar-Kit GitHub)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PARS-04 | GrammarKit-generated parser builds PSI tree with typed nodes for each Mako construct | BNF grammar generates parser + PSI element interfaces and implementations in `src/main/gen`; `GenerateParserTask` in Gradle activates this; `MakoParserDefinition.createElement()` dispatches by node type; `MakoElementTypes` holder class provides typed `IElementType` constants for each grammar rule |
| PARS-05 | Parser recovers gracefully from malformed Mako (partial parses, not full failure) | GrammarKit `pin` + `recoverWhile` attributes enable per-construct error recovery; pinning the opening tag marker commits the parser to a construct even if the rest is malformed; `recoverWhile` skips unknown tokens until the next recognized construct boundary |
</phase_requirements>

---

## Summary

Phase 3 activates the commented-out `generateMakoParser` Gradle task in `build.gradle.kts`, creates a `Mako.bnf` grammar file in `src/main/grammars/`, and wires the generated parser into the existing `MakoParserDefinition`. The lexer (Phase 2) already produces all the token types the grammar will reference. The parser's job is to group those flat token streams into a hierarchical PSI tree with distinct node types for each Mako construct.

GrammarKit's BNF parser generation produces three kinds of output: a `MakoParser.java` parser class, a set of PSI element interface files (e.g., `MakoDefTag.java`, `MakoExpression.java`), and a `MakoTypes.java` element type holder. The `MakoParserDefinition.createElement()` method — currently a pass-through `ASTWrapperPsiElement` fallback — must be updated to dispatch on the generated element types and instantiate the corresponding generated PSI classes. For definition nodes (`<%def>`, `<%block>`), mixins implementing `PsiNamedElement` provide `getName()` and `setName()` without string parsing.

Error recovery is the most nuanced part of Phase 3. GrammarKit uses two complementary mechanisms: `pin` commits the parser to a rule after a specific token is matched, preventing full backtrack on failure; `recoverWhile` skips tokens until a predicate rule matches, keeping the partial PSI tree structurally sound. Mako's tag constructs have natural pin points (`TAG_OPEN` is the pinning token) and natural recovery boundaries (the next `TAG_OPEN`, `END_TAG`, or `CONTROL_LINE`). The current no-op parser (Phase 2 gap closure) is replaced entirely by the GrammarKit-generated parser.

**Primary recommendation:** Write `src/main/grammars/Mako.bnf` with `generateTokens=false` so the BNF reuses the existing `MakoTokenTypes.kt` constants rather than generating duplicate token types; use `pin` on `TAG_OPEN` and `recoverWhile` pointing to a predicate rule that matches any new construct start or `END_TAG`; add `mixin` + `implements` attributes for `def_tag` and `block_tag` rules to get `PsiNamedElement` for free via handwritten mixin classes.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| GrammarKit Gradle plugin | 2023.3.0.2 (already in `libs.versions.toml`) | `GenerateParserTask` drives BNF-to-parser generation in Gradle | Already in project; just needs the commented-out task uncommented |
| Grammar-Kit BNF format | N/A (file format) | `.bnf` grammar definition file consumed by GrammarKit | The only grammar format supported by the `GenerateParserTask` |
| `GeneratedParserUtilBase` | Platform (bundled since IntelliJ 12.1) | Error recovery, token matching, `recoverWhile` predicate execution | Included in IntelliJ Platform — do NOT copy it into the project |
| `PsiParser` | Platform (bundled) | Interface returned by `MakoParserDefinition.createParser()` | The generated `MakoParser` implements this interface |
| `PsiBuilder` | Platform (bundled) | Token stream + marker API used by the generated parser | Platform-provided; the parser receives it via `createParser()` |
| `PsiNamedElement` | Platform (bundled) | Interface for PSI nodes whose names are retrievable and settable | Implement on `MakoDefTag` and `MakoBlockTag` via mixin pattern |
| `ASTWrapperPsiElement` | Platform (bundled) | Base class for generated PSI implementations | Generated PSI impl classes extend this (or its `NavigatablePsiElement` variant) |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `ParsingTestCase` | IntelliJ test framework | Base class for parser integration tests | Use for `.mako` file parse tests; replaces the no-op parser with real structure |
| PsiViewer plugin | IDE dev plugin | Visual PSI tree inspection during development | Enable via `idea.is.internal=true` in `Help > Edit Custom Properties`; use "Tools > View PSI Structure" to verify node types in the running plugin |
| `PsiFileBase` | Platform (bundled) | Already used by `MakoFile.kt` — no change needed in Phase 3 | `MakoFile` already extends it; Phase 3 does not change MakoFile |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| GrammarKit-generated parser (recommended) | Hand-written `PsiParser` with `PsiBuilder.Marker` | Hand-written gives fine control but is hundreds of lines of boilerplate; GrammarKit generates correct error recovery, token advancement, and PSI factory code automatically |
| GrammarKit-generated PSI classes | `ICompositeElementType` with custom `createElement` | `ICompositeElementType` is an advanced pattern; appropriate when you want to completely bypass GrammarKit PSI generation and manage your own node classes — overkill for Phase 3 |
| `mixin` pattern for PsiNamedElement | `psiImplUtilClass` with static methods | Both work; `mixin` is simpler for Kotlin (extend the generated Impl class, override methods); `psiImplUtilClass` is more Java-idiomatic; use `mixin` |

---

## Architecture Patterns

### Recommended Project Structure After Phase 3

```
src/main/
├── grammars/
│   ├── MakoLexer.flex               # Already exists (Phase 2) — no changes
│   └── Mako.bnf                     # NEW: GrammarKit BNF grammar for parser
├── kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
│   └── lang/
│       ├── MakoTokenTypes.kt        # Already exists — referenced from Mako.bnf
│       ├── MakoTokenSets.kt         # Already exists — no changes
│       ├── MakoLexerAdapter.kt      # Already exists — no changes
│       ├── MakoParserDefinition.kt  # UPDATE: createParser() and createElement()
│       └── psi/
│           ├── MakoFile.kt          # Already exists — no changes
│           └── impl/                # NEW: mixin classes for PsiNamedElement
│               ├── MakoDefTagMixin.kt
│               └── MakoBlockTagMixin.kt
├── gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/
│   ├── _MakoLexer.java             # Already exists (Phase 2) — regenerated as before
│   ├── parser/
│   │   └── MakoParser.java         # GENERATED by GenerateParserTask from Mako.bnf
│   └── psi/
│       ├── MakoTypes.java          # GENERATED: IElementType constants for grammar rules
│       ├── MakoDefTag.java         # GENERATED: PSI interface for <%def> nodes
│       ├── MakoBlockTag.java       # GENERATED: PSI interface for <%block> nodes
│       ├── MakoExpression.java     # GENERATED: PSI interface for ${...} nodes
│       ├── MakoControlLine.java    # GENERATED: PSI interface for % control nodes
│       └── impl/
│           ├── MakoDefTagImpl.java # GENERATED: PSI implementation
│           └── ... (one impl per interface)
```

**Critical note on generated source separation:** Generated files go in `src/main/gen`, hand-written mixin classes go in `src/main/kotlin`. Never mix them in the same source root — the `GenerateParserTask` with `purgeOldFiles=true` will delete anything in its output directory.

### Pattern 1: BNF Grammar Header Attributes

**What:** The BNF file header configures how code is generated. The attributes here are the most important decisions.

```bnf
// Source: https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html
{
  parserClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.parser.MakoParser"
  parserUtilClass="com.intellij.lang.parser.GeneratedParserUtilBase"

  // PSI output configuration
  psiClassPrefix="Mako"
  psiImplClassSuffix="Impl"
  psiPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi"
  psiImplPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl"

  // Element type holder — this class gets MakoTypes.DEF_TAG, MakoTypes.EXPRESSION, etc.
  elementTypeHolderClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes"
  elementTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoElementType"
  tokenTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenType"

  // KEY DECISION: Set to false so the generator does NOT duplicate IElementType constants
  // that already exist in MakoTokenTypes.kt from Phase 2
  generateTokens=false

  // Reference the existing token constants using the tokens block
  tokens=[
    EXPR_START="EXPR_START"
    EXPR_END="EXPR_END"
    EXPR_CONTENT="EXPR_CONTENT"
    FILTER_SEP="FILTER_SEP"
    CONTROL_LINE="CONTROL_LINE"
    TAG_OPEN="TAG_OPEN"
    TAG_CLOSE="TAG_CLOSE"
    END_TAG="END_TAG"
    TAG_ATTR_NAME="TAG_ATTR_NAME"
    TAG_ATTR_EQ="TAG_ATTR_EQ"
    TAG_ATTR_VALUE="TAG_ATTR_VALUE"
    CODE_OPEN="CODE_OPEN"
    CODE_CONTENT="CODE_CONTENT"
    MODULE_OPEN="MODULE_OPEN"
    MODULE_CONTENT="MODULE_CONTENT"
    CODE_CLOSE="CODE_CLOSE"
    LINE_COMMENT="LINE_COMMENT"
    DOC_OPEN="DOC_OPEN"
    DOC_CONTENT="DOC_CONTENT"
    DOC_CLOSE="DOC_CLOSE"
    TEMPLATE_TEXT="TEMPLATE_TEXT"
  ]
}
```

**WARNING on `generateTokens=false`:** When `generateTokens=false`, the BNF tokens block maps token names to the string names of existing IElementType constants. The generated parser's token matching uses `builder_.getTokenType()` comparisons — the constants must match what the lexer actually returns. Mismatches cause silent parse failures (tokens are skipped, not matched). Verify by running the parser against a test file and checking the PSI tree in PsiViewer.

### Pattern 2: BNF Grammar Rules for Mako Constructs

**What:** Grammar rules that produce distinct typed PSI nodes. Each top-level named rule becomes a PSI interface.

```bnf
// Source: https://github.com/JetBrains/Grammar-Kit/blob/master/HOWTO.md
// and https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html

makoFile ::= item_*
private item_ ::= def_tag | block_tag | inherit_tag | include_tag | namespace_tag
                | expression | control_line | code_block | module_block
                | doc_comment | line_comment | TEMPLATE_TEXT

// Named block tags that carry a name attribute — implement PsiNamedElement
def_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE
            item_*
            END_TAG
    {
      pin=1
      recoverWhile=tag_recover
      mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoDefTagMixin"
      implements="com.intellij.psi.PsiNamedElement"
      methods=[getName setName getNameIdentifier]
    }

block_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE
              item_*
              END_TAG
    {
      pin=1
      recoverWhile=tag_recover
      mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoBlockTagMixin"
      implements="com.intellij.psi.PsiNamedElement"
      methods=[getName setName getNameIdentifier]
    }

// Single-construct tags (no children, no name)
inherit_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE
    { pin=1 recoverWhile=tag_recover }

include_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE
    { pin=1 recoverWhile=tag_recover }

namespace_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE
    { pin=1 recoverWhile=tag_recover }

// Expression: ${...}
expression ::= EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END
    { pin=1 recoverWhile=expression_recover }

// Control line: % keyword ...
control_line ::= CONTROL_LINE

// Code block: <% ... %>
code_block ::= CODE_OPEN CODE_CONTENT* CODE_CLOSE
    { pin=1 recoverWhile=tag_recover }

// Module block: <%! ... %>
module_block ::= MODULE_OPEN MODULE_CONTENT* CODE_CLOSE
    { pin=1 recoverWhile=tag_recover }

// Doc comment block: <%doc> ... </%doc>
doc_comment ::= DOC_OPEN DOC_CONTENT* DOC_CLOSE
    { pin=1 recoverWhile=tag_recover }

// Line comment: ## ...
line_comment ::= LINE_COMMENT

// Recovery predicates — stop skipping when any new construct boundary is seen
// The ! prefix means "do NOT consume this token — just check if it's present"
private tag_recover ::= !(TAG_OPEN | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN)
private expression_recover ::= !(EXPR_END | TAG_OPEN | END_TAG | CONTROL_LINE)
```

**Design note on TAG_OPEN disambiguation:** The lexer emits `TAG_OPEN` as a single token for `<%def`, `<%block`, `<%inherit`, `<%include`, `<%namespace`, and `<%page` — the tag keyword is part of the token text. The BNF grammar cannot distinguish them by token type alone. Two approaches:

1. **Single `named_tag` rule + attribute inspection:** Define one grammar rule for all named tags; use the tag name attribute value (TAG_ATTR_NAME token text) to classify in PSI. Simpler grammar, but all named tags share one PSI class.

2. **Multiple rules with `external` token filter:** Use a GrammarKit external rule to call a Java method that checks the TAG_OPEN token text and matches only specific tags. More complex but produces distinct PSI types per tag.

**Recommendation for Phase 3:** Use approach 1 (single rule) since PARS-04 requires distinct node types for the listed constructs, but the lexer emits identical TAG_OPEN tokens. In Phase 3 the simplest path is: produce distinct node types by matching on `TAG_ATTR_NAME` value in the grammar rules, or split TAG_OPEN into specific token types in the lexer (e.g., `TAG_OPEN_DEF`, `TAG_OPEN_BLOCK`). The latter is cleaner and keeps grammar simple; it requires a small lexer update.

**CRITICAL DECISION required:** Choose between (a) splitting TAG_OPEN in the lexer or (b) using a single-rule approach. See Open Questions section.

### Pattern 3: PsiNamedElement via Mixin

**What:** Handwritten mixin classes extend the generated PSI Impl class and add `PsiNamedElement` methods. These live in `src/main/kotlin` (not gen).

```kotlin
// Source: https://github.com/JetBrains/Grammar-Kit/blob/master/HOWTO.md
// MakoDefTagMixin.kt — placed in src/main/kotlin/.../lang/psi/impl/
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

// Extends the generated MakoDefTagImpl from src/main/gen
abstract class MakoDefTagMixin(node: ASTNode) : MakoDefTagImpl(node), MakoDefTag {

    override fun getName(): String? {
        // TAG_ATTR_VALUE token holds the name= attribute value (including quotes)
        val attrValue = findChildByType<PsiElement>(MakoTokenTypes.TAG_ATTR_VALUE)
        return attrValue?.text?.trim('"', '\'')
    }

    override fun setName(name: String): PsiElement {
        // Replace the TAG_ATTR_VALUE token text — used by rename refactoring (Phase later)
        // For Phase 3, a minimal implementation is acceptable
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)
    }
}
```

**Note on abstract vs concrete:** The generated `MakoDefTagImpl` is a concrete class. The mixin must declare itself `abstract` if it extends the generated impl — the generated class will extend the mixin (not the other way). GrammarKit generates `class MakoDefTagImpl extends MakoDefTagMixin` when `mixin` is set. This is the correct pattern; do NOT try to instantiate the mixin directly.

### Pattern 4: Updating MakoParserDefinition

**What:** `createParser()` and `createElement()` in the existing `MakoParserDefinition.kt` must be updated from the Phase 2 stubs to real implementations.

```kotlin
// MakoParserDefinition.kt — updated for Phase 3
// Source: https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html

override fun createParser(project: Project): PsiParser = MakoParser()
// MakoParser is the class at parserClass in Mako.bnf — generated into src/main/gen

override fun createElement(node: ASTNode): PsiElement {
    // MakoTypes is the elementTypeHolderClass from Mako.bnf — generated into src/main/gen
    // Factory.createElement dispatches to the correct generated PSI Impl class based on node type
    return MakoTypes.Factory.createElement(node)
}
```

**Critical:** After this change, `createElement()` no longer falls back to `ASTWrapperPsiElement`. The `MakoTypes.Factory.createElement()` must handle all element types that the parser can produce, including the file root type. If an element type is encountered that the factory does not recognize (e.g., the file-level `IFileElementType`), `createElement()` throws. The generated factory handles rule-level types; the file root is handled by `MakoFile` (via `createFile()`) not `createElement()` — so no change to `createFile()` is needed.

### Pattern 5: Gradle GenerateParserTask Configuration

**What:** Uncomment and configure the `generateMakoParser` task in `build.gradle.kts`. It is already imported (`import org.jetbrains.grammarkit.tasks.GenerateParserTask`) and commented out with a stub.

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/tools-gradle-grammar-kit-plugin.html
// build.gradle.kts — Phase 3: activate generateMakoParser

val generateMakoParser = register<GenerateParserTask>("generateMakoParser") {
    sourceFile.set(file("src/main/grammars/Mako.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java")
    pathToPsiRoot.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi")
    purgeOldFiles.set(true)
}

compileKotlin {
    dependsOn(generateMakoLexer, generateMakoParser)
}
```

**Note on `targetRootOutputDir`:** This is the root from which `pathToParser` and `pathToPsiRoot` are computed. With `targetRootOutputDir = file("src/main/gen")`, the parser is generated at `src/main/gen/com/github/.../parser/MakoParser.java` and PSI files at `src/main/gen/com/github/.../psi/MakoDefTag.java`, etc. This directory is already in `sourceSets.main.java.srcDirs` from Phase 2 (`srcDirs("src/main/gen")`).

**Note on `purgeOldFiles=true`:** The GrammarKit task will delete all files in the output directories before regenerating. This is correct and expected; it prevents stale generated files from interfering. Never commit generated files — they are always regenerated from the `.bnf` source.

### Pattern 6: ParsingTestCase for Phase 3

**What:** `ParsingTestCase` provides fixture-based parser testing. You create input `.mako` test files and expected PSI tree `.txt` reference files, then the test compares actual vs expected.

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/parsing-test.html
class MakoParsingTest : ParsingTestCase("", "mako", MakoParserDefinition()) {
    override fun getTestDataPath() = "src/test/testData/parser"

    fun testDefBlock() = doTest(true)
    fun testMalformedTag() = doTest(true)    // Verifies PARS-05: partial PSI, no full failure
}
```

The `doTest(true)` call:
1. Reads `testData/parser/<testMethodName>.mako` as input
2. Parses it with `MakoParserDefinition`
3. Compares the resulting PSI tree against `testData/parser/<testMethodName>.txt`
4. If the `.txt` file does not exist, the test creates it (first run is a "golden master" run)

**Generating expected PSI output:** The easiest way is:
1. Run the plugin in the sandbox IDE (`./gradlew runIde`)
2. Open a `.mako` test file
3. Enable internal mode: `Help > Edit Custom Properties` → add `idea.is.internal=true`
4. Use `Tools > View PSI Structure` → click "Copy PSI" to get the tree text
5. Paste into the expected `.txt` file

### Anti-Patterns to Avoid

- **Activating `generateMakoParser` without a `.bnf` file:** Gradle build fails with a file-not-found error. Create `Mako.bnf` before uncommenting the task.
- **Using `generateTokens=true` (the default) when token types already exist in `MakoTokenTypes.kt`:** GrammarKit generates a second set of `IElementType` constants in `MakoTypes.java`. These are different objects from `MakoTokenTypes` constants — the lexer returns `MakoTokenTypes.TAG_OPEN` but the generated parser checks against `MakoTypes.TAG_OPEN`. These are different instances; token matching silently fails and the PSI tree is flat.
- **Placing mixin classes in `src/main/gen`:** The `purgeOldFiles=true` flag deletes them on every regeneration. Mixins MUST be in `src/main/kotlin`.
- **Making the mixin class concrete when it extends the generated Impl:** GrammarKit generates `class MakoDefTagImpl extends MakoDefTagMixin`. If MakoDefTagMixin is concrete and also tries to extend MakoDefTagImpl, it creates a circular dependency. Declare the mixin `abstract`.
- **Not adding `compileKotlin { dependsOn(generateMakoParser) }`:** The generated `MakoParser.java` must exist before `MakoParserDefinition.kt` is compiled (since it references `MakoParser`). Without the `dependsOn`, a clean build fails because `MakoParser` class doesn't exist yet when Kotlin compilation starts.
- **Applying `recoverWhile` without `pin`:** The Grammar-Kit documentation states that `recoverWhile` requires the attributed rule to also have a `pin`. Without `pin`, error recovery does not activate correctly.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Parser state machine | Hand-written `PsiBuilder.Marker` nesting in Kotlin | `GenerateParserTask` from `Mako.bnf` | GrammarKit generates correct marker creation, error recovery, and `treeBuilt` calls; hand-rolling for 10+ node types is hundreds of error-prone lines |
| Error recovery logic | Custom token-skipping loops in `PsiParser` | `pin` + `recoverWhile` in BNF grammar attributes | GrammarKit's `GeneratedParserUtilBase.parseAsTree()` implements the recovery algorithm correctly; custom implementations miss edge cases around nested markers |
| PSI element type dispatch | `when (node.elementType) { ... }` in `createElement()` | `MakoTypes.Factory.createElement(node)` from generated `MakoTypes.java` | The generated factory is type-safe and complete; a hand-written switch must be manually kept in sync with every grammar rule |
| `PsiNamedElement.getName()` string parsing | Text parsing on `node.text` to extract name | Mixin with `findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)` | `findChildByType` is PSI-safe and navigates by token type; text parsing breaks on whitespace variations and is not resilient to grammar changes |

**Key insight:** GrammarKit treats the `.bnf` as the single source of truth. The generated parser, PSI interfaces, PSI implementations, and element type holder are all synchronized with the grammar. Any hand-rolled equivalent must be manually synchronized — a maintenance burden that grows with every grammar change.

---

## Common Pitfalls

### Pitfall 1: TAG_OPEN Token Cannot Distinguish Tag Types Without Lexer Help

**What goes wrong:** The grammar attempts to write separate rules for `def_tag`, `block_tag`, `inherit_tag`, etc., but the lexer emits a single `TAG_OPEN` token for all of `<%def`, `<%block`, `<%inherit`, etc. GrammarKit cannot match `TAG_OPEN` against a specific tag keyword value — it only matches by token type, not token text.

**Why it happens:** The Phase 2 lexer decision (from prior decisions) was to use a single `TAG_OPEN` token type for all named block tags. This was appropriate for the lexer phase but creates ambiguity at the grammar level.

**How to avoid — Option A (recommended for Phase 3):** Split `TAG_OPEN` into specific token types in the lexer before writing the BNF. In `MakoLexer.flex`, replace the single `"<%" [a-zA-Z]` rule with per-tag rules that return distinct token types: `TAG_OPEN_DEF`, `TAG_OPEN_BLOCK`, `TAG_OPEN_INHERIT`, `TAG_OPEN_INCLUDE`, `TAG_OPEN_NAMESPACE`, `TAG_OPEN_PAGE`. Add these to `MakoTokenTypes.kt`. This changes the lexer output (add tests to verify no regression), but makes the BNF grammar trivially simple.

**How to avoid — Option B:** Use a single grammar rule `tag_statement` for all named tags. The rule matches `TAG_OPEN` generically, and the specific tag type is determined at PSI level by inspecting the token text. All tag nodes share one PSI type; they are distinguishable programmatically but not structurally in the PSI tree.

**Warning signs:** PsiViewer shows all block tags as the same node type (e.g., `tag_statement`) rather than distinct `def_tag`, `block_tag`, `inherit_tag` nodes.

### Pitfall 2: Double-Generating Token Type Constants

**What goes wrong:** The BNF file uses `generateTokens=true` (the default) or omits the `tokens=[...]` block, so GrammarKit generates a second set of `IElementType` constants in `MakoTypes.java`. The generated parser uses `MakoTypes.TAG_OPEN` for matching, but the lexer returns `MakoTokenTypes.TAG_OPEN`. These are distinct Java objects even if they have the same string name. Token matching silently fails — the parser never matches any token and produces a flat PSI tree.

**Why it happens:** By default GrammarKit generates both tokens and PSI. The project already has `MakoTokenTypes.kt` from Phase 2. If the BNF doesn't declare `generateTokens=false` and correctly reference the existing constants, two parallel sets of constants coexist.

**How to avoid:** Set `generateTokens=false` in the BNF header. Reference existing token constants in the `tokens=[...]` block. Verify by adding a parser test and checking that the PSI tree has the expected node types. A flat tree (all tokens under the file root) is the symptom of failed token matching.

**Warning signs:** PsiViewer shows the entire file as a flat list under the `MakoFile` root, with no structured subtrees for `<%def>` or `${...}` constructs.

### Pitfall 3: recoverWhile Consuming Too Many Tokens

**What goes wrong:** A poorly written `recoverWhile` predicate causes the recovery rule to consume nearly all remaining tokens, leaving the rest of the file as a single error node.

**Why it happens:** The `recoverWhile` predicate must be a *negative* predicate — it matches while the next token is NOT a recognized boundary. If the boundary set is too narrow (e.g., only `END_TAG`), the recovery consumes all tokens including other construct starts.

**How to avoid:** The recovery predicate must include ALL tokens that can start a new top-level construct. For Mako: `!(TAG_OPEN | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN | LINE_COMMENT)`. Test with files that have two adjacent malformed tags and verify both produce distinct error nodes rather than one large error block.

**Warning signs:** A file with two malformed `<%def>` tags shows only one error element spanning from the first `<%def>` to the end of file.

### Pitfall 4: Mixin Class Circular Extension

**What goes wrong:** Build fails with `ClassNotFoundException` or `cannot access class` during compilation because the mixin extends the generated Impl class, but the generated Impl class also extends the mixin — circular dependency.

**Why it happens:** GrammarKit generates `class MakoDefTagImpl extends MakoDefTagMixin`. If the mixin `class MakoDefTagMixin extends MakoDefTagImpl`, we get a cycle. The correct relationship is: GrammarKit's generated Impl extends the handwritten mixin; the mixin is `abstract` and references only the PSI interface (not the Impl).

**How to avoid:** Declare the mixin `abstract` and extend `ASTWrapperPsiElement` directly (or no explicit superclass if GrammarKit handles it). The `psiImplClassSuffix` in the BNF header controls the generated name. Reference only the generated *interface* (e.g., `MakoDefTag`) from the mixin, not the Impl.

**Warning signs:** Compilation error referencing a class that supposedly extends itself, or `ClassNotFoundException: MakoDefTagImpl` at runtime.

### Pitfall 5: Generated Files Committed to Git

**What goes wrong:** `src/main/gen` files are committed to git and then diverge from what `GenerateParserTask` would produce, causing build failures or incorrect behavior.

**Why it happens:** Phase 2 already committed `_MakoLexer.java`. The same pattern applied to parser-generated files creates maintenance problems.

**How to avoid:** The generated parser/PSI files should be in `.gitignore` OR always regenerated as part of `./gradlew build`. Phase 2 committed `_MakoLexer.java` as a pragmatic decision for IDE support (the generated file was stable). For Phase 3, the grammar will change more frequently during development. Consider adding generated parser files to `.gitignore` and relying on the Gradle task to regenerate them. Document this decision.

---

## Code Examples

Verified patterns from official sources:

### Minimal BNF Grammar Header

```bnf
// Source: https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html
// https://github.com/JetBrains/Grammar-Kit/blob/master/README.md
{
  parserClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.parser.MakoParser"
  psiClassPrefix="Mako"
  psiImplClassSuffix="Impl"
  psiPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi"
  psiImplPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl"
  elementTypeHolderClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes"
  elementTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoElementType"
  tokenTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenType"
  generateTokens=false
  tokens=[
    TAG_OPEN="TAG_OPEN"
    TAG_CLOSE="TAG_CLOSE"
    // ... all MakoTokenTypes constants by name
  ]
}
```

### Error Recovery Grammar Pattern

```bnf
// Source: https://github.com/JetBrains/Grammar-Kit/blob/master/HOWTO.md
// Pin=1 means: commit to this rule once the first token matches (TAG_OPEN)
// recoverWhile=tag_recover: on failure, skip tokens while tag_recover matches

def_tag ::= TAG_OPEN TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE TAG_CLOSE item_* END_TAG
    { pin=1 recoverWhile=tag_recover }

// Negative predicate: recovery stops when any construct-starting token appears
private tag_recover ::= !(TAG_OPEN | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN)
```

### MakoParserDefinition.kt - Updated createElement

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html
override fun createParser(project: Project): PsiParser = MakoParser()

override fun createElement(node: ASTNode): PsiElement = MakoTypes.Factory.createElement(node)
```

### MakoElementType.kt and MakoTokenType.kt

```kotlin
// Source: pattern from intellij-plugins open-source plugins
// These are base classes referenced in the BNF header — must be created before running generator
package com.github.kimuth.jetbrainsmakotemplateplugin.lang

import com.intellij.psi.tree.IElementType

class MakoElementType(debugName: String) : IElementType(debugName, MakoLanguage)
class MakoTokenType(debugName: String) : IElementType(debugName, MakoLanguage)
```

### ParsingTestCase Test Structure

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/parsing-test.html
class MakoParsingTest : ParsingTestCase("", "mako", MakoParserDefinition()) {
    override fun getTestDataPath() = "src/test/testData/parser"

    // Verifies PARS-04: typed nodes exist for each construct
    fun testWellFormedFile() = doTest(true)

    // Verifies PARS-05: malformed tag still produces partial tree
    fun testMalformedDefTag() = doTest(true)
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hand-written `PsiParser` with `PsiBuilder.Marker` | GrammarKit BNF-generated `MakoParser.java` | Stable since Grammar-Kit 1.x | Generated code handles all edge cases in token advancement and marker nesting |
| `ASTWrapperPsiElement` fallback for all nodes (Phase 2 stub) | `MakoTypes.Factory.createElement(node)` dispatching to typed Impl classes | Phase 3 introduces this | Each construct has a distinct class visible in PsiViewer and queryable by type |
| No `PsiNamedElement` (Phase 2) | `PsiNamedElement` via mixin on def_tag and block_tag | Phase 3 introduces this | `getName()` without string parsing; required for Phase 5 Structure View and later rename refactoring |
| `generateMakoParser` commented out | `generateMakoParser` task active in `build.gradle.kts` | Phase 3 introduces this | Parser is regenerated on every build from the `.bnf` source of truth |

**Deprecated/outdated:**
- `GeneratedParserUtilBase` as a copy in the project: Not needed since IntelliJ 12.1 — it is bundled. Do not copy it.
- Two-pass grammar generation: Grammar-Kit Gradle plugin does NOT support two-pass generation, which means `method mixins` (as distinct from `class mixins`) are not supported via Gradle. Use `mixin` attribute (class-level mixin) rather than `methods` attribute with external method bodies.

---

## Open Questions

1. **Should TAG_OPEN be split into per-tag token types in the lexer, or should the grammar use a single rule for all named tags?**
   - What we know: The Phase 2 lexer uses a single `TAG_OPEN` for all named block tags (`<%def`, `<%block`, `<%inherit`, etc.). PARS-04 requires "distinct node types" for `<%def>`, `<%block>`, `<%inherit>`, `<%include>`, `<%namespace>`. GrammarKit cannot distinguish rules by token text — only by token type.
   - What's unclear: Whether splitting `TAG_OPEN` into `TAG_OPEN_DEF`, `TAG_OPEN_BLOCK`, etc. in the lexer is the intended approach. It requires updating `MakoLexer.flex`, `MakoTokenTypes.kt`, and `MakoTokenSets.kt`. It also means the Phase 2 lexer tests must be updated.
   - Recommendation: Split `TAG_OPEN` into per-tag token types in the first Phase 3 plan. This is the clean solution that gives GrammarKit the information it needs. The prior decision note says "Phase 3 parser can split if needed" — so this is the expected path. The lexer change is small (6 specific rules replacing 1 generic rule).

2. **Should `MakoElementType` and `MakoTokenType` be new classes, or can existing `IElementType` instances in `MakoTokenTypes.kt` be reused?**
   - What we know: The BNF header requires `elementTypeClass` and `tokenTypeClass` to reference concrete subclasses of `IElementType`. The existing `MakoTokenTypes.kt` uses plain `IElementType(name, MakoLanguage)` instances (not a custom subclass). GrammarKit generates `MakoElementType` instances for grammar rules (non-terminal nodes) and `MakoTokenType` instances for tokens.
   - What's unclear: Whether the generator strictly requires these to be distinct subclasses, or whether `IElementType` instances created directly are compatible.
   - Recommendation: Create thin `MakoElementType` and `MakoTokenType` subclasses (one line each) as shown in the Code Examples section. This follows the standard IntelliJ plugin pattern and avoids any compatibility risk.

3. **Should generated PSI files be added to `.gitignore` or committed?**
   - What we know: Phase 2 committed `_MakoLexer.java` to avoid requiring a build step before the IDE can recognize the project. Parser-generated files (`MakoParser.java`, `MakoDefTag.java`, etc.) follow the same pattern.
   - What's unclear: Whether the IntelliJ IDEA project setup requires generated files to be committed for the IDE to work without a prior `./gradlew build`.
   - Recommendation: Commit generated files at the end of each plan (as Phase 2 did). Document that these are generated and should not be edited. The `purgeOldFiles=true` flag ensures stale files are removed on regeneration.

4. **How does `MakoFile` (the root PSI node) interact with `createElement()`?**
   - What we know: `MakoFile` is created by `createFile()`, not `createElement()`. The `IFileElementType` for the root is handled separately by the platform. `createElement()` is only called for non-file composite nodes produced by the parser.
   - What's unclear: Whether the generated `MakoTypes.Factory.createElement()` will throw if the file root type is accidentally passed to it.
   - Recommendation: The generated factory only handles element types defined in the BNF grammar; the file root uses `IFileElementType` which is registered separately in `MakoParserDefinition.FILE`. No change to `MakoFile.kt` or `createFile()` is needed in Phase 3.

---

## Sources

### Primary (HIGH confidence)
- https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html — Grammar-Kit BNF grammar structure, step-by-step PSI generation workflow, `psiClassPrefix`, `elementTypeHolderClass`, `Generate Parser Code` action
- https://plugins.jetbrains.com/docs/intellij/implementing-parser-and-psi.html — `PsiParser`/`PsiBuilder` contract, marker pairs for AST node creation, `PsiNamedElement` requirement for rename/find usages, `ASTWrapperPsiElement` as base implementation
- https://plugins.jetbrains.com/docs/intellij/tools-gradle-grammar-kit-plugin.html — `GenerateParserTask` parameters (`sourceFile`, `targetRootOutputDir`, `pathToParser`, `pathToPsiRoot`, `purgeOldFiles`), Gradle 7.4+ requirement, default Grammar-Kit version 2022.3.2, JFlex 1.9.2
- https://plugins.jetbrains.com/docs/intellij/parsing-test.html — `ParsingTestCase` usage, test data structure (input + `.txt` reference files), `doTest(true)` method, PsiViewer "Copy PSI" workflow
- https://github.com/JetBrains/Grammar-Kit/blob/master/HOWTO.md — `mixin`, `implements`, `methods` attributes; `PsiNamedElement` via mixin pattern; `pin` + `recoverWhile` semantics; predicate rules with `!` prefix; `elementType` for mixing generated and hand-written PSI
- https://github.com/JetBrains/Grammar-Kit/blob/master/README.md — `generateTokens` and `generatePSI` global boolean attributes; `tokens=[...]` block syntax; `elementTypeHolderClass` generates `Factory.createElement()`; `parserUtilClass` (do not copy `GeneratedParserUtilBase`)
- https://github.com/JetBrains/Grammar-Kit/blob/master/TUTORIAL.md — `pin` semantics ("matching is considered successful even if only the first element matched"), `recoverWhile` semantics ("skip tokens while predicate matches"), live preview workflow

### Secondary (MEDIUM confidence)
- WebSearch: "generateTokens and generatePSI global boolean attributes" — verified against Grammar-Kit README; multiple sources confirm existence of these flags
- WebSearch: "recoverWhile requires pin" — confirmed by Grammar-Kit HOWTO examples; all `recoverWhile` examples include `pin`
- WebSearch: "GrammarKit Gradle plugin does not support two-pass generation, therefore it does not support method mixins" — from official GrammarKit Gradle plugin documentation; verified that class-level `mixin` is the correct approach

### Tertiary (LOW confidence)
- WebSearch: Specific `MakoElementType` / `MakoTokenType` subclass pattern — shown as common practice in community examples but not explicitly mandated by official docs; the BNF `elementTypeClass` attribute may work with plain `IElementType` instances

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — GrammarKit + `GenerateParserTask` verified via official SDK docs; version 2023.3.0.2 already in project `libs.versions.toml`
- Architecture patterns: HIGH — BNF header attributes, `mixin` + `PsiNamedElement` pattern, `pin` + `recoverWhile` all verified against Grammar-Kit HOWTO and official IntelliJ SDK docs
- Pitfalls: HIGH for token-type conflicts (MEDIUM for `generateTokens=false` details — should be tested) / HIGH for mixin/extends pattern
- Open questions: All four open questions are genuine decision points that affect implementation scope; none are blockers but all should be resolved in the first plan

**Research date:** 2026-02-19
**Valid until:** 2026-05-19 (Grammar-Kit APIs are stable; plugin version may update but generation patterns are stable across minor versions)

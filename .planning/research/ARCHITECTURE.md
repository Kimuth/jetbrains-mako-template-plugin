# Architecture Research

**Domain:** JetBrains custom language plugin — multi-language template language (Mako = HTML + directives + Python)
**Researched:** 2026-02-19
**Confidence:** MEDIUM (training-data based; WebFetch/WebSearch unavailable during this session; patterns verified against existing codebase structure and consistent IntelliJ Platform documentation knowledge through August 2025)

---

## The Core Architectural Decision

**Recommendation: Implement Mako as a custom Language using the Template Language framework (TemplateLanguage + TemplateDataLanguageConfigurable), with language injection for embedded Python blocks.**

Rationale: Mako is not simply "HTML with additions" — it has its own lexical grammar, its own directive syntax, and control structures that fundamentally alter the document structure. A pure "language injection into HTML" approach treats HTML as the primary language and Mako constructs as injected fragments, which inverts the actual semantics. The Template Language API is designed exactly for this case: a template language that hosts a data language (HTML), with its own constructs layered in between.

---

## Standard Architecture for JetBrains Custom Language Plugins

### System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     IDE Feature Layer                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │Completion│  │Navigation│  │Annotator │  │Folding   │  ...        │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
├───────┴──────────────┴─────────────┴──────────────┴─────────────────┤
│                     PSI (Program Structure Interface) Layer          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  MakoFile (PsiFile)                                         │    │
│  │    ├── HtmlContent (outerjoin into HTML PSI via injection)  │    │
│  │    ├── MakoDirective (<%def>, <%block>, etc.)               │    │
│  │    ├── MakoExpression (${...})                              │    │
│  │    ├── MakoControlLine (% for, % if)                        │    │
│  │    └── MakoPythonBlock (<%...%>, injected Python PSI)       │    │
│  └─────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────┤
│                     Parser Layer                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  MakoParser (implements PsiParser)                          │    │
│  │    builds PSI tree from token stream                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────┤
│                     Lexer Layer                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  MakoLexer (JFlex-generated or hand-written)                │    │
│  │    tokenizes: HTML_CONTENT | MAKO_TAG_START | EXPR_START    │    │
│  │    | PYTHON_BLOCK | CONTROL_LINE | COMMENT | etc.           │    │
│  └─────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────┤
│                     File Type / Language Registration Layer          │
│  ┌──────────┐  ┌───────────────┐  ┌────────────────────────┐        │
│  │MakoFileType│  │MakoLanguage  │  │MakoSyntaxHighlighter  │        │
│  │(.mako ext) │  │(extends       │  │(TextAttributeKey map) │        │
│  │           │  │ TemplateLanguage│ │                       │        │
│  └──────────┘  └───────────────┘  └────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `MakoLanguage` | Language identity object; singleton; extends `TemplateLanguage` | `MakoFileType`, `MakoParserDefinition`, all extension point registrations |
| `MakoFileType` | File extension binding (.mako); icon; associated language | `MakoLanguage`, IDE file system |
| `MakoLexer` | Tokenizes raw character stream into typed tokens | `MakoParser` (consumes tokens), `MakoSyntaxHighlighter` (token colors) |
| `MakoTokenTypes` | Token type constants (IElementType instances) | `MakoLexer`, `MakoParser`, `MakoSyntaxHighlighter` |
| `MakoElementTypes` | PSI node type constants (IElementType instances) | `MakoParser`, PSI element classes |
| `MakoParser` | Builds PSI tree; marks errors; creates AST nodes | `MakoLexer` (via PsiBuilder), PSI node types |
| `MakoParserDefinition` | Factory: creates Lexer, Parser, PsiFile, PSI elements | `MakoLexer`, `MakoParser`, `MakoFile` |
| `MakoFile` | Root PSI node (implements PsiFile); holds the document model | All PSI children, IDE document model |
| PSI Element Classes | Typed tree nodes (MakoDirectiveElement, MakoExpressionElement, etc.) | Each other via parent/child PSI, feature implementations |
| `MakoSyntaxHighlighter` | Maps token types to TextAttributeKey colors | `MakoTokenTypes`, IDE color scheme |
| `MakoSyntaxHighlighterFactory` | Creates `MakoSyntaxHighlighter` instances | `MakoSyntaxHighlighter` |
| `MakoAnnotator` | Semantic highlighting and error marking (post-parse) | PSI tree, IDE annotation model |
| `MakoCompletionContributor` | Code completion proposals | PSI tree context, IDE completion framework |
| `MakoReferenceContributor` | Navigate to def/block definitions, file includes | PSI tree, `PsiReference` implementations |
| `MakoFoldingBuilder` | Defines foldable regions (defs, blocks, control structures) | PSI tree, IDE folding model |
| Python Language Injector | Injects Python PSI into `${...}` and `<%...%>` blocks | PSI tree, IntelliJ Language Injection API, Python plugin PSI |
| HTML Template Data Configurator | Declares HTML as the template data language for Mako | `TemplateDataLanguageConfigurable`, HTML plugin |

---

## The Template Language API: What It Is and Why It Fits

**Confidence: MEDIUM** (consistent with IntelliJ Platform docs through August 2025; verify against current platform source)

IntelliJ Platform provides `com.intellij.lang.TemplateLanguage` as a marker interface/base class for languages that act as "wrappers" around a data language. The canonical examples are Velocity, Freemarker, Twig, Blade, and ERB — all of which are "HTML with template constructs added."

**How it works:**

1. `MakoLanguage` extends `TemplateLanguage` instead of `Language`
2. Register `MakoLanguage` as a template language that hosts HTML via `TemplateDataLanguageConfigurable`
3. The IDE creates a "composite" PSI tree where HTML subtrees are created by the HTML parser and Mako subtrees are created by the Mako parser — they are interleaved in the same file
4. HTML plugin features (tag completion, attribute validation) automatically apply to the HTML portions because the platform knows this is a template language hosting HTML

**Critical registration in plugin.xml:**

```xml
<!-- Declare Mako as a Language -->
<fileType name="Mako" implementationClass="...MakoFileType"
          fieldName="INSTANCE" language="Mako"
          extensions="mako" patterns="*.mako"/>

<!-- Declare Mako as a TemplateLanguage hosting HTML -->
<templateDataLanguageConfigurable
    id="Mako"
    implementationClass="...MakoTemplateDataLanguageConfigurable"/>

<!-- ParserDefinition links language to lexer+parser -->
<lang.parserDefinition language="Mako"
    implementationClass="...MakoParserDefinition"/>

<!-- Syntax highlighter -->
<lang.syntaxHighlighterFactory language="Mako"
    implementationClass="...MakoSyntaxHighlighterFactory"/>
```

---

## Data Flow: From File on Disk to IDE Features

### Parsing Flow

```
.mako file bytes
    |
    v
MakoLexer (tokenize)
    |
    +---> HTML_CONTENT tokens
    +---> MAKO_EXPR_START / EXPR_CONTENT / MAKO_EXPR_END  (${...})
    +---> MAKO_TAG_START / TAG_CONTENT / MAKO_TAG_END      (<%def ...>)
    +---> CONTROL_LINE_START / CONTROL_LINE_CONTENT        (% for ...)
    +---> PYTHON_BLOCK_START / PYTHON_CONTENT / BLOCK_END  (<%  %>)
    +---> COMMENT_START / COMMENT_CONTENT / COMMENT_END    (## ...)
    |
    v
MakoParser (PsiBuilder consumes tokens)
    |
    +---> marks HTML_CONTENT spans (passed to HTML parser for re-parsing)
    +---> marks MAKO_EXPRESSION nodes
    +---> marks MAKO_DIRECTIVE nodes (def, block, inherit, include, etc.)
    +---> marks MAKO_CONTROL_LINE nodes
    +---> marks MAKO_PYTHON_BLOCK nodes
    +---> error markers for malformed constructs
    |
    v
PSI Tree (MakoFile as root)
    |
    +---> HTML outer layer parsed by HTML plugin (TemplateLanguage mechanism)
    +---> Mako nodes interleaved in tree
    +---> Python injected into expression/block nodes (via LanguageInjector)
    |
    v
Feature Providers (read PSI tree)
    |
    +---> MakoAnnotator (error highlighting, semantic coloring)
    +---> MakoCompletionContributor (<%def> name completion, filter names)
    +---> MakoReferenceContributor (<%include file="..."> → file navigation)
    +---> MakoFoldingBuilder (collapse <%def>...</%def> regions)
    +---> Python plugin features (inside injected Python regions)
    +---> HTML plugin features (inside HTML regions via template data mechanism)
```

### Language Injection Flow (for Python inside Mako)

```
PSI Node: MakoExpressionContent  (content of ${...})
    |
    v
MakoPythonInjector (implements MultiHostInjector)
    |
    +---> calls registrar.startInjecting(PythonLanguage.INSTANCE)
    +---> addPlace(prefix, suffix, host, range)
    +---> registrar.doneInjecting()
    |
    v
Injected Python PSI fragment
    |
    v
Python plugin: completion, type inference, error checking (if Python plugin available)
```

---

## Recommended Project Structure

```
src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
├── lang/
│   ├── MakoLanguage.kt              # Language singleton (extends TemplateLanguage)
│   ├── MakoFileType.kt              # FileType (.mako extension, icon)
│   ├── MakoIcons.kt                 # Icon constants
│   └── psi/
│       ├── MakoFile.kt              # Root PsiFile implementation
│       ├── MakoElementTypes.kt      # AST node type constants
│       ├── MakoTokenTypes.kt        # Lexer token type constants
│       └── elements/
│           ├── MakoDirectiveElement.kt      # <%def>, <%block>, etc.
│           ├── MakoExpressionElement.kt     # ${...}
│           ├── MakoControlLineElement.kt    # % for/if/while
│           ├── MakoPythonBlockElement.kt    # <% ... %>
│           └── MakoNamedElement.kt          # elements with navigable names
│
├── lexer/
│   ├── MakoLexer.kt                 # Hand-written or JFlex adapter
│   ├── _MakoLexer.flex              # JFlex grammar (if JFlex used)
│   └── MakoMergingLexer.kt          # Optional: merge adjacent tokens
│
├── parser/
│   ├── MakoParserDefinition.kt      # Factory: lexer+parser+psifile
│   └── MakoParser.kt                # PsiParser implementation
│
├── highlighting/
│   ├── MakoSyntaxHighlighter.kt     # Token → TextAttributeKey map
│   ├── MakoSyntaxHighlighterFactory.kt
│   ├── MakoColorSettingsPage.kt     # IDE color scheme settings page
│   └── MakoAnnotator.kt             # Semantic error/warning annotations
│
├── completion/
│   └── MakoCompletionContributor.kt # Code completion
│
├── navigation/
│   ├── MakoReferenceContributor.kt  # PsiReferenceContributor
│   └── MakoGotoSymbolContributor.kt # Navigate > Symbol for def names
│
├── injection/
│   └── MakoPythonInjector.kt        # MultiHostInjector for Python in ${}/<%>
│
├── folding/
│   └── MakoFoldingBuilder.kt        # FoldingBuilderEx
│
├── template/
│   └── MakoTemplateDataLanguageConfigurable.kt  # HTML as template data language
│
└── settings/
    └── MakoSettings.kt              # Optional: plugin settings (PersistentStateComponent)

src/main/resources/
├── META-INF/
│   └── plugin.xml                   # All extension point registrations
├── icons/
│   └── mako-file.svg                # .mako file icon
└── messages/
    └── MakoBundle.properties        # User-facing strings
```

### Structure Rationale

- **lang/**: Core language identity objects (Language, FileType). These are singletons with no dependencies on other plugin components — build first.
- **lang/psi/**: PSI element types and token types are pure data/constants. Build alongside lang/.
- **lexer/**: Depends only on token types. JFlex grammar is the most maintainable approach for a language of Mako's complexity.
- **parser/**: Depends on lexer and PSI element types. `ParserDefinition` is the integration point that wires everything together.
- **highlighting/**: Depends on token types and parser. Syntax highlighter is independent of the parser; annotator depends on PSI.
- **injection/**: Depends on PSI elements and Python plugin API. Build after core PSI is stable.
- **completion/navigation/folding/**: All depend on stable PSI tree. Build after injection.

---

## Architectural Patterns

### Pattern 1: JFlex Lexer for Multi-Mode Tokenization

**What:** Use JFlex lexer generator with multiple lexer states (modes) to handle Mako's nested syntaxes.
**When to use:** Mako requires at least 4 lexer states: HTML_CONTENT, MAKO_EXPRESSION (inside `${`), MAKO_BLOCK (inside `<%`), CONTROL_LINE (after `% ` at line start).
**Trade-offs:** JFlex requires a separate `.flex` grammar file and a Gradle generation step, but produces a correct, maintainable lexer. Hand-writing a multi-state lexer is error-prone and hard to modify.

**Example JFlex state structure:**
```
%state YYINITIAL      // HTML content mode
%state MAKO_EXPR      // inside ${...}
%state MAKO_TAG       // inside <%...>
%state MAKO_BLOCK     // inside <% ... %> (Python block)
%state CONTROL_LINE   // after "% " at line start
%state LINE_COMMENT   // after "## "

<YYINITIAL> {
  "${" { yybegin(MAKO_EXPR); return MAKO_EXPR_START; }
  "<%" { yybegin(MAKO_TAG); return MAKO_TAG_START; }
  "^% " { yybegin(CONTROL_LINE); return CONTROL_LINE_START; }
  "^## " { yybegin(LINE_COMMENT); return COMMENT_START; }
  [^] { return HTML_CONTENT; }
}
```

### Pattern 2: CompositeElement PSI for Directive Nodes

**What:** Each Mako directive type (<%def>, <%block>, <%inherit>, etc.) gets its own `ASTWrapperPsiElement` subclass with typed accessor methods.
**When to use:** When features need to work with directive-specific attributes (e.g., the `name=""` attribute of `<%def>`, the `file=""` attribute of `<%include>`).
**Trade-offs:** More classes upfront, but enables type-safe PSI navigation used by completion and reference resolution.

**Example:**
```kotlin
class MakoDefDirective(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
    val defName: String? get() = findChildByType<PsiElement>(MakoTokenTypes.ATTR_VALUE)?.text
    val arguments: String? get() = findChildByType<PsiElement>(MakoTokenTypes.ARGS_VALUE)?.text
    override fun getName(): String? = defName
    override fun setName(name: String): PsiElement = // rename support
}
```

### Pattern 3: MultiHostInjector for Python Regions

**What:** Register a `MultiHostInjector` that injects Python language into expression (`${...}`) and block (`<% ... %>`) PSI nodes. This delegates Python analysis entirely to PyCharm's Python plugin.
**When to use:** For all Python content within Mako templates — expressions and module-level blocks.
**Trade-offs:** Requires `com.intellij.modules.python` dependency in plugin.xml (PyCharm-only). Python plugin provides completion, type inference, and error detection "for free" inside injected regions.

**Example:**
```kotlin
class MakoPythonInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is MakoExpressionContent) return
        val pythonLang = Language.findLanguageByID("Python") ?: return
        registrar.startInjecting(pythonLang)
            .addPlace(null, null, context, context.textRangeInParent)
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(MakoExpressionContent::class.java, MakoPythonBlockContent::class.java)
}
```

### Pattern 4: TemplateLanguage for HTML Interop

**What:** Extending `TemplateLanguage` instead of `Language` enables the platform's multi-root PSI mechanism where HTML subtrees are parsed by the HTML parser within a Mako file.
**When to use:** Required for any template language that hosts HTML. Without this, the IDE cannot provide HTML completion, tag validation, or CSS/JS injection inside HTML portions of a Mako file.
**Trade-offs:** Requires HTML plugin dependency. The TemplateDataLanguageConfigurable needs to specify HTML as the template data language.

---

## Template Language API vs. Custom Language: Decision Analysis

**Confidence: MEDIUM**

| Criterion | Template Language API (Recommended) | Pure Custom Language | Language Injection into HTML |
|-----------|-------------------------------------|---------------------|------------------------------|
| HTML features in templates | Yes — platform handles it | No — must reimplement | Yes — HTML is primary |
| Mako-specific PSI | Yes — full control | Yes — full control | Limited — injected fragments only |
| Python injection | Yes — compatible | Yes — compatible | Complex — injection within injection |
| Build complexity | Medium | Medium | High — inverted model |
| Matches Mako semantics | Yes — template wraps HTML | Partially | No — HTML wraps Mako |
| Existing examples | Velocity, Twig, Blade plugins | Scala, Ruby, etc. | JSP (partially) |
| Risk of breakage | Low — stable API | Low | High — platform support varies |

**Verdict:** Use Template Language API. Pure custom Language means rebuilding HTML analysis from scratch. Language injection into HTML inverts the semantic model (HTML is not the primary language; Mako is). Template Language API is exactly what was designed for this use case.

---

## Anti-Patterns

### Anti-Pattern 1: Treating Mako as "HTML with Annotations"

**What people do:** Register the file type as HTML, use language injection to add Mako constructs as injected "islands" within HTML.
**Why it's wrong:** Mako control structures (`% for`, `% if`) and directives (`<%def>`) are not annotations on HTML — they structurally control which HTML is rendered. The injected fragment model breaks when a single Mako construct spans multiple HTML subtrees (e.g., `% for` loop over `<li>` items).
**Do this instead:** Register Mako as the primary language (TemplateLanguage), HTML as the hosted data language.

### Anti-Pattern 2: Token Types as Strings

**What people do:** Define token types as `IElementType("HTML_CONTENT")` directly inline in the lexer or parser.
**Why it's wrong:** Token types must be singletons registered in a central constants object. Duplicated `IElementType` instances with the same name cause conflicts in the platform's type system.
**Do this instead:** Define all token types in a single `MakoTokenTypes` object and reference them by field name everywhere.

### Anti-Pattern 3: Building Features Before Core PSI is Stable

**What people do:** Start adding completion, navigation, and annotation while the lexer/parser is still changing significantly.
**Why it's wrong:** Every feature depends on specific PSI node types and their structure. A parser refactoring that changes node structure cascades into every feature implementation simultaneously.
**Do this instead:** Build in dependency order. Stabilize the PSI tree shape before adding feature providers.

### Anti-Pattern 4: Skipping the PsiFile Subclass

**What people do:** Use `PsiFileBase` directly without a typed `MakoFile` subclass.
**Why it's wrong:** Features like go-to-symbol, file-level inspections, and the TemplateLanguage mechanism all need to call methods on the file root. Without a typed subclass, these require awkward casts everywhere.
**Do this instead:** Always create a named `MakoFile` class extending `PsiFileBase` and implementing `PsiFile`.

### Anti-Pattern 5: Bundling Python Reimplementation

**What people do:** Attempt to reparse Python inside `${...}` with a custom mini-Python lexer rather than injecting Python from PyCharm's Python plugin.
**Why it's wrong:** PyCharm already has a full Python parser, type inference engine, and completion system. Any reimplementation will be inferior and unmaintainable. Language injection gives you the full Python plugin experience in those regions.
**Do this instead:** Depend on `com.intellij.modules.python`, inject `PythonLanguage.INSTANCE` into Python content regions, and let the Python plugin handle all Python features.

---

## Integration Points

### External Services / Plugin Dependencies

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| HTML Plugin (`com.intellij.modules.web.core`) | Template Language API — HTML is the "template data language" | Required for HTML completion inside Mako files |
| Python Plugin (`com.intellij.modules.python`) | `MultiHostInjector` — inject Python PSI into expression/block regions | PyCharm-only dependency; makes plugin PyCharm-specific |
| IntelliJ Color Settings | `ColorSettingsPage` extension point + `TextAttributeKey` constants | Allows users to customize Mako syntax colors |
| IntelliJ File Icons | `IconProvider` or `FileIconProvider` extension point | Custom icon for .mako files in project tree |

### Internal Component Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Lexer ↔ Parser | PsiBuilder token consumption; lexer produces stream, parser marks subtrees | Lexer is stateless from parser perspective; only forward traversal |
| Parser ↔ PSI Elements | `ASTNode` types defined in `MakoElementTypes`; parser creates nodes, PSI wraps them | PSI element factory in `ParserDefinition.createElement()` maps node types to classes |
| PSI ↔ Feature Providers | All features read PSI via `psiElement.findChildByType<>()`, `psiElement.getParent()`, etc. | Features must not modify PSI outside write actions |
| PSI ↔ Python Injector | Injector pattern-matches on specific PSI element classes | Injector runs on every PSI update; must be fast (avoid heavy computation) |
| Plugin ↔ HTML Plugin | `TemplateDataLanguageConfigurable` + `TemplateLanguage` base class | Platform handles multi-root PSI creation automatically once registered |

---

## Build Order (Component Dependencies)

```
Phase 1 — Language Foundation (no dependencies)
    MakoTokenTypes         (pure constants)
    MakoElementTypes       (pure constants)
    MakoLanguage           (singleton, no deps)
    MakoFileType           (depends on MakoLanguage)
    MakoIcons              (pure constants)

Phase 2 — Lexer (depends on Phase 1)
    _MakoLexer.flex / MakoLexer
    [JFlex generation step]

Phase 3 — Parser + PSI (depends on Phases 1-2)
    MakoFile               (root PsiFile, depends on MakoLanguage)
    PSI element classes    (depend on MakoElementTypes)
    MakoParser             (depends on lexer + element types)
    MakoParserDefinition   (wires all of Phase 1-3 together)
    plugin.xml lang.parserDefinition registration

Phase 4 — Syntax Highlighting (depends on Phases 1-3)
    MakoSyntaxHighlighter  (depends on MakoTokenTypes)
    MakoSyntaxHighlighterFactory
    MakoColorSettingsPage  (depends on MakoSyntaxHighlighter + TextAttributeKey constants)
    plugin.xml registrations

Phase 5 — Template Language Integration (depends on Phase 3)
    MakoTemplateDataLanguageConfigurable
    plugin.xml templateDataLanguageConfigurable registration
    HTML plugin dependency added to plugin.xml

Phase 6 — Language Injection (depends on Phase 3 + Python plugin)
    MakoPythonInjector     (depends on stable PSI element types)
    plugin.xml multiHostInjector registration
    Python plugin dependency added to plugin.xml

Phase 7 — Semantic Features (depends on Phases 3-6)
    MakoAnnotator          (depends on stable PSI)
    MakoFoldingBuilder     (depends on stable PSI)
    MakoCompletionContributor (depends on PSI + injection working)
    MakoReferenceContributor  (depends on PSI + file system)
    MakoGotoSymbolContributor (depends on PSI + <%def> names)
```

---

## Scalability Considerations

This is a plugin, not a server — "scalability" means how well the architecture handles growing complexity of the plugin itself:

| Concern | Early (MVP) | Mid (Full Feature Set) | Mature |
|---------|-------------|------------------------|--------|
| Lexer complexity | 4-5 states | 6-8 states (add filter expressions, multiline) | Stable — lexer rarely changes after initial pass |
| Parser changes | Expect frequent restructuring | Refine error recovery | Add deprecated syntax warnings |
| PSI node proliferation | 5-8 node types | 15-20 node types | Stable |
| Python injection accuracy | Simple range injection | Handle nested expressions (`${x.method(${y})}`) | Stable |
| HTML interop | Basic template data language | Handle Mako inside HTML attributes | Stable |

---

## Key Architectural Risk

**Risk: Lexer state explosion for Mako's filter expressions**
**Confidence: MEDIUM**

Mako supports filter expressions: `${x | h,trim}` where `h` and `trim` are filter names applied to the expression. This requires the lexer to recognize `|` inside an expression context differently from `|` in Python code (bitwise OR). If not handled in the lexer, the parser must disambiguate — which is harder and produces worse error messages. This is a known complexity that should be addressed in the lexer design from the beginning.

**Risk: <%doc> blocks containing HTML**
**Confidence: LOW (training data only)**

Mako `<%doc>` blocks are documentation comments that can contain arbitrary text including HTML. Whether this content should be parsed as HTML or as plain text is a design decision. Treating it as plain text is simpler; treating it as HTML enables HTML completion inside doc comments. Recommend treating as plain text initially.

---

## Sources

- IntelliJ Platform SDK Documentation — Custom Language Support tutorial (training data, HIGH confidence for patterns described; verify against https://plugins.jetbrains.com/docs/intellij/custom-language-support.html)
- IntelliJ Platform SDK — Template Languages section (training data, MEDIUM confidence; verify against https://plugins.jetbrains.com/docs/intellij/template-languages.html)
- Existing open-source examples with MEDIUM confidence: IntelliJ community Velocity plugin, Twig plugin, Blade plugin use TemplateLanguage base class
- Existing codebase analysis: `build.gradle.kts` (IntelliJ Platform 2025.2.5), `plugin.xml` (scaffold structure), `gradle.properties` (build 252+)
- Note: WebSearch and WebFetch were unavailable during this research session. All claims based on training data (cutoff August 2025). Mark patterns requiring platform-specific API calls for verification during implementation.

---

*Architecture research for: JetBrains Mako Template Plugin*
*Researched: 2026-02-19*

# Architecture Research

**Domain:** JetBrains custom language plugin — HTML language injection via TemplateLanguageFileViewProvider
**Researched:** 2026-02-22
**Confidence:** HIGH (verified against live Handlebars plugin source from JetBrains/intellij-plugins GitHub, TemplateDataElementType and TemplateDataLanguageMappings source from JetBrains/intellij-community, and existing Mako plugin codebase)

---

## The Core Architectural Decision for v0.3.0

**Goal:** Inject the HTML language into TEMPLATE_TEXT regions of `.mako` files so PyCharm delivers full HTML editing (tag/attr completion, Emmet, error detection) inside Mako files.

**Recommended approach:** Implement `TemplateLanguageFileViewProvider` (via `MakoFileViewProvider` + `MakoFileViewProviderFactory`). This is the canonical IntelliJ Platform mechanism for template languages that host a data language. Reference implementation: Handlebars plugin (`HbFileViewProvider.java`) in JetBrains/intellij-plugins.

**Why not MultiHostInjector for HTML?** `MultiHostInjector` is already used for Python injection into `${...}` and `<% %>` regions. It operates at the level of PSI host nodes — injecting into existing Mako PSI elements. HTML, by contrast, must cover all TEMPLATE_TEXT tokens simultaneously as a second full PSI tree of the file. `MultiHostInjector` cannot create a parallel file-level PSI tree; `TemplateLanguageFileViewProvider` is the correct mechanism for that. The two mechanisms operate at different levels and do not conflict.

---

## How TemplateLanguageFileViewProvider Works

The platform's `MultiplePsiFilesPerDocumentFileViewProvider` allows one document (one VirtualFile, one CharSequence) to back multiple PSI trees simultaneously. `TemplateLanguageFileViewProvider` extends this to declare which language is the "base" (Mako Template, owns the main PSI tree) and which is the "template data language" (HTML, gets a second PSI tree).

**Key mechanics (verified from source):**

1. `MakoFileViewProvider.getBaseLanguage()` returns `MakoLanguage`.
2. `MakoFileViewProvider.getTemplateDataLanguage()` returns `HTMLLanguage.INSTANCE` (default, user-configurable via IDE Settings > Languages & Frameworks > Template Data Languages).
3. `MakoFileViewProvider.getLanguages()` returns `setOf(MakoLanguage, templateDataLanguage)`.
4. `MakoFileViewProvider.createFile(lang)`:
   - For `MakoLanguage`: delegates to `MakoParserDefinition.createFile(this)` → returns `MakoFile` (existing, unchanged).
   - For HTML: delegates to HTML's `ParserDefinition.createFile(this)`, then calls `(htmlPsiFile as PsiFileImpl).setContentElementType(templateDataElementType)`.
5. `TemplateDataElementType` is the bridge: constructed with `(debugName, HTMLLanguage, TEMPLATE_TEXT_TOKEN_TYPE, OUTER_ELEMENT_TYPE)`. It re-lexes the file using the Mako lexer, extracts only the TEMPLATE_TEXT tokens, presents their concatenated text to the HTML parser, and stitches `OuterLanguageElement` placeholders back into the HTML PSI tree wherever Mako constructs appear.
6. `OUTER_ELEMENT_TYPE` is a new `IElementType` added to `MakoTokenTypes` — it identifies placeholder leaf nodes in the HTML PSI tree that represent Mako directives/expressions (the "foreign" content from HTML's perspective).

The result: the document has two PSI trees. The Mako PSI tree (rooted at `MakoFile`) spans the whole file and contains all Mako nodes plus `TEMPLATE_TEXT` leaf tokens. The HTML PSI tree (rooted at an `HtmlFile`) also spans the whole file but sees only the TEMPLATE_TEXT content; Mako constructs appear as opaque `OuterLanguageElement` leaves. All existing Mako features (folding, structure view, completion, annotator, Python injection) operate on the Mako PSI tree and are unaffected.

---

## System Overview — Before and After

### Before v0.3.0 (current state)

```
VirtualFile (.mako)
    |
    v
SingleRootFileViewProvider
    |
    v
MakoFile (PSI tree, entire document)
    ├── MakoDefTag, MakoBlockTag, ...     <- Mako PSI nodes
    ├── MakoExpression (${...})           <- Python injected via MultiHostInjector
    ├── MakoCodeBlock (<% %>)             <- Python injected via MultiHostInjector
    └── MakoTemplateTextContent           <- TEMPLATE_TEXT tokens (plain text, no HTML analysis)

Python injection (MultiHostInjector):
    MakoExpression/CodeBlock → injected Python PSI (virtual file fragment)
```

### After v0.3.0

```
VirtualFile (.mako)
    |
    v
MakoFileViewProvider  (MultiplePsiFilesPerDocumentFileViewProvider + TemplateLanguageFileViewProvider)
    |
    +---> MakoFile (PSI tree, full document)           [base language = MakoLanguage]
    |         ├── MakoDefTag, MakoBlockTag, ...
    |         ├── MakoExpression (${...})
    |         ├── MakoCodeBlock (<% %>)
    |         └── MakoTemplateTextContent (TEMPLATE_TEXT tokens)
    |
    +---> HtmlFile (PSI tree, same document)           [template data language = HTML]
              ├── XmlTag (<div>, <p>, <a href=...>)
              ├── XmlAttributeValue
              ├── OuterLanguageElement  <-- placeholder for <%def ...>...</%def>
              ├── XmlText ("Hello, ")
              ├── OuterLanguageElement  <-- placeholder for ${name}
              └── XmlText ("!")

Python injection (MultiHostInjector — unchanged):
    MakoExpression/CodeBlock (in Mako PSI) → injected Python PSI (virtual file fragment)
```

---

## Component Map: New vs Modified vs Unchanged

### New Components (must be created)

| Component | Location | Purpose |
|-----------|----------|---------|
| `MakoFileViewProvider` | `lang/MakoFileViewProvider.kt` | Implements `MultiplePsiFilesPerDocumentFileViewProvider` + `TemplateLanguageFileViewProvider`. Holds base language (Mako) and template data language (HTML). Implements `createFile()`, `getLanguages()`, `getTemplateDataLanguage()`, `getContentElementType()`, `cloneInner()`. |
| `MakoFileViewProviderFactory` | `lang/MakoFileViewProviderFactory.kt` | Implements `FileViewProviderFactory`. Returns a new `MakoFileViewProvider`. Registered in plugin.xml via `lang.fileViewProviderFactory language="Mako Template"`. |
| `OUTER_ELEMENT_TYPE` | `MakoTokenTypes.kt` (addition) | New `IElementType("MAKO_OUTER_ELEMENT", MakoLanguage)` — marks placeholder leaves in the HTML PSI tree that correspond to Mako constructs. Used as the 4th argument to `TemplateDataElementType`. |
| `TEMPLATE_DATA_ELEMENT_TYPE` | `MakoFileViewProvider.kt` (inline companion) | A `TemplateDataElementType("MAKO_TEMPLATE_DATA", HTMLLanguage.INSTANCE, MakoTokenTypes.TEMPLATE_TEXT, MakoTokenTypes.OUTER_ELEMENT_TYPE)` instance. Drives the HTML PSI tree construction. Cached per data language (same pattern as Handlebars). |

### Modified Components (must be updated)

| Component | Change Required | Risk |
|-----------|----------------|------|
| `plugin.xml` | Add `<lang.fileViewProviderFactory language="Mako Template" implementationClass="...MakoFileViewProvider Factory"/>`. No other registration changes needed — `TemplateDataLanguageMappings` handles the user-facing "Settings > Template Data Languages" UI automatically once `TemplateLanguageFileViewProvider` is implemented. | Low |
| `MakoTokenTypes.kt` | Add `OUTER_ELEMENT_TYPE` constant. | Low — additive only |
| `MakoParserDefinition.kt` | No change to logic required. `createFile(viewProvider)` already works correctly: returns `MakoFile(viewProvider)`. The `viewProvider` argument will now be a `MakoFileViewProvider` instead of `SingleRootFileViewProvider`, but `MakoFile` accepts any `FileViewProvider`. | None |
| `MakoFile.kt` | No change required. `PsiFileBase(viewProvider, MakoLanguage)` works with any `FileViewProvider`. | None |

### Explicitly Unchanged Components

| Component | Why Unchanged |
|-----------|--------------|
| `MakoPythonInjector` | Operates on `MakoExpression`, `MakoCodeBlock`, `MakoModuleBlock` nodes in the Mako PSI tree. These nodes exist only in the Mako PSI tree, not in the HTML PSI tree. `MultiHostInjector` and `TemplateLanguageFileViewProvider` are independent mechanisms at different levels. Python injection continues to work exactly as before. |
| `MakoAnnotator` | Registered as `annotator language="Mako Template"` — only fires on Mako PSI elements. The guard `element.containingFile.language != MakoLanguage` continues to filter correctly. |
| `MakoFoldingBuilder` | Registered as `lang.foldingBuilder language="Mako Template"` — only fires on Mako PSI elements. The folding regions are computed from the Mako PSI tree, not the HTML PSI tree. |
| `MakoStructureViewFactory` | Registered for Mako Template language — unaffected. |
| `MakoCompletionContributor` | `language="any"` with internal guard. HTML completion in TEMPLATE_TEXT regions is handled by the HTML PSI tree, not the Mako completion contributor. The internal `file.language.id` guard prevents false positives. |
| `MakoSyntaxHighlighter` | Highlights Mako-specific tokens (tags, expressions, comments). HTML syntax highlighting in TEMPLATE_TEXT is handled by the HTML PSI tree's own highlighter. |
| `MakoLexerAdapter` / `_MakoLexer.flex` | No changes. The Mako lexer already emits `TEMPLATE_TEXT` tokens for non-Mako regions. `TemplateDataElementType` uses this lexer (via `MakoParserDefinition.createLexer()`) to identify which tokens to pass to the HTML parser. |
| `MakoLanguage` | Already implements `TemplateLanguage` — this is a prerequisite for `TemplateLanguageFileViewProvider` and is already in place. |
| All PSI mixin classes | No changes needed. |
| All test infrastructure | Existing tests test the Mako PSI tree, which is unchanged. New tests will verify HTML PSI tree availability. |

---

## Data Flow Changes

### File Opening (new flow)

```
VirtualFile opened
    |
    v
FileViewProviderFactory.createFileViewProvider() dispatched by platform
    |
    v [MakoFileViewProviderFactory matches language="Mako Template"]
MakoFileViewProvider constructed
    |
    +-- getBaseLanguage() -> MakoLanguage
    +-- getTemplateDataLanguage() ->
            TemplateDataLanguageMappings.getInstance(project).getMapping(file)
                if null -> TemplateDataLanguagePatterns.getTemplateDataLanguageByFileName(file)
                if null -> HTMLLanguage.INSTANCE (default hardcoded in MakoFileViewProvider)
    |
    v
Platform calls createFile(MakoLanguage):
    -> MakoParserDefinition.createFile(viewProvider) -> MakoFile (unchanged path)

Platform calls createFile(HTMLLanguage):
    -> HTML ParserDefinition.createFile(viewProvider) -> HtmlFile
    -> (htmlFile as PsiFileImpl).setContentElementType(TEMPLATE_DATA_ELEMENT_TYPE)
    -> TEMPLATE_DATA_ELEMENT_TYPE.parseContents():
         1. Creates Mako lexer
         2. Scans full file, collects TEMPLATE_TEXT token ranges
         3. Concatenates TEMPLATE_TEXT text (replacing Mako constructs with whitespace/placeholders)
         4. Parses concatenated text as HTML
         5. Inserts OuterLanguageElement nodes at positions of non-TEMPLATE_TEXT tokens
    -> HTML PSI tree now available via viewProvider.getPsi(HTMLLanguage.INSTANCE)
```

### Caret Position Dispatch

```
Caret in TEMPLATE_TEXT region (e.g., "<div class=")
    |
    +-- Mako PSI: MakoTemplateTextContent node — Mako features fire (folding header check, etc.)
    +-- HTML PSI: XmlTag / XmlAttribute node — HTML features fire (tag completion, error check)

Caret in ${...} region
    |
    +-- Mako PSI: MakoExpression node — Python injection active, Python completion fires
    +-- HTML PSI: OuterLanguageElement — HTML does not analyze this region
```

### Python Injection (unchanged flow)

```
MakoPythonInjector.getLanguagesToInject(registrar, context):
    context = MakoExpression or MakoCodeBlock (from Mako PSI tree)
    -> registrar.startInjecting(Python)
    -> registrar.addPlace(..., host, TextRange)    [host is in Mako PSI tree]
    -> registrar.doneInjecting()
    -> Platform creates injected Python virtual file fragment
```

No interaction with HTML PSI tree. The two injection paths are independent.

---

## Architectural Patterns

### Pattern 1: TemplateDataElementType with Cached Instance

**What:** `TemplateDataElementType` is constructed lazily and cached per template data language ID (ConcurrentMap). This avoids re-creating the type object on every file open.

**Why:** `TemplateDataElementType` extends `IFileElementType` — creating it registers it with the platform. Creating duplicates for the same language causes `AssertionError`. Handlebars uses `ConcurrentMap<String, TemplateDataElementType>` keyed by language ID.

**Example (Kotlin adaptation):**
```kotlin
// In MakoFileViewProvider companion object
private val TEMPLATE_DATA_BY_LANG = ConcurrentHashMap<String, TemplateDataElementType>()

fun getTemplateDataElementType(dataLang: Language): TemplateDataElementType {
    return TEMPLATE_DATA_BY_LANG.getOrPut(dataLang.id) {
        TemplateDataElementType(
            "MAKO_TEMPLATE_DATA",
            dataLang,
            MakoTokenTypes.TEMPLATE_TEXT,     // the "inner" token type (data language content)
            MakoTokenTypes.OUTER_ELEMENT_TYPE  // placeholder for Mako constructs in HTML tree
        )
    }
}
```

### Pattern 2: Default Template Data Language via Code, User-Overridable via Settings

**What:** `MakoFileViewProvider.getTemplateDataLanguage()` uses `TemplateDataLanguageMappings.getInstance(project).getMapping(file)` as the primary source (user-configured in IDE Settings), falling back to `HTMLLanguage.INSTANCE` as the hardcoded default.

**Why:** The `TemplateDataLanguageMappings` service stores per-file/per-directory/per-project overrides in `templateLanguages.xml` (project settings). If no user override exists, the plugin's default (HTML) is used. There is no plugin.xml extension point to declare a default — the default is established by the fallback return value in `getTemplateDataLanguage()`.

**Example:**
```kotlin
private fun resolveTemplateDataLanguage(manager: PsiManager, file: VirtualFile): Language {
    // 1. User-configured project mapping takes precedence
    val configured = TemplateDataLanguageMappings.getInstance(manager.project).getMapping(file)
    if (configured != null) return configured
    // 2. Hardcoded default: HTML (the vast majority of .mako files are HTML templates)
    return HTMLLanguage.INSTANCE
}
```

### Pattern 3: supportsIncrementalReparse = false

**What:** Override `supportsIncrementalReparse()` to return `false` for the base language. This forces full re-parse on every edit rather than attempting incremental tree patching.

**Why:** The HTML PSI tree is derived from the Mako lexer output. Incremental re-parse of the Mako PSI tree does not automatically rebuild the HTML PSI tree. Disabling incremental re-parse ensures both trees stay consistent. This is the established pattern in Handlebars and other template language plugins. Performance cost is minimal for typical Mako files.

### Pattern 4: cloneInner for Injected Files

**What:** `cloneInner(virtualFile)` must return a new `MakoFileViewProvider` with `physical=false`.

**Why:** The platform calls this when creating an injected file context (e.g., for Python injection fragments). Returning `null` or failing here causes `NullPointerException` during injection. Handlebars passes through `myBaseLanguage` and `myTemplateLanguage` to preserve the language configuration.

---

## Anti-Patterns

### Anti-Pattern 1: Registering HTML Injection via MultiHostInjector for TEMPLATE_TEXT

**What people do:** Try to inject HTML into `MakoTemplateTextContent` nodes using `MultiHostInjector`, similar to how Python is injected into `MakoExpression`.

**Why it's wrong:** `MultiHostInjector` creates injected virtual file fragments — small synthetic files containing only the injected content. It does not create a parallel PSI tree for the whole file. The HTML PSI tree produced by `MultiHostInjector` would be fragmented across many disjoint injection sites, preventing HTML features from seeing the full document context (e.g., checking that `<div>` opened in one `TEMPLATE_TEXT` region is closed in another). `TemplateLanguageFileViewProvider` creates a single coherent HTML PSI tree for the entire file, which is what HTML tooling expects.

**Do this instead:** Use `TemplateLanguageFileViewProvider` as described above.

### Anti-Pattern 2: Hardcoding HTMLLanguage.INSTANCE Without Checking User Mappings

**What people do:** Return `HTMLLanguage.INSTANCE` unconditionally from `getTemplateDataLanguage()`.

**Why it's wrong:** Users may want to use a different template data language (e.g., XML for some Mako templates, or plain text). `TemplateDataLanguageMappings` provides the platform's standard Settings UI for this. Ignoring it makes the plugin non-configurable and breaks user expectations set by other template language plugins.

**Do this instead:** Check `TemplateDataLanguageMappings.getInstance(project).getMapping(file)` first, fall back to `HTMLLanguage.INSTANCE` only when null.

### Anti-Pattern 3: Creating TemplateDataElementType as a Static Constant

**What people do:** Declare `val TEMPLATE_DATA_ELEMENT_TYPE = TemplateDataElementType("MAKO_TEMPLATE_DATA", HTMLLanguage.INSTANCE, ...)` as a top-level Kotlin object or companion object `val`.

**Why it's wrong:** `TemplateDataElementType` is parameterized by the template data language. If the user changes the template data language (Settings > Template Data Languages), a different language needs a different `TemplateDataElementType` instance. A static constant locks to HTML permanently. Additionally, object initialization order in Kotlin can cause `HTMLLanguage.INSTANCE` to not yet be registered when the constant initializes.

**Do this instead:** Cache instances in a `ConcurrentHashMap<String, TemplateDataElementType>` keyed by language ID, created on demand.

### Anti-Pattern 4: Modifying MakoParserDefinition.createFile() to Return Different Types

**What people do:** Attempt to switch between `MakoFile` and an HTML-specific file type in `MakoParserDefinition.createFile()` based on the `viewProvider`.

**Why it's wrong:** `MakoParserDefinition` is registered for `language="Mako Template"`. It must always return a `MakoFile`. The HTML PSI file is created via HTML's own `ParserDefinition` — `MakoFileViewProvider.createFile(HTMLLanguage)` delegates to `LanguageParserDefinitions.INSTANCE.forLanguage(HTMLLanguage)`. No changes to `MakoParserDefinition` are needed.

**Do this instead:** Leave `MakoParserDefinition.createFile()` unchanged. Handle both language cases in `MakoFileViewProvider.createFile(lang)`.

### Anti-Pattern 5: Registering templateDataLanguage in plugin.xml

**What people do:** Search for a plugin.xml extension point to declare HTML as the default template data language for `.mako` files.

**Why it's wrong:** No such extension point exists. `TemplateDataLanguagePatterns` is a user-editable application service persisted in `templateLanguages.xml` (IDE settings), not a plugin-declared extension. The plugin's default is established by the fallback in `getTemplateDataLanguage()`. The IDE's "Settings > Languages & Frameworks > Template Data Languages" UI lets users override this per-file/directory/project.

**Do this instead:** Return `HTMLLanguage.INSTANCE` as the fallback in `getTemplateDataLanguage()` when no user mapping is configured.

---

## Integration Points

### Platform Integration

| Integration Point | How It Works | Notes |
|------------------|-------------|-------|
| `lang.fileViewProviderFactory` | `MakoFileViewProviderFactory` registered in plugin.xml. Platform calls it whenever a `.mako` file is opened. | One registration, no language attribute change needed. |
| `TemplateDataLanguageMappings` | Platform service — no registration. `MakoFileViewProvider` reads it at construction time. | Provides Settings UI for user to change template data language per-file. |
| HTML PSI features | Platform dispatches HTML completion, tag error detection, Emmet etc. to the HTML PSI tree. | No explicit registration. Works automatically once `MakoFileViewProvider.getPsi(HTMLLanguage)` returns a valid `HtmlFile`. |
| `TemplateDataElementType.parseContents()` | Called by the platform when it needs the HTML PSI tree (lazy evaluation of the chameleon node). Uses `MakoParserDefinition.createLexer()` to tokenize. | Only `TEMPLATE_TEXT` tokens are passed to the HTML parser. All other Mako tokens become `OuterLanguageElement` placeholders. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|--------------|-------|
| `MakoFileViewProvider` to `MakoParserDefinition` | `MakoFileViewProvider.createFile(MakoLanguage)` calls `LanguageParserDefinitions.INSTANCE.forLanguage(MakoLanguage).createFile(this)` — standard platform dispatch | `MakoParserDefinition` unchanged |
| `MakoFileViewProvider` to `HTMLLanguage` parser | `MakoFileViewProvider.createFile(HTMLLanguage)` calls `LanguageParserDefinitions.INSTANCE.forLanguage(HTMLLanguage).createFile(this)` — delegates to HTML plugin | HTML plugin must be available; it is bundled in all JetBrains IDEs |
| `TemplateDataElementType` to `MakoLexerAdapter` | `createBaseLexer(viewProvider)` creates a fresh `MakoLexerAdapter` for re-tokenizing the document | Uses `viewProvider.baseLanguage` to look up `MakoParserDefinition` |
| `MakoPythonInjector` to Mako PSI | Operates on `MakoExpression`, `MakoCodeBlock`, `MakoModuleBlock` from the Mako PSI tree | Completely independent from HTML PSI tree |
| `MakoAnnotator` to Mako PSI | Fires only for `language="Mako Template"` elements | `element.containingFile.language != MakoLanguage` guard remains correct |

---

## Build Order for v0.3.0 Implementation

The implementation has clear dependencies — steps must be done in this order:

### Step 1: Add OUTER_ELEMENT_TYPE to MakoTokenTypes

**File:** `src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt`

Add:
```kotlin
@JvmField val OUTER_ELEMENT_TYPE = IElementType("MAKO_OUTER_ELEMENT", MakoLanguage)
```

No other files change in this step. This is a pure additive change.

### Step 2: Create MakoFileViewProvider

**File:** `src/main/kotlin/com/schtilig/mako/lang/MakoFileViewProvider.kt`

Implement `MultiplePsiFilesPerDocumentFileViewProvider` + `TemplateLanguageFileViewProvider`. Key implementation decisions:

- Constructor takes `(PsiManager, VirtualFile, Boolean /*physical*/, Language /*baseLanguage*/, Language /*templateDataLanguage*/)`.
- `getBaseLanguage()` returns `MakoLanguage`.
- `getTemplateDataLanguage()` returns the stored template data language.
- `createFile(lang)`: for Mako, delegates to `MakoParserDefinition.createFile(this)`; for HTML, delegates to HTML ParserDefinition and sets `contentElementType` to `getTemplateDataElementType(lang)`.
- `getContentElementType(lang)`: returns `getTemplateDataElementType(lang)` for the HTML language, `null` otherwise.
- `supportsIncrementalReparse(rootLanguage)`: returns `false`.
- `cloneInner(virtualFile)`: returns new `MakoFileViewProvider(manager, virtualFile, false, baseLanguage, templateDataLanguage)`.
- Static helper `resolveTemplateDataLanguage(manager, file)`: checks `TemplateDataLanguageMappings`, falls back to `HTMLLanguage.INSTANCE`.
- Companion object caches `TemplateDataElementType` instances per language ID.

### Step 3: Create MakoFileViewProviderFactory

**File:** `src/main/kotlin/com/schtilig/mako/lang/MakoFileViewProviderFactory.kt`

Implement `FileViewProviderFactory`:
```kotlin
class MakoFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        return MakoFileViewProvider(manager, file, eventSystemEnabled, MakoLanguage)
    }
}
```

### Step 4: Register in plugin.xml

**File:** `src/main/resources/META-INF/plugin.xml`

Add inside `<extensions defaultExtensionNs="com.intellij">`:
```xml
<!-- v0.3.0: HTML Language Injection via TemplateLanguageFileViewProvider -->
<lang.fileViewProviderFactory
    language="Mako Template"
    implementationClass="com.schtilig.mako.lang.MakoFileViewProviderFactory"/>
```

No other plugin.xml changes are required. The HTML language's extension points (completion, error detection, Emmet) are automatically dispatched to the HTML PSI tree once it is available.

### Step 5: Verify and Test

Verification order:
1. `./gradlew check` — confirm all existing tests still pass (Mako PSI tree unchanged).
2. `./gradlew runIde` — open a `.mako` file with HTML content, verify:
   - HTML tag completion fires in TEMPLATE_TEXT regions.
   - HTML error detection (unclosed tags) appears in TEMPLATE_TEXT regions.
   - Mako folding still works on `<%def>`, `<%block>`.
   - Python injection still works in `${...}` and `<% %>`.
   - Structure View still shows `<%def>` / `<%block>` items.
3. New tests: verify `viewProvider.getPsi(HTMLLanguage.INSTANCE)` returns a non-null `HtmlFile` for a `.mako` file.

---

## Coexistence: Python MultiHostInjector + HTML TemplateLanguageFileViewProvider

This is the central architectural concern. The two mechanisms are fully independent and do not conflict:

**Layer separation:**

| Mechanism | Operates On | PSI Tree | Scope |
|-----------|-------------|----------|-------|
| `MakoPythonInjector` (MultiHostInjector) | `MakoExpression`, `MakoCodeBlock`, `MakoModuleBlock` nodes | Mako PSI tree | Per-node; creates tiny injected file fragments |
| `MakoFileViewProvider` (TemplateLanguageFileViewProvider) | `TEMPLATE_TEXT` tokens across the whole file | Creates an HTML PSI tree for the full document | File-level; parallel PSI tree |

**Why they don't interfere:**

1. `MakoPythonInjector` targets specific PSI node types (`MakoExpression`, etc.) that exist only in the Mako PSI tree. The HTML PSI tree contains `OuterLanguageElement` placeholders for those regions — the Python injector never sees the HTML PSI tree.

2. The HTML PSI tree only sees `TEMPLATE_TEXT` tokens. Mako expressions (`${...}`) and code blocks (`<% %>`) are represented as `OuterLanguageElement` nodes in the HTML tree — opaque placeholders that HTML tooling ignores. No HTML completion or error detection fires inside `${...}` or `<% %>`.

3. `MultiHostRegistrar` (used by Python injector) creates an injected-language virtual file that is separate from both the Mako PSI tree and the HTML PSI tree. It registers the injection against the host node's document range — this range falls within Mako PSI nodes, not within TEMPLATE_TEXT tokens, so no overlap with the HTML PSI tree occurs.

4. Platform dispatch: when the caret is in a `${...}` region, the platform queries the Mako PSI tree first (base language). The Python injection is active there. When the caret is in a TEMPLATE_TEXT region, both the Mako PSI tree (for Mako-specific features) and the HTML PSI tree (for HTML-specific features) are active simultaneously.

---

## Scaling Considerations

This is an IDE plugin, not a user-facing application. Scaling here means performance with large files and many open files.

| Concern | Approach |
|---------|---------|
| Re-parsing on every keystroke | `supportsIncrementalReparse = false` forces full re-parse. Acceptable for typical Mako files (< 2000 lines). May become slow for very large templates. Mitigated by IntelliJ's background thread parsing. |
| Two PSI trees per file | Memory cost is proportional to file size. Both trees are cached by the platform. The HTML PSI tree is lazy (parsed on first access). Acceptable for the scale of Mako template files. |
| TemplateDataElementType caching | ConcurrentHashMap prevents duplicate type registration and avoids recreation cost. One instance per template data language ID (typically one: HTML). |

---

## Sources

- **Handlebars `HbFileViewProvider.java`** (verified live source): [github.com/JetBrains/intellij-plugins](https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/file/HbFileViewProvider.java) — canonical reference implementation for `TemplateLanguageFileViewProvider` + `TemplateDataElementType` + caching pattern
- **Handlebars `HbFileViewProviderFactory.java`** (verified live source): [github.com/JetBrains/intellij-plugins](https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/file/HbFileViewProviderFactory.java) — factory pattern
- **Handlebars `HbLanguage.java`** (verified live source): [github.com/JetBrains/intellij-plugins](https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/HbLanguage.java) — `getDefaultTemplateLang()` = `HtmlFileType.INSTANCE` pattern for default HTML
- **Handlebars `plugin.xml`** (verified live source): [github.com/JetBrains/intellij-plugins](https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/resources/META-INF/plugin.xml) — `lang.fileViewProviderFactory` registration
- **`TemplateDataElementType.java`** (verified live source): [github.com/JetBrains/intellij-community](https://github.com/JetBrains/intellij-community/blob/master/platform/analysis-impl/src/com/intellij/psi/templateLanguages/TemplateDataElementType.java) — constructor signature `(debugName, Language, templateElementType, outerElementType)`, `parseContents()` mechanics
- **`TemplateDataLanguageMappings.java`** (verified live source): [github.com/JetBrains/intellij-community](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/psi/templateLanguages/TemplateDataLanguageMappings.java) — `getMapping(file)` → `getDefaultMappingForFile()` → `TemplateDataLanguagePatterns` chain; confirms no plugin.xml extension point for default mapping
- **`TemplateDataLanguagePatterns.java`** (verified live source): [github.com/JetBrains/intellij-community](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/psi/templateLanguages/TemplateDataLanguagePatterns.java) — user-editable application service; confirms default is controlled by `getTemplateDataLanguage()` fallback in the FileViewProvider, not plugin.xml
- **`OuterLanguageElement.java`** (verified live source): [github.com/JetBrains/intellij-community](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/psi/templateLanguages/OuterLanguageElement.java) — marker interface; implementation is `OuterLanguageElementImpl`
- **IntelliJ Platform SDK — File View Providers**: [plugins.jetbrains.com/docs/intellij/file-view-providers.html](https://plugins.jetbrains.com/docs/intellij/file-view-providers.html) — confirms `lang.fileViewProviderFactory` extension point and `MultiplePsiFilesPerDocumentFileViewProvider`
- **Existing Mako plugin codebase**: `MakoLanguage.kt`, `MakoParserDefinition.kt`, `MakoFile.kt`, `MakoTokenTypes.kt`, `MakoPythonInjector.kt`, `plugin.xml` — verified for integration points and existing constraints

---

*Architecture research for: Mako plugin v0.3.0 HTML language injection*
*Researched: 2026-02-22*

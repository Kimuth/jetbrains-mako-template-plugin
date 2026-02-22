# Stack Research

**Domain:** JetBrains plugin — HTML language injection via TemplateLanguageFileViewProvider for Mako template language
**Researched:** 2026-02-22
**Confidence:** HIGH (APIs verified directly from PyCharm 2025.2.6 JARs via javap; extension points verified from LangExtensionPoints.xml extracted from app-client.jar; RST plugin from same SDK used as canonical reference implementation)

---

## Context: What Already Exists

This is a **subsequent milestone** research document. The existing plugin (`com.schtilig.mako` v0.2.0) already has:

- `MakoLanguage` extending `TemplateLanguage` (required precondition — already done)
- `MakoParserDefinition` with `createFile()` returning `MakoFile(viewProvider)` — must change to return HTML-aware PSI when language = HTML
- `MakoFileType` as a `LanguageFileType`
- `MakoPythonInjector` using `MultiHostInjector` for Python injection — preserved unchanged
- All other features (folding, structure view, completion, annotator) — preserved unchanged

The research question is specifically: what APIs enable HTML language injection into the TEMPLATE_TEXT regions?

---

## Recommended Stack

### Core APIs for HTML Injection (IntelliJ Platform 2025.2+ / build 252+)

All classes verified present in `pycharm-community-2025.2.6-win` Gradle cache JARs.

| Class / Interface | JAR | Purpose | Why This One |
|-------------------|-----|---------|--------------|
| `com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider` | `util-8.jar` | Interface your FileViewProvider must implement | Required contract for multi-PSI-tree files; platform dispatches HTML features to the HTML PSI tree when this interface is present |
| `com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider` | `app-client.jar` | Abstract base class for the FileViewProvider implementation | Manages the concurrent map of Language → PsiFileImpl; provides `getAllFiles()`, `cloneInner()` contract |
| `com.intellij.psi.templateLanguages.TemplateDataElementType` | `app-client.jar` | IFileElementType subclass that builds the HTML PSI sub-tree from TEMPLATE_TEXT tokens | Does the heavy lifting: takes the base Mako lexer, collects only `TEMPLATE_TEXT` tokens into a virtual HTML file, inserts `OuterLanguageElementImpl` placeholders where Mako directives appear |
| `com.intellij.psi.tree.OuterLanguageElementType` | `app-client.jar` | IElementType for "foreign" (Mako-directive) leaf nodes in the HTML PSI tree | Required as the 4th constructor argument to `TemplateDataElementType`; created once as a singleton |
| `com.intellij.psi.templateLanguages.TemplateDataLanguageMappings` | `app-client.jar` | Project service — per-file Language mapping (user can override via Settings) | `getInstance(project).getMapping(virtualFile)` returns user-configured language or null; fall back to `HTMLLanguage.INSTANCE` |
| `com.intellij.psi.FileViewProviderFactory` | `util-8.jar` | Factory interface registered in plugin.xml to create the custom FileViewProvider | Single method: `createFileViewProvider(file, language, psiManager, eventSystemEnabled)` |
| `com.intellij.lang.html.HTMLLanguage` | `app-client.jar` | The HTML Language singleton | `HTMLLanguage.INSTANCE` — used as default template data language and in `getLanguages()` set |
| `com.intellij.ide.highlighter.HtmlFileType` | `app-client.jar` | File type used as fake file type when building the HTML PSI sub-tree | Used internally by `TemplateDataElementType`; no direct use needed |

### Extension Points to Register in plugin.xml

| Extension Point | Bean Class | Attribute | Value | Why |
|-----------------|------------|-----------|-------|-----|
| `com.intellij.lang.fileViewProviderFactory` | `com.intellij.lang.LanguageExtensionPoint` | `language` | `"Mako Template"` | Tells the platform to use `MakoFileViewProvider` instead of the default single-PSI FileViewProvider for Mako files |

**Critical note on EP name:** There are two EP names that sound similar:
- `lang.fileViewProviderFactory` — keyed by `language`, uses `LanguageExtensionPoint` — **this is the correct one for Mako**
- `fileType.fileViewProviderFactory` — keyed by file type name, uses `FileTypeExtensionPoint` — used by generic file type providers, not needed here

The RST plugin bundled in PyCharm 2025.2.6 confirms `lang.fileViewProviderFactory` registration (verified in `META-INF/plugin.xml` of `restructuredtext.jar`).

### Method Signatures (Verified from bytecode)

**`TemplateLanguageFileViewProvider` interface** (from `util-8.jar`):
```kotlin
interface TemplateLanguageFileViewProvider : FileViewProvider {
    fun getBaseLanguage(): Language          // returns MakoLanguage
    fun getTemplateDataLanguage(): Language  // returns HTML (or user-mapped language)
    fun getContentElementType(language: Language): IElementType?  // default impl provided
}
```

**`MultiplePsiFilesPerDocumentFileViewProvider` abstract class** (from `app-client.jar`):
```kotlin
abstract class MultiplePsiFilesPerDocumentFileViewProvider(
    manager: PsiManager, file: VirtualFile, eventSystemEnabled: Boolean
) : AbstractFileViewProvider() {
    abstract fun getBaseLanguage(): Language
    abstract fun cloneInner(fileCopy: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider
    // Must implement: createFile(language) to return PSI for each language
}
```

**`TemplateDataElementType` constructor** (from `app-client.jar`):
```java
public TemplateDataElementType(
    String debugName,          // e.g., "MAKO_TEMPLATE_DATA"
    Language language,         // HTMLLanguage.INSTANCE — the data language
    IElementType templateElementType,  // MakoTokenTypes.TEMPLATE_TEXT — tokens that ARE template data
    IElementType outerElementType      // OuterLanguageElementType instance — placeholder for Mako directives
)
```

**`FileViewProviderFactory` interface** (from `util-8.jar`):
```java
interface FileViewProviderFactory {
    FileViewProvider createFileViewProvider(
        VirtualFile file,
        Language language,
        PsiManager manager,
        boolean eventSystemEnabled
    );
}
```

---

## Architecture: What to Build

Three new classes are needed. Nothing else changes.

### Class 1: `MakoHtmlTemplateDataElementType` (optional subclass)

OR simply a top-level constant. The simplest approach is a singleton val:

```kotlin
// In MakoFileViewProvider.kt or MakoTokenTypes.kt companion object
val OUTER_ELEMENT_TYPE = OuterLanguageElementType("MAKO_OUTER_ELEMENT", MakoLanguage)

val MAKO_TEMPLATE_DATA = TemplateDataElementType(
    "MAKO_TEMPLATE_DATA",
    HTMLLanguage.INSTANCE,
    MakoTokenTypes.TEMPLATE_TEXT,    // these tokens become the HTML sub-tree content
    OUTER_ELEMENT_TYPE                // Mako directive tokens become placeholders in HTML tree
)
```

The `createBaseLexer` default implementation in `TemplateDataElementType` calls `getBaseLanguage()` on the view provider and creates a new `MakoLexerAdapter` via `ParserDefinition.createLexer()`. This is correct for Mako — the same lexer that produces `TEMPLATE_TEXT` tokens is used to split the file.

### Class 2: `MakoFileViewProvider`

```kotlin
class MakoFileViewProvider(
    manager: PsiManager,
    virtualFile: VirtualFile,
    eventSystemEnabled: Boolean,
    private val myTemplateDataLanguage: Language = HTMLLanguage.INSTANCE
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, virtualFile, eventSystemEnabled),
    TemplateLanguageFileViewProvider {

    override fun getBaseLanguage(): Language = MakoLanguage

    override fun getTemplateDataLanguage(): Language = myTemplateDataLanguage

    override fun getLanguages(): Set<Language> = setOf(MakoLanguage, myTemplateDataLanguage)

    override fun createFile(lang: Language): PsiFile? = when (lang) {
        MakoLanguage -> {
            // Create MakoFile — same as MakoParserDefinition does today
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(MakoLanguage)
            def?.createFile(this)
        }
        myTemplateDataLanguage -> {
            // Create HTML PSI — delegate to HTML's ParserDefinition
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(myTemplateDataLanguage)
            (def?.createFile(this) as? PsiFileImpl)?.also {
                it.contentElementType = MAKO_TEMPLATE_DATA
            }
        }
        else -> null
    }

    override fun getContentElementType(language: Language): IElementType? =
        if (language == myTemplateDataLanguage) MAKO_TEMPLATE_DATA else null

    override fun cloneInner(fileCopy: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider =
        MakoFileViewProvider(manager, fileCopy, false, myTemplateDataLanguage)
}
```

**Key design decision on `getTemplateDataLanguage`:** The RST plugin (canonical bundled reference) hardcodes `PythonLanguage.getInstance()`. For Mako, hard-code `HTMLLanguage.INSTANCE` as the default. Optionally read `TemplateDataLanguageMappings.getInstance(manager.project).getMapping(virtualFile)` to let users override via Settings > Languages & Frameworks > Template Data Languages — this is the idiomatic platform pattern for configurable template data language mappings.

### Class 3: `MakoFileViewProviderFactory`

```kotlin
class MakoFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        // Honor user's per-file language mapping; default to HTML
        val templateDataLanguage =
            TemplateDataLanguageMappings.getInstance(manager.project).getMapping(file)
                ?: HTMLLanguage.INSTANCE
        return MakoFileViewProvider(manager, file, eventSystemEnabled, templateDataLanguage)
    }
}
```

### plugin.xml Change

Add one extension inside the existing `<extensions defaultExtensionNs="com.intellij">` block:

```xml
<!-- HTML language injection via TemplateLanguageFileViewProvider -->
<lang.fileViewProviderFactory
    language="Mako Template"
    implementationClass="com.schtilig.mako.lang.MakoFileViewProviderFactory"/>
```

No other plugin.xml changes are needed. The existing `com.intellij.modules.platform` dependency already provides `HTMLLanguage` and `HtmlFileType`. No additional module dependency is required.

---

## What NOT to Change

| Component | Status | Why Leave Alone |
|-----------|--------|-----------------|
| `MakoLanguage` extending `TemplateLanguage` | Already correct | Platform detects TemplateLanguage for template data language mapping UI |
| `MakoParserDefinition` | No changes needed | `createFile()` is called by `MakoFileViewProvider.createFile(MakoLanguage)` — no direct invocation change |
| `MakoFile` constructor | No changes needed | `MakoFile(viewProvider)` still receives the `MakoFileViewProvider` — PsiFileBase handles multi-provider correctly |
| `MakoPythonInjector` | No changes needed | Python injection via `MultiHostInjector` is orthogonal to the template language mechanism; both coexist |
| All other features (folding, structure view, annotator, completion) | No changes needed | These operate on the Mako PSI tree (base language); the HTML PSI tree is separate |
| `MakoTokenTypes.TEMPLATE_TEXT` | No changes needed | This is the token passed as `templateElementType` to `TemplateDataElementType` |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `com.intellij.multiHostInjector` for HTML injection | MultiHostInjector injects language as a fragment into existing PSI hosts; for TEMPLATE_TEXT this would fight with the TemplateLanguageFileViewProvider mechanism | `TemplateLanguageFileViewProvider` + `TemplateDataElementType` (the platform's dedicated multi-PSI-tree mechanism) |
| `fileType.fileViewProviderFactory` EP | Keyed by file type name (string), not language; works for generic file types not registered as a Language | `lang.fileViewProviderFactory` (keyed by language ID) |
| Subclassing `TemplateDataElementType` | Not needed for basic HTML injection; the default `createBaseLexer` uses `MakoLexerAdapter` correctly | Use `TemplateDataElementType` directly as a singleton val |
| Hard-coding `HTMLLanguage` without `TemplateDataLanguageMappings` fallback | Prevents users from configuring the template data language (e.g., plain text, XML) via Settings | Call `TemplateDataLanguageMappings.getInstance(project).getMapping(file)` first, fall back to `HTMLLanguage.INSTANCE` |
| `ConfigurableTemplateLanguageFileViewProvider` | An empty marker interface extending `TemplateLanguageFileViewProvider` (verified: no additional methods); adds no functionality | Implement `TemplateLanguageFileViewProvider` directly |

---

## Alternatives Considered

| Recommended | Alternative | When Alternative Is Better |
|-------------|-------------|---------------------------|
| `TemplateLanguageFileViewProvider` + `TemplateDataElementType` | `MultiHostInjector` injecting HTML into TEMPLATE_TEXT PSI nodes | MultiHostInjector is appropriate when the HTML regions are small/scattered fragments, not whole-file structure. For Mako where TEMPLATE_TEXT IS the file's primary content, the FileViewProvider approach is the platform-intended design |
| Hardcode default to `HTMLLanguage.INSTANCE` | Use only `TemplateDataLanguageMappings` (no default) | If all Mako files are guaranteed to contain HTML; but Mako is used for text generation too, so a UI-configurable default is more correct |
| Single `MAKO_TEMPLATE_DATA` constant for `TemplateDataElementType` | Separate subclass per template data language | Only needed if different lexer logic is required per data language; not the case for Mako |

---

## Reference Implementation: RST Plugin (PyCharm 2025.2.6)

The `restructuredtext.jar` plugin bundled with PyCharm 2025.2.6 implements exactly this pattern for RST files embedding Python:

| RST Component | Mako Equivalent |
|---------------|-----------------|
| `RestFileViewProvider` | `MakoFileViewProvider` |
| `RestFileProviderFactory` | `MakoFileViewProviderFactory` |
| `RestPythonTemplateType` (extends `TemplateDataElementType`) | `MAKO_TEMPLATE_DATA` constant (no subclass needed) |
| `RestPythonElementTypes.PYTHON_BLOCK_DATA` | `MAKO_TEMPLATE_DATA` |
| Returns `PythonLanguage.getInstance()` from `getTemplateDataLanguage()` | Returns `HTMLLanguage.INSTANCE` |
| Registered as `lang.fileViewProviderFactory language="ReST"` | Register as `lang.fileViewProviderFactory language="Mako Template"` |

---

## Version Compatibility

| Component | Platform Version | Notes |
|-----------|-----------------|-------|
| `TemplateLanguageFileViewProvider` | Stable since IntelliJ Platform 2017+; present in build 252 | API unchanged — `getBaseLanguage()`, `getTemplateDataLanguage()`, `getContentElementType()` |
| `TemplateDataElementType` constructor `(String, Language, IElementType, IElementType)` | Stable since IntelliJ Platform 2017+; present in build 252 | Verified in `app-client.jar` from PyCharm 2025.2.6 Gradle download |
| `TemplateDataLanguageMappings` | Project service; present in build 252 | `getInstance(project)`, `getMapping(file)`, `getDefaultMapping(file)` verified |
| `HTMLLanguage.INSTANCE` | Always available in PyCharm (depends on `com.intellij.modules.platform` or bundled HTML plugin) | In `app-client.jar` — no additional module dependency needed |
| `lang.fileViewProviderFactory` EP | Present in `LangExtensionPoints.xml` and `PyCharmCorePlugin.xml` from PyCharm 2025.2.6 | Double-confirmed in two XML sources |
| `MultiplePsiFilesPerDocumentFileViewProvider` | Present in `app-client.jar` from PyCharm 2025.2.6 | Stable API; `cloneInner` and `createFile` signatures unchanged |

---

## Implications for Testing

The existing test infrastructure (`BasePlatformTestCase`) supports multi-PSI file tests. For HTML injection:

- HTML completion tests: `myFixture.configureByText(MakoFileType, "<div><caret>")` then `myFixture.completeBasic()` — should offer HTML tag completions
- Verify Mako PSI still works: existing parser tests should pass unchanged
- Verify Python injection still works: existing `MakoPythonInjectorTest` tests should pass unchanged
- The `MakoFileViewProvider` must be registered before `MakoParserDefinition` in the `IdeaTestFixture` — platform registration order matters in tests

---

## Sources

- PyCharm Community 2025.2.6 `app-client.jar` — `javap -p` on `TemplateLanguageFileViewProvider`, `TemplateDataElementType`, `TemplateDataLanguageMappings`, `MultiplePsiFilesPerDocumentFileViewProvider`, `OuterLanguageElementType`, `TemplateDataLanguagePatterns`, `ConfigurableTemplateLanguageFileViewProvider`, `HTMLLanguage`, `HtmlFileType` (HIGH confidence — primary source)
- PyCharm Community 2025.2.6 `util-8.jar` — `javap -p` on `TemplateLanguageFileViewProvider`, `FileViewProviderFactory`, `ITemplateDataElementType`, `OuterLanguageElement` (HIGH confidence — primary source)
- PyCharm Community 2025.2.6 `restructuredtext.jar` (bundled plugin) — `javap -p` on `RestFileViewProvider`, `RestFileProviderFactory`, `RestPythonTemplateType`, `RestPythonElementTypes`; `META-INF/plugin.xml` showing `lang.fileViewProviderFactory language="ReST"` registration (HIGH confidence — canonical reference implementation)
- `META-INF/LangExtensionPoints.xml` extracted from `app-client.jar` — `lang.fileViewProviderFactory` EP definition with `LanguageExtensionPoint` beanClass confirmed (HIGH confidence — authoritative EP list)
- `META-INF/PyCharmCorePlugin.xml` extracted from `app-client.jar` — dual confirmation of `lang.fileViewProviderFactory` EP registration and `TemplateDataLanguagePatterns` application service (HIGH confidence)
- JetBrains IntelliJ Platform community discussion: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206765105-Tutorial-Custom-templating-language-plugin (MEDIUM confidence — confirms architectural pattern)

---

*Stack research for: HTML language injection into Mako template plugin (JetBrains IntelliJ Platform 2025.2+)*
*Researched: 2026-02-22*

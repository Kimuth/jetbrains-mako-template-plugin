# Phase 18: FileViewProvider Scaffolding - Research

**Researched:** 2026-02-22
**Domain:** IntelliJ Platform `TemplateLanguageFileViewProvider` / `TemplateDataElementType` — HTML PSI tree injection for a custom template language plugin (PyCharm 2025.2+, build 252)
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| HINJ-01 | User sees HTML syntax coloring in template body regions of `.mako` files | Automatic once `TemplateLanguageFileViewProvider` creates the HTML PSI tree; HTML `SyntaxHighlighter` runs on that tree with zero additional code |
| HINJ-02 | User gets HTML tag and attribute completion in template body regions | Automatic — HTML `CompletionContributor` fires in HTML PSI context; requires `getLanguages()` to return both `MakoLanguage` and `HTMLLanguage` |
| HINJ-03 | User gets Emmet abbreviation expansion in template body regions | Automatic — Emmet reads the PSI language at caret; HTML PSI context activates it with no plugin-side code |
| HINJ-04 | User sees HTML error squiggles for malformed markup in template body | Automatic — HTML `Annotator` runs on HTML PSI tree; `OuterLanguageElement` boundaries keep Mako constructs opaque to the HTML parser |
| HINJ-05 | CSS completion and validation active inside `<style>` tags | Automatic — HTML plugin injects CSS into `<style>` tags internally once HTML PSI tree exists |
| HINJ-06 | JavaScript completion and validation active inside `<script>` tags | Automatic — HTML plugin injects JS via `HtmlScriptContentProvider` once HTML PSI tree exists |
</phase_requirements>

---

## Summary

Phase 18 adds a `TemplateLanguageFileViewProvider` to the existing Mako plugin, creating a parallel HTML PSI tree for every `.mako` file. The canonical IntelliJ Platform mechanism is well-established and verified against live PyCharm 2025.2.6 JARs, the Handlebars plugin source, and the bundled RST plugin.

The implementation requires three new Kotlin files (`MakoFileViewProvider.kt`, `MakoFileViewProviderFactory.kt`) and two additive changes (one new constant in `MakoTokenTypes.kt`, one `<lang.fileViewProviderFactory>` line in `plugin.xml`). All six HINJ requirements activate **automatically** once the HTML PSI tree exists — no additional HTML-specific code is required in this phase.

The primary risk is not the new code, but regression in existing features. Two existing files require defensive guards before the view provider is wired in: `MakoPythonInjector` needs its PSI root lookup changed to use `viewProvider.getPsi(MakoLanguage)`, and `MakoAnnotator` needs an `OuterLanguageElement` guard. `MakoFoldingBuilder` and `MakoStructureViewElement` do not require changes because they operate on PSI-level types that cannot appear in the HTML tree.

One prior-research open question is now resolved: `TemplateDataLanguagePatterns` is an application-level `PersistentStateComponent` — there is no plugin.xml extension point for pre-populating it. The default HTML mapping is established solely by the fallback return of `HTMLLanguage.INSTANCE` in `getTemplateDataLanguage()`. Users can override it in IDE Settings > Languages & Frameworks > Template Data Languages.

**Primary recommendation:** Implement `MakoFileViewProvider` + `MakoFileViewProviderFactory` + `OUTER_ELEMENT_TYPE` as a single plan; add defensive guards to `MakoPythonInjector` and `MakoAnnotator` in the same plan; register via `lang.fileViewProviderFactory` in plugin.xml. All HINJ requirements become active automatically once the HTML PSI tree is valid.

---

## Standard Stack

### Core

| Class / Interface | JAR | Purpose | Why Standard |
|-------------------|-----|---------|--------------|
| `com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider` | `util-8.jar` | Interface contract for multi-PSI-tree files | Required contract — platform dispatches HTML features to HTML PSI tree only when this interface is implemented |
| `com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider` | `app-client.jar` | Abstract base class; manages per-language PSI tree cache | Provides `getAllFiles()`, `cloneInner()` contract; all template language plugins use this base |
| `com.intellij.psi.templateLanguages.TemplateDataElementType` | `app-client.jar` | `IFileElementType` subclass that builds the HTML PSI tree from `TEMPLATE_TEXT` tokens | Does the heavy lifting: re-lexes file, collects `TEMPLATE_TEXT`, inserts `OuterLanguageElementImpl` placeholders for Mako constructs |
| `com.intellij.psi.tree.OuterLanguageElementType` | `app-client.jar` | `IElementType` for Mako-construct placeholder nodes in the HTML PSI tree | 4th constructor arg to `TemplateDataElementType`; created once as a singleton |
| `com.intellij.psi.templateLanguages.TemplateDataLanguageMappings` | `app-client.jar` | Project service; per-file language mapping from IDE Settings | `getInstance(project).getMapping(virtualFile)` returns user-configured language or null |
| `com.intellij.lang.html.HTMLLanguage` | `app-client.jar` | The HTML Language singleton | `HTMLLanguage.INSTANCE` — default template data language for `.mako` files |
| `com.intellij.psi.FileViewProviderFactory` | `util-8.jar` | Factory interface registered in plugin.xml | Single method: `createFileViewProvider(file, language, psiManager, eventSystemEnabled)` |

All classes verified via `javap -p` against PyCharm Community 2025.2.6 `app-client.jar` and `util-8.jar`.

### Extension Points to Register

| Extension Point | Bean Class | Key Attribute | Value | Source Verified |
|-----------------|------------|---------------|-------|-----------------|
| `lang.fileViewProviderFactory` | `com.intellij.lang.LanguageExtensionPoint` | `language` | `"Mako Template"` | `PyCharmCorePlugin.xml` from `app-client.jar` + RST plugin in `restructuredtext.jar` |

**Critical note:** `lang.fileViewProviderFactory` (language-keyed) is the correct EP. `fileType.fileViewProviderFactory` (file-type-keyed) is a different EP that does NOT trigger `TemplateLanguageFileViewProvider` behavior. Both are confirmed present in `LangExtensionPoints.xml` / `PyCharmCorePlugin.xml` from PyCharm 2025.2.6.

### Resolved: templateDataLanguagePatterns EP Does Not Exist for Plugin Use

The prior-research open question is now resolved by inspection of `PyCharmCorePlugin.xml` from the live JAR:

`TemplateDataLanguagePatterns` is registered as:
```xml
<applicationService serviceImplementation="com.intellij.psi.templateLanguages.TemplateDataLanguagePatterns" />
```

It is a **`PersistentStateComponent`** (user-editable application state) — verified by `javap`. Its `setAssocTable()` method is the only mutation path. There is **no plugin.xml extension point** for pre-populating default file-extension mappings. The default HTML mapping is established by the fallback in `getTemplateDataLanguage()` returning `HTMLLanguage.INSTANCE` when no user configuration exists. The PITFALLS.md reference to `<templateDataLanguagePatterns>` as a possible plugin.xml EP is incorrect — do not attempt to register it.

---

## Architecture Patterns

### Recommended Project Structure (Additions Only)

```
src/main/kotlin/com/schtilig/mako/
└── lang/
    ├── MakoFileViewProvider.kt         # NEW — implements MultiplePsiFilesPerDocumentFileViewProvider + TemplateLanguageFileViewProvider
    ├── MakoFileViewProviderFactory.kt  # NEW — implements FileViewProviderFactory
    ├── MakoTokenTypes.kt               # MODIFIED — add OUTER_ELEMENT_TYPE constant
    ├── injection/
    │   └── MakoPythonInjector.kt       # MODIFIED — defensive PSI root lookup
    └── annotation/
        └── MakoAnnotator.kt            # MODIFIED — add OuterLanguageElement guard
```

### Pattern 1: TemplateDataElementType Singleton via ConcurrentHashMap

**What:** `TemplateDataElementType` instances are cached in a `ConcurrentHashMap<String, TemplateDataElementType>` keyed by language ID. Never instantiated per-file.

**When to use:** Always — `TemplateDataElementType` extends `IFileElementType`; creating duplicate instances for the same language causes `AssertionError` (platform uses object identity for element type comparison).

**Example:**
```kotlin
// Source: HbFileViewProvider.java (JetBrains/intellij-plugins) + verified against app-client.jar
companion object {
    private val TEMPLATE_DATA_BY_LANG = ConcurrentHashMap<String, TemplateDataElementType>()

    fun getTemplateDataElementType(lang: Language): TemplateDataElementType {
        return TEMPLATE_DATA_BY_LANG.getOrPut(lang.id) {
            TemplateDataElementType(
                "MAKO_TEMPLATE_DATA",
                lang,
                MakoTokenTypes.TEMPLATE_TEXT,    // 3rd arg: the token that IS the HTML content
                MakoTokenTypes.OUTER_ELEMENT_TYPE // 4th arg: placeholder for Mako constructs in HTML tree
            )
        }
    }
}
```

### Pattern 2: OUTER_ELEMENT_TYPE as OuterLanguageElementType

**What:** Use `OuterLanguageElementType` (not plain `IElementType`) for the outer element type. `OuterLanguageElementType` implements `ILeafElementType` and returns an `OuterLanguageElementImpl` leaf node from `createLeafNode()`.

**Example:**
```kotlin
// Source: javap on OuterLanguageElementType.class from app-client.jar
// In MakoTokenTypes.kt
@JvmField val OUTER_ELEMENT_TYPE = OuterLanguageElementType("MAKO_OUTER_ELEMENT", MakoLanguage)
```

Import: `com.intellij.psi.tree.OuterLanguageElementType`

### Pattern 3: MakoFileViewProvider Class Structure

**What:** Implements both `MultiplePsiFilesPerDocumentFileViewProvider` and `TemplateLanguageFileViewProvider`. Stores the template data language passed from the factory.

**Example:**
```kotlin
// Source: adapted from HbFileViewProvider.java + RestFileViewProvider from restructuredtext.jar
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

    override fun getContentElementType(language: Language): IElementType? =
        if (language == myTemplateDataLanguage) getTemplateDataElementType(language) else null

    override fun createFile(lang: Language): PsiFile? = when {
        lang.isKindOf(MakoLanguage) ->
            LanguageParserDefinitions.INSTANCE.forLanguage(MakoLanguage)?.createFile(this)
        lang.isKindOf(myTemplateDataLanguage) -> {
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(myTemplateDataLanguage) ?: return null
            (def.createFile(this) as? PsiFileImpl)?.also { htmlFile ->
                // CRITICAL: set contentElementType immediately after createFile
                htmlFile.contentElementType = getTemplateDataElementType(myTemplateDataLanguage)
            }
        }
        else -> null
    }

    override fun supportsIncrementalReparse(rootLanguage: Language): Boolean = false

    override fun cloneInner(virtualFile: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider =
        MakoFileViewProvider(manager, virtualFile, false, myTemplateDataLanguage)

    companion object {
        // ... ConcurrentHashMap singleton cache (Pattern 1)
    }
}
```

### Pattern 4: MakoFileViewProviderFactory

**What:** Factory reads user-configured template data language from `TemplateDataLanguageMappings`; falls back to `HTMLLanguage.INSTANCE`.

**Example:**
```kotlin
// Source: HbFileViewProviderFactory.java (JetBrains/intellij-plugins)
class MakoFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        val project = manager.project  // null-safe: project may be null during early IDE init
        val templateDataLanguage = if (project != null) {
            TemplateDataLanguageMappings.getInstance(project).getMapping(file)
                ?: HTMLLanguage.INSTANCE
        } else {
            HTMLLanguage.INSTANCE
        }
        return MakoFileViewProvider(manager, file, eventSystemEnabled, templateDataLanguage)
    }
}
```

### Pattern 5: Defensive Guard in MakoPythonInjector

**What:** Change the PSI root lookup from `context.containingFile` to `context.containingFile.viewProvider.getPsi(MakoLanguage)` to guarantee the Mako PSI tree is used even in the dual-tree environment.

**Example:**
```kotlin
// Current (will break once TemplateLanguageFileViewProvider is active):
private fun collectCodeAndExpressionHosts(file: PsiFile): List<PsiLanguageInjectionHost> {
    val codeBlocks = PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)
    // ...

// Fixed (defensive — always searches the Mako PSI root):
private fun collectCodeAndExpressionHosts(context: PsiElement): List<PsiLanguageInjectionHost> {
    val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
    val codeBlocks = PsiTreeUtil.findChildrenOfType(makoFile, MakoCodeBlock::class.java)
    val expressions = PsiTreeUtil.findChildrenOfType(makoFile, MakoExpression::class.java)
    return (codeBlocks + expressions).sortedBy { it.textOffset }
}
```

Also update the call site in `getLanguagesToInject`: change `collectCodeAndExpressionHosts(context.containingFile ?: return)` to `collectCodeAndExpressionHosts(context)`.

### Pattern 6: OuterLanguageElement Guard in MakoAnnotator

**What:** Add early return if the element is an `OuterLanguageElement`. The Mako PSI tree now contains `OuterLanguageElementImpl` nodes at TEMPLATE_TEXT positions.

**Example:**
```kotlin
// Import: com.intellij.psi.templateLanguages.OuterLanguageElement
override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is OuterLanguageElement) return  // ADD THIS — Mako tree now contains these
    if (element.containingFile.language != MakoLanguage) return
    // ... existing logic unchanged
}
```

### Pattern 7: plugin.xml Registration

**What:** One line addition to register the factory.

**Example:**
```xml
<!-- v0.3.0: HTML Language Injection via TemplateLanguageFileViewProvider -->
<lang.fileViewProviderFactory
    language="Mako Template"
    implementationClass="com.schtilig.mako.lang.MakoFileViewProviderFactory"/>
```

`language="Mako Template"` must exactly match `MakoLanguage.getID()` — which is `"Mako Template"` (set in `MakoLanguage.kt`: `Language("Mako Template")`).

### Anti-Patterns to Avoid

- **Wrong extension point key:** Using `fileType.fileViewProviderFactory` instead of `lang.fileViewProviderFactory` causes silent failure — no HTML PSI tree created, no error shown. The language-keyed EP is `lang.fileViewProviderFactory`.
- **TemplateDataElementType instantiated in createFile():** Creates a new `IElementType` instance per file open; `contentElementType` comparison fails; HTML tree is never correctly built. Must be a singleton cached in `ConcurrentHashMap`.
- **TEMPLATE_TEXT as the 3rd argument with wrong semantics:** The 3rd arg to `TemplateDataElementType` constructor (`templateElementType`) is the Mako-syntax token to EXCLUDE from the HTML tree; the 4th arg (`outerElementType`) is the placeholder type for the HTML PSI tree. For Mako: `TEMPLATE_TEXT` is the HTML content token — pass it as the 3rd arg. Pass `OUTER_ELEMENT_TYPE` as the 4th arg. (These semantics match `HbFileViewProvider.java`'s usage.)
- **Skipping contentElementType assignment:** If `(htmlFile as PsiFileImpl).contentElementType = ...` is omitted in `createFile()`, the HTML parser processes the raw Mako bytes including `<%def`, `%for` etc. — produces a deeply broken HTML PSI tree with false completions and errors everywhere.
- **Registering templateDataLanguagePatterns in plugin.xml:** This extension point does not exist for plugin-side registration (confirmed from live JAR inspection). `TemplateDataLanguagePatterns` is an application-level service, not a plugin extension point.
- **Using `context.containingFile` directly in MakoPythonInjector:** Must change to `viewProvider.getPsi(MakoLanguage)` before the FileViewProvider is activated — otherwise Python injection fires zero hosts when the dual-tree is active.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTML PSI tree from TEMPLATE_TEXT tokens | Custom token-to-HTML parser | `TemplateDataElementType` | Handles `OuterLanguageElement` insertion, incremental parsing flags, HTML file type fake file creation — 2000+ lines of battle-tested platform code |
| Per-file language dispatch | Custom `FileViewProvider` from scratch | `MultiplePsiFilesPerDocumentFileViewProvider` abstract base | Manages thread-safe per-language PSI file cache, `getAllFiles()`, `cloneInner()` contract |
| User-configurable template data language | Custom settings panel | `TemplateDataLanguageMappings` | Platform-standard "Settings > Languages & Frameworks > Template Data Languages" UI; already exists, zero code required |
| HTML tag/attribute completion | Custom completion contributor | HTML plugin's built-in `CompletionContributor` | Fires automatically in HTML PSI context; implementing this manually would duplicate ~10k lines of HTML plugin code |
| Emmet in template files | Custom Emmet handler | Platform Emmet integration | Reads PSI language at caret; fires automatically in HTML PSI context |
| CSS/JS inside `<style>`/`<script>` tags | Custom CSS/JS injection | HTML plugin's `HtmlScriptContentProvider` | Handles this internally within the HTML PSI tree once it exists |

**Key insight:** Once `TemplateLanguageFileViewProvider` creates a valid HTML PSI tree, the HTML plugin's full feature set (completion, error detection, Emmet, CSS/JS injection, folding, live templates, breadcrumbs) activates automatically. Phase 18 is a "build the foundation, get everything for free" phase.

---

## Common Pitfalls

### Pitfall 1: Silent Factory Registration Failure

**What goes wrong:** Factory is registered under `fileType.fileViewProviderFactory` instead of `lang.fileViewProviderFactory`. Factory is silently ignored. No HTML PSI tree. No error message.

**Why it happens:** Both EP names contain "fileViewProviderFactory". Platform SDK docs sometimes use the short form without the prefix.

**How to avoid:** Register exactly as `lang.fileViewProviderFactory` with `language="Mako Template"`. Verify immediately with: `(file.viewProvider as? TemplateLanguageFileViewProvider) != null` in a test.

**Warning signs:** `file.viewProvider.allFiles.size == 1` after registration; no HTML PSI tree in PSI Viewer.

### Pitfall 2: contentElementType Not Set on HTML PsiFile

**What goes wrong:** `createFile(HTMLLanguage)` creates the HTML file via HTML's `ParserDefinition` but never calls `(htmlFile as PsiFileImpl).contentElementType = MAKO_TEMPLATE_DATA_TYPE`. The HTML parser receives the raw Mako bytes. `<%def`, `%for`, `${...}` appear as broken HTML tokens.

**How to avoid:** Always set `contentElementType` immediately after `def.createFile(this)` in the HTML branch of `createFile()`.

**Warning signs:** PSI Viewer shows `<%def` or `%for` inside HTML tree as raw text tokens; HTML tag completion suggests `<%` as a tag name.

### Pitfall 3: TemplateDataElementType Not a Singleton

**What goes wrong:** New `TemplateDataElementType` instance per `createFile()` call. Platform element type identity check fails. HTML tree is never correctly populated.

**How to avoid:** `ConcurrentHashMap<String, TemplateDataElementType>` in companion object. See Pattern 1.

**Warning signs:** HTML tree is empty or contains only whitespace; multiple `TemplateDataElementType` instances in memory profiler.

### Pitfall 4: Python Injector Receives HTML PsiFile Root

**What goes wrong:** `context.containingFile` in `MakoPythonInjector.getLanguagesToInject` returns the HTML `PsiFile` instead of the Mako `PsiFile`. `PsiTreeUtil.findChildrenOfType(htmlFile, MakoCodeBlock::class.java)` returns empty. Zero Python injection hosts. All `${...}` expressions turn red.

**How to avoid:** Change `collectCodeAndExpressionHosts` to use `context.containingFile.viewProvider.getPsi(MakoLanguage)` as the search root. Do this BEFORE activating the FileViewProvider.

**Warning signs:** Python injection stops working after FileViewProvider registration; `${name}` shows "Unresolved reference" for all names.

### Pitfall 5: getTemplateDataLanguage NPE During Early Startup

**What goes wrong:** `TemplateDataLanguageMappings.getInstance(project)` throws `NullPointerException` when `project` is null during early IDE startup or before project is fully loaded.

**How to avoid:** Guard with `val project = manager.project ?: return HTMLLanguage.INSTANCE` before calling `TemplateDataLanguageMappings`.

**Warning signs:** NPE in IDE log during project open; `.mako` files fail to open without language support.

### Pitfall 6: MakoFoldingBuilder — Language.ANY Check Is Sufficient (No Change Needed)

**What this means:** The `MakoFoldingBuilder.collectControlLineNodes()` method already has a guard: `val isDummy = node.psi.language == Language.ANY`. After adding `TemplateLanguageFileViewProvider`, `OuterLanguageElementImpl` nodes have `language == Language.ANY` — so they ARE treated as dummy blocks and recursed into. This is the correct behavior: control line nodes can appear inside template text regions that are now represented as outer language elements. The existing `Language.ANY` check handles this correctly without modification.

**Confirmed safe:** `MakoFoldingBuilder` does not need changes for Phase 18.

### Pitfall 7: MakoAnnotator — OuterLanguageElement Check

**What goes wrong:** The Mako PSI tree now contains `OuterLanguageElementImpl` nodes at TEMPLATE_TEXT positions. `MakoAnnotator.annotate()` currently has a `containingFile.language != MakoLanguage` guard — this is a file-level check, not an element-level check. When `annotate()` is called with an `OuterLanguageElement` element (which has `language == Language.ANY`), the guard passes (the containing file IS a Mako file), and the element may be cast to `MakoTemplateTextContent` in `checkForInvalidDirective` — which will throw `ClassCastException`.

**How to avoid:** Add `if (element is OuterLanguageElement) return` as the first line of `annotate()`. Import `com.intellij.psi.templateLanguages.OuterLanguageElement`.

**Warning signs:** `ClassCastException: OuterLanguageElementImpl cannot be cast to MakoTemplateTextContent` in IDE log after FileViewProvider activation.

---

## Code Examples

### Complete MakoFileViewProvider

```kotlin
// Source: adapted from HbFileViewProvider.java (JetBrains/intellij-plugins) and
//         RestFileViewProvider (restructuredtext.jar bundled in PyCharm 2025.2.6)
package com.schtilig.mako.lang

import com.schtilig.mako.MakoLanguage
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.tree.IElementType
import java.util.concurrent.ConcurrentHashMap

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

    override fun getContentElementType(language: Language): IElementType? =
        if (language == myTemplateDataLanguage) getTemplateDataElementType(language) else null

    override fun createFile(lang: Language): PsiFile? = when {
        lang.isKindOf(MakoLanguage) ->
            LanguageParserDefinitions.INSTANCE.forLanguage(MakoLanguage)?.createFile(this)
        lang.isKindOf(myTemplateDataLanguage) -> {
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(myTemplateDataLanguage) ?: return null
            (def.createFile(this) as? PsiFileImpl)?.also { htmlFile ->
                htmlFile.contentElementType = getTemplateDataElementType(myTemplateDataLanguage)
            }
        }
        else -> null
    }

    override fun supportsIncrementalReparse(rootLanguage: Language): Boolean = false

    override fun cloneInner(virtualFile: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider =
        MakoFileViewProvider(manager, virtualFile, false, myTemplateDataLanguage)

    companion object {
        private val TEMPLATE_DATA_BY_LANG = ConcurrentHashMap<String, TemplateDataElementType>()

        fun getTemplateDataElementType(lang: Language): TemplateDataElementType {
            return TEMPLATE_DATA_BY_LANG.getOrPut(lang.id) {
                TemplateDataElementType(
                    "MAKO_TEMPLATE_DATA",
                    lang,
                    MakoTokenTypes.TEMPLATE_TEXT,       // 3rd: HTML content token (from Mako perspective)
                    MakoTokenTypes.OUTER_ELEMENT_TYPE   // 4th: placeholder type for Mako constructs in HTML tree
                )
            }
        }
    }
}
```

### Complete MakoFileViewProviderFactory

```kotlin
// Source: adapted from HbFileViewProviderFactory.java (JetBrains/intellij-plugins)
package com.schtilig.mako.lang

import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings

class MakoFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        val project = manager.project
        val templateDataLanguage = if (project != null) {
            TemplateDataLanguageMappings.getInstance(project).getMapping(file)
                ?: HTMLLanguage.INSTANCE
        } else {
            HTMLLanguage.INSTANCE
        }
        return MakoFileViewProvider(manager, file, eventSystemEnabled, templateDataLanguage)
    }
}
```

### OUTER_ELEMENT_TYPE Addition to MakoTokenTypes.kt

```kotlin
// Add to the end of MakoTokenTypes object — import: com.intellij.psi.tree.OuterLanguageElementType
@JvmField val OUTER_ELEMENT_TYPE = OuterLanguageElementType("MAKO_OUTER_ELEMENT", MakoLanguage)
```

### plugin.xml Addition

```xml
<!-- v0.3.0: HTML Language Injection via TemplateLanguageFileViewProvider -->
<lang.fileViewProviderFactory
    language="Mako Template"
    implementationClass="com.schtilig.mako.lang.MakoFileViewProviderFactory"/>
```

### MakoPythonInjector Fix (collectCodeAndExpressionHosts)

```kotlin
// Before (breaks in dual-tree environment):
private fun collectCodeAndExpressionHosts(file: PsiFile): List<PsiLanguageInjectionHost> {
    val codeBlocks = PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)
    val expressions = PsiTreeUtil.findChildrenOfType(file, MakoExpression::class.java)
    return (codeBlocks + expressions).sortedBy { it.textOffset }
}

// After (defensive — always searches Mako PSI root):
private fun collectCodeAndExpressionHosts(context: PsiElement): List<PsiLanguageInjectionHost> {
    val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
    val codeBlocks = PsiTreeUtil.findChildrenOfType(makoFile, MakoCodeBlock::class.java)
    val expressions = PsiTreeUtil.findChildrenOfType(makoFile, MakoExpression::class.java)
    return (codeBlocks + expressions).sortedBy { it.textOffset }
}
```

Update the two call sites in `getLanguagesToInject`:
- Change `collectCodeAndExpressionHosts(context.containingFile ?: return)` to `collectCodeAndExpressionHosts(context)`

### MakoAnnotator Guard

```kotlin
// Add as the FIRST line inside annotate(), before all other checks:
override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is OuterLanguageElement) return   // ADD: Mako PSI now contains these at TEMPLATE_TEXT positions
    if (element.containingFile.language != MakoLanguage) return
    // ... rest unchanged
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `MultiHostInjector` for HTML injection into template files | `TemplateLanguageFileViewProvider` + `TemplateDataElementType` | Introduced in IntelliJ Platform 2013+; universally adopted | File-level HTML PSI tree vs. fragmented injection sites; enables tag matching across Mako expression boundaries |
| Manually setting template data language in IDE Settings per-project | `TemplateLanguageFileViewProvider` makes it available; hardcoded default in `getTemplateDataLanguage()` fallback | N/A | Users who install the plugin get HTML features without manual configuration |
| `fileType.fileViewProviderFactory` EP | `lang.fileViewProviderFactory` EP (language-keyed) | `lang.fileViewProviderFactory` confirmed in `PyCharmCorePlugin.xml` for PyCharm 2025.2 | Correct EP for template language plugins; `fileType.fileViewProviderFactory` is for non-language file type providers |

**Deprecated/outdated (from prior research, now confirmed):**
- `templateDataLanguagePatterns` as a plugin.xml extension point: Does not exist. `TemplateDataLanguagePatterns` is an application-level `PersistentStateComponent` only — confirmed by `PyCharmCorePlugin.xml` and `javap`. Remove from consideration.
- `ConfigurableTemplateLanguageFileViewProvider`: An empty marker interface extending `TemplateLanguageFileViewProvider` — no additional methods; adds no functionality. Implement `TemplateLanguageFileViewProvider` directly.

---

## Open Questions

1. **TemplateDataElementType argument 3 vs. 4 semantics (now resolved)**
   - What we know: The 3rd arg (`myTemplateElementType`) is described as "the token type that marks the Mako syntax tokens to process for the outer language" — i.e., the token whose ranges will become `OuterLanguageElement` nodes in the HTML tree. The 4th arg (`myOuterElementType`) is the `IElementType` to use for those `OuterLanguageElement` leaf nodes.
   - Field names from `javap`: `myTemplateElementType` (3rd) and `myOuterElementType` (4th).
   - Resolution: `TEMPLATE_TEXT` is the token that the HTML parser should see. It must be the `myTemplateElementType` (3rd arg). `OUTER_ELEMENT_TYPE` is the type used to create placeholder nodes in the HTML PSI tree where Mako syntax appears — it is the `myOuterElementType` (4th arg). This matches the ARCHITECTURE.md and STATE.md decisions.
   - The prior research notes that ARCHITECTURE.md describes `OUTER_ELEMENT_TYPE` as the "4th argument" and `TEMPLATE_TEXT` as the "3rd argument" — this is correct and consistent with the field names in `TemplateDataElementType`.

2. **MakoFoldingBuilder behavior with OuterLanguageElementImpl nodes (confirmed safe)**
   - What we know: `collectControlLineNodes()` guards with `val isDummy = node.psi.language == Language.ANY` to recurse into GrammarKit dummy blocks. `OuterLanguageElementImpl` nodes also have `language == Language.ANY`.
   - Consequence: The folding builder treats outer language elements as dummy blocks and recurses into them to find `CONTROL_LINE` tokens. This is harmless because `CONTROL_LINE` tokens do not appear inside `OuterLanguageElement` nodes (they are Mako-specific tokens that live in the Mako PSI tree, not in the HTML PSI tree).
   - No change needed to `MakoFoldingBuilder`.

3. **MakoCompletionContributor `language="any"` post-FileViewProvider (defer to Phase 19)**
   - What we know: `MakoCompletionContributor` is registered `language="any"` so it fires in TEMPLATE_TEXT positions (now in the HTML PSI tree). The internal `file.language.id` guard currently checks `context.containingFile.language.id != MakoLanguage.id` — unclear whether `containingFile` returns the HTML file or the Mako file for elements in TEMPLATE_TEXT regions.
   - Recommendation: Defer to Phase 19 (Regression Hardening). The guard may need to be changed to `parameters.position.containingFile.viewProvider.baseLanguage != MakoLanguage` if double-firing is observed.

---

## Build Order

All changes must be made in this order to avoid compilation failures:

1. **Add `OUTER_ELEMENT_TYPE` to `MakoTokenTypes.kt`** — pure additive; needed by Step 2
2. **Add defensive guard to `MakoAnnotator.kt`** — add `OuterLanguageElement` early return
3. **Fix `MakoPythonInjector.kt`** — change `collectCodeAndExpressionHosts` to use `viewProvider.getPsi(MakoLanguage)`
4. **Create `MakoFileViewProvider.kt`** — references `MakoTokenTypes.OUTER_ELEMENT_TYPE`
5. **Create `MakoFileViewProviderFactory.kt`** — references `MakoFileViewProvider`
6. **Register in `plugin.xml`** — add `lang.fileViewProviderFactory` extension
7. **Run `./gradlew check`** — verify all existing tests pass
8. **Run `./gradlew runIde`** — manual verification: HTML coloring, tag completion, Emmet in TEMPLATE_TEXT regions

---

## Verification Checklist

After implementation, confirm ALL of the following:

- `file.viewProvider.allFiles.size == 2` for any `.mako` file opened in a test
- PSI Viewer shows HTML PSI tree with `HtmlDocumentImpl`, `XmlTagImpl` nodes for TEMPLATE_TEXT regions
- PSI Viewer shows Mako PSI tree unchanged — `MakoDefTag`, `MakoBlockTag`, `MakoExpression` nodes present
- `${...}` expressions appear as `OuterLanguageElementImpl` in the HTML PSI tree
- `./gradlew check` passes 100% (all existing lexer, parser, folding, structure, completion, annotator, injection tests)
- `./gradlew runIde` — HTML tag completion fires in TEMPLATE_TEXT when typing `<di`
- `./gradlew runIde` — Emmet expansion fires when typing `div.container` + Tab in TEMPLATE_TEXT
- `./gradlew runIde` — Python injection still works in `${...}` regions
- `./gradlew runIde` — `<%def>` / `<%block>` folding still works
- `./gradlew runIde` — Structure View still shows `<%def>` / `<%block>` items

---

## Sources

### Primary (HIGH confidence)

- PyCharm Community 2025.2.6 `app-client.jar` — `javap -p` on `TemplateLanguageFileViewProvider`, `TemplateDataElementType`, `TemplateDataLanguageMappings`, `MultiplePsiFilesPerDocumentFileViewProvider`, `OuterLanguageElementType`, `TemplateDataLanguagePatterns`
- PyCharm Community 2025.2.6 `util-8.jar` — `javap -p` on `FileViewProviderFactory`
- `META-INF/LangExtensionPoints.xml` extracted from `app-client.jar` — `lang.fileViewProviderFactory` EP definition with `LanguageExtensionPoint` beanClass; `fileType.fileViewProviderFactory` EP with `FileTypeExtensionPoint` beanClass
- `META-INF/PyCharmCorePlugin.xml` extracted from `app-client.jar` — `lang.fileViewProviderFactory` EP; `TemplateDataLanguagePatterns` as `applicationService` (confirming it is NOT a plugin-extensible EP); `TemplateDataLanguagePusher`
- `HbFileViewProvider.java` (live source via GitHub): Canonical reference implementation — `ConcurrentHashMap` caching, `createFile` pattern, `cloneInner`, `supportsIncrementalReparse = false`
- `HbFileViewProviderFactory.java` (live source via GitHub): Factory pattern with `TemplateDataLanguageMappings` + `HTMLLanguage.INSTANCE` fallback
- Existing Mako plugin codebase — `MakoLanguage.kt`, `MakoParserDefinition.kt`, `MakoFile.kt`, `MakoTokenTypes.kt`, `MakoPythonInjector.kt`, `MakoAnnotator.kt`, `MakoFoldingBuilder.kt`, `plugin.xml`
- `.planning/research/ARCHITECTURE.md` — verified architecture patterns from prior research (2026-02-22)
- `.planning/research/STACK.md` — verified stack from prior research including RST plugin reference (2026-02-22)
- `.planning/research/PITFALLS.md` — detailed pitfall catalog from prior research (2026-02-22)
- `.planning/STATE.md` — four locked implementation decisions confirmed

### Secondary (MEDIUM confidence)

- JetBrains developer statement: "you'll get that for almost free" / "JavaScript gets embedded into the HTML tree automatically" (intellij-support.jetbrains.com) — confirms automatic CSS/JS and Emmet behavior
- Pebble IntelliJ Plugin (bjansen/pebble-intellij) — confirms Emmet activates automatically after Template Data Language = HTML
- JetBrains YouTrack PY-13775 — confirms current Mako plugin lacks `TemplateLanguageFileViewProvider`; Template Data Language setting has no effect on `.mako` files without it

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Standard stack (APIs + EP names) | HIGH | Verified via `javap` against live PyCharm 2025.2.6 JARs; EP names confirmed from extracted XML files |
| Architecture (class structure + build order) | HIGH | Cross-verified against Handlebars + RST plugin reference implementations; prior research ARCHITECTURE.md verified |
| Pitfalls (all 7) | HIGH | Root causes verified from live JAR inspection and source code analysis; `TemplateDataLanguagePatterns` EP question resolved definitively |
| Features unlocked (HTML coloring, completion, Emmet, CSS/JS) | MEDIUM | Automatic activation confirmed from JetBrains developer statements and comparable plugin docs; needs empirical confirmation in `runIde` |
| Argument 3 vs. 4 semantics of TemplateDataElementType | HIGH | Field names from `javap` (`myTemplateElementType`, `myOuterElementType`) disambiguate; consistent with prior STATE.md decisions |

**Research date:** 2026-02-22
**Valid until:** 2026-04-22 (platform API stability — template language APIs unchanged since 2017; PyCharm 2025.2 is the locked target)

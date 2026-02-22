# Pitfalls Research

**Domain:** JetBrains custom language plugin — adding HTML injection via TemplateLanguageFileViewProvider to an existing Mako TemplateLanguage plugin
**Researched:** 2026-02-22
**Confidence:** MEDIUM-HIGH (web-verified patterns from official JetBrains SDK docs, community forum discussions, and open-source reference implementations; HTML-specific verification against JetBrains plugin source and community posts)

> This document supersedes the 2026-02-19 PITFALLS.md for the v0.3.0 milestone.
> It focuses specifically on adding TemplateLanguageFileViewProvider + HTML injection to an
> existing plugin that already has a working TemplateLanguage subclass, JFlex lexer, GrammarKit
> parser, and MultiHostInjector-based Python injection. General plugin pitfalls from the earlier
> document are preserved where still relevant.

---

## Critical Pitfalls

### Pitfall 1: fileViewProviderFactory Registered Under Wrong Extension Point Key

**What goes wrong:**
The `fileViewProviderFactory` extension point is registered under `com.intellij.fileType.fileViewProviderFactory`, NOT `com.intellij.lang.fileViewProviderFactory`. Developers frequently confuse these because "file view provider" sounds like a language-scoped extension. If you register under the wrong key, the factory is silently ignored — the platform continues using `SingleRootFileViewProvider` and no HTML PSI tree is created. HTML completion and coloring simply don't appear, with no error or warning.

**Why it happens:**
IntelliJ Platform has both a `lang.fileViewProviderFactory` and a `fileType.fileViewProviderFactory` extension. The language-scoped variant is for a different purpose (LSP-based integrations). The file type variant is what template languages need. The platform SDK documentation uses the abbreviated form `fileViewProviderFactory` in examples without always specifying the full qualified extension point name.

**How to avoid:**
Register exactly as follows in `plugin.xml`:
```xml
<fileType.fileViewProviderFactory
    filetype="Mako Template"
    implementationClass="com.schtilig.mako.lang.MakoFileViewProviderFactory"/>
```
The `filetype` attribute value must exactly match `MakoFileType.getName()` — in this plugin, `"Mako Template"`. Case-sensitive string match; any mismatch means silent fallback to the default provider.

**Warning signs:**
- No HTML PSI tree visible in PSI Viewer for `.mako` files after registering the factory.
- `(file.viewProvider as? TemplateLanguageFileViewProvider)` cast returns null at runtime.
- `file.viewProvider.allFiles` has exactly one element instead of two (Mako + HTML).

**Phase to address:** Phase 1 of HTML injection (FileViewProvider scaffolding). The registration must be verified before building any higher-level functionality on top of it.

---

### Pitfall 2: createFile for HTML Language Returns Null — Missing contentElementType

**What goes wrong:**
`TemplateLanguageFileViewProvider.createFile(lang: Language)` must handle three cases: Mako (base), HTML (template data), and any other language returning `null`. For the HTML case, the method must set `contentElementType` on the returned `PsiFile` to a `TemplateDataElementType` instance. Forgetting to set `contentElementType` causes the HTML PSI tree to parse the entire Mako file as HTML without removing Mako-specific tokens — the HTML parser sees `<%def name="foo():">` and produces a deeply broken tree. HTML tag completion becomes wildly incorrect, and the HTML error annotator fires false positives everywhere.

**Why it happens:**
Developers model `createFile` as a simple language-switch:
```kotlin
// WRONG — missing contentElementType assignment
override fun createFile(lang: Language): PsiFile? {
    return when {
        lang.isKindOf(MakoLanguage) -> MakoParserDefinition().createFile(this)
        lang.isKindOf(HTMLLanguage.INSTANCE) ->
            LanguageParserDefinitions.INSTANCE.forLanguage(lang)?.createFile(this)
        else -> null
    }
}
```
The HTML file is created but the `TemplateDataElementType` that tells the platform "only parse TEMPLATE_TEXT tokens as HTML" is never installed.

**How to avoid:**
```kotlin
override fun createFile(lang: Language): PsiFile? {
    return when {
        lang.isKindOf(MakoLanguage) -> MakoParserDefinition().createFile(this)
        lang.isKindOf(HTMLLanguage.INSTANCE) -> {
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(lang) ?: return null
            val file = def.createFile(this)
            (file as? PsiFileImpl)?.contentElementType = MAKO_HTML_TEMPLATE_DATA_TYPE
            file
        }
        else -> null
    }
}
```
Where `MAKO_HTML_TEMPLATE_DATA_TYPE` is a singleton `TemplateDataElementType(...)` constructed with the Mako language, `MakoTokenTypes.TEMPLATE_TEXT` as the template element type, and an outer element type for the Mako-side OuterLanguageElement placeholder.

**Warning signs:**
- PSI Viewer shows HTML tree containing raw Mako tag text like `<%def` as HTML tokens.
- HTML tag completion suggests completing `<%def` as an HTML tag.
- `MakoAnnotator` now sees HTML PSI nodes it doesn't understand, throwing `ClassCastException`.

**Phase to address:** Phase 1 of HTML injection (FileViewProvider scaffolding). Must be correct before testing any HTML feature.

---

### Pitfall 3: TEMPLATE_TEXT Token Not Declared as the Outer Element Type — PSI Corruption

**What goes wrong:**
`TemplateDataElementType` takes two element type parameters: `templateElementType` (the Mako-language token type that represents the Mako syntax markers to be EXCLUDED from the HTML tree) and `outerElementType` (the token type representing the HTML content regions to be INCLUDED in the HTML tree). Swapping these, or using the wrong Mako token as `templateElementType`, causes one of two failures:
1. All Mako tokens appear in the HTML tree — HTML parser produces garbage.
2. All TEMPLATE_TEXT tokens are stripped from the Mako tree — folding and structure view see an empty file.

In this plugin, `TEMPLATE_TEXT` is the token for HTML content. It should be the `outerElementType` (content to be represented in the HTML tree) — NOT the `templateElementType` (Mako markers to be stripped out). Getting this backwards is a common mistake.

**Why it happens:**
The naming is confusing. Developers read "templateElementType" as "the element type for template content (HTML)" when it actually means "the element type for template *syntax* markers (Mako)." The outer element type is the HTML content, which becomes `OuterLanguageElementImpl` nodes in the Mako tree at those positions.

**How to avoid:**
Understand the semantics:
- `templateElementType` = the Mako-syntax tokens that must NOT appear in the HTML tree (e.g., `OPEN_TAG`, `CLOSE_TAG`, tag-specific tokens, expression delimiters). These mark where Mako syntax is.
- `outerElementType` = `TEMPLATE_TEXT` — the token that represents raw HTML content. These become `OuterLanguageElementImpl` nodes in the Mako PSI tree, replaced by real HTML PSI nodes in the HTML tree.

In practice, pass `MakoTokenTypes.TEMPLATE_TEXT` as the `outerElementType` argument:
```kotlin
val MAKO_HTML_TEMPLATE_DATA_TYPE = TemplateDataElementType(
    "MAKO_TEMPLATE_DATA",
    HTMLLanguage.INSTANCE,
    MakoTokenTypes.TEMPLATE_TEXT,  // this IS the outer/HTML content token
    MakoElementType("OUTER_LANGUAGE_ELEMENT")  // placeholder for OuterLanguageElement in Mako tree
)
```

**Warning signs:**
- `MakoFoldingBuilder.buildFoldRegions()` receives a tree with no `MakoDefTag` or `MakoBlockTag` nodes.
- `MakoStructureViewElement` returns no children.
- PSI Viewer shows `OuterLanguageElementImpl` nodes where Mako syntax tokens should be.

**Phase to address:** Phase 1 of HTML injection (FileViewProvider scaffolding). Verified with PSI Viewer before proceeding.

---

### Pitfall 4: Existing MakoFoldingBuilder and MakoStructureViewElement Break on OuterLanguageElement Nodes

**What goes wrong:**
After adding `TemplateLanguageFileViewProvider`, the Mako PSI tree still covers the entire file but now contains `OuterLanguageElementImpl` nodes at positions that used to be `TEMPLATE_TEXT` leaf nodes. Code that does `element.node.elementType == MakoTokenTypes.TEMPLATE_TEXT` will no longer match at those positions — it sees `OuterLanguageElementType` instead.

More critically: `MakoFoldingBuilder` already uses `walkAllNodes` with a visitor and `Language.ANY` detection. If `OuterLanguageElementImpl` nodes have `language == Language.ANY` (which they do by default), the existing `DUMMY_BLOCK` guard in the current code (`element.language == Language.ANY`) will treat ALL template text regions as dummy blocks and skip them — this is correct for folding purposes, but may cause subtle issues if that guard was only intended for GrammarKit internal nodes.

Additionally, `PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)` called from `MakoPythonInjector` operates on whichever PSI file is passed as the root. After adding `TemplateLanguageFileViewProvider`, calling `context.containingFile` in the injector may return the Mako `PsiFile` or the HTML `PsiFile` depending on which tree the `context` element came from. If it returns the HTML file, `PsiTreeUtil.findChildrenOfType(htmlFile, MakoCodeBlock::class.java)` returns empty — and the multi-host Python injection fires zero hosts.

**Why it happens:**
- `TemplateLanguageFileViewProvider` creates TWO separate `PsiFile` roots for the same underlying document — one rooted in MakoLanguage, one in HTMLLanguage.
- `PsiElement.containingFile` returns the root of whichever tree the element lives in.
- If `MakoPythonInjector.elementsToInjectIn()` returns `MakoExpression` and `MakoCodeBlock`, these elements only exist in the Mako PSI tree. The injector's `context.containingFile` should always be the Mako file. But if the wrong file root is used in `collectCodeAndExpressionHosts`, the collection will be empty.

**How to avoid:**
- In `MakoPythonInjector.collectCodeAndExpressionHosts`, explicitly get the Mako PSI file from the view provider, not from `context.containingFile` directly:
  ```kotlin
  val makoFile = context.containingFile.viewProvider.getPsi(MakoLanguage) ?: return
  ```
- In `MakoFoldingBuilder`, the existing `Language.ANY` guard is likely sufficient but must be verified with the PSI Viewer after adding `TemplateLanguageFileViewProvider` to confirm that `OuterLanguageElementImpl` nodes are correctly skipped.
- `MakoStructureViewElement` iterates `PsiTreeUtil.findChildrenOfType` — verify it is called on the Mako language PSI root, not the HTML root.

**Warning signs:**
- Python injection stops working after adding `TemplateLanguageFileViewProvider` — expressions turn red with "Unresolved reference" even for simple variables.
- `MakoFoldingBuilder` produces zero fold regions.
- `MakoStructureViewElement` shows empty structure view.
- IDE log shows `ProcessCanceledException` or NPE in `MakoPythonInjector` during indexing.

**Phase to address:** Phase 1 (FileViewProvider scaffolding) — must audit all PSI consumers before wiring up the view provider. Phase 2 (regression testing) — all existing tests must pass after Phase 1.

---

### Pitfall 5: getTemplateDataLanguage Returns PlainText Because the Mapping Is Not Configured

**What goes wrong:**
`ConfigurableTemplateLanguageFileViewProvider.getTemplateDataLanguage()` typically consults `TemplateDataLanguageMappings.getInstance(project).getMapping(virtualFile)`. If no mapping is configured (either by the user in Settings or by a programmatic default), this returns null or falls back to `PlainTextLanguage.INSTANCE`. An HTML PSI tree is never created, HTML completion never fires, and no error is shown — the plugin silently produces no HTML editing support.

**Why it happens:**
Developers implement `getTemplateDataLanguage()` as:
```kotlin
override fun getTemplateDataLanguage(): Language {
    return TemplateDataLanguageMappings.getInstance(project)
        .getMapping(virtualFile) ?: HTMLLanguage.INSTANCE
}
```
The fallback to `HTMLLanguage.INSTANCE` is correct, but the null path can still be reached if `project` is null during early initialization (e.g., when the view provider is created before the project is fully loaded). Without a null check on `project`, a NPE crashes the view provider creation and the file opens without any language support at all.

**How to avoid:**
```kotlin
override fun getTemplateDataLanguage(): Language {
    val project = manager.project ?: return HTMLLanguage.INSTANCE
    return TemplateDataLanguageMappings.getInstance(project)
        .getMapping(virtualFile) ?: HTMLLanguage.INSTANCE
}
```
Additionally, to give users the right default without requiring manual configuration, register a `templateDataLanguagePatterns` extension in `plugin.xml` that maps `.mako` files to HTML by default:
```xml
<templateDataLanguagePatterns>
    <pattern ext="mako" language="HTML"/>
    <pattern ext="mak" language="HTML"/>
</templateDataLanguagePatterns>
```
This extension point causes the IDE to pre-populate the mapping in Settings → Template Data Languages, so new users get HTML immediately without manual setup.

**Warning signs:**
- No HTML syntax highlighting or completion in `.mako` files after installing the plugin fresh.
- `Settings → Languages & Frameworks → Template Data Languages` shows Mako files with no language assigned.
- Users need to manually select HTML in the Template Data Languages settings before anything works.

**Phase to address:** Phase 1 (FileViewProvider scaffolding) + `plugin.xml` defaults registration.

---

### Pitfall 6: MakoFile (PsiFileBase) Incompatible With TemplateLanguageFileViewProvider

**What goes wrong:**
The existing `MakoFile` extends `PsiFileBase`. When `TemplateLanguageFileViewProvider` is active, the platform calls `createFile(MakoLanguage)` inside the view provider, which in turn calls `MakoParserDefinition.createFile(viewProvider)`. This construction path is unchanged and produces the correct `MakoFile` instance. The risk is that some code may call `PsiManager.findFile(virtualFile)` and expect a single `PsiFile`, while now there are two (Mako + HTML). Code that casts the result to `MakoFile` without checking the language will fail with a `ClassCastException` when it happens to get the HTML `PsiFile` back.

Additionally, `PsiFile.getViewProvider()` on the Mako `PsiFile` and the HTML `PsiFile` will both return the SAME `TemplateLanguageFileViewProvider` instance. Code that does `file.viewProvider.psi` (which returns `viewProvider.getPsi(viewProvider.baseLanguage)`) — i.e., the Mako file — may or may not behave as expected depending on whether the HTML or Mako file was the entry point.

**Why it happens:**
The pattern `PsiManager.findFile(virtualFile) as? MakoFile` worked before because there was only one PSI file per virtual file. With `TemplateLanguageFileViewProvider`, `PsiManager.findFile` returns the base-language PSI file (Mako), so this particular cast should still work. But anywhere code accesses `file.viewProvider.allFiles` and iterates, it may encounter the HTML file unexpectedly.

**How to avoid:**
- Never cast `PsiFile` to `MakoFile` directly without a language guard: `(file as? MakoFile) ?: return`
- When explicitly needing the Mako file, use: `viewProvider.getPsi(MakoLanguage)`
- When explicitly needing the HTML file, use: `viewProvider.getPsi(HTMLLanguage.INSTANCE)`
- Audit all existing usages of `context.containingFile` in annotators, completion contributors, and injectors to ensure they resolve to the expected language tree.

**Warning signs:**
- `ClassCastException: HtmlFileImpl cannot be cast to MakoFile` in IDE logs.
- `MakoAnnotator` receives `HtmlFileImpl` as the file parameter and throws.
- `MakoCompletionContributor` fires for HTML element positions it doesn't understand.

**Phase to address:** Phase 1 (audit before adding FileViewProvider) + Phase 2 (regression testing).

---

### Pitfall 7: MakoAnnotator Fires on Both Mako and HTML PSI Trees

**What goes wrong:**
`MakoAnnotator` is registered for `language="Mako Template"` in `plugin.xml`. With `TemplateLanguageFileViewProvider`, the platform runs annotators for each language tree in the file. The Mako annotator fires correctly for the Mako tree. But if any element in the Mako tree contains an `OuterLanguageElementImpl` placeholder, and the annotator's visitor descends into it, it may encounter HTML PSI fragments and attempt to cast them to Mako PSI types — throwing `ClassCastException` or producing false-positive annotations.

Conversely, the HTML annotator (built into the platform) fires for the HTML tree and may produce warnings about Mako syntax in `OuterLanguageElement` positions — e.g., reporting `<%def name="foo():">` as invalid HTML text content. These false-positive HTML errors are visible to users as red squiggles in Mako syntax.

**Why it happens:**
- The Mako annotator was written assuming a single-language PSI tree. `OuterLanguageElementImpl` nodes were not present before and have no corresponding Mako PSI interface.
- The HTML annotator validates its own tree and encounters the Mako markers as `OuterLanguageElementImpl` stubs, which it correctly ignores for syntax. But some HTML inspections may not respect `OuterLanguageElementImpl` boundaries.

**How to avoid:**
- In `MakoAnnotator.annotate()`, add an early guard: `if (element is OuterLanguageElement) return`
- The HTML false-positive errors: platform HTML annotation generally suppresses errors in `OuterLanguageElementImpl` regions. If it doesn't, implement a custom `HtmlUnknownTagInspectionSuppressor` or verify with the HTML plugin docs for the correct suppression mechanism.
- Test with `runPluginVerifier` against PyCharm 2025.2 to ensure the HTML plugin version bundled in that build correctly suppresses errors in template regions.

**Warning signs:**
- `ClassCastException` in `MakoAnnotator` after adding HTML injection.
- Red squiggles on Mako tags (`<%def`, `<%block`) that are reported as "invalid HTML element."
- `MakoAnnotator` called with `element.language != MakoLanguage` — add an assertion to detect this early.

**Phase to address:** Phase 1 (add guards before wiring up) + Phase 3 (HTML correctness verification).

---

### Pitfall 8: completion.contributor language="any" Causes Double-Firing in HTML Regions

**What goes wrong:**
`MakoCompletionContributor` is registered with `language="any"` — a deliberate v0.1.0 decision because TEMPLATE_TEXT tokens needed completion at `<%` positions in what was then the only language layer. After adding `TemplateLanguageFileViewProvider`, the HTML PSI tree is active for TEMPLATE_TEXT regions. When the user presses Ctrl+Space in a TEMPLATE_TEXT region, completion fires for both the HTML layer (HTML tag completion) and then `MakoCompletionContributor` fires again via the `language="any"` registration. The Mako contributor's internal language guard (`file.language.id` check) should prevent false completions — but the guard must be verified to correctly check the containing file language and not the element language.

The deeper risk: the HTML completion contributor itself is order-sensitive. Registering `language="any"` without explicit ordering may cause `MakoCompletionContributor` to execute BEFORE the HTML contributor, and if `MakoCompletionContributor` calls `result.stopHere()` somewhere (even implicitly via returning early), HTML completion is suppressed.

**Why it happens:**
The `language="any"` registration was the correct fix for v0.1.0 — the alternative `language="Mako Template"` was documented to prevent completion from firing for TEMPLATE_TEXT positions. With `TemplateLanguageFileViewProvider`, those positions now belong to the HTML tree, not the Mako tree. The original problem that required `language="any"` may no longer apply, or may need a different solution.

**How to avoid:**
After adding `TemplateLanguageFileViewProvider`, revisit whether `language="any"` is still necessary:
- If HTML completion fires correctly for TEMPLATE_TEXT regions via the HTML tree, then `language="Mako Template"` may be sufficient again for the Mako-specific completions (tag names after `<%`).
- If `language="any"` is retained, add the guard inside the contributor:
  ```kotlin
  if (parameters.position.containingFile.viewProvider.baseLanguage != MakoLanguage) return
  ```
  This ensures the contributor only fires when the base language of the file is Mako, not when the element happens to be in an HTML injection context inside some other file type.

**Warning signs:**
- HTML tag completion (`<div`, `<span`, etc.) stops working in `.mako` files after adding HTML injection.
- Double completion items appear — Mako items AND HTML items mixed.
- Completion popup appears but clicking HTML items inserts the wrong text.

**Phase to address:** Phase 3 (HTML feature verification) — requires comparing behavior before and after view provider addition.

---

### Pitfall 9: Python MultiHostInjector Receives HTML PsiFile Context

**What goes wrong:**
`MakoPythonInjector.getLanguagesToInject(registrar, context)` is invoked for each `context` element whose class is in `elementsToInjectIn()` — `MakoExpression`, `MakoCodeBlock`, `MakoModuleBlock`. These elements only exist in the Mako PSI tree. However, `collectCodeAndExpressionHosts(context.containingFile)` passes `context.containingFile` as the root for `PsiTreeUtil.findChildrenOfType`. If for any reason `context.containingFile` returns the HTML `PsiFile` (e.g., due to a view provider implementation bug or during index rebuilding), `findChildrenOfType` will return empty and the multi-host injection will fail silently — no Python is injected, all Python expressions turn red.

Additionally, the multi-host injection strategy (one `startInjecting/doneInjecting` call for ALL `MakoCodeBlock` and `MakoExpression` hosts in the file) depends on `hosts.firstOrNull() == context` as the trigger. If the file-level search returns elements in a different order when the view provider is active (e.g., because the Mako tree is rebuilt or elements are cached differently), the "first host" test may fail and injection never starts.

**Why it happens:**
`MultiplePsiFilesPerDocumentFileViewProvider` caches PSI trees per language. The element's `containingFile` should reliably return the Mako `PsiFile` for elements of Mako types. But during early IDE startup, file indexing, or after a document invalidation, the PSI tree may be in a partial state.

**How to avoid:**
Defensively obtain the Mako PSI root from the view provider:
```kotlin
private fun collectCodeAndExpressionHosts(context: PsiElement): List<PsiLanguageInjectionHost> {
    val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
    val codeBlocks = PsiTreeUtil.findChildrenOfType(makoFile, MakoCodeBlock::class.java)
    val expressions = PsiTreeUtil.findChildrenOfType(makoFile, MakoExpression::class.java)
    return (codeBlocks + expressions).sortedBy { it.textOffset }
}
```
This guarantees the search root is always the Mako-language PSI tree regardless of how `context` was obtained.

**Warning signs:**
- Python injection works initially but stops after a `File → Invalidate Caches / Restart`.
- `MakoPythonInjector` logs show `hosts.isEmpty()` via logger output (add logging to detect this during development).
- Python completion inside `${...}` stops showing project symbols.

**Phase to address:** Phase 1 (defensive coding in injector before adding view provider) — change the injector to use `viewProvider.getPsi(MakoLanguage)` defensively before any other HTML work begins.

---

### Pitfall 10: TemplateDataElementType Singleton Must Not Be Re-Instantiated Per File

**What goes wrong:**
`TemplateDataElementType` is an `IElementType` subclass. `IElementType` instances must be singletons — the platform uses object identity (not equality) to compare element types across the PSI tree. Creating a new `TemplateDataElementType` instance inside `createFile()` for each new file means each file gets a different `IElementType` object. The `contentElementType` set on the HTML `PsiFile` won't match the token type the platform is looking for in the Mako token stream — the HTML tree is never correctly built.

**Why it happens:**
Developers initialize `TemplateDataElementType` inside the `createFile` method:
```kotlin
// WRONG — creates new instance per file
override fun createFile(lang: Language): PsiFile? {
    val templateDataType = TemplateDataElementType("MAKO_HTML_TEMPLATE_DATA", ...)
    val file = def.createFile(this)
    (file as PsiFileImpl).contentElementType = templateDataType  // different object each call
    file
}
```

**How to avoid:**
Declare `TemplateDataElementType` as a companion object singleton, initialized once:
```kotlin
companion object {
    val MAKO_TEMPLATE_DATA_TYPE = TemplateDataElementType(
        "MAKO_TEMPLATE_DATA",
        HTMLLanguage.INSTANCE,
        MakoTokenTypes.TEMPLATE_TEXT,
        MakoElementType("OUTER_LANGUAGE_ELEMENT")
    )
}
```
Then reference `MAKO_TEMPLATE_DATA_TYPE` in `createFile`. The `IElementType` is registered globally on first instantiation and reused thereafter.

**Warning signs:**
- HTML tree appears empty or contains only whitespace nodes, even though TEMPLATE_TEXT spans exist.
- `TemplateDataElementType` class shows multiple instances in a memory profiler.
- HTML completion works on first file open but breaks after reloading the project.

**Phase to address:** Phase 1 (FileViewProvider scaffolding) — design the singleton before writing any per-file code.

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Skip `templateDataLanguagePatterns` registration | Fewer extension points to learn | Users must manually configure HTML in Settings → Template Data Languages on every new project; bad UX for new users | Never — it is a 3-line `plugin.xml` addition |
| Hardcode `HTMLLanguage.INSTANCE` instead of reading from `TemplateDataLanguageMappings` | Simpler implementation | Users cannot change the template data language (e.g., to Plain Text for testing); `ConfigurableTemplateLanguageFileViewProvider` pattern is the standard | Acceptable for MVP if configurability is added before public release |
| Reuse `TEMPLATE_TEXT` as both the outerElementType and the `IFileElementType` content type | Fewer types to define | `TemplateDataElementType` uses identity comparison; sharing types across roles silently misroutes token-type decisions | Never |
| Skip `OuterLanguageElement` guard in `MakoAnnotator` | No immediate visible bug in typical files | Crashes on files with complex interleaved Mako+HTML once `OuterLanguageElementImpl` nodes appear | Never — add the guard as part of adding the view provider |
| Test HTML injection only in `runIde` (manual testing) | Faster verification | Automated tests don't catch regressions; view provider bugs silently break HTML on every lexer or parser change | Acceptable as a temporary measure; automated tests must be added within same milestone |
| Pass `context.containingFile` directly to `PsiTreeUtil.findChildrenOfType` in the Python injector | Works in current codebase | Breaks when view provider is active; injector sees HTML file instead of Mako file, returns zero hosts | Never after adding view provider — fix before adding it |

---

## Integration Gotchas

Common mistakes when connecting the HTML injection to the existing plugin systems.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `TemplateDataElementType` constructor | Passing `TEMPLATE_TEXT` as `templateElementType` (first position) instead of `outerElementType` (third position) | `TEMPLATE_TEXT` is the HTML content token; it goes in the `outerElementType` slot; Mako-syntax tokens go in `templateElementType` |
| `fileType.fileViewProviderFactory` registration | Using `filetype="Mako"` instead of `filetype="Mako Template"` | Value must equal `MakoFileType.getName()` exactly — `"Mako Template"` |
| `getTemplateDataLanguage()` null handling | Calling `TemplateDataLanguageMappings.getInstance(project)` when `project` is null | Check `manager.project ?: return HTMLLanguage.INSTANCE` before accessing mappings |
| HTML `PsiFile` creation | Calling `createFile` for HTML without setting `contentElementType` | Must set `(htmlFile as PsiFileImpl).contentElementType = MAKO_TEMPLATE_DATA_TYPE` immediately after creating the HTML file |
| Python injector root lookup | Using `context.containingFile` as search root in `PsiTreeUtil.findChildrenOfType` | Use `context.containingFile.viewProvider.getPsi(MakoLanguage)` to guarantee Mako tree root |
| `MakoCompletionContributor` language guard | `file.language.id == "Mako Template"` where `file` is `parameters.position.containingFile` | After adding view provider, `containingFile` may return the HTML file in HTML regions; guard with `parameters.position.containingFile.viewProvider.baseLanguage == MakoLanguage` |
| `MakoAnnotator` PSI traversal | No guard against `OuterLanguageElement` nodes in Mako tree | Add `if (element is OuterLanguageElement) return` at the top of `annotate()` |
| `TemplateDataElementType` instantiation | Creating inside `createFile()` per-file | Must be a static singleton declared in companion object |
| `configureByText` in tests | Using `configureByText("test.mako", content)` — creates in-memory file before MakoFileType registered | Continue using the existing `addFileToProject + configureFromExistingVirtualFile` pattern established in v0.1.0 tests |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| HTML PSI tree rebuilt on every keystroke because `getTemplateDataLanguage()` returns a new Language instance | IDE lag on typing in `.mako` files; CPU spike from HTML re-parsing | Return a stable singleton language (e.g., `HTMLLanguage.INSTANCE`) — never create new Language instances in `getTemplateDataLanguage()` | Every keystroke |
| `TemplateDataElementType` per-file construction causes new IElementType registration per file | Memory leak in `IElementType` registry; O(n) type lookups degrade | Make `TemplateDataElementType` a singleton | After 100+ file opens |
| `PsiTreeUtil.findChildrenOfType` on both Mako and HTML trees | Double traversal per injection request | Ensure injector only traverses the Mako tree; HTML tree traversal is unnecessary | Files > 200 lines |
| Language injection overlapping TemplateLanguageFileViewProvider regions | Python injection fragments computed redundantly against template-data regions that are now HTML-owned | Verify that Python injection `TextRange` offsets remain within Mako token boundaries (not TEMPLATE_TEXT regions) — they do by design, but confirm after adding view provider | Any file with both `${}` and surrounding HTML |

---

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces after adding HTML injection.

- [ ] **FileViewProvider registered:** `file.viewProvider.allFiles.size == 2` for any `.mako` file — verify in PSI Viewer or a test.
- [ ] **HTML tree correct structure:** PSI Viewer shows HTML PSI nodes (e.g., `HtmlDocumentImpl`, `XmlTagImpl`) for TEMPLATE_TEXT regions, not raw Mako tokens.
- [ ] **Mako tree intact:** PSI Viewer shows `MakoDefTag`, `MakoBlockTag`, `MakoExpression`, etc. in the Mako tree — none replaced by `OuterLanguageElementImpl` except at TEMPLATE_TEXT boundaries.
- [ ] **Python injection still works:** After registering the view provider, `${someVar}` inside a `.mako` file still gets Python language injection (visible as "Injected Language Fragment" in PSI Viewer).
- [ ] **Folding still works:** `<%def>` and `<%block>` regions still fold; no crash or empty fold region list in MakoFoldingBuilder.
- [ ] **Structure View still works:** `<%def>` and `<%block>` declarations appear in the Structure View panel after adding the view provider.
- [ ] **Mako completion still works:** Typing `<%` still triggers Mako tag name completions — not only HTML completions.
- [ ] **No false HTML errors:** `<%def name="foo():">` lines do not show red "invalid HTML" squiggles from the HTML annotator.
- [ ] **HTML completion fires in TEMPLATE_TEXT:** Typing `<di` in a TEMPLATE_TEXT region offers HTML completion (`<div>`, `<dialog>`, etc.).
- [ ] **Default data language configured:** A fresh project shows HTML pre-configured for `.mako` files in `Settings → Template Data Languages`.
- [ ] **All existing tests pass:** The full `./gradlew check` passes without modifications to existing test infrastructure.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Wrong extension point key for factory registration | LOW | Change `fileType.fileViewProviderFactory` attribute name in `plugin.xml`; no code change required |
| `contentElementType` not set on HTML file | LOW-MEDIUM | Add the assignment in `createFile`; verify with PSI Viewer |
| `TemplateDataElementType` arguments transposed | MEDIUM | Swap constructor arguments; all HTML PSI trees for all open files must be invalidated (File → Invalidate Caches / Restart); re-verify with PSI Viewer |
| Python injection broken due to wrong file root | MEDIUM | Update `collectCodeAndExpressionHosts` to use `viewProvider.getPsi(MakoLanguage)`; re-run all injection tests |
| Folding/structure view broken after view provider | MEDIUM | Add `OuterLanguageElement` guards in `MakoFoldingBuilder` and `MakoStructureViewElement`; re-run folding and structure tests |
| `TemplateDataElementType` instantiated per-file | MEDIUM | Refactor to companion object singleton; File → Invalidate Caches / Restart required to clear corrupt PSI state |
| HTML false-positive errors visible to users | LOW-MEDIUM | Implement annotation suppressor or verify `OuterLanguageElementImpl` handling in platform HTML annotator |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Wrong extension point key | Phase 1: FileViewProvider registration | `file.viewProvider is TemplateLanguageFileViewProvider` assertion in a test |
| `contentElementType` not set | Phase 1: FileViewProvider `createFile` | PSI Viewer shows HTML tree with structural HTML nodes |
| `TemplateDataElementType` argument order | Phase 1: TemplateDataElementType construction | PSI Viewer shows Mako tree with `OuterLanguageElementImpl` only at TEMPLATE_TEXT positions |
| Folding/structure breaks on `OuterLanguageElement` | Phase 1: Audit existing PSI consumers before wiring | All existing folding and structure view tests pass after Phase 1 |
| Python injector wrong file root | Phase 1: Defensive injector fix | Existing Python injection tests pass after Phase 1 |
| `getTemplateDataLanguage` null project | Phase 1: `createFile` null safety | No NPE during project open or file load in manual testing |
| Default data language not configured | Phase 1: `templateDataLanguagePatterns` in plugin.xml | Fresh project shows HTML pre-configured in Settings → Template Data Languages |
| `MakoFile`/`HtmlFileImpl` cast errors | Phase 1: Language guards in existing code | No ClassCastException in IDE logs during file operations |
| `MakoAnnotator` `OuterLanguageElement` crash | Phase 1: Add `OuterLanguageElement` guard | `MakoAnnotator` tests pass; no ClassCastException during annotation pass |
| Completion contributor double-firing | Phase 2: Post-view-provider regression testing | HTML and Mako completion each fire exactly once in appropriate regions |
| `TemplateDataElementType` not a singleton | Phase 1: Singleton declaration | Memory profiler shows single `TemplateDataElementType` instance across multiple file opens |

---

## Sources

- IntelliJ Platform Plugin SDK — File View Providers documentation: https://plugins.jetbrains.com/docs/intellij/file-view-providers.html (MEDIUM confidence — confirmed extension point registration syntax)
- IntelliJ Platform API Changes 2025: https://plugins.jetbrains.com/docs/intellij/api-changes-list-2025.html (HIGH confidence — no breaking changes to template language APIs in 2025.2)
- JetBrains Support Forum — Template language plugin tutorial: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206765105-Tutorial-Custom-templating-language-plugin (MEDIUM confidence — community knowledge, not official docs)
- JetBrains Support Forum — Example of custom template language plugin: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206780275-Example-of-a-custom-language-plugin-for-a-templating-language (MEDIUM confidence)
- JetBrains Support Forum — Code formatting for template languages: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206757715-Code-formatting-for-template-languages (MEDIUM confidence)
- JetBrains Support Forum — Custom template language insert outer language: https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000572430-Custom-Template-Language-insert-outer-language-code-at-any-place-not-only-template-fragments- (MEDIUM confidence)
- IntelliJ Community source — TemplateDataElementType.java at build 241: https://github.com/JetBrains/intellij-community/blob/idea/241.18034.62/platform/analysis-impl/src/com/intellij/psi/templateLanguages/TemplateDataElementType.java (HIGH confidence — source code)
- templ-jetbrains reference implementation: https://github.com/templ-go/templ-jetbrains (MEDIUM confidence — open source plugin following same pattern)
- Handlebars plugin plugin.xml: https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/resources/META-INF/plugin.xml (MEDIUM confidence — reference implementation)
- JetBrains blog — Determining template data language by file extension: https://blog.jetbrains.com/idea/2009/03/determining-template-data-language-by-a-file-extension/ (MEDIUM confidence — older post, core mechanism still applies)
- Codebase analysis: MakoLanguage.kt, MakoFile.kt, MakoParserDefinition.kt, MakoPythonInjector.kt, plugin.xml, MakoFoldingBuilder.kt, MakoStructureViewElement.kt (HIGH confidence — direct source inspection)

---

*Pitfalls research for: Adding HTML injection to existing JetBrains Mako template language plugin*
*Researched: 2026-02-22*

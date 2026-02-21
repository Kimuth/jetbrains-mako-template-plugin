# Phase 7: Completion - Research

**Researched:** 2026-02-21
**Domain:** IntelliJ Platform CompletionContributor — keyword and attribute completion for a custom template language
**Confidence:** HIGH

---

## Summary

Phase 7 adds autocomplete for Mako tag names and tag attributes. The IntelliJ Platform provides a well-documented `CompletionContributor` + `CompletionProvider` extension model for exactly this use case. The contributor registers context-sensitive patterns and supplies `LookupElement` items to the completion popup. For Mako, two providers are needed: one for tag-name completion (triggered in TEMPLATE_TEXT context after `<%`) and one for attribute-name completion (triggered when the caret is inside a tag's attribute list).

The primary challenge is that the Mako lexer matches complete tag-open tokens (`<%def`, `<%block`, …) rather than leaving a partial `<%` token for the parser to complete. When completion fires after the user types `<%`, the dummy identifier appended by IntelliJ (`IntellijIdeaRulezzz`) produces a string like `<%IntellijIdeaRulezzz` which does not match any lexer rule and falls through to `TEMPLATE_TEXT`. This means the approach of matching on `TAG_OPEN_DEF` / `TAG_OPEN_BLOCK` PSI nodes will not work for tag-name completion; instead, the contributor must work at the TEMPLATE_TEXT level, inspecting the raw preceding text via `parameters.getPosition()` and `CompletionParameters.getOriginalFile()` to detect the `<%` prefix manually.

For attribute completion, the situation is simpler: once a complete `<%def ` has been typed, the lexer enters `TAG_ATTRS` state and the parser produces `MakoDefTag` / `MakoBlockTag` / etc. nodes. Inside those nodes the caret position will be a `TAG_ATTR_NAME` leaf (possibly preceded by whitespace), so the pattern `psiElement(TAG_ATTR_NAME).withSuperParent(2, MakoDefTag.class)` cleanly identifies the context. If nothing has been typed yet (caret on whitespace inside the tag), the fallback is to inspect `parameters.getPosition().getParent()` to detect the containing tag node type and supply appropriate attributes.

**Primary recommendation:** Implement two `CompletionContributor` classes (or two `extend()` calls in one class): `MakoTagNameCompletionContributor` for `<%` → tag names, and `MakoTagAttributeCompletionContributor` for attribute names inside open tags. Register both under `language="Mako Template"` in plugin.xml. Use `BasePlatformTestCase` + `myFixture` for tests.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| COMP-01 | User gets autocomplete suggestions for Mako tag names (`<%def`, `<%block`, `<%inherit`, etc.) | CompletionContributor with TEMPLATE_TEXT context pattern + text inspection for `<%` prefix; add `LookupElementBuilder` items for all 7 tag names with insert handler |
| COMP-02 | User gets autocomplete suggestions for Mako tag attributes (`name=`, `file=`, `buffered=`) | CompletionContributor with TAG_ATTR_NAME or whitespace-inside-tag pattern; per-tag attribute map derived from Mako docs; `LookupElementBuilder` with `=` suffix and optional insert handler |
</phase_requirements>

---

## Standard Stack

### Core (no new dependencies — all already on classpath)

| Class | Package | Purpose | Why Standard |
|-------|---------|---------|--------------|
| `CompletionContributor` | `com.intellij.codeInsight.completion` | Base class; register one per language or per provider group | Platform standard; only way to implement keyword completion |
| `CompletionProvider<CompletionParameters>` | `com.intellij.codeInsight.completion` | Provides suggestions for a specific pattern | Encapsulates completion logic, called by contributor |
| `CompletionType.BASIC` | `com.intellij.codeInsight.completion` | The standard Ctrl+Space completion type | All Mako completions are BASIC type |
| `PlatformPatterns.psiElement()` | `com.intellij.patterns` | Entry point for element pattern DSL | Standard pattern DSL for targeting PSI positions |
| `LookupElementBuilder` | `com.intellij.codeInsight.lookup` | Builds individual completion items | Standard builder with icon, tail text, insert handler |
| `InsertHandler<LookupElement>` | `com.intellij.codeInsight.completion` | Post-insertion side effects (add `=`, move caret) | Used when inserting `name=` should leave caret after `=` |

### Supporting

| Class | Package | Purpose | When to Use |
|-------|---------|---------|-------------|
| `CompletionResultSet` | `com.intellij.codeInsight.completion` | Container for lookup items | Add items via `resultSet.addElement()` |
| `PsiTreeUtil` | `com.intellij.psi.util` | Navigate PSI tree | `getParentOfType()` to find containing tag node |
| `BasePlatformTestCase` | `com.intellij.testFramework.fixtures` | Test base class | Used by all non-parser tests in this project already |
| `CompletionParameters` | `com.intellij.codeInsight.completion` | Position, file, completion type | Access via `parameters.getPosition()` to get leaf PSI |

### No New Build Dependencies

No changes to `build.gradle.kts` are required. All completion API classes are bundled with the IntelliJ Platform (already on classpath via `pycharmCommunity()`).

---

## Architecture Patterns

### Recommended Source Structure

```
src/main/kotlin/.../lang/completion/
├── MakoCompletionContributor.kt   # Single class with two extend() registrations
src/test/kotlin/.../lang/
├── MakoCompletionTest.kt          # BasePlatformTestCase; all completion tests
src/test/testData/completion/
├── TagNameCompletion.mako          # Input with <caret> for tag-name completion tests
├── TagAttrCompletion_Def.mako      # Input for <%def attribute completion
├── TagAttrCompletion_Inherit.mako  # Input for <%inherit attribute completion
└── NoCompletionInText.mako         # Input verifying no false positives in HTML
```

### Pattern 1: Tag-Name Completion (COMP-01)

**What:** When the user has typed `<%` and triggers completion, offer the 7 Mako directive names.

**The challenge:** The Mako lexer does not produce a partial `<%` token. When IntelliJ appends the dummy identifier to make `<%IntellijIdeaRulezzz`, the lexer falls through the `[$<%#]` fallback rule and produces a `TEMPLATE_TEXT` leaf. The caret position element is therefore `TEMPLATE_TEXT`. The contributor must use `parameters.getPosition().text` or `parameters.getOriginalPosition()` to check whether the text immediately before the caret (in the original file) is `<%`.

**Approach — manual prefix check in `addCompletions`:**

```kotlin
// Source: IntelliJ Platform SDK docs, code-completion.html + compiler inspection
class MakoCompletionContributor : CompletionContributor() {

    init {
        // COMP-01: Tag-name completion after <%
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),   // broad — narrowed manually inside provider
            TagNameCompletionProvider()
        )
        // COMP-02: Attribute completion inside open tags
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(MakoTokenTypes.TAG_ATTR_NAME),
            TagAttrCompletionProvider()
        )
    }

    /** Override to supply the dummy identifier — avoid breaking the lexer worse */
    override fun beforeCompletion(context: CompletionInitializationContext) {
        // Default "IntellijIdeaRulezzz" is safe; no override needed for tag names
        // because we match on TEMPLATE_TEXT and inspect raw text anyway.
    }
}
```

**TagNameCompletionProvider:**

```kotlin
private val TAG_NAMES = listOf("<%def", "<%block", "<%inherit", "<%include",
                                "<%namespace", "<%page", "<%doc")

private class TagNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position          // leaf at caret in completion file
        val originalFile = parameters.originalFile  // real Mako file

        // Guard: only fire in Mako files
        if (originalFile.language.id != "Mako Template") return

        // Check preceding text: must end with <%
        val offset = parameters.offset
        val docText = originalFile.text
        if (offset < 2 || docText.substring(offset - 2, offset) != "<%") return

        // Check that we're NOT already inside a tag attribute position
        if (position.parent?.let { it is MakoDefTag || it is MakoBlockTag || /* etc */ false } == true) return

        TAG_NAMES.forEach { tagName ->
            result.addElement(
                LookupElementBuilder.create(tagName)
                    .withPresentableText(tagName)
                    .withBoldness(true)
                    // Insert only the suffix after <% (already typed)
                    .withInsertHandler { insertCtx, _ ->
                        // Replace the <% with the full tag name
                        val startOffset = insertCtx.startOffset - 2
                        insertCtx.document.replaceString(startOffset, insertCtx.tailOffset, tagName)
                    }
            )
        }
    }
}
```

**Note on prefix matching:** `result.getPrefixMatcher().prefix` will contain the dummy identifier text, not `<%def`. Use `withPrefixMatcher("")` or supply a custom `PrefixMatcher` that always matches, letting the insert handler do the replacement. Alternatively, use `result.withPrefixMatcher(tagName.drop(2))` to narrow.

### Pattern 2: Attribute Completion (COMP-02)

**What:** Inside `<%def `, `<%block `, `<%inherit `, `<%include `, `<%namespace `, `<%page ` — offer the attributes valid for that specific tag.

**How it works:** Once the full tag keyword is typed, the lexer enters `TAG_ATTRS` state. The parser creates a composite `MakoDefTagImpl`, `MakoInheritTagImpl`, etc. node. When the user types a partial attribute name, the caret element is `TAG_ATTR_NAME`. When there is nothing yet typed (whitespace), the element may be a `WHITE_SPACE` leaf whose parent is the tag composite node.

**Per-tag attribute map (from Mako docs):**

```kotlin
private val TAG_ATTRIBUTES: Map<Class<out PsiElement>, List<String>> = mapOf(
    MakoDefTag::class.java     to listOf("name", "buffered", "cached", "cache_key", "cache_timeout", "cache_type", "filter", "decorator"),
    MakoBlockTag::class.java   to listOf("name", "filter", "cached", "cache_key", "cache_timeout", "cache_type"),
    MakoInheritTag::class.java to listOf("file"),
    MakoIncludeTag::class.java to listOf("file", "args"),
    MakoNamespaceTag::class.java to listOf("name", "file", "import", "module"),
    MakoPageTag::class.java    to listOf("args", "expression_filter", "cached", "cache_key", "cache_timeout", "cache_type")
)
```

**TagAttrCompletionProvider:**

```kotlin
private class TagAttrCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position

        // Walk up to find the containing tag node
        var element: PsiElement? = position.parent
        while (element != null && element !is PsiFile) {
            val attrs = TAG_ATTRIBUTES[element.javaClass]
            if (attrs != null) {
                attrs.forEach { attr ->
                    result.addElement(
                        LookupElementBuilder.create(attr)
                            .withInsertHandler { ctx, _ ->
                                // Insert attr="" and place caret between quotes
                                val doc = ctx.document
                                doc.insertString(ctx.tailOffset, "=\"\"")
                                ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                            }
                    )
                }
                return
            }
            element = element.parent
        }
    }
}
```

### Pattern 3: Preventing False Positives (COMP-03 implicit in success criterion 3)

**What:** Completions must not appear in plain HTML/template text outside `<%` constructs.

**How it works:** The `TagNameCompletionProvider` performs an explicit check: `if (docText.substring(offset - 2, offset) != "<%") return`. This ensures completion only fires when the user is directly after `<%`. The `TagAttrCompletionProvider` is registered on `psiElement(MakoTokenTypes.TAG_ATTR_NAME)`, which only matches inside the lexer `TAG_ATTRS` state — a very tight context that cannot fire in TEMPLATE_TEXT.

**The language guard:** Registering the contributor with `language="Mako Template"` in plugin.xml ensures the contributor is never called for HTML, Python, or other languages within the same file (including injected language fragments).

### Anti-Patterns to Avoid

- **Matching on `TAG_OPEN_DEF` leaf for tag-name completion:** The dummy identifier breaks the lexer before these tokens are produced; the tag node does not exist at completion time.
- **Using `psiElement().withText(startsWith("<%"))` pattern:** `withText()` matches the full element text, but the TEMPLATE_TEXT element may span many characters before `<%`.
- **Global `psiElement()` pattern with no language guard:** Will fire for Python or HTML injected fragments; always add the language guard or use the `language=` attribute in plugin.xml registration.
- **Not checking `TAG_ATTR_NAME` in the extend pattern:** If you register with `psiElement()` for attribute completion, you will also fire during tag-name positions.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Completion popup triggering | Custom keystroke listener | `CompletionContributor` registered via `com.intellij.completion.contributor` | Platform manages popup timing, ranking, prefix filtering |
| Prefix matching | Custom string startsWith logic | `CompletionResultSet.getPrefixMatcher()` | Platform's prefix matcher handles case sensitivity and camelCase |
| Insert-time text manipulation | Manual `Document.setText()` | `InsertHandler<LookupElement>` | Platform provides `InsertionContext` with correct offsets; handles undo |
| Completion item sorting/ranking | Custom sort logic | `LookupElementBuilder.withPriority()` or `PrioritizedLookupElement.withPriority()` | Platform sorts automatically; override only for tuning |
| Suppressing popups in comments | Manual check in provider | `CompletionConfidence` extension (if needed) | Platform already suppresses in comment token types; MakoTokenTypes.LINE_COMMENT is in COMMENTS set |

**Key insight:** The IntelliJ completion framework handles almost everything outside of "what items to show in what context" — filtering, ranking, insert, caret positioning. Only the data layer (item list, context check) needs custom code.

---

## Common Pitfalls

### Pitfall 1: Dummy Identifier Breaks Lexer Context

**What goes wrong:** When IntelliJ fires completion at offset N, it appends `IntellijIdeaRulezzz` to the completion copy of the document. If the user typed `<%d`, the lexer sees `<%dIntellijIdeaRulezzz` — which matches no tag rule and lands in `TEMPLATE_TEXT`. The PSI tree has no `MakoDefTag` node; the contributor sees a TEMPLATE_TEXT leaf at the caret.

**Why it happens:** Mako's lexer uses greedy longest-match for tag keywords (`<%def`, `<%block`, …). A partial prefix with the appended dummy string matches neither the tag rules nor the code-block rule, falling through to `TEMPLATE_TEXT`.

**How to avoid:** For COMP-01 (tag name completion), do not rely on the PSI node type to detect "we're after `<%`". Instead, read `parameters.originalFile.text.substring(offset - 2, offset)` (or a wider window to handle already-typed partial names). This is the approach used by many template-language contributors.

**Warning signs:** If the `addCompletions` method is never called, check that the `psiElement()` pattern actually matches something in the completion-modified PSI. Log `parameters.position.elementType` to see what leaf type exists at the caret.

### Pitfall 2: insert Handler Offset Drift

**What goes wrong:** When the insert handler replaces text, `InsertionContext.tailOffset` and `startOffset` are relative to the completion-modified document. If you insert `="` and then try to position the caret two characters back, off-by-one errors leave the caret in the wrong place.

**Why it happens:** `InsertionContext` offsets are zero-based and shift when you insert/replace characters earlier in the string.

**How to avoid:** Insert the suffix first, then use `ctx.editor.caretModel.moveToOffset(ctx.tailOffset - N)` where N is measured from the new tailOffset after insertion. Test with `myFixture.checkResult()`.

### Pitfall 3: Language Mismatch — Completion Fires in Injected Python

**What goes wrong:** The contributor fires inside injected Python fragments (`${...}`, `<% %>`). The Python language ID is "Python", not "Mako Template", so registering with `language="Mako Template"` in plugin.xml prevents this — but only if the contributor is correctly registered.

**Why it happens:** If `language` attribute is omitted from the plugin.xml registration, the contributor fires for all languages.

**How to avoid:** Always set `language="Mako Template"` in the `<completion.contributor>` tag. Verify by writing a test with `<caret>` inside `${...}` and asserting no Mako completions appear.

### Pitfall 4: Attribute Completion Without TAG_ATTR_NAME Parent Walk

**What goes wrong:** The caret may land on `WHITE_SPACE` (between existing attributes), not on a `TAG_ATTR_NAME` token. Registering only on `psiElement(TAG_ATTR_NAME)` misses this case.

**Why it happens:** Whitespace between attributes is `TokenType.WHITE_SPACE`, which does not have type `TAG_ATTR_NAME`. The `extend()` pattern does not fire.

**How to avoid:** Add a second `extend()` registration on `psiElement(TokenType.WHITE_SPACE).withParent(...)` or walk up from `WHITE_SPACE` in the provider to detect the containing tag composite. Keep the tag-type detection in `addCompletions` rather than the pattern, so both entry points share one code path.

### Pitfall 5: `<%doc>` is Not a Directive That Needs Attribute Completion

**What goes wrong:** The attribute map includes `<%doc>` in COMP-01 (tag names), but `<%doc>` has no attributes. If the attribute provider fires inside a `MakoDocComment` node, it will find no entry in `TAG_ATTRIBUTES` and silently return — which is correct. But if `<%doc>` appears in the tag-name list with an insert handler that adds a space, the user lands in the doc block, not a tag-attrs context.

**How to avoid:** In the tag-name completion insert handler, insert the closing `>` for `<%doc>` directly (e.g., `<%doc>\n</%doc>`) rather than trailing space.

---

## Code Examples

Verified patterns from official sources:

### plugin.xml Registration

```xml
<!-- Source: plugins.jetbrains.com/docs/intellij/completion-contributor.html -->
<extensions defaultExtensionNs="com.intellij">
    <completion.contributor
        language="Mako Template"
        implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.completion.MakoCompletionContributor"/>
</extensions>
```

### Minimal CompletionContributor with Two Providers

```kotlin
// Source: plugins.jetbrains.com/docs/intellij/completion-contributor.html
class MakoCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement(),
               TagNameCompletionProvider())

        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement(MakoTokenTypes.TAG_ATTR_NAME),
               TagAttrCompletionProvider())
    }
}
```

### Test Pattern — BasePlatformTestCase with myFixture

```kotlin
// Source: plugins.jetbrains.com/docs/intellij/completion-test.html
// Matches existing project test pattern (see MakoLexerTest.kt)
class MakoCompletionTest : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData/completion"

    fun testTagNameCompletion() {
        myFixture.configureByText("test.mako", "<%<caret>")
        val items = myFixture.completeBasic()
        assertNotNull(items)
        val strings = items.map { it.lookupString }
        assertContainsElements(strings, "<%def", "<%block", "<%inherit", "<%include",
                                        "<%namespace", "<%page", "<%doc>")
    }

    fun testDefAttrCompletion() {
        myFixture.configureByText("test.mako", "<%def <caret>>")
        val items = myFixture.completeBasic()
        assertNotNull(items)
        val strings = items.map { it.lookupString }
        assertContainsElements(strings, "name", "buffered", "cached", "filter")
    }

    fun testNoCompletionInPlainHtml() {
        myFixture.configureByText("test.mako", "<html><body><caret></body></html>")
        val items = myFixture.completeBasic()
        // items may be null (auto-completed single item) or non-null but must not contain Mako tags
        val strings = items?.map { it.lookupString } ?: emptyList()
        assertDoesntContain(strings, "<%def", "<%block")
    }
}
```

### LookupElementBuilder with Insert Handler

```kotlin
// Source: plugin-dev.com/intellij/custom-language/code-completion
LookupElementBuilder.create("name")
    .withBoldness(true)
    .withTailText("=\"...\"", true)
    .withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.tailOffset, "=\"\"")
        ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
        ctx.commitDocument()
    }
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Override `fillCompletionVariants()` directly | Use `extend()` + `CompletionProvider` inner class | IntelliJ 13+ | Cleaner separation; `extend()` is the documented pattern |
| `LookupItem` / raw string variants | `LookupElementBuilder` fluent API | IntelliJ 12+ | Rich presentation (icon, tail text, type text) |
| `LightJavaCodeInsightFixtureTestCase` for all tests | `BasePlatformTestCase` for non-Java plugins | Stable since 2020 | Project already uses `BasePlatformTestCase` — stay consistent |

**Deprecated/outdated:**
- `LookupItem`: Replaced by `LookupElementBuilder`; do not use
- `CompletionData`: Old pre-2.x API; entirely removed

---

## Mako Tag Attribute Reference (from makotemplates.org docs)

Derived from official Mako documentation at makotemplates.org for COMP-02 attribute map:

| Tag | Attributes |
|-----|-----------|
| `<%def>` | `name` (required), `buffered`, `cached`, `cache_key`, `cache_timeout`, `cache_type`, `cache_url`, `cache_dir`, `cache_region`, `filter`, `decorator` |
| `<%block>` | `name` (optional), `filter`, `cached`, `cache_key`, `cache_timeout`, `cache_type`, `cache_url`, `cache_dir`, `cache_region` |
| `<%inherit>` | `file` (required) |
| `<%include>` | `file` (required), `args` |
| `<%namespace>` | `name`, `file`, `import`, `module` |
| `<%page>` | `args`, `expression_filter`, `cached`, `cache_key`, `cache_timeout`, `cache_type`, `cache_url`, `cache_dir`, `cache_region` |
| `<%doc>` | none (content only, no attributes) |

---

## Open Questions

1. **Tag-name insert handler: what to insert after the tag name?**
   - What we know: After `<%def`, the user needs to provide attributes then `>`. A natural completion would insert `<%def name="">` with caret between the quotes.
   - What's unclear: Whether to use a live template / postfix template for full snippet insertion, or keep it simple (just insert the tag name and let the user type the rest).
   - Recommendation: Start simple — insert the tag name with a trailing space and no template. If the user requests snippet behavior later, it can be added separately.

2. **Auto-popup after `%` (second character of `<%`):**
   - What we know: `TypedHandlerDelegate.checkAutoPopup()` can schedule a popup on specific characters. The platform doesn't auto-popup after `%` by default.
   - What's unclear: Whether the success criterion "triggers a completion popup" implies auto-popup on typing `<%`, or just that Ctrl+Space works after `<%`.
   - Recommendation: The requirements say "typing `<%` triggers a completion popup" — implement `TypedHandlerDelegate` to call `AutoPopupController.scheduleAutoPopup()` on `>` if preceded by `<`, or on `%` if preceded by `<`. This is an optional enhancement to COMP-01; manual Ctrl+Space satisfies the core requirement.

3. **Attribute completion when caret is on whitespace inside tag:**
   - What we know: The `extend()` on `psiElement(TAG_ATTR_NAME)` won't fire if the caret is on whitespace before any attribute name is typed.
   - What's unclear: Whether the caret on whitespace between `<%def` and `>` produces a `WHITE_SPACE` PSI leaf whose parent is `MakoDefTagImpl`, or whether the parser error-recovers and the tree is malformed at that point.
   - Recommendation: Test empirically with `runIde`. If whitespace approach is needed, add a second `extend()` on `psiElement(TokenType.WHITE_SPACE)` with parent walk.

---

## Sources

### Primary (HIGH confidence)
- Official IntelliJ Platform Plugin SDK — Code Completion: https://plugins.jetbrains.com/docs/intellij/code-completion.html
- Official IntelliJ Platform Plugin SDK — Completion Contributor tutorial: https://plugins.jetbrains.com/docs/intellij/completion-contributor.html
- Official IntelliJ Platform Plugin SDK — Completion Test tutorial: https://plugins.jetbrains.com/docs/intellij/completion-test.html
- Official IntelliJ Platform Plugin SDK — Element Patterns: https://plugins.jetbrains.com/docs/intellij/element-patterns.html
- JetBrains intellij-plugins Handlebars plugin.xml (completion.contributor registration): https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/resources/META-INF/plugin.xml
- Mako Template docs — Defs: https://docs.makotemplates.org/en/latest/defs.html
- Mako Template docs — Syntax: https://docs.makotemplates.org/en/latest/syntax.html
- Mako Template docs — Caching: https://docs.makotemplates.org/en/latest/caching.html

### Secondary (MEDIUM confidence)
- plugin-dev.com code completion guide (cross-verified with SDK docs): https://www.plugin-dev.com/intellij/custom-language/code-completion/
- JetBrains Handlebars HbKeywordCompletionContributor source (verified registration pattern): https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/completion/HbKeywordCompletionContributor.java

### Tertiary (LOW confidence)
- JetBrains Support Forum — dummy identifier behavior in custom languages (unverified edge cases): https://intellij-support.jetbrains.com/hc/en-us/community/posts/5143764172178-Completion-Contributor-Changed-dummy-indentifier-received-by-other-contributors

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — extension point, classes, and registration pattern confirmed via official SDK docs
- Architecture: HIGH — two-provider approach confirmed by Handlebars reference implementation; tag-name detection via raw text inspection is LOW (confirmed conceptually; exact PSI shape during completion needs empirical verification with `runIde`)
- Pitfalls: MEDIUM — dummy identifier behavior confirmed via community forum + SDK docs; whitespace-inside-tag behavior is LOW and flagged as Open Question

**Research date:** 2026-02-21
**Valid until:** 2026-05-21 (stable IntelliJ Platform completion API; unlikely to change in 90 days)

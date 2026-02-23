# Phase 20: HTML Feature Verification and False-Positive Audit - Research

**Researched:** 2026-02-22
**Domain:** IntelliJ Platform template language syntax highlighting delegation, false-positive HTML error suppression (`TemplateLanguageErrorFilter`), and CSS/JS sub-language injection for `TemplateLanguageFileViewProvider`-based plugins (PyCharm Community 2025.2.6, build 252)
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| CRCT-01 | `${...}` expressions inside HTML attribute values (e.g., `<div class="${cls}">`) do not produce false-positive HTML errors | `TemplateLanguageErrorFilter` (in `app-client.jar`) suppresses HTML `PsiErrorElement` highlights near template expression boundaries; `OuterLanguageElement` boundary already makes the token opaque to the HTML parser — the open question is whether the platform's HTML annotator fires a false positive at the boundary position in the running IDE |
| CRCT-02 | Mako control lines (`%for`, `%if`, `%endif`) do not produce false-positive HTML errors | Same mechanism as CRCT-01 — control lines are `CONTROL_LINE` tokens in the Mako PSI tree and appear as `OuterLanguageElement` placeholders in the HTML tree; `TemplateLanguageErrorFilter` suppresses HTML errors at outer-language boundaries |
</phase_requirements>

---

## Summary

Phase 20 has three distinct technical work streams that must be addressed to close the v0.3.0 milestone:

**Stream 1 — HINJ-01 (HTML syntax coloring in TEMPLATE_TEXT):** The `MakoSyntaxHighlighter` returns `EMPTY_KEYS` for `TEMPLATE_TEXT` tokens, which is correct — the Mako syntax highlighter should not color HTML content. The gap is that the editor highlighter used at render time is the standard `LexerEditorHighlighter` driven by `MakoSyntaxHighlighter`, which knows nothing about HTML. The fix is to implement a `MakoEditorHighlighter` that extends `LayeredLexerEditorHighlighter` and calls `registerLayer(MakoTokenTypes.TEMPLATE_TEXT, LayerDescriptor(htmlSyntaxHighlighter, ""))`. This highlighter is vended by a `MakoEditorHighlighterProvider` registered under the `editorHighlighterProvider` extension point with `filetype="Mako Template"`. This pattern is verified from the RST plugin (`RestEditorHighlighter.java`, `RestEditorHighlighterProvider.java`) in the live `restructuredtext.jar` and the Handlebars plugin (`HbTemplateHighlighter.java`, `HbHighlighterProvider.java`).

**Stream 2 — CRCT-01/CRCT-02 (false-positive HTML errors):** The mechanism is `TemplateLanguageErrorFilter`, a concrete subclass of `HighlightErrorFilter` that is specifically designed for this exact scenario. It takes a `TokenSet` of template expression edge tokens, the `Class<?>` of the `FileViewProvider`, and optional language IDs of known sub-languages. The filter's `shouldHighlightErrorElement()` method examines the HTML `PsiErrorElement`'s position relative to `OuterLanguageElement` boundaries in the HTML PSI tree and suppresses errors that are adjacent to or caused by template expression boundaries. Registered under `com.intellij.highlightErrorFilter` (a `ProjectExtensionPointName`). The Handlebars plugin's `HbErrorFilter` is the canonical reference implementation.

**Stream 3 — HINJ-05/HINJ-06 (CSS/JS in `<style>`/`<script>`):** These are IDE runtime verification items, not automated-test items. If the HTML PSI tree is valid (confirmed by existing tests), the platform's HTML plugin typically handles CSS/JS injection into `<style>` and `<script>` elements internally. The failure observed in Phase 18 (Emmet firing HTML handler instead of CSS/JS) may resolve once the `MakoEditorHighlighterProvider` supplies a layered highlighter, or may require a separate `HtmlScriptContentProvider` registration for the Mako file type. This requires empirical verification in `runIde` — not a code-first solution.

**Primary recommendation:** Implement the three streams as separate plans: (1) `MakoEditorHighlighter` + `MakoEditorHighlighterProvider` + `MakoErrorFilter` in one automated code plan, (2) `./gradlew check` verification, and (3) human IDE verification of CSS/JS sub-injection and false-positive behavior.

---

## Standard Stack

### Core

| Class / Interface | JAR | Purpose | Why Standard |
|-------------------|-----|---------|--------------|
| `com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter` | `app-client.jar` | Base class for a syntax highlighter that layers multiple token-level highlighters | The canonical approach for template languages — the outer language's highlighter runs first; sub-language highlighters are registered for specific token types via `registerLayer()` |
| `com.intellij.openapi.editor.ex.util.LayerDescriptor` | `app-client.jar` | Descriptor bundling a `SyntaxHighlighter` with a token separator and optional background key | Required argument to `LayeredLexerEditorHighlighter.registerLayer()` |
| `com.intellij.openapi.fileTypes.EditorHighlighterProvider` | `app-client.jar` | Factory interface for `EditorHighlighter` instances, registered per `FileType` | The platform queries this factory when opening a file in the editor — if registered, its `EditorHighlighter` replaces the default `LexerEditorHighlighter` |
| `com.intellij.codeInsight.highlighting.TemplateLanguageErrorFilter` | `app-client.jar` | Abstract `HighlightErrorFilter` subclass purpose-built for template languages | Suppresses `PsiErrorElement` highlights in the HTML PSI tree that are caused by template expression boundaries (`OuterLanguageElement`) — exactly the mechanism for CRCT-01/CRCT-02 |
| `com.intellij.codeInsight.highlighting.HighlightErrorFilter` | `app-client.jar` | Abstract base class; registered via `com.intellij.highlightErrorFilter` EP | The platform's general-purpose error-suppression hook; `TemplateLanguageErrorFilter` extends this |
| `com.intellij.openapi.fileTypes.SyntaxHighlighterFactory` | `app-client.jar` | Factory for `SyntaxHighlighter` instances by `Language` or `FileType` | Used inside `LayeredLexerEditorHighlighter` constructors to obtain the HTML `SyntaxHighlighter` for the template data language |

All classes verified via `javap -p` and `jar tf` against `pycharm-community-2025.2.6-win/lib/app-client.jar` and `pycharm-community-2025.2.6-win/plugins/restructuredtext/lib/restructuredtext.jar`.

### Extension Points

| Extension Point Name | Bean Class | Key Attribute | How to Use |
|----------------------|------------|---------------|------------|
| `editorHighlighterProvider` | `com.intellij.openapi.fileTypes.FileTypeExtensionPoint` | `filetype` | `filetype="Mako Template"` — must match `MakoFileType.getName()` which returns `"Mako Template"` |
| `com.intellij.highlightErrorFilter` | `ProjectExtensionPointName<HighlightErrorFilter>` | `implementation` | Registered as `<highlightErrorFilter implementation="..."/>` in `plugin.xml` |

Both EP names verified:
- `editorHighlighterProvider` — from `META-INF/PlatformExtensionPoints.xml` extracted from `app-client.jar` (line 7115 of `PyCharmCorePlugin.xml`)
- `com.intellij.highlightErrorFilter` — EP string literal from `HighlightErrorFilter` class constant pool: `#10 = Utf8 com.intellij.highlightErrorFilter`; registration from `PyCharmCorePlugin.xml` line 6708 and actual registrations at lines 10928-10929

---

## Architecture Patterns

### Pattern 1: LayeredLexerEditorHighlighter for HTML Coloring in Template Files

**What:** A `LayeredLexerEditorHighlighter` subclass registers the HTML `SyntaxHighlighter` for `TEMPLATE_TEXT` tokens. When the editor renders tokens, tokens typed as `TEMPLATE_TEXT` are colored by the HTML highlighter instead of the Mako highlighter.

**When to use:** When `MakoSyntaxHighlighter` already returns `EMPTY_KEYS` for `TEMPLATE_TEXT` and HTML coloring is absent in the editor despite a valid HTML PSI tree.

**Reference implementation:** `RestEditorHighlighter.java` in `restructuredtext.jar` (verified bytecode):
- Constructor calls `super(SyntaxHighlighterFactory.getSyntaxHighlighter(RestLanguage.INSTANCE, project, file), colors)`
- Then calls `registerLayer(RestTokenTypes.PYTHON_LINE, new LayerDescriptor(pythonHighlighter, "", EditorColors.INJECTED_LANGUAGE_FRAGMENT))`
- Null-guards project and file before attempting layer registration

**Mako equivalent pattern:**
```kotlin
// Source: verified from RestEditorHighlighter.java bytecode and HbTemplateHighlighter.java logic
class MakoEditorHighlighter(
    colors: EditorColorsScheme,
    project: Project?,
    file: VirtualFile?
) : LayeredLexerEditorHighlighter(
    SyntaxHighlighterFactory.getSyntaxHighlighter(MakoLanguage, project, file)!!,
    colors
) {
    init {
        if (project != null && file != null) {
            // Resolve template data language (user override or HTML default)
            val templateDataLanguage =
                TemplateDataLanguageMappings.getInstance(project)?.getMapping(file)
                    ?: HTMLLanguage.INSTANCE
            val htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
                templateDataLanguage, project, file
            )
            if (htmlHighlighter != null) {
                registerLayer(
                    MakoTokenTypes.TEMPLATE_TEXT,
                    LayerDescriptor(htmlHighlighter, "")
                )
            }
        }
    }
}
```

**Factory companion:**
```kotlin
class MakoEditorHighlighterProvider : EditorHighlighterProvider {
    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        file: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter =
        MakoEditorHighlighter(colors, project, file)
}
```

**plugin.xml registration:**
```xml
<!-- Phase 20: Layered editor highlighter — HTML coloring in TEMPLATE_TEXT regions -->
<editorHighlighterProvider
    filetype="Mako Template"
    implementationClass="com.schtilig.mako.lang.highlighting.MakoEditorHighlighterProvider"/>
```

**CRITICAL: `filetype` attribute must be `"Mako Template"` — matches `MakoFileType.getName()`.** Do not use `fileType` (camelCase) — the attribute name in `FileTypeExtensionPoint` is `filetype` (all lowercase), as confirmed from `PyCharmCorePlugin.xml` RST registration: `filetype="ReST"`.

### Pattern 2: TemplateLanguageErrorFilter for CRCT-01/CRCT-02

**What:** A concrete subclass of `TemplateLanguageErrorFilter` passes three arguments to the superclass constructor: a `TokenSet` of Mako tokens at expression boundaries, the `MakoFileViewProvider.class`, and optional known sub-language IDs.

**How it works:** The filter's `shouldHighlightErrorElement(PsiErrorElement)` checks whether the error element is:
1. Inside a file backed by a `TemplateLanguageFileViewProvider` that matches the provided class
2. Near (adjacent to or within) template expression edge tokens
3. In a position that is explained by an `OuterLanguageElement` boundary rather than genuine HTML malformation

If all conditions are met, the method returns `false` and the platform suppresses the red squiggle.

**Reference implementation:** `HbErrorFilter.java` in the Handlebars plugin:
```java
public final class HbErrorFilter extends TemplateLanguageErrorFilter {
  public HbErrorFilter() {
    super(
        TokenSet.create(OPEN, OPEN_PARTIAL, OPEN_BLOCK, OPEN_INVERSE),
        HbFileViewProvider.class,
        "HTML"
    );
  }
}
```

**Mako equivalent pattern:**
```kotlin
// Source: adapted from HbErrorFilter.java
// Import: com.intellij.codeInsight.highlighting.TemplateLanguageErrorFilter
// Import: com.intellij.psi.tree.TokenSet
class MakoErrorFilter : TemplateLanguageErrorFilter(
    TokenSet.create(
        MakoTokenTypes.EXPR_START,   // ${
        MakoTokenTypes.EXPR_END,     // }
        MakoTokenTypes.CONTROL_LINE  // % keyword ...
    ),
    MakoFileViewProvider::class.java,
    "HTML"
)
```

**plugin.xml registration:**
```xml
<!-- Phase 20: Suppress false-positive HTML errors near Mako expression/control line boundaries -->
<highlightErrorFilter
    implementation="com.schtilig.mako.lang.annotation.MakoErrorFilter"/>
```

**TemplateLanguageErrorFilter constructor signatures (verified via javap):**
- `TemplateLanguageErrorFilter(TokenSet, Class<?>)` — 2-arg, no sub-language IDs
- `TemplateLanguageErrorFilter(TokenSet, Class<?>, String...)` — 3-arg, with known sub-language names (vararg)
- Also: `NotNullLazyValue<TokenSet>` variants of both

The third argument is the language ID string(s) of known sub-languages inside the template. The filter uses them to determine which positions are "known good" injection sites. Based on the class constant pool, `"JavaScript"` and `"CSS"` are referenced internally — pass `"HTML"` as the primary sub-language (matching Handlebars pattern).

### Pattern 3: Test for CRCT-01/CRCT-02 (Automated — No False Positives)

**What:** `MakoFileViewProviderTest.kt` (which already uses `BasePlatformTestCase` + `addFileToProject`) is the right home for CRCT tests. After adding `MakoErrorFilter`, tests can assert that `doHighlighting()` returns no `ERROR` severity items on Mako expression/control line positions in a mixed HTML/Mako file.

**Pattern:**
```kotlin
fun testNoFalsePositiveOnMakoExpression() {
    val file = myFixture.addFileToProject(
        "crct_test.mako",
        "<div class=\"\${cls}\">Hello</div>"
    )
    myFixture.configureFromExistingVirtualFile(file.virtualFile)
    val highlights = myFixture.doHighlighting()
    val htmlErrors = highlights.filter {
        it.severity == HighlightSeverity.ERROR &&
        it.description?.contains("Mako") == false  // not our own annotator
    }
    assertTrue(
        "Expected no false-positive HTML errors on Mako expression in attribute value, got: $htmlErrors",
        htmlErrors.isEmpty()
    )
}
```

**Caveat:** `doHighlighting()` in `BasePlatformTestCase` may or may not run the HTML annotator, depending on which inspections are enabled in the test environment. If the test returns zero highlights initially (because the HTML annotator is not active in the test environment), the test still confirms the structural invariant. The definitive false-positive check is the human IDE verification.

### Pattern 4: Success Criterion 3 — viewProvider.getPsi(HTMLLanguage.INSTANCE) Test

**What:** Success criterion 3 ("a test asserts that `viewProvider.getPsi(HTMLLanguage.INSTANCE)` returns a non-null `HtmlFile`") is **already satisfied** by the existing `testMakoFileViewProviderProvidesHtmlPsi()` test in `MakoFileViewProviderTest.kt` (added in Phase 18-01). This test:
- Creates a `.mako` fixture with HTML markup using `addFileToProject`
- Asserts `viewProvider.getPsi(HTMLLanguage.INSTANCE)` is not null

No new test is needed for criterion 3. The planner should note this to avoid duplicate work.

### Anti-Patterns to Avoid

- **`fileType` vs `filetype` attribute in editorHighlighterProvider:** The `FileTypeExtensionPoint` bean uses `filetype` (all lowercase). Using `fileType` (camelCase) will silently fail — the platform will not find the provider and will fall back to the standard `LexerEditorHighlighter`. Verified from RST registration: `<editorHighlighterProvider filetype="ReST" implementationClass="..."/>`.
- **Not null-guarding `project`/`file` in MakoEditorHighlighter constructor:** `getEditorHighlighter()` passes null `project` and `file` during early IDE init (color scheme previews, etc.). The RST pattern explicitly null-guards before calling `registerLayer`.
- **Using `HighlightErrorFilter` directly instead of `TemplateLanguageErrorFilter`:** `HighlightErrorFilter` has a single abstract method `shouldHighlightErrorElement(PsiErrorElement)`. Implementing it directly requires manually checking `OuterLanguageElement` boundaries. `TemplateLanguageErrorFilter` encapsulates all that logic — always use it for template language false-positive suppression.
- **Wrong TokenSet in MakoErrorFilter:** The TokenSet passed to `TemplateLanguageErrorFilter` must be the **Mako-specific** tokens at the boundaries of expressions (the tokens adjacent to `OuterLanguageElement` regions), NOT all Mako tokens. Use `EXPR_START` (`${`), `EXPR_END` (`}`), and `CONTROL_LINE` (the control line token). Do not include `TEMPLATE_TEXT` — that is the token type for regions that ARE the HTML content.
- **Attempting CSS/JS `registerLayer` for style/script in MakoEditorHighlighter:** The HTML PSI tree already handles CSS/JS injection inside `<style>`/`<script>` elements internally. Adding additional `registerLayer` calls for CSS/JS token types in the Mako lexer would conflict. If HINJ-05/06 are still broken after HINJ-01 is fixed, the investigation belongs in `HtmlScriptContentProvider` territory, not in `MakoEditorHighlighter`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTML syntax coloring in TEMPLATE_TEXT | Custom token → color mapping in MakoSyntaxHighlighter | `LayeredLexerEditorHighlighter` + `registerLayer(TEMPLATE_TEXT, htmlHighlighter)` | The platform's layered highlighter correctly handles incremental updates, color scheme changes, and sub-highlighter composition; manual token mapping would need to be updated for every HTML token type |
| Suppress false-positive HTML errors on `${cls}` | Custom `HighlightErrorFilter` with manual `OuterLanguageElement` boundary traversal | `TemplateLanguageErrorFilter` | `TemplateLanguageErrorFilter` already implements `isNearTemplateExpressions()` and `findBaseLanguageElement()` — the complex boundary-checking logic that gets edge cases right |
| CSS/JS injection in `<style>`/`<script>` | `MultiHostInjector` or custom language injection contributor | Verify if platform HTML plugin auto-handles it once HINJ-01 is fixed | HTML plugin's internal CSS/JS injection (`HtmlScriptContentProvider`) should activate for the HTML PSI tree; investigate in IDE first before building infrastructure |

---

## Common Pitfalls

### Pitfall 1: editorHighlighterProvider Not Activating

**What goes wrong:** `MakoEditorHighlighter` is implemented and registered but HTML coloring still does not appear. The layered highlighter is not being used.

**Why it happens:** Either (a) the `filetype` attribute value does not exactly match `MakoFileType.getName()` = `"Mako Template"`, or (b) there is a cached `LexerEditorHighlighter` that has not been invalidated after the extension point registration.

**How to avoid:** Verify `filetype="Mako Template"` matches exactly. In a test: use `EditorHighlighterFactory.getInstance().createEditorHighlighter(project, MakoFileType.INSTANCE)` and cast it to `MakoEditorHighlighter` to confirm the factory is called.

**Warning signs:** HTML tags in TEMPLATE_TEXT regions still appear with no color (same as before Phase 20).

### Pitfall 2: NullPointerException in MakoEditorHighlighter Constructor

**What goes wrong:** `SyntaxHighlighterFactory.getSyntaxHighlighter(MakoLanguage, null, null)` returns null — causing NPE when passed to `LayeredLexerEditorHighlighter` super constructor.

**Why it happens:** `getEditorHighlighter()` is called with null `project` or null `file` (e.g., color scheme previews in Settings). `SyntaxHighlighterFactory.getSyntaxHighlighter()` returns null when the language has no registered highlighter factory.

**How to avoid:** The Mako highlighter factory IS registered (`MakoSyntaxHighlighterFactory` in plugin.xml); it should always return a valid highlighter. But add null guard anyway: `?: return` before super-call; fall back to `MakoSyntaxHighlighter()` directly.

**Warning signs:** NPE in IDE event log when opening Settings → Colors & Fonts or when the IDE starts up.

### Pitfall 3: TemplateLanguageErrorFilter Does Not Fire

**What goes wrong:** `MakoErrorFilter` is registered but `${cls}` in `<div class="${cls}">` still shows a red squiggle.

**Why it happens:** Either (a) the `MakoFileViewProvider.class` argument does not match the actual view provider class used at runtime, or (b) the `TokenSet` passed does not include the edge tokens adjacent to the `OuterLanguageElement` in the HTML PSI tree.

**How to avoid:** Confirm `MakoFileViewProvider.class` is the exact class (not a subclass or companion object class) returned by `file.viewProvider` for `.mako` files. Confirm `EXPR_START` and `EXPR_END` are the tokens immediately before/after `OuterLanguageElement` in the HTML PSI tree by inspecting the PSI Viewer for `<div class="${cls}">`.

**Warning signs:** `MakoErrorFilter.shouldHighlightErrorElement()` is called but returns `true` because `isTemplateViewProvider()` returns false — log in a debug build to confirm.

### Pitfall 4: CSS/JS Still Not Working After HINJ-01 Fix

**What goes wrong:** After `MakoEditorHighlighter` is in place and HTML coloring works, `<style>` and `<script>` content still shows HTML Emmet suggestions instead of CSS/JS.

**Why it happens:** The platform's HTML-to-CSS/JS injection uses `HtmlScriptContentProvider` registered per HTML embedding. For non-standard template files, the injection may not activate automatically because the `HtmlFileViewProvider` path is different from the `TemplateLanguageFileViewProvider` path.

**How to avoid:** Before writing code, verify empirically in `runIde` whether HINJ-05/06 now work after HINJ-01 is fixed. If they do (likely because the layered editor highlighter also activates CSS/JS layer dispatch), no code change is needed. If they do not, investigate registering a `HtmlScriptContentProvider` for the Mako language context.

**Warning signs:** Same symptom as Phase 18 — Emmet expansion in `<style>` produces HTML tags, not CSS properties.

### Pitfall 5: HINJ-04 Partial (HTML Error Squiggles Inconsistent) — Likely Intentional

**What goes wrong:** Investigation of HINJ-04 shows squiggles on `<span>` without close tag but NOT on `<p>` or `<html>` without close tag.

**Why it happens:** HTML5 optional-close-tag rules. The IntelliJ HTML annotator applies `<p>` optional-close-tag semantics — `<p>` is optional-close in HTML5, so the annotator does not flag it as an error. `<span>` is not optional-close, so it IS flagged.

**How to avoid:** Do not attempt to "fix" this — it is the correct HTML5 behavior. The annotation investigation for Phase 20 should document this as expected behavior (spec-compliant) rather than a gap. A test fixture can assert the exact behavior: squiggle on `<span>`, no squiggle on `<p>`.

**Warning signs:** Spending time trying to make `<p>` show a squiggle — this would require overriding HTML5 optional-close-tag rules in the platform HTML annotator, which is out of scope.

---

## Code Examples

Verified patterns from JAR inspection and reference plugins:

### MakoEditorHighlighter (new file)

```kotlin
// Source: adapted from RestEditorHighlighter.java (restructuredtext.jar, PyCharm 2025.2.6)
// and HbTemplateHighlighter.java (JetBrains/intellij-plugins/handlebars)
package com.schtilig.mako.lang.highlighting

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings

class MakoEditorHighlighter(
    colors: EditorColorsScheme,
    project: Project?,
    file: VirtualFile?
) : LayeredLexerEditorHighlighter(
    SyntaxHighlighterFactory.getSyntaxHighlighter(MakoLanguage, project, file)
        ?: MakoSyntaxHighlighter(),
    colors
) {
    init {
        if (project != null && file != null) {
            val templateDataLanguage =
                TemplateDataLanguageMappings.getInstance(project)?.getMapping(file)
                    ?: HTMLLanguage.INSTANCE
            val htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
                templateDataLanguage, project, file
            )
            if (htmlHighlighter != null) {
                registerLayer(
                    MakoTokenTypes.TEMPLATE_TEXT,
                    LayerDescriptor(htmlHighlighter, "")
                )
            }
        }
    }
}
```

### MakoEditorHighlighterProvider (new file)

```kotlin
// Source: adapted from RestEditorHighlighterProvider.java (restructuredtext.jar)
package com.schtilig.mako.lang.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MakoEditorHighlighterProvider : EditorHighlighterProvider {
    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        file: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter = MakoEditorHighlighter(colors, project, file)
}
```

### MakoErrorFilter (new file)

```kotlin
// Source: adapted from HbErrorFilter.java (JetBrains/intellij-plugins/handlebars/inspections)
// TemplateLanguageErrorFilter verified in com.intellij.codeInsight.highlighting package, app-client.jar
package com.schtilig.mako.lang.annotation

import com.schtilig.mako.lang.MakoFileViewProvider
import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.codeInsight.highlighting.TemplateLanguageErrorFilter
import com.intellij.psi.tree.TokenSet

class MakoErrorFilter : TemplateLanguageErrorFilter(
    TokenSet.create(
        MakoTokenTypes.EXPR_START,    // ${ — opening of ${...} expression
        MakoTokenTypes.EXPR_END,      // }  — closing of ${...} expression
        MakoTokenTypes.CONTROL_LINE   // %for, %if, %endif, %endfor lines
    ),
    MakoFileViewProvider::class.java,
    "HTML"
)
```

### plugin.xml Additions

```xml
<!-- Phase 20: Layered editor highlighter — HTML coloring in TEMPLATE_TEXT regions (HINJ-01) -->
<editorHighlighterProvider
    filetype="Mako Template"
    implementationClass="com.schtilig.mako.lang.highlighting.MakoEditorHighlighterProvider"/>

<!-- Phase 20: Suppress false-positive HTML errors near Mako expression/control line boundaries (CRCT-01, CRCT-02) -->
<highlightErrorFilter
    implementation="com.schtilig.mako.lang.annotation.MakoErrorFilter"/>
```

### CRCT Automated Test (addition to MakoFileViewProviderTest.kt)

```kotlin
fun testNoFalsePositiveHtmlErrorOnMakoExpression() {
    val file = myFixture.addFileToProject(
        "crct_expression_test.mako",
        "<div class=\"\${cls}\">Hello</div>"
    )
    myFixture.configureFromExistingVirtualFile(file.virtualFile)
    val highlights = myFixture.doHighlighting()
    // Filter out Mako's own annotations — only check for HTML-sourced errors
    val htmlErrors = highlights.filter { info ->
        info.severity == HighlightSeverity.ERROR &&
        info.description?.let { desc ->
            !desc.startsWith("Unclosed") && !desc.startsWith("Unknown Mako")
        } ?: true
    }
    assertTrue(
        "Expected no false-positive HTML errors for '\${cls}' in attribute value, got: $htmlErrors",
        htmlErrors.isEmpty()
    )
}

fun testNoFalsePositiveHtmlErrorOnMakoControlLines() {
    val file = myFixture.addFileToProject(
        "crct_control_line_test.mako",
        "%for item in items:\n<li>\${item}</li>\n%endfor\n"
    )
    myFixture.configureFromExistingVirtualFile(file.virtualFile)
    val highlights = myFixture.doHighlighting()
    val htmlErrors = highlights.filter { info ->
        info.severity == HighlightSeverity.ERROR &&
        info.description?.let { desc ->
            !desc.startsWith("Unclosed") && !desc.startsWith("Unknown Mako")
        } ?: true
    }
    assertTrue(
        "Expected no false-positive HTML errors for %for/%endfor control lines, got: $htmlErrors",
        htmlErrors.isEmpty()
    )
}
```

---

## Current State Inventory (What Phase 20 Inherits)

| Item | Status | Phase 18/19 Action | Phase 20 Action |
|------|--------|-------------------|-----------------|
| Dual PSI tree (Mako + HTML) | WORKING | Created in 18-01 | No change needed |
| HTML tag/attribute completion | WORKING | Automatic from 18-01 | Verify still working |
| Emmet in TEMPLATE_TEXT | WORKING | Automatic from 18-01 | Verify still working |
| Python injection | WORKING | Guard added in 18-01 | No change needed |
| Code folding | WORKING | No change needed | No change needed |
| Structure view | WORKING | Guard added in 19-01 | No change needed |
| HTML syntax coloring (HINJ-01) | BROKEN | `MakoSyntaxHighlighter` returns EMPTY_KEYS for TEMPLATE_TEXT — correct, but no layered highlighter | Implement `MakoEditorHighlighter` + `MakoEditorHighlighterProvider` |
| HTML error squiggles (HINJ-04) | PARTIAL | OuterLanguageElement boundaries exist; span squiggle works | Confirm `<p>` behavior is HTML5 spec-correct; add `MakoErrorFilter` |
| CSS in `<style>` (HINJ-05) | BROKEN | Platform CSS injection not firing | Verify after HINJ-01 fix; may be automatic |
| JS in `<script>` (HINJ-06) | BROKEN | Platform JS injection not firing | Verify after HINJ-01 fix; may be automatic |
| `${cls}` false positive (CRCT-01) | UNKNOWN | OuterLanguageElement boundary should prevent it in theory | Implement `MakoErrorFilter`; verify in IDE |
| `%for` false positive (CRCT-02) | UNKNOWN | Same as CRCT-01 | Implement `MakoErrorFilter`; verify in IDE |
| `viewProvider.getPsi(HTMLLanguage)` test | PASSING | `testMakoFileViewProviderProvidesHtmlPsi()` added in 18-01 | No new test needed — criterion 3 already satisfied |

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `MultiHostInjector` for HTML sub-language regions | `TemplateLanguageFileViewProvider` + `TemplateDataElementType` | Established 2013+; universally adopted | File-level HTML PSI tree vs. fragmented injection sites |
| No editor highlighter override (default `LexerEditorHighlighter` from `MakoSyntaxHighlighter`) | `LayeredLexerEditorHighlighter` with `registerLayer(TEMPLATE_TEXT, htmlHighlighter)` | Phase 20 work | TEMPLATE_TEXT regions display HTML colors at the editor render level |
| No error filter (HTML errors bleed through `OuterLanguageElement` boundaries) | `TemplateLanguageErrorFilter` via `com.intellij.highlightErrorFilter` EP | Phase 20 work | False-positive HTML errors on `${...}` and `%for`/`%if` lines suppressed |

---

## Open Questions

1. **Will HINJ-05/HINJ-06 (CSS/JS injection) work after MakoEditorHighlighter is added?**
   - What we know: The platform HTML plugin injects CSS into `<style>` elements and JS into `<script>` elements internally when it encounters those elements in an HTML PSI tree. The Phase 18 failure (Emmet firing HTML handler) may have been caused by the lack of a proper editor highlighter rather than a missing injection registrar.
   - What's unclear: Whether `LayeredLexerEditorHighlighter` alone fixes the Emmet dispatch path for CSS/JS context.
   - Recommendation: Implement `MakoEditorHighlighter` first; run `./gradlew runIde`; test CSS/JS manually before writing `HtmlScriptContentProvider` code. Do NOT write code speculatively for HINJ-05/06.

2. **Does `TemplateLanguageErrorFilter` suppress CRCT-01/CRCT-02 false positives in a BasePlatformTestCase test?**
   - What we know: `BasePlatformTestCase` registers extensions from `plugin.xml`; `com.intellij.highlightErrorFilter` is a `ProjectExtensionPointName` — it should be active.
   - What's unclear: Whether the HTML annotator that produces the false positives actually fires in `BasePlatformTestCase.doHighlighting()` or only in the running IDE. If the HTML annotator does not fire in test mode, the automated test proves the filter is registered but does not prove it suppresses real false positives.
   - Recommendation: Write the automated test; if it trivially passes (zero HTML errors because the HTML annotator is inactive in tests), treat it as structural coverage and rely on the human IDE verification for runtime confirmation.

3. **HINJ-04 partial — should the Phase 20 test assert `<p>` produces NO squiggle (HTML5 optional close)?**
   - What we know: `<p>` without a closing tag is valid HTML5; the IntelliJ HTML annotator applies this rule.
   - What's unclear: Whether the test should assert this behavior positively (documenting it as expected) or whether it should be left as a human-only verification.
   - Recommendation: Add a focused test asserting NO error on `<p>` without close tag in a `.mako` file — this documents that the partial HINJ-04 behavior is intentional and spec-correct, not a bug.

---

## Sources

### Primary (HIGH confidence)

- `pycharm-community-2025.2.6-win/lib/app-client.jar` — `javap -p -c` on `TemplateLanguageErrorFilter`, `HighlightErrorFilter`, `LayeredLexerEditorHighlighter`, `LayerDescriptor`, `EditorHighlighterProvider`; constant pool inspection confirmed EP name `"com.intellij.highlightErrorFilter"`
- `pycharm-community-2025.2.6-win/lib/app-client.jar` — `META-INF/PyCharmCorePlugin.xml` extracted and grepped: confirmed `highlightErrorFilter` EP definition (line 6708), `editorHighlighterProvider` EP definition (line 7115), and two existing `highlightErrorFilter` registrations (lines 10928-10929: `HtmlClosingTagErrorFilter`, `InjectedHtmlErrorFilter`)
- `pycharm-community-2025.2.6-win/plugins/restructuredtext/lib/restructuredtext.jar` — `javap -c` on `RestEditorHighlighter` (extends `LayeredLexerEditorHighlighter`, registers Python and DjangoTemplate layers) and `RestEditorHighlighterProvider` (implements `EditorHighlighterProvider`); `META-INF/plugin.xml` extracted confirming `editorHighlighterProvider filetype="ReST"` registration
- Existing Mako plugin codebase — `MakoFileViewProvider.kt`, `MakoFileViewProviderFactory.kt`, `MakoTokenTypes.kt`, `MakoSyntaxHighlighter.kt`, `MakoAnnotator.kt`, `MakoFileViewProviderTest.kt` — confirmed current state and what's already implemented
- `HbErrorFilter.java` fetched from `github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/inspections/HbErrorFilter.java` — confirmed `extends TemplateLanguageErrorFilter` with `TokenSet.create(OPEN, OPEN_PARTIAL, OPEN_BLOCK, OPEN_INVERSE), HbFileViewProvider.class, "HTML"` constructor call
- `HbHighlighterProvider.java` and `HbTemplateHighlighter.java` (JetBrains/intellij-plugins) — confirmed `EditorHighlighterProvider` interface + `LayeredLexerEditorHighlighter` pattern; `HbHighlighter` extends `SyntaxHighlighterBase` (not `TemplateLanguageSyntaxHighlighter` — that class does not appear to be the right mechanism)

### Secondary (MEDIUM confidence)

- `.planning/phases/18-fileviewprovider-scaffolding/18-02-SUMMARY.md` and `18-03-SUMMARY.md` — human-reported failures of HINJ-01/05/06; root causes documented; Phase 20 scope items confirmed
- `.planning/phases/19-regression-hardening/19-01-SUMMARY.md` — confirmed known limitations carried to Phase 20 (cross-injection scoping, single-line module block indent)

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| EditorHighlighterProvider pattern (HINJ-01) | HIGH | Verified from `RestEditorHighlighter.java` bytecode + `plugin.xml` registration; pattern confirmed from Handlebars plugin too |
| TemplateLanguageErrorFilter pattern (CRCT-01/02) | HIGH | Class verified in `app-client.jar` via `javap`; EP name extracted from constant pool; `HbErrorFilter` implementation confirmed; 3-arg constructor signature confirmed |
| EP registration syntax | HIGH | `highlightErrorFilter` and `editorHighlighterProvider` both verified from `PyCharmCorePlugin.xml` extracted from live JAR |
| CSS/JS auto-activation after HINJ-01 fix | LOW | Plausible based on platform HTML plugin architecture but requires empirical `runIde` verification; not confirmed by JAR inspection |
| CRCT automated test effectiveness | MEDIUM | Pattern is correct; whether HTML annotator fires in test mode is unknown — may need IDE verification |
| HINJ-04 HTML5 optional-close behavior | MEDIUM | Explanation is consistent with HTML5 spec and IntelliJ HTML annotator behavior; confirmed by human observation in 18-03 |

**Research date:** 2026-02-22
**Valid until:** 2026-04-22 (template language APIs are stable; PyCharm 2025.2 is the locked target; RST and Handlebars reference implementations are stable)

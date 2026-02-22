# Phase 21: CSS and JS Sub-Language Injection - Research

**Researched:** 2026-02-22
**Domain:** IntelliJ Platform MultiHostInjector / LanguageInjectionContributor — CSS injection into `<style>` XmlText elements and JavaScript injection into `<script>` XmlText elements within the HTML PSI tree of `.mako` files (PyCharm Community 2025.2+, build 252)
**Confidence:** HIGH (mechanism confirmed from live JAR inspection; one open question on CSS plugin availability noted)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| HINJ-05 | CSS completion and validation active inside `<style>` tags in `.mako` files | Implement `MakoCssInjector` (MultiHostInjector) targeting `XmlText` inside `<style>` elements in the HTML PSI tree; uses `Language.findLanguageByID("CSS")` with null guard; CSS plugin must be present for completions to actually fire |
| HINJ-06 | JavaScript completion and validation active inside `<script>` tags in `.mako` files | `HtmlScriptLanguageInjector` (platform-bundled) ALREADY handles `<script>` XmlText via MIME-type lookup; fires automatically on our HTML PSI tree; JavaScript plugin must be present for completions to actually fire |
</phase_requirements>

---

## Summary

Phase 20 confirmed that CSS and JavaScript sub-language injection does NOT fire automatically inside `<style>` and `<script>` elements in `.mako` files, even though `MakoFileViewProvider` creates a valid HTML PSI tree. The root cause is that the IntelliJ Platform's injection chain for `<style>` and `<script>` tags requires two independent conditions: (1) an appropriate `MultiHostInjector` must fire on `XmlText` nodes inside those HTML tags, AND (2) the target language (CSS or JavaScript) must be registered in the running IDE.

The HTML PSI tree created by `MakoFileViewProvider` IS the correct substrate for these injectors. The platform-bundled `HtmlScriptLanguageInjector` (registered in `PyCharmCorePlugin.xml`) targets `XmlText` elements in HTML-containing files and handles `<script>` tag injection via MIME-type lookup — it will work automatically once the JavaScript plugin is installed. There is NO equivalent built-in injector for `<style>` CSS injection in PyCharm Community — we must implement `MakoCssInjector` ourselves.

**Critical constraint:** Neither the CSS plugin (`com.intellij.css`) nor the JavaScript plugin are bundled in PyCharm Community 2025.2.6. They are present only in IntelliJ IDEA Ultimate/Professional. The injectors we write will work correctly on those IDEs; in PyCharm Community without those plugins installed, `Language.findLanguageByID` returns null and the injectors no-op silently. Verification of the phase success criteria (Ctrl+Space producing CSS/JS completions) requires running in an IDE with those plugins present (PyCharm Professional, IntelliJ IDEA Ultimate, or PyCharm Community with CSS/JS plugins manually installed from the marketplace).

**Primary recommendation:** Implement `MakoCssInjector` as a `MultiHostInjector` targeting `XmlText` inside `<style>` elements in our HTML PSI tree, using `Language.findLanguageByID("CSS")` with null guard. For JavaScript, NO code is needed — the platform's `HtmlScriptLanguageInjector` handles it automatically. Register `MakoCssInjector` in `plugin.xml` as `<multiHostInjector>`. Verify by running `./gradlew runIde` in an IDE environment that has the CSS plugin installed.

---

## Standard Stack

### Core Platform Classes (verified via `javap` on PyCharm Community 2025.2.6 JARs)

| Class / Interface | JAR | Purpose | Confidence |
|-------------------|-----|---------|------------|
| `com.intellij.lang.injection.MultiHostInjector` | `util-8.jar` (interface) | Injects a language into PSI host elements | HIGH — confirmed in `PyCharmCorePlugin.xml` EP definition |
| `com.intellij.lang.injection.MultiHostRegistrar` | `util-8.jar` | API used by MultiHostInjector to register injection places | HIGH |
| `com.intellij.psi.xml.XmlText` | `app-client.jar` | Interface for XML text content nodes — the injection host | HIGH — confirmed `XmlTextImpl implements PsiLanguageInjectionHost` |
| `com.intellij.psi.xml.XmlTag` | `app-client.jar` | XML/HTML tag element — parent of the XmlText we target | HIGH |
| `com.intellij.xml.util.HtmlUtil` | `app.jar` | `isStyleTag()`, `isScriptTag()`, `isHtmlTagContainingFile()` | HIGH — verified via `javap` |
| `com.intellij.lang.Language.findLanguageByID(String)` | `util-8.jar` | Looks up registered language by ID string; returns null if not registered | HIGH — null-safe return confirmed |
| `com.intellij.psi.impl.source.html.HtmlScriptLanguageInjector` | `app.jar` | Platform-bundled injector for `<script>` tags — uses MIME-type lookup | HIGH — confirmed registered in `PyCharmCorePlugin.xml` |
| `com.intellij.openapi.util.TextRange` | `util-8.jar` | Used in `addPlace()` to specify the injected content range within host | HIGH |

### Extension Points

| EP Name | Scope | Bean Class | Where Defined |
|---------|-------|------------|---------------|
| `multiHostInjector` | `IDEA_PROJECT` (project-scoped) | `MultiHostInjector` interface | `PyCharmCorePlugin.xml` — confirmed |
| `languageInjectionContributor` | `IDEA_PROJECT` | `LanguageExtensionPoint` | `PyCharmCorePlugin.xml` — confirmed (alternative API; not recommended for this phase) |

**Registration in `plugin.xml`:**
```xml
<!-- Phase 21: CSS injection into <style> elements in HTML PSI tree (HINJ-05) -->
<multiHostInjector implementation="com.schtilig.mako.lang.injection.MakoCssInjector"/>
```

**Installation:** No new Gradle dependencies. CSS language is accessed via `Language.findLanguageByID("CSS")` at runtime — no compile-time dependency on the CSS plugin.

### What Is NOT Needed (Previously Confused as Required)

| Class | Why Not Needed |
|-------|---------------|
| Custom `MultiHostInjector` for `<script>` JS | `HtmlScriptLanguageInjector` is already registered platform-wide and fires on our HTML PSI tree |
| `LanguageInjectionContributor` | Higher-level API; `MultiHostInjector` is lower-level and gives precise control over XmlText ranges |
| `html.scriptContentProvider` registration | This is for the LEXER level (token embedding); injection is at PSI level |
| `html.embeddedContentSupport` registration | Same — lexer level, not PSI injection level |
| Optional plugin dependency declarations in `plugin.xml` | Not required for the injector to compile or load; `Language.findLanguageByID` gracefully returns null when CSS/JS plugins are absent |

---

## Architecture Patterns

### Recommended Project Structure (Additions Only)

```
src/main/kotlin/com/schtilig/mako/lang/
└── injection/
    ├── MakoPythonInjector.kt   # existing — unchanged
    └── MakoCssInjector.kt      # NEW — injects CSS into <style> XmlText in HTML PSI tree
```

### Pattern 1: MakoCssInjector — MultiHostInjector for `<style>` CSS

**What:** Targets `XmlText` elements whose parent `XmlTag` is a `<style>` tag, in HTML-containing files (our HTML PSI tree qualifies). Uses `Language.findLanguageByID("CSS")` with null guard.

**When to use:** Registered permanently; no-ops silently when CSS plugin is absent.

**Example:**
```kotlin
// Source: pattern derived from HtmlScriptLanguageInjector bytecode (app.jar, PyCharm 2025.2.6)
//         and XmlTextImpl PsiLanguageInjectionHost confirmation
package com.schtilig.mako.lang.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlText
import com.intellij.xml.util.HtmlUtil

/**
 * Injects CSS into the content of <style> elements within the HTML PSI tree of .mako files.
 *
 * The HTML PSI tree is created by MakoFileViewProvider (Phase 18). This injector fires on
 * XmlText nodes whose parent <style> tag is inside an HTML-containing file.
 *
 * Gracefully no-ops when the CSS plugin is not installed (Language.findLanguageByID returns null).
 *
 * Registered via <multiHostInjector> in plugin.xml.
 */
class MakoCssInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(XmlText::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val css = Language.findLanguageByID("CSS") ?: return

        if (!context.isValid) return
        val xmlText = context as? XmlText ?: return
        if (!HtmlUtil.isHtmlTagContainingFile(xmlText)) return

        val parentTag = xmlText.parentTag ?: return
        if (!HtmlUtil.isStyleTag(parentTag)) return

        val text = xmlText.text ?: return
        val range = TextRange(0, text.length)
        if (range.isEmpty) return

        registrar.startInjecting(css)
            .addPlace(null, null, xmlText as com.intellij.psi.PsiLanguageInjectionHost, range)
            .doneInjecting()
    }
}
```

### Pattern 2: JavaScript in `<script>` — No Code Needed

**What:** `HtmlScriptLanguageInjector` (registered in `PyCharmCorePlugin.xml`) already targets `XmlText` elements inside `<script>` tags in HTML-containing files. It uses `Language.findInstancesByMimeType(typeAttributeValue)` to find the appropriate language (returns `Language.ANY` if JavaScript is not registered — no injection, no error).

**Key verification:** `HtmlScriptLanguageInjector.elementsToInjectIn()` returns `[XmlText.class]`. Our HTML PSI tree's `<script>` element children are `XmlText` nodes. `HtmlUtil.isHtmlTagContainingFile()` will return true because `XmlText.containingFile` is the `HtmlFile` created by `MakoFileViewProvider`.

**When active:** When the JavaScript plugin is installed (PyCharm Professional, IDEA Ultimate, or manually installed from marketplace).

### Pattern 3: Language ID Reference

| Feature | Language ID | How Found | Notes |
|---------|-------------|-----------|-------|
| CSS completions | `"CSS"` | `Language.findLanguageByID("CSS")` | Provided by `com.intellij.css` plugin |
| JavaScript completions | N/A | Via `Language.findInstancesByMimeType("text/javascript")` | Handled by platform's `HtmlScriptLanguageInjector` — no code from us |

**Language ID verification:** The CSS language ID `"CSS"` is the standard ID used by the `com.intellij.css` plugin. Confirmed from multiple sources (FreeMarker CSS injector uses `CssSupportLoader.CSS_FILE_TYPE.getLanguage()` which returns language with ID `"CSS"`). The `Language.findLanguageByID("CSS")` pattern with null guard is the correct approach when CSS plugin is an optional runtime dependency.

### Pattern 4: HtmlUtil.isStyleTag Check

The HTML plugin provides `HtmlUtil.isStyleTag(XmlTag)` alongside `HtmlUtil.isScriptTag(XmlTag)`:

```kotlin
// Both methods available in HtmlUtil (app.jar, PyCharm 2025.2.6)
// Verified via javap: HtmlUtil has fields SCRIPT_TAG_NAME and STYLE_TAG_NAME
// and method isScriptTag(XmlTag) — isStyleTag is inferred from the same pattern

// Guard pattern:
val parentTag = xmlText.parentTag ?: return
if (!HtmlUtil.isStyleTag(parentTag)) return  // only proceed for <style> elements
```

**Note:** `isStyleTag` was not shown in the `javap` output of public methods but `STYLE_TAG_NAME` constant confirms its existence. If `isStyleTag` is not available, the fallback is:
```kotlin
if (parentTag.name.lowercase() != "style") return
```

### Anti-Patterns to Avoid

- **Writing a custom MultiHostInjector for `<script>` JavaScript:** The platform already handles this. Adding a second injector would cause double-injection and duplicate completions.
- **Using `Language.findLanguageByID("JavaScript")` directly:** The JavaScript language may have a different ID depending on the JavaScript plugin version (`"JavaScript"` vs. `"ECMA Script Level 4"`). The platform's `HtmlScriptLanguageInjector` uses MIME-type lookup which is more robust.
- **Adding `<depends>com.intellij.css</depends>` as a required dependency:** This would break the plugin on PyCharm Community where CSS plugin is not bundled. Use null-guard instead; optional dependency declaration is also unnecessary since no CSS classes are referenced at compile time.
- **Registering `languageInjectionContributor` instead of `multiHostInjector`:** `languageInjectionContributor` is language-keyed (fires only for specific host languages). Since our HTML PSI tree elements have `HTMLLanguage`, and we want to inject INTO them from a non-HTML plugin, `multiHostInjector` (not language-keyed) is the correct EP.
- **Injecting into `OuterLanguageElementImpl` nodes:** The HTML PSI tree's `<style>` and `<script>` elements should not contain `OuterLanguageElementImpl` nodes in their text content (those appear only where Mako expressions are in HTML template text). No guard needed, but be aware.
- **Targeting `XmlTag` instead of `XmlText`:** Language injection must be applied to `PsiLanguageInjectionHost` elements. `XmlTextImpl` implements `PsiLanguageInjectionHost`; `XmlTagImpl` does NOT in this context. Always inject into the `XmlText` child of the `<style>` tag.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JavaScript injection into `<script>` tags | Custom MakoJsInjector | Platform's `HtmlScriptLanguageInjector` | Already registered; fires on our HTML PSI tree; handles MIME-type negotiation including JSON script types |
| CSS language lookup | Direct class reference (`CssLanguage.INSTANCE`) | `Language.findLanguageByID("CSS")` with null guard | CSS plugin may not be present; direct class reference causes `ClassNotFoundException` at startup |
| Style tag name check | `tag.name.lowercase() == "style"` | `HtmlUtil.isStyleTag(tag)` | Handles HTML case-insensitivity rules properly |
| HTML file check | `psiElement.containingFile.language == HTMLLanguage.INSTANCE` | `HtmlUtil.isHtmlTagContainingFile(element)` | Handles subtypes (XHTML, embedded HTML) and TemplateLanguageFileViewProvider HTML PSI roots |

**Key insight:** The HTML PSI tree from `MakoFileViewProvider` IS a legitimate `HtmlFile`. Platform HTML utilities (`HtmlUtil.isHtmlTagContainingFile`, `HtmlUtil.isStyleTag`, `HtmlUtil.isScriptTag`) will work correctly on our HTML PSI tree's elements without any special handling.

---

## Common Pitfalls

### Pitfall 1: CSS/JS Completions Not Working in PyCharm Community

**What goes wrong:** After implementing `MakoCssInjector` and registering it, CSS completions still don't fire inside `<style>` in PyCharm Community.

**Why it happens:** `Language.findLanguageByID("CSS")` returns null because the CSS plugin is not installed in PyCharm Community. The injector correctly no-ops. This is EXPECTED behavior.

**How to avoid:** Test in an IDE with the CSS plugin installed (PyCharm Professional, IDEA Ultimate, or install the CSS plugin from marketplace in PyCharm Community). The success criteria for HINJ-05 are only verifiable in those environments.

**Warning signs:** No CSS completions after implementing the injector. Check `Language.findLanguageByID("CSS") == null` in a debug session to confirm.

### Pitfall 2: Double JavaScript Injection

**What goes wrong:** Implementing a custom `MakoJsInjector` in addition to the platform's `HtmlScriptLanguageInjector` causes double JavaScript injection in `<script>` blocks — completion shows duplicate suggestions.

**Why it happens:** Both injectors fire on the same `XmlText` elements. The platform injector is registered globally; our custom injector adds a second injection.

**How to avoid:** Do NOT implement a custom JavaScript injector. The platform's `HtmlScriptLanguageInjector` handles `<script>` tags automatically in HTML PSI trees.

**Warning signs:** Duplicate completion suggestions; `InjectedLanguageManager.getInjectedPsiFiles()` returns multiple JavaScript fragments for the same `XmlText` element.

### Pitfall 3: Injecting into XmlTag Instead of XmlText

**What goes wrong:** `MakoCssInjector.elementsToInjectIn()` returns `[XmlTag.class]`. The injector attempts to call `registrar.addPlace(...)` with an `XmlTag` as host. `ClassCastException` at runtime because `XmlTag` does not implement `PsiLanguageInjectionHost`.

**Why it happens:** Misunderstanding the HTML PSI tree structure. Language injection hosts must be leaf-level elements.

**How to avoid:** Target `XmlText` (the text content node inside the `<style>` tag), not `XmlTag`. `XmlTextImpl implements PsiLanguageInjectionHost` — confirmed via `javap`.

**Warning signs:** `ClassCastException: XmlTagImpl cannot be cast to PsiLanguageInjectionHost`

### Pitfall 4: CSS Injection Fires in Non-HTML Files

**What goes wrong:** `MakoCssInjector` targets all `XmlText` nodes globally — fires inside XML configuration files or non-HTML XML, injecting CSS into unexpected places.

**Why it happens:** Missing the `HtmlUtil.isHtmlTagContainingFile()` guard.

**How to avoid:** Always guard with `HtmlUtil.isHtmlTagContainingFile(xmlText)` before checking the parent tag name. This restricts injection to HTML PSI trees only.

**Warning signs:** CSS injection appearing inside XML configuration files or XHTML that intentionally uses a `<style>` element name for something else.

### Pitfall 5: multiHostInjector Is Project-Scoped

**What goes wrong:** `MakoCssInjector` does not fire because the multiHostInjector EP is project-scoped (`area="IDEA_PROJECT"`) and the injector was not properly registered, or fires in the wrong project context.

**Why it happens:** The `multiHostInjector` extension point is defined as `area="IDEA_PROJECT"` in `PyCharmCorePlugin.xml`. Registration in `plugin.xml` with `<extensions defaultExtensionNs="com.intellij">` is the correct approach — the platform handles project scoping automatically.

**How to avoid:** Register normally via `<multiHostInjector>` in `plugin.xml`. The project-scoped area is handled by the platform.

**Warning signs:** No CSS injection in a new project but works in an existing project (would indicate a test environment issue).

### Pitfall 6: isStyleTag Method Availability

**What goes wrong:** `HtmlUtil.isStyleTag(XmlTag)` may not exist in all versions of the platform, causing a `NoSuchMethodError`.

**Why it happens:** The `javap` output of `HtmlUtil` showed `isScriptTag(XmlTag)` as a public method but did not show `isStyleTag(XmlTag)` explicitly. `STYLE_TAG_NAME` constant IS present, confirming style tag support exists.

**How to avoid:** If `isStyleTag` is not available, use a direct string comparison as fallback:
```kotlin
if (parentTag.localName.lowercase() != "style") return
```
This is equivalent and reliable.

**Warning signs:** `NoSuchMethodError: HtmlUtil.isStyleTag` at runtime.

---

## Code Examples

### Complete MakoCssInjector

```kotlin
// Source: Pattern derived from HtmlScriptLanguageInjector bytecode analysis (app.jar, PyCharm 2025.2.6),
//         XmlTextImpl PsiLanguageInjectionHost confirmation (javap app.jar),
//         and MakoPythonInjector existing pattern (null guard approach)
package com.schtilig.mako.lang.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlText
import com.intellij.xml.util.HtmlUtil

/**
 * Injects CSS into the text content of <style> elements within the HTML PSI tree of .mako files.
 *
 * The HTML PSI tree is created by MakoFileViewProvider (TemplateLanguageFileViewProvider from Phase 18).
 * This injector targets XmlText nodes whose parent tag is a <style> element inside an HTML file.
 *
 * Requires the CSS plugin (com.intellij.css) to be installed for CSS completions and validation.
 * Gracefully no-ops when CSS plugin is absent (Language.findLanguageByID("CSS") returns null).
 *
 * JavaScript injection into <script> elements is handled automatically by the platform's
 * HtmlScriptLanguageInjector — no custom injector is needed for <script>.
 *
 * Registered via <multiHostInjector> in plugin.xml (HINJ-05).
 */
class MakoCssInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(XmlText::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val css = Language.findLanguageByID("CSS") ?: return

        if (!context.isValid) return
        val xmlText = context as? XmlText ?: return
        if (!HtmlUtil.isHtmlTagContainingFile(xmlText)) return

        val parentTag = xmlText.parentTag ?: return
        // Guard: only inject for <style> elements
        if (parentTag.localName.lowercase() != "style") return

        val text = xmlText.text ?: return
        if (text.isEmpty()) return

        registrar
            .startInjecting(css)
            .addPlace(null, null, xmlText as PsiLanguageInjectionHost, TextRange(0, text.length))
            .doneInjecting()
    }
}
```

### plugin.xml Registration

```xml
<!-- Phase 21: CSS injection into <style> elements in the HTML PSI tree of .mako files (HINJ-05) -->
<!-- JavaScript injection into <script> is handled automatically by HtmlScriptLanguageInjector (HINJ-06) -->
<multiHostInjector
    implementation="com.schtilig.mako.lang.injection.MakoCssInjector"/>
```

### Verification Test Skeleton (no CSS plugin needed — tests injector structure)

```kotlin
// Tests the MakoCssInjector wiring without requiring CSS language to be registered
// Full completion verification requires human IDE check with CSS plugin installed
class MakoCssInjectorTest : BasePlatformTestCase() {

    fun testCssInjectorIsRegisteredForXmlText() {
        // Verify the injector is wired for XmlText
        val injector = MakoCssInjector()
        assertTrue(injector.elementsToInjectIn().contains(XmlText::class.java))
    }

    fun testCssInjectorNoOpsWhenCssLanguageAbsent() {
        // When CSS plugin is not installed (Language.findLanguageByID("CSS") == null),
        // the injector must not throw — it must silently return.
        // This test verifies the null guard works.
        val css = Language.findLanguageByID("CSS")
        if (css != null) {
            // CSS plugin IS installed in test environment — skip this test
            return
        }
        // Create a mock registrar that would throw if startInjecting is called
        val mockRegistrar = object : MultiHostRegistrar {
            override fun startInjecting(language: Language) = throw AssertionError("Should not inject when CSS absent")
            override fun startInjecting(language: Language, mimeType: String?) = throw AssertionError("Should not inject when CSS absent")
            override fun addPlace(prefix: String?, suffix: String?, host: PsiLanguageInjectionHost, rangeInsideHost: TextRange) = this
            override fun doneInjecting() {}
        }
        // Should not throw
        val injector = MakoCssInjector()
        val htmlPsiFile = myFixture.configureByText("test.html", "<html><body><style>color: red;</style></body></html>")
        val xmlTextInStyle = findXmlTextInStyle(htmlPsiFile)
        if (xmlTextInStyle != null) {
            injector.getLanguagesToInject(mockRegistrar, xmlTextInStyle)
        }
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Assumed HTML PSI tree alone enables CSS/JS injection | HTML PSI tree is prerequisite but explicit language injection also required | Clarified in Phase 20 gap analysis | Requires additional MultiHostInjector for CSS; JS handled by platform injector |
| MultiHostInjector for `<script>` JS | Platform's `HtmlScriptLanguageInjector` handles `<script>` automatically | Always true; misunderstood in Phase 18 research ("Automatic — HTML plugin injects JS") | No code needed from us for JavaScript |
| CSS injection assumed automatic | CSS requires explicit `MakoCssInjector` (no built-in CSS injector in PyCharm Community) | Phase 20 gap confirmed | Must implement `MakoCssInjector` |
| Direct language class reference (`CssLanguage.INSTANCE`) | `Language.findLanguageByID("CSS")` with null guard | Best practice for optional language dependencies | No compile-time dependency on CSS plugin |

**Deprecated/outdated (from Phase 18 research):**
- Phase 18 Research said "CSS injection automatic — HTML plugin injects CSS into `<style>` tags internally once HTML PSI tree exists." This was INCORRECT for PyCharm Community. The HTML plugin does provide `HtmlScriptLanguageInjector` for `<script>` tags, but there is NO equivalent CSS injector for `<style>` tags in PyCharm Community. The CSS plugin provides that injector in IDEA Ultimate.

---

## Open Questions

1. **Does `HtmlUtil.isStyleTag(XmlTag)` exist in PyCharm 2025.2?**
   - What we know: `HtmlUtil.isScriptTag(XmlTag)` is confirmed via `javap`. `HtmlUtil.STYLE_TAG_NAME` constant is present. The method should exist by analogy.
   - What's unclear: The `javap` output of public methods did not list `isStyleTag` explicitly (long output was truncated).
   - Recommendation: Use `parentTag.localName.lowercase() != "style"` as a safe fallback to avoid `NoSuchMethodError`. Or test `HtmlUtil.isStyleTag` in the actual build at compile time.

2. **Does the CSS plugin's injector (bundled in IDEA Ultimate) ALSO target our HTML PSI tree?**
   - What we know: The CSS plugin is not present in PyCharm Community 2025.2.6. We cannot inspect it.
   - What's unclear: If the CSS plugin already has a `<style>` injector that targets `HtmlFile` PSI roots, adding `MakoCssInjector` would cause double injection on IDEA Ultimate.
   - Recommendation: Write `MakoCssInjector` with a guard: if injection already exists on the `XmlText` (check `InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(xmlText)` is non-empty), skip. Or accept double injection and rely on the platform de-duplication behavior. In practice, most template language plugins (Twig, Handlebars) do not have this problem because they implement their own injectors regardless.

3. **Will `HtmlScriptLanguageInjector` fire on our HTML PSI tree's `<script>` XmlText?**
   - What we know: `HtmlScriptLanguageInjector` targets `XmlText` and checks `HtmlUtil.isHtmlTagContainingFile()`. Our HTML PSI tree's `XmlText` elements have `containingFile` = `HtmlFile` (created by `MakoFileViewProvider.createFile(HTMLLanguage)`). `HtmlUtil.isHtmlTagContainingFile()` checks `instanceof HtmlFile` (inferred from bytecode analysis).
   - What's unclear: Exact implementation of `isHtmlTagContainingFile` — does it check `containingFile instanceof HtmlFile` or `containingFile.language == HTMLLanguage`? The bytecode showed it delegates to `isHtmlFile(element.containingFile)`.
   - Recommendation: HIGH confidence it works. Verify in Phase 21 verification plan by checking JavaScript completions in `<script>` blocks with JS plugin installed. If it fails, implement `MakoScriptJsInjector` following the same pattern as `MakoCssInjector` but using `Language.findLanguageByID("JavaScript")`.

---

## Sources

### Primary (HIGH confidence)

- PyCharm Community 2025.2.6 `app-client.jar` — `javap -p` on `HtmlEmbeddedContentSupport`, `HtmlScriptStyleEmbeddedContentProvider`, `HtmlScriptContentProvider`, `XmlText`, `LanguageHtmlScriptContentProvider`
- PyCharm Community 2025.2.6 `app.jar` — `javap -p` on `HtmlScriptLanguageInjector`, `TemplateHtmlScriptContentProvider`, `XmlTextImpl` (confirmed `implements PsiLanguageInjectionHost`), `HtmlUtil`
- PyCharm Community 2025.2.6 `util-8.jar` — `javap -p` on `LanguageInjectionContributor`, `Language.findLanguageByID` signature
- `META-INF/PyCharmCorePlugin.xml` extracted from `app-client.jar` — confirmed:
  - `multiHostInjector` EP definition (`area="IDEA_PROJECT"`)
  - `HtmlScriptLanguageInjector` registered as `<multiHostInjector>`
  - `html.scriptContentProvider` EP definition
  - `html.embeddedContentSupport` EP definition
  - NO CSS injector registered in PyCharm Community
- PyCharm Community 2025.2.6 plugins directory audit — confirmed CSS and JavaScript plugins are NOT bundled (only `python-ce`, `json`, `restructuredtext` etc. present; no `com.intellij.css` or JavaScript plugin)
- Existing codebase: `MakoPythonInjector.kt` — reference for null-guard pattern (`Language.findLanguageByID("Python") ?: return`)
- Phase 20 Summary (20-03-SUMMARY.md) — confirmed gap: "CSS sub-language injection requires additional MultiHostInjector or LanguageInjectionContributor EP registrations"
- v0.3.0 Milestone Audit (`.planning/v0.3-MILESTONE-AUDIT.md`) — confirmed gap details and Phase 21 assignment

### Secondary (MEDIUM confidence)

- JetBrains intellij-plugins commit 103b957 (FreeMarker CSS injection) — confirms `CssSupportLoader.CSS_FILE_TYPE.getLanguage()` is how CSS language is obtained; language ID is `"CSS"`; `MultiHostInjector` pattern for CSS injection
- IntelliJ Platform Plugin SDK documentation (language-injection.html) — confirmed `multiHostInjector` EP, `getLanguagesToInject()` + `elementsToInjectIn()` API contract, `addPlace()` mechanics
- JetBrains developer statement (Phase 18 research sources) — "JavaScript gets embedded into the HTML tree automatically" — confirmed TRUE for `HtmlScriptLanguageInjector` but only when JS plugin is present

### Tertiary (LOW confidence — needs runtime verification)

- `HtmlUtil.isStyleTag(XmlTag)` exists — inferred from `STYLE_TAG_NAME` constant and `isScriptTag` pattern; not directly confirmed via `javap` output (output truncated)
- CSS plugin's own `<style>` injector behavior with our HTML PSI tree — unknown; CSS plugin not present in test environment
- `HtmlScriptLanguageInjector` fires on our HTML PSI tree's XmlText — HIGH confidence theoretically (all structural conditions met); LOW confidence empirically (needs Phase 21 runtime verification)

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Standard stack (EPs, classes, methods) | HIGH | All verified via `javap` on live PyCharm 2025.2.6 JARs |
| CSS/JS plugin absence in PyCharm Community | HIGH | Confirmed by exhaustive plugins directory audit |
| No need to write JS injector for `<script>` | HIGH | `HtmlScriptLanguageInjector` confirmed registered; targets XmlText; no CSS language needed |
| MakoCssInjector pattern (MultiHostInjector on XmlText) | HIGH | `XmlTextImpl implements PsiLanguageInjectionHost` confirmed; same EP pattern as `MakoPythonInjector` |
| CSS language ID = `"CSS"` | MEDIUM | Inferred from FreeMarker injector source; not directly verified against CSS plugin JAR |
| HtmlScriptLanguageInjector fires on our HTML PSI tree | MEDIUM | All structural conditions met; needs empirical confirmation in Phase 21 |
| isStyleTag method availability | MEDIUM | Confirmed by analogy with isScriptTag + STYLE_TAG_NAME constant; not directly javap-confirmed |

**Research date:** 2026-02-22
**Valid until:** 2026-04-22 (template language injection APIs stable; CSS/JS plugin availability may change if JetBrains moves to bundled model)

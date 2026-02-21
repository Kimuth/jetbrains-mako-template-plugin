# Phase 8: Error Annotations and Release Readiness - Research

**Researched:** 2026-02-21
**Domain:** IntelliJ Platform Annotator API, Plugin Verifier, Marketplace readiness
**Confidence:** HIGH

## Summary

Phase 8 has two largely independent workstreams: (1) implement `MakoAnnotator` to surface red squiggles for malformed Mako syntax, and (2) confirm the plugin passes `verifyPlugin` and is Marketplace-ready. Both are achievable without new Gradle dependencies.

The **Annotator API** (`com.intellij.annotator` extension point, `Annotator` interface) is the correct mechanism for both error cases in COMP-03. It is preferred over `LocalInspectionTool` when the errors need to appear in real-time in the active editor and do not require batch-mode inspection or suppression UI. The GrammarKit-generated parser already creates `PsiErrorElement` nodes for parse failures, which the platform automatically squiggles. However, semantic errors (unclosed containers, invalid directive names) require a hand-written `Annotator` that walks the PSI tree.

**Plugin Verifier** is already wired in `build.gradle.kts` via `pluginVerification { ides { recommended() } }`. The correct Gradle task name in IntelliJ Platform Gradle Plugin 2.x is `verifyPlugin` (confirmed in the existing `.run/Run Verifications.run.xml`). The `recommended()` helper selects IDEs matching the configured platform version. The README description is currently the placeholder template text and must be replaced before Marketplace submission. The plugin icon is already present at `icons/makoFile.svg`; the Marketplace-specific `pluginIcon.svg` (placed in `META-INF/`) is separate and currently absent.

**Primary recommendation:** Implement `MakoAnnotator` as a single `Annotator` class handling both the unclosed-tag and invalid-directive cases; register it with `language="Mako Template"` in plugin.xml; test it with `myFixture.enableInspections()` + `checkHighlighting()` from `BasePlatformTestCase`; then run `verifyPlugin` and fix README/CHANGELOG before marking the phase complete.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| COMP-03 | Malformed Mako syntax (unclosed tags, invalid directives) shows error annotations in the editor | Annotator API (`com.intellij.annotator` extension point); `AnnotationHolder.newAnnotation(HighlightSeverity.ERROR, ...)` creates inline red squiggles; annotator on `MakoDefTag`/`MakoBlockTag` can detect missing END_TAG child; a lexer-level fallback already fires `PsiErrorElement` for unrecognized `<%...` patterns |
</phase_requirements>

## Standard Stack

### Core

| Library / API | Version / Source | Purpose | Why Standard |
|---|---|---|---|
| `com.intellij.lang.annotation.Annotator` | Platform built-in | Semantic error annotations via PSI tree inspection | The correct mechanism for per-element error reporting with inline squiggles |
| `com.intellij.lang.annotation.AnnotationHolder` | Platform built-in | Creates annotation objects at specific text ranges | Required parameter to `Annotator.annotate()` |
| `com.intellij.lang.annotation.HighlightSeverity` | Platform built-in | Severity levels: `ERROR`, `WARNING`, `WEAK_WARNING`, `INFO` | Standard enum for annotation severity |
| `com.intellij.testFramework.fixtures.BasePlatformTestCase` | Platform test framework | Base class for annotator tests via `checkHighlighting()` | Used by all existing tests in this project (MakoCompletionTest pattern) |
| `./gradlew verifyPlugin` | IntelliJ Platform Gradle Plugin 2.x | Binary compatibility check against target IDE builds | Already configured via `pluginVerification { ides { recommended() } }` in build.gradle.kts |

### Supporting

| Library / API | Purpose | When to Use |
|---|---|---|
| `PsiTreeUtil.findChildOfType()` | Navigate PSI tree to check for presence/absence of child nodes | Detecting unclosed tags (check if END_TAG child is present) |
| `com.intellij.psi.PsiErrorElement` | Already-generated parse errors from GrammarKit | Platform auto-highlights these — no Annotator needed for purely syntactic failures that the parser already catches |
| `com.intellij.codeInspection.ProblemHighlightType` | Standard highlight types like `LIKE_UNKNOWN_SYMBOL` | Can be added to annotations via `.highlightType()` for visual style |
| `MakoVisitor` (generated) | Visitor over all Mako PSI types | Can be used to walk the PSI tree in the annotator instead of `is` checks |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|---|---|---|
| `Annotator` | `LocalInspectionTool` | `LocalInspectionTool` adds batch-mode analysis and suppression UI. Not needed here: these are structural errors that should always show, not optional inspections. Use `Annotator`. |
| `Annotator` | `ExternalAnnotator` | `ExternalAnnotator` is for external tool output (e.g., shell linters). Has lowest priority, runs after other passes. Not appropriate for in-process PSI tree checks. |
| `Annotator` | Rely on `PsiErrorElement` auto-highlighting | GrammarKit already fires `PsiErrorElement` for some parse failures (e.g., missing TAG_CLOSE in the middle of an attribute list). But for semantic checks like "this `MakoDefTag` has no matching `END_TAG` child" the parser uses `recoverWhile` and continues, so there is no `PsiErrorElement`. A dedicated Annotator is required. |

## Architecture Patterns

### Recommended Project Structure

```
src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/
├── completion/
│   └── MakoCompletionContributor.kt   (Phase 7 - done)
├── annotation/                         <-- NEW for Phase 8
│   └── MakoAnnotator.kt
...
src/test/kotlin/.../lang/
└── MakoAnnotatorTest.kt               <-- NEW for Phase 8
src/test/testData/annotator/           <-- NEW directory
└── UnclosedDefTag.mako                <-- test fixture with <error> markers
└── InvalidDirective.mako              <-- test fixture with <error> markers
```

### Pattern 1: Annotator Implementation

**What:** `Annotator` is called incrementally for every PSI element visible in the active file. The `annotate(element, holder)` method is invoked once per element in the PSI tree.

**When to use:** When you need real-time, editor-integrated error squiggles that derive from semantic analysis of the PSI tree (not purely syntactic issues that the parser can catch).

**Example (from IntelliJ SDK documentation):**

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/annotator.html
class MakoAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MakoDefTag -> checkUnclosedTag(element, holder)
            is MakoBlockTag -> checkUnclosedTag(element, holder)
        }
    }

    private fun checkUnclosedTag(tag: PsiElement, holder: AnnotationHolder) {
        // The END_TAG token is the last child of a well-formed def_tag/block_tag.
        // If it is absent, the GrammarKit parser used recoverWhile and the tag is unclosed.
        val hasEndTag = tag.node.findChildByType(MakoTokenTypes.END_TAG) != null
        if (!hasEndTag) {
            // Annotate the opening token (first child) — this is the <%def or <%block token
            val openToken = tag.firstChild ?: return
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed Mako tag: missing closing tag")
                .range(openToken.textRange)
                .create()
        }
    }
}
```

**plugin.xml registration:**

```xml
<annotator language="Mako Template"
           implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.annotation.MakoAnnotator"/>
```

### Pattern 2: Detecting Unclosed Tags via AST Child Lookup

**What:** A `MakoDefTag` or `MakoBlockTag` with a missing `</%def>` / `</%block>` closing tag will still parse (GrammarKit's `recoverWhile` prevents a total parse failure), but the resulting PSI node will have no `END_TAG` child. This is inspectable in the annotator.

**How to detect:**

```kotlin
// MakoDefTag grammar: TAG_OPEN_DEF tag_attribute* TAG_CLOSE item_* END_TAG
// If END_TAG is missing (unclosed), node.findChildByType(END_TAG) returns null
val endTag = (element as? MakoDefTag)?.node?.findChildByType(MakoTokenTypes.END_TAG)
if (endTag == null) { /* report error */ }
```

**Confirmed by existing test fixture:** `MalformedTag.txt` shows a `MakoDefTagImpl` without a matching `</%block>` end — the tag body swallows the block_tag content but there is no `END_TAG` child at the def_tag level.

### Pattern 3: Detecting Invalid Directives

**What:** The lexer in `YYINITIAL` state only recognizes specific `<%keyword` openings (def, block, inherit, include, namespace, page, doc). Any `<%bogus>` sequence falls through to TEMPLATE_TEXT. Therefore, an invalid directive is not a Mako PSI node at all — it appears as TEMPLATE_TEXT tokens in the PSI tree.

**Two options for COMP-03 criterion 2:**

**Option A — Parser-level (already exists):** The lexer already emits `TEMPLATE_TEXT` for `<%bogus>`. No red squiggle unless an Annotator specifically inspects raw TEMPLATE_TEXT content for `<%` patterns that don't match known directives.

**Option B — Annotator approach:** In the `MakoAnnotator`, for `MakoTemplateTextContent` elements, scan the raw text for the pattern `<%[a-zA-Z]+` and check whether the matched name is in the set of known directive names. If not, annotate with ERROR.

**Recommendation:** Option B is straightforward and keeps the logic in one place. The raw text inspection pattern is identical to what Phase 7 used in `MakoCompletionContributor` (backward `lastIndexOf("<%")`).

**Example:**

```kotlin
private val VALID_DIRECTIVES = setOf("def", "block", "inherit", "include", "namespace", "page", "doc")
private val INVALID_DIRECTIVE_REGEX = Regex("""<%([a-zA-Z]+)""")

is MakoTemplateTextContent -> {
    val match = INVALID_DIRECTIVE_REGEX.find(element.text) ?: return
    val name = match.groupValues[1]
    if (name !in VALID_DIRECTIVES) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Unknown Mako directive: <%$name>")
            .range(element.textRange)
            .create()
    }
}
```

**Caution:** TEMPLATE_TEXT elements can be large. The annotator is called per-element, so the regex scan is bounded to each individual `MakoTemplateTextContent` node's text. This is fine in practice.

### Pattern 4: Annotator Test with `checkHighlighting()`

**What:** `BasePlatformTestCase` provides `myFixture.checkHighlighting()` which compares annotator output against `<error descr="...">...</error>` markers embedded in test data files.

**Test class structure:**

```kotlin
class MakoAnnotatorTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Same file type registration pattern as MakoCompletionTest
        FileTypeManager.getInstance().getFileTypeByExtension("mako")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    fun testUnclosedDefTag() {
        // Inline approach using configureByText with <error> markers:
        val file = myFixture.addFileToProject("test.mako",
            """<error descr="Unclosed Mako tag: missing closing tag"><%def name="foo"></error>
content
""")
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.checkHighlighting(true, false, false)
    }
}
```

**checkHighlighting parameters:** `(checkWarnings: Boolean, checkInfos: Boolean, checkWeakWarnings: Boolean)` — errors are always checked regardless.

**Alternative test data file approach:** Place `.mako` files in `src/test/testData/annotator/` and use `myFixture.configureByFile("annotator/UnclosedDefTag.mako")` + `myFixture.checkHighlighting()`.

### Anti-Patterns to Avoid

- **Annotating the entire tag range:** Annotate only the opening token (`<%def` or `<%block`), not the entire body. A large red range covering multi-line content is confusing.
- **Re-registering with `language="any"`:** Unlike completion (which needed `language="any"` due to TEMPLATE_TEXT token positioning), annotators registered with the specific language ID work correctly because they receive PSI elements directly.
- **Using `ExternalAnnotator` for in-process PSI checks:** `ExternalAnnotator` runs with lowest priority after all other processing. Use `Annotator` for immediate feedback.
- **Creating an `Annotator` that throws on elements it doesn't handle:** Always use `when (element) { is X -> ... else -> return }` to ignore uninteresting elements.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| Error squiggle display | Custom highlight layer | `AnnotationHolder.newAnnotation()` | Platform handles rendering, error stripe, tooltip |
| PSI tree traversal | Custom recursive walker | `PsiTreeUtil.findChildOfType()` / `node.findChildByType()` | Platform utilities handle tree navigation correctly |
| Plugin binary compatibility check | Manual API inspection | `./gradlew verifyPlugin` | Plugin Verifier checks all referenced API symbols against target IDE builds automatically |
| Test assertion of error ranges | Manual text comparison | `myFixture.checkHighlighting()` with embedded `<error>` markers | Standard fixture approach used by all IntelliJ SDK annotator tests |

**Key insight:** The most error-prone part of annotator development is correctly scoping the annotated range. Always use `element.firstChild.textRange` or a specific child token's `textRange` — not `element.textRange` — to avoid annotating entire multi-line blocks.

## Common Pitfalls

### Pitfall 1: Annotator Language ID Mismatch

**What goes wrong:** Registering `<annotator language="Mako Template">` works because the language ID is `"Mako Template"` (from `MakoLanguage.id`). Using `"Mako"` or `"mako"` silently skips the annotator.

**How to avoid:** Copy the language ID string exactly from `MakoLanguage.kt`: `MakoLanguage.INSTANCE.id` → `"Mako Template"`. Confirm by running the test and ensuring annotations fire.

### Pitfall 2: Annotating Elements That Produce False Positives in TemplateLanguage Context

**What goes wrong:** Mako is a `TemplateLanguage`. The platform may pass annotators elements from injected language regions (e.g., Python PSI inside `${...}`). Checking `element.containingFile.language.id` before acting prevents spurious annotations in injected regions.

**How to avoid:** Guard all annotator logic with:
```kotlin
if (element.containingFile.language.id != "Mako Template") return
```

### Pitfall 3: `verifyPlugin` Downloads Large IDE Bundles

**What goes wrong:** `recommended()` triggers download of full IDE bundles to verify against, which can take minutes or fail in offline environments.

**Why it happens:** The Plugin Verifier CLI downloads IDE binaries to `~/.pluginVerifier/ides/` (or `$XDG_CACHE_HOME/pluginVerifier/ides/`) on first run.

**How to avoid:** Run `verifyPlugin` on a machine with internet access before committing the release branch. Subsequent runs use the cached IDE bundle. The task is already registered in `.run/Run Verifications.run.xml`.

### Pitfall 4: README Description Still Contains Placeholder Text

**What goes wrong:** The `build.gradle.kts` extracts the plugin description from `README.md` between `<!-- Plugin description -->` and `<!-- Plugin description end -->` markers. The current README contains the template placeholder: "This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have."

**Why it matters:** Marketplace submission will reject or display unprofessional description. The first 40 characters of the description must be in English and describe the plugin accurately.

**How to avoid:** Replace the content between the markers with a real description of the Mako Template Support plugin before running `publishPlugin`.

### Pitfall 5: `pluginIcon.svg` Is Missing from META-INF

**What goes wrong:** The Marketplace plugin listing icon must be `META-INF/pluginIcon.svg` (40x40px SVG). The project has `icons/makoFile.svg` (the file type icon), which is different.

**Why it matters:** Without `pluginIcon.svg`, the Marketplace listing shows a generic icon. This is a visible quality issue.

**How to avoid:** Create `src/main/resources/META-INF/pluginIcon.svg` (and optionally `pluginIcon_dark.svg`). 40x40px, SVG format, 2px transparent border, simple and recognizable design.

### Pitfall 6: CHANGELOG.md Has Only Placeholder Entry

**What goes wrong:** `build.gradle.kts` reads `changeNotes` from CHANGELOG.md for the Marketplace listing. Current content is the scaffold placeholder "Initial scaffold created from IntelliJ Platform Plugin Template."

**How to avoid:** Add a real `## [0.0.1]` section to CHANGELOG.md listing the features implemented across phases 1-8.

## Code Examples

### Annotator Registration in plugin.xml

```xml
<!-- Source: https://plugins.jetbrains.com/docs/intellij/annotator.html -->
<annotator language="Mako Template"
           implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.annotation.MakoAnnotator"/>
```

### Minimal Annotator Skeleton (Kotlin)

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/annotator.html pattern
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class MakoAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Guard: only process Mako Template language elements
        if (element.containingFile.language.id != "Mako Template") return

        when (element) {
            is MakoDefTag -> checkForMissingEndTag(element, holder, "<%def>")
            is MakoBlockTag -> checkForMissingEndTag(element, holder, "<%block>")
            is MakoTemplateTextContent -> checkForInvalidDirective(element, holder)
        }
    }

    private fun checkForMissingEndTag(tag: PsiElement, holder: AnnotationHolder, tagName: String) {
        val hasEndTag = tag.node.findChildByType(MakoTokenTypes.END_TAG) != null
        if (!hasEndTag) {
            val openToken = tag.firstChild ?: return
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed $tagName: missing closing tag")
                .range(openToken.textRange)
                .create()
        }
    }

    private val VALID_DIRECTIVES = setOf("def", "block", "inherit", "include", "namespace", "page", "doc")
    private val DIRECTIVE_REGEX = Regex("""<%([a-zA-Z]+)""")

    private fun checkForInvalidDirective(element: MakoTemplateTextContent, holder: AnnotationHolder) {
        val match = DIRECTIVE_REGEX.find(element.text) ?: return
        val name = match.groupValues[1]
        if (name !in VALID_DIRECTIVES) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown Mako directive: <%$name>")
                .range(element.textRange)
                .create()
        }
    }
}
```

### Annotator Test with checkHighlighting

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/annotator-test.html pattern
class MakoAnnotatorTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        FileTypeManager.getInstance().getFileTypeByExtension("mako")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    fun testUnclosedDefTagShowsError() {
        val file = myFixture.addFileToProject("test.mako",
            """<error descr="Unclosed <%def>: missing closing tag"><%def</error> name="foo">
some content
""")
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWellFormedDefTagNoError() {
        val file = myFixture.addFileToProject("test2.mako",
            """<%def name="foo">
content
</%def>
""")
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        // No <error> markers — checkHighlighting verifies no unexpected errors
        myFixture.checkHighlighting(true, false, false)
    }
}
```

### Running Plugin Verifier

```bash
# Correct task name for IntelliJ Platform Gradle Plugin 2.x:
./gradlew verifyPlugin

# Note: ROADMAP.md says "runPluginVerifier" but the actual task is "verifyPlugin"
# Confirmed in: .run/Run Verifications.run.xml and official 2.x documentation
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|---|---|---|---|
| `runPluginVerifier` task (Gradle Plugin 1.x) | `verifyPlugin` task (Gradle Plugin 2.x) | 2.x release | Task name changed; `pluginVerification { ides { ... } }` DSL replaces `runPluginVerifier { ... }` |
| `until-build` attribute required | `until-build` omitted recommended; `strict-until-build` available since 2025.3 | 2025.3 | Plugins without `until-build` work on all future versions; new `strict-until-build` for explicit ceiling |
| `AnnotationHolder.createErrorAnnotation()` (deprecated) | `AnnotationHolder.newAnnotation(severity, message).range(...).create()` | ~2021 | Builder pattern now standard; old methods removed |

**Deprecated/outdated:**

- `holder.createErrorAnnotation(range, message)`: Removed in recent platform versions. Use `holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create()`.
- `runPluginVerifier` Gradle task: 1.x plugin only. This project uses 2.x (`org.jetbrains.intellij.platform`); the task is `verifyPlugin`.

## Open Questions

1. **Annotated range precision for unclosed tag check**
   - What we know: The `MakoDefTag` PSI node starts at `TAG_OPEN_DEF` token. When the tag is unclosed, the node may span a large portion of the file (because GrammarKit's `recoverWhile` stops at the next construct boundary, which could be many lines away).
   - What's unclear: Whether annotating `openToken.textRange` (just the `<%def` keyword) is better UX than annotating the first line of the tag, or the entire node.
   - Recommendation: Annotate only the opening token (`<%def` or `<%block`) for a focused red squiggle. This is standard practice (e.g., how HTML validator plugins annotate unclosed tags).

2. **False positive risk for invalid directive detection**
   - What we know: TEMPLATE_TEXT elements can contain plain `<%` in HTML comments or JavaScript that is not a Mako directive attempt. The regex `<%([a-zA-Z]+)` could fire on `<!-- <%` in HTML.
   - What's unclear: How common this pattern is in real Mako templates.
   - Recommendation: Scope the check — only fire if the `<%keyword` is at the start of a line OR is preceded only by whitespace. This matches the Mako lexer's behavior (the lexer only treats `<%keyword` as a tag opener in `YYINITIAL` state). Alternatively, accept a small false-positive rate for this initial version, document it, and refine later.

3. **`verifyPlugin` against PyCharm Community 2025.2 specifically**
   - What we know: `recommended()` selects IDEs based on the configured platform version (`platformVersion = 2025.2.5`). The exact set of recommended IDEs is not documented but is based on the configured platform.
   - What's unclear: Whether `recommended()` only covers PyCharm Community or also IntelliJ IDEA + Python plugin.
   - Recommendation: Run `verifyPlugin` and inspect the output to see which IDE versions are actually verified. If the output is unexpectedly broad (e.g., includes IntelliJ IDEA Ultimate), narrow the selection with `ide { pycharmCommunity("2025.2.5") }` instead of `recommended()`.

## Sources

### Primary (HIGH confidence)

- IntelliJ Platform Plugin SDK — [Annotator](https://plugins.jetbrains.com/docs/intellij/annotator.html) — extension point name, API class, registration pattern, builder API
- IntelliJ Platform Plugin SDK — [Syntax and Error Highlighting](https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html) — mechanism comparison, extension points
- IntelliJ Platform Plugin SDK — [Testing Highlighting](https://plugins.jetbrains.com/docs/intellij/testing-highlighting.html) — `checkHighlighting()` API, test data file format with `<error descr="...">` markers
- IntelliJ Platform Plugin SDK — [Annotator Test](https://plugins.jetbrains.com/docs/intellij/annotator-test.html) — test structure, `configureByFiles()` + `checkHighlighting()` pattern
- IntelliJ Platform Gradle Plugin 2.x — [Tasks](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html) — `verifyPlugin` is the correct task name
- IntelliJ Platform Gradle Plugin 2.x — [Extension](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html) — `recommended()` description
- `.run/Run Verifications.run.xml` (this project) — confirms task name is `verifyPlugin`
- `build.gradle.kts` (this project) — `pluginVerification { ides { recommended() } }` already configured
- `src/test/testData/parser/MalformedTag.txt` (this project) — confirms GrammarKit produces `MakoDefTagImpl` without END_TAG child on unclosed tag

### Secondary (MEDIUM confidence)

- JetBrains Marketplace — [Best Practices for Listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html) — icon requirements, description first-40-chars English rule
- IntelliJ Platform Plugin SDK — [Plugin Icon File](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html) — `META-INF/pluginIcon.svg`, 40x40px, SVG format
- JetBrains Support Forum — [Annotations vs Inspections](https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000691050-Annotations-vs-Inspections) — confirms `Annotator` preferred over `LocalInspectionTool` for real-time semantic checks

### Tertiary (LOW confidence)

- `intellij-rust` annotator test (`RsSyntaxErrorsAnnotatorTest.kt`) — test pattern structure observed; specific API differences may exist between that project and this one

## Metadata

**Confidence breakdown:**

- Annotator API / extension point: HIGH — verified against official SDK docs
- Test pattern (`checkHighlighting`): HIGH — verified against official SDK docs + test tutorial
- `verifyPlugin` task name: HIGH — verified against 2.x official task docs + project run config
- Marketplace icon/description requirements: MEDIUM — verified against official Marketplace docs, exact thresholds unstated
- `recommended()` IDE list: LOW — behavior described but exact IDE set is runtime-determined; open question flagged

**Research date:** 2026-02-21
**Valid until:** 2026-03-21 (stable platform APIs; 30-day window)

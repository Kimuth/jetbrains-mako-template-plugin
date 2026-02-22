# Feature Research

**Domain:** HTML language injection in a JetBrains Mako template plugin (v0.3.0 milestone)
**Researched:** 2026-02-22
**Confidence:** MEDIUM (IntelliJ Platform TemplateLanguageFileViewProvider behavior confirmed from multiple official JetBrains sources and community reference implementations; specific CSS/JS sub-injection behavior confirmed from JetBrains developer statements; Emmet behavior in template data language context confirmed from Pebble plugin documentation)

---

## Research Notes

**Scope:** This document covers features unlocked by HTML language injection only. It assumes the existing plugin (v0.2.0) already provides: file type recognition, lexer, parser, PSI tree, Python injection, syntax highlighting, code folding, structure view, tag completion, and error annotations. This research answers: "When HTML injection is added, what do users gain, what needs explicit implementation work, and what should be avoided?"

**How TemplateLanguageFileViewProvider works (architecture prerequisite):**
The platform's `TemplateLanguageFileViewProvider` (implemented via `MultiplePsiFilesPerDocumentFileViewProvider`) creates two PSI trees from the same document bytes:
1. The Mako PSI tree — already exists; contains all nodes (MakoFile, MakoDefTag, MakoExpression, MakoCodeBlock, TEMPLATE_TEXT leaves, etc.)
2. An HTML PSI tree — built by parsing only the TEMPLATE_TEXT tokens; Mako-specific regions appear as opaque `OuterLanguageElement` leaves in this tree

With `TemplateDataElementType` wiring the TEMPLATE_TEXT tokens into the HTML parser, the HTML plugin's full feature set activates for those regions. The Mako plugin provides this wiring; the HTML plugin provides the features.

**Sources used:**
- JetBrains community tutorial: "Tutorial: Custom templating language plugin" (intellij-support.jetbrains.com)
- JetBrains developer statement on TemplateDataElementType: "you'll get that for almost free" — confirms automatic OuterLanguageElement insertion
- JetBrains developer statement on CSS/JS: "JavaScript gets embedded into the HTML tree automatically in the presence of JS plugin" — confirms CSS/JS sub-injection is automatic
- Pebble IntelliJ plugin documentation: confirms Emmet works after Template Data Language set to HTML
- YouTrack PY-13775: "Setting Template Data Language for Mako templates has no effect" — confirms current Mako plugin does NOT have TemplateLanguageFileViewProvider; this v0.3.0 work fixes that gap
- JetBrains Platform SDK: FileViewProviders, Language Injection documentation

---

## Feature Landscape

### Table Stakes (Users Expect These)

These are the features users expect the moment they see "HTML injection" in the plugin's changelog. Missing any of these makes HTML injection feel broken or incomplete.

| Feature | Why Expected | Complexity | Automatic vs. Explicit | Notes |
|---------|--------------|------------|------------------------|-------|
| **HTML syntax coloring in template body** | The template body is HTML — without coloring it looks like plain text; users already experience this in `.html` files | LOW (once TemplateLanguageFileViewProvider is wired) | AUTOMATIC — HTML SyntaxHighlighter runs on HTML PSI tree | The HTML PSI tree covers all TEMPLATE_TEXT tokens; zero per-token wiring needed in the Mako plugin |
| **HTML tag completion (`<div>`, `<span>`, etc.)** | Users type `<` and expect tag suggestions just like in `.html` files | LOW (once TemplateLanguageFileViewProvider is wired) | AUTOMATIC — HTML CompletionContributor fires in HTML PSI context | Requires correct `getLanguages()` in FileViewProvider returning both Mako and HTML |
| **HTML attribute completion (`class=`, `href=`, `id=`, etc.)** | Users click inside a tag and expect attribute suggestions | LOW | AUTOMATIC — HTML CompletionContributor provides attributes for known tags | Works for standard HTML tags; custom attributes require HTML schema customization (out of scope) |
| **HTML error squiggles for malformed markup** | Users expect red underlines on `<div class=` (missing value) or `<diiv>` (unknown tag) | MEDIUM | MOSTLY AUTOMATIC — HTML Annotator runs on HTML PSI tree; Mako syntax regions appear as OuterLanguageElements and are skipped | Risk: `${...}` inside HTML attributes may still produce false positives if the HTML parser misinterprets the EXPR_START token boundary — needs testing |
| **HTML tag auto-closing** | Typing `<div>` and getting `</div>` inserted automatically | LOW | AUTOMATIC — HTML plugin's TypedHandler handles this in HTML PSI context | No Mako-specific code needed |
| **HTML brace/tag matching** | Clicking `<div>` highlights matching `</div>` | LOW | AUTOMATIC — HTML's BraceMatcher applies in HTML PSI context | Coexists with existing Mako `MakoPairedBraceMatcher`; they operate on different token types |
| **Default template data language configured to HTML for .mako files** | Without a default, users must manually go to Settings > Template Data Languages and set HTML for every project; this friction defeats the purpose | MEDIUM | EXPLICIT — requires `templateDataLanguageProvider` extension point registration | Must register `com.intellij.fileType.templateDataLanguageProvider` pointing to HTML for `MakoFileType`; confirmed by PY-13775 that this does not exist in the current plugin |

### Differentiators (Competitive Advantage)

These features come from HTML injection but are not universally expected by Mako users. They represent the "above and beyond" value of proper HTML injection.

| Feature | Value Proposition | Complexity | Automatic vs. Explicit | Notes |
|---------|-------------------|------------|------------------------|-------|
| **Emmet abbreviation expansion in HTML regions** | Type `div.container>ul>li*3` then Tab and get fully expanded HTML — major productivity feature for HTML authors | LOW (once HTML injection is working) | AUTOMATIC — Emmet activates when caret is in HTML PSI context; confirmed by Pebble plugin docs | Emmet's language check looks at the PSI language at the caret position; if that position is in HTML PSI, Emmet fires. No plugin-side code needed. |
| **CSS language injection inside `<style>` tags** | Users get CSS coloring, property completion, and lint inside inline style blocks | LOW (once HTML injection is working) | AUTOMATIC — HTML plugin injects CSS into `<style>` tags internally; confirmed by JetBrains developer: "JavaScript gets embedded into the HTML tree automatically in the presence of JS plugin" — same mechanism for CSS | The CSS injection comes from the HTML plugin, not the Mako plugin. The Mako plugin just needs the HTML tree; CSS injection happens inside that tree. |
| **JavaScript injection inside `<script>` tags** | Users get JS coloring, completion, and lint inside inline script blocks | LOW (once HTML injection is working) | AUTOMATIC — HTML plugin injects JavaScript into `<script>` tags via `HtmlScriptContentProvider` | PyCharm Community includes JavaScript plugin. CSS/JS injection inside HTML sub-trees is a platform-level feature. |
| **HTML code folding for long elements** | Long `<table>` or `<section>` blocks can be folded just like in `.html` files | LOW | AUTOMATIC — HTML FoldingBuilder applies to HTML PSI tree | Coexists with existing Mako FoldingBuilder for `<%def>`, `<%block>`, control flow. Both run. |
| **HTML inspections (e.g., deprecated attributes, accessibility warnings)** | IDE flags `<font>` as deprecated, flags missing `alt` on `<img>` | LOW | AUTOMATIC — HTML inspections run on HTML PSI tree | Users who have these inspections enabled in `.html` files get them automatically |
| **HTML live templates (e.g., `html5` snippet)** | Standard HTML live templates expand in `.mako` files just as in `.html` files | LOW | AUTOMATIC — live template language check matches HTML context | Zero plugin-side work |
| **Breadcrumb navigation inside HTML structure** | Editor breadcrumb shows `body > main > section > div` as the user navigates | LOW | AUTOMATIC — HTML BreadcrumbsInfoProvider applies to HTML PSI tree | Users of breadcrumbs in HTML files get it in Mako files too |
| **HTML reformatting (Ctrl+Alt+L) for template body** | Code formatter applies HTML indentation rules to template body regions | MEDIUM | PARTLY AUTOMATIC — `SimpleTemplateLanguageFormattingModelBuilder` delegates to HTML formatter; but must be registered for Mako language | Requires registering `lang.formatter` for Mako Template language pointing to `SimpleTemplateLanguageFormattingModelBuilder` or a custom formatter. If not registered, Ctrl+Alt+L does nothing useful. |

### Anti-Features (Commonly Requested, Often Problematic)

These features seem desirable but create disproportionate implementation cost, user confusion, or maintenance burden.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **"Smart" HTML error suppression inside Mako expressions** | Users complain that `<div class="${some_expr}">` gets a false-positive attribute-value error | The OuterLanguageElement mechanism handles this at the PSI level — TEMPLATE_TEXT boundaries respect EXPR_START/EXPR_END tokens, so the HTML PSI tree sees the expression as an opaque leaf, not as malformed text. If errors still appear after correct TemplateDataElementType wiring, they come from HTML inspections, not the HTML parser. | Let the OuterLanguageElement mechanism do its job. If specific false positives persist, suppress them with a targeted `HtmlUnknownAttributeInspection` suppression rather than a blanket approach. Do not attempt to build a custom error-filter layer — too fragile. |
| **Injecting HTML via MultiHostInjector (instead of TemplateLanguageFileViewProvider)** | Seems simpler — existing Python injection uses MultiHostInjector | MultiHostInjector is for injecting a foreign language into specific host PSI nodes. It cannot inject HTML into the whole "background" of a file (TEMPLATE_TEXT is not a single host node — it is many fragmented leaves across the Mako PSI tree). Attempting this produces fragmentary HTML PSI with no context between fragments — tag completion fails, folding fails, Emmet fails. | Use TemplateLanguageFileViewProvider. It is the correct mechanism for "the whole file background is language X." |
| **Injecting HTML into TEMPLATE_TEXT via `languageInjectionContributor`** | Seems like a lighter-weight alternative | Same problem as MultiHostInjector above — injection-based approaches create isolated islands of HTML PSI, not a continuous HTML tree. Tag matching across a Mako expression boundary (e.g., `<div>${expr}</div>`) requires OuterLanguageElement continuity, which only TemplateLanguageFileViewProvider provides. | TemplateLanguageFileViewProvider is the only correct approach for this use case. |
| **Configurable "template data language" beyond HTML** | Power users want to set data language to "plain text" or "XML" for non-HTML Mako templates | The `templateDataLanguageProvider` extension point sets the default. The IDE's Template Data Languages settings UI already lets users override per-file/folder — this is built into the platform. No custom UI needed. | Register the default as HTML via the extension point; let the platform's existing UI handle overrides. Do not build a custom settings panel for this. |
| **HTML tag completion for Mako-specific pseudo-tags (`<%def>`, `<%block>`)** | Users might expect the HTML completion list to include Mako directives | Mako tags are not HTML tags. The existing Mako CompletionContributor handles `<%` completion already. Mixing Mako directives into the HTML completion list pollutes the HTML completions with non-HTML items and confuses users who expect valid HTML. | Keep Mako completion (in MakoCompletionContributor) and HTML completion (via HTML PSI) fully separate. They fire in different PSI contexts. |
| **CSS-in-Python-expression highlighting** | Users want `<div style="${computed_style}">` to show CSS syntax inside the Python expression | The `${...}` region is Python-injected, not CSS. The content is a Python expression that happens to produce a CSS string at runtime — the IDE has no way to know this without runtime information. | Out of scope. The Python injection already provides Python completion inside `${...}`. Users who write runtime CSS strings can use IntelliJ's manual "Inject Language" action. |

---

## Feature Dependencies

```
[TemplateLanguageFileViewProvider + TemplateDataElementType]
    └──enables──> [HTML Syntax Coloring in TEMPLATE_TEXT]
    └──enables──> [HTML Tag Completion]
    └──enables──> [HTML Attribute Completion]
    └──enables──> [HTML Error Squiggles]
    └──enables──> [HTML Auto-closing Tags]
    └──enables──> [HTML Brace Matching]
    └──enables──> [HTML Code Folding]
    └──enables──> [HTML Inspections]
    └──enables──> [Emmet Expansion]     (AUTOMATIC — HTML PSI context activates Emmet)
    └──enables──> [CSS in <style> tags] (AUTOMATIC — HTML plugin handles internally)
    └──enables──> [JS in <script> tags] (AUTOMATIC — HTML plugin handles internally)
    └──enables──> [HTML Live Templates]
    └──enables──> [Breadcrumb Navigation in HTML structure]

[templateDataLanguageProvider extension point]
    └──required by──> [Default HTML language for .mako files]
                           └──without this──> [Users must configure manually per-project]

[SimpleTemplateLanguageFormattingModelBuilder registration]
    └──enables──> [HTML Reformatting (Ctrl+Alt+L)]

[Existing MakoPythonInjector (already built)]
    └──coexists-with──> [TemplateLanguageFileViewProvider]  (different layers, no conflict)
    └──note──> MultiHostInjector operates on Mako PSI tree; TemplateLanguageFileViewProvider
               creates a parallel HTML PSI tree; they do not interfere

[Existing MakoCompletionContributor (already built)]
    └──coexists-with──> [HTML CompletionContributor]
    └──risk──> contributor registered language='any' — fires in HTML PSI positions too;
               existing MakoLanguage identity guard inside contributor prevents false firing
               but must be verified in the HTML PSI context after TemplateLanguageFileViewProvider is added

[Existing MakoFoldingBuilder (already built)]
    └──coexists-with──> [HTML FoldingBuilder]
    └──note──> both FoldingBuilders run; Mako folds <%def>/<%block>/control flow;
               HTML folds <div>/<section> etc.; no conflict expected

[Existing MakoAnnotator (already built)]
    └──coexists-with──> [HTML Annotator]
    └──note──> Mako annotator fires on Mako PSI tree; HTML annotator fires on HTML PSI tree;
               they are independent; no conflict expected
```

### Dependency Notes

- **TemplateLanguageFileViewProvider is the single enabler:** All HTML features flow from this one architectural change. It is the prerequisite for everything in this milestone.
- **CSS and JS injection are free:** Do not implement separate CSS or JS injectors — the HTML plugin handles them inside the HTML PSI tree automatically.
- **Emmet is free:** Do not implement custom Emmet hooks — Emmet reads the PSI language at caret and fires in HTML context automatically.
- **The templateDataLanguageProvider extension point is not optional:** Without it, HTML injection only works after the user manually configures the Template Data Language setting. This is too much friction — most users will not find it. The extension point makes HTML the default for `.mako` files at plugin install time.
- **Existing features must be regression-tested:** TemplateLanguageFileViewProvider changes how `getContainingFile()` resolves for elements in the file — code in MakoFoldingBuilder, MakoStructureViewFactory, MakoAnnotator, and MakoPythonInjector must be verified to still resolve against the correct (Mako) PSI root, not the HTML PSI root.

---

## MVP Definition

### Launch With (v0.3.0)

Minimum viable HTML injection — what is needed to make the feature meaningful to users.

- [ ] **TemplateLanguageFileViewProvider + TemplateDataElementType** — the foundational mechanism that creates the parallel HTML PSI tree from TEMPLATE_TEXT tokens; without this nothing else in this list works
- [ ] **`templateDataLanguageProvider` extension point → HTML** — registers HTML as the default template data language for `.mako` files; without this the feature only works after manual per-project configuration
- [ ] **Regression verification: existing Mako features still work** — folding, structure view, Python injection, completion, annotator must all pass their existing tests after the FileViewProvider change
- [ ] **False-positive audit** — verify that Mako syntax (`${...}` in attribute values, `% for` control lines) does not produce HTML error squiggles; confirm OuterLanguageElement boundaries are correct

### Add After Validation (v0.3.x)

Features to add once the core HTML injection is validated and shipped.

- [ ] **HTML reformatting integration** — register `SimpleTemplateLanguageFormattingModelBuilder` for Mako language; lets Ctrl+Alt+L apply HTML formatting rules to the template body; risk: mixed-language reformatting can mangle Mako control lines — validate carefully before shipping
- [ ] **MakoCompletionContributor guard verification** — the existing `language='any'` contributor may surface in HTML PSI positions in unexpected ways; add explicit guard if issues are found in testing

### Future Consideration (v0.4+)

Features that build on HTML injection but are separate milestones.

- [ ] **Go-to-definition for `<%inherit>`, `<%include>`, `<%namespace>` file references** — uses HTML PSI cross-file reference resolution pattern; depends on stable HTML injection
- [ ] **HTML-aware inspections for Mako-specific attribute patterns** — e.g., detect invalid HTML produced by Mako expressions; requires understanding of runtime semantics

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| TemplateLanguageFileViewProvider + TemplateDataElementType | HIGH | MEDIUM | P1 |
| templateDataLanguageProvider → HTML default | HIGH | LOW | P1 |
| HTML syntax coloring (automatic once FVP wired) | HIGH | LOW (free) | P1 |
| HTML tag/attribute completion (automatic) | HIGH | LOW (free) | P1 |
| HTML error squiggles (automatic, needs false-positive audit) | HIGH | LOW (free, MEDIUM for audit) | P1 |
| Emmet expansion (automatic) | MEDIUM | LOW (free) | P1 |
| CSS in `<style>` tags (automatic) | MEDIUM | LOW (free) | P1 |
| JS in `<script>` tags (automatic) | MEDIUM | LOW (free) | P1 |
| Regression verification of existing features | HIGH | MEDIUM | P1 |
| HTML reformatting (Ctrl+Alt+L) | MEDIUM | MEDIUM | P2 |
| CompletionContributor guard audit | LOW | LOW | P2 |

**Priority key:**
- P1: Must have for v0.3.0 launch
- P2: Should have, add in v0.3.x
- P3: Nice to have, future milestone

---

## Automatic vs. Explicit Implementation Summary

This table directly answers the research question: which features come from TemplateLanguageFileViewProvider automatically, and which need explicit code.

| Feature | Automatic or Explicit | What Triggers It | Risk |
|---------|-----------------------|-----------------|------|
| HTML syntax coloring | AUTOMATIC | HTML SyntaxHighlighter runs on HTML PSI tree | None |
| HTML tag completion | AUTOMATIC | HTML CompletionContributor fires in HTML PSI | Verify `language='any'` Mako contributor doesn't conflict |
| HTML attribute completion | AUTOMATIC | HTML CompletionContributor | None |
| HTML tag auto-close | AUTOMATIC | HTML TypedHandler fires | None |
| HTML brace/tag matching | AUTOMATIC | HTML BraceMatcher fires | None |
| HTML code folding | AUTOMATIC | HTML FoldingBuilder fires | Coexistence with MakoFoldingBuilder — verify no overlap |
| HTML inspections | AUTOMATIC | HTML Annotator fires on HTML PSI | Potential false positives where Mako syntax meets HTML boundaries |
| Emmet expansion | AUTOMATIC | Emmet checks PSI language at caret — HTML = fire | None; confirmed by Pebble plugin docs |
| CSS inside `<style>` tags | AUTOMATIC | HTML plugin injects CSS internally | None; platform handles it |
| JS inside `<script>` tags | AUTOMATIC | HTML plugin injects JS via HtmlScriptContentProvider | None; platform handles it |
| HTML live templates | AUTOMATIC | Live template language check hits HTML PSI | None |
| HTML breadcrumbs | AUTOMATIC | HTML BreadcrumbsInfoProvider fires | None |
| HTML reformatting | EXPLICIT | Must register `SimpleTemplateLanguageFormattingModelBuilder` for Mako | Mixed-language formatting can mangle control lines — test before shipping |
| Default HTML for .mako files | EXPLICIT | Must register `templateDataLanguageProvider` extension point | Without it, users must configure manually — makes feature invisible |
| HTML PSI tree construction from TEMPLATE_TEXT | EXPLICIT | Must implement `TemplateLanguageFileViewProvider` + `TemplateDataElementType` | Core implementation — everything else depends on this |
| `fileViewProviderFactory` registration | EXPLICIT | Must register `com.intellij.fileType.fileViewProviderFactory` for MakoFileType | Plugin.xml extension point registration |
| Python injection coexistence | EXPLICIT (verification) | Must verify MakoPythonInjector still works correctly after FileViewProvider change | Layers are separate but shared document coordinates must remain consistent |

---

## Competitor Feature Analysis

Reference: how comparable template language plugins handle HTML injection.

| Feature | Handlebars/Mustache plugin (JetBrains) | Pebble plugin (bjansen) | Django templates (PyCharm bundled) | Our Approach |
|---------|-----------------------------------------|------------------------|-------------------------------------|--------------|
| HTML injection mechanism | TemplateLanguageFileViewProvider | TemplateLanguageFileViewProvider | TemplateLanguageFileViewProvider | TemplateLanguageFileViewProvider — same pattern |
| Default template data language | HTML (set by plugin) | Configured by user in Template Data Languages settings | HTML (set by plugin for `.html` Django files) | Register `templateDataLanguageProvider` → HTML |
| Emmet in template files | Yes | Yes (confirmed in Pebble docs) | Yes | Automatic after TemplateLanguageFileViewProvider |
| CSS in `<style>` tags | Yes | Yes | Yes | Automatic |
| JS in `<script>` tags | Yes | Yes | Yes | Automatic |
| False-positive HTML errors for template syntax | Handled by OuterLanguageElement in HTML PSI | Handled by OuterLanguageElement | Handled by OuterLanguageElement | Same mechanism — OuterLanguageElement leaves are opaque to HTML parser |
| Formatting | SimpleTemplateLanguageFormattingModelBuilder | Registered | Registered | Register in v0.3.x |

**Key insight:** Every mature IntelliJ template language plugin uses TemplateLanguageFileViewProvider for HTML injection. The pattern is fully established and de-risked by multiple production plugins. The Mako plugin's architecture (TemplateLanguage subclass, GrammarKit PSI, TEMPLATE_TEXT token type) aligns correctly with this pattern.

**Confirmed gap:** YouTrack PY-13775 ("Setting Template Data Language for Mako templates has no effect") demonstrates that without TemplateLanguageFileViewProvider, even the IDE's manual Template Data Language setting has no effect on Mako files. This v0.3.0 milestone is the correct fix.

---

## Sources

- JetBrains IntelliJ Support: "Tutorial: Custom templating language plugin" (intellij-support.jetbrains.com/hc/en-us/community/posts/206765105) — architecture of TemplateLanguageFileViewProvider
- JetBrains IntelliJ Support: "Example of a custom language plugin for a templating language" (intellij-support.jetbrains.com/hc/en-us/community/posts/206780275) — "you'll get that for almost free" quote; JS automatic embedding confirmation
- JetBrains IntelliJ Support: "Inject custom language into JS/CSS code" (intellij-support.jetbrains.com/hc/en-us/community/posts/204145604) — CSS/JS automatic behavior
- Pebble IntelliJ Plugin (github.com/bjansen/pebble-intellij) — "Emmet expansions" listed as automatic after Template Data Language = HTML
- JetBrains YouTrack PY-13775 (youtrack.jetbrains.com/issue/PY-13775) — confirms current Mako plugin lacks TemplateLanguageFileViewProvider; Template Data Language setting has no effect
- JetBrains Documentation: "Template Data Languages" (jetbrains.com/help/idea/template-data-languages-settings.html) — feature list when data language is configured
- JetBrains Documentation: "Template languages: Velocity and FreeMarker" (jetbrains.com/help/idea/template-data-languages.html) — reference for comparable template language features
- IntelliJ Platform Plugin SDK: "File View Providers" (plugins.jetbrains.com/docs/intellij/file-view-providers.html)
- IntelliJ Platform Plugin SDK: "Language Injection" (plugins.jetbrains.com/docs/intellij/language-injection.html)

---

*Feature research for: HTML language injection in Mako JetBrains plugin (v0.3.0 milestone)*
*Researched: 2026-02-22*

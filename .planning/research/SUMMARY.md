# Project Research Summary

**Project:** JetBrains Mako Template Language Plugin — v0.3.0 HTML Injection Milestone
**Domain:** IntelliJ Platform custom language plugin — TemplateLanguageFileViewProvider HTML injection (PyCharm 2025.2+)
**Researched:** 2026-02-22
**Confidence:** HIGH (all four research files verified against live PyCharm 2025.2.6 JARs, Handlebars plugin source, and IntelliJ community source)

## Executive Summary

This is a subsequent-milestone research document for an existing, working plugin (v0.2.0). The plugin already implements `MakoLanguage` extending `TemplateLanguage`, a JFlex lexer, GrammarKit parser, PSI tree, Python injection via `MultiHostInjector`, syntax highlighting, code folding, structure view, and tag completion. The v0.3.0 milestone adds one architectural feature: HTML language injection into the TEMPLATE_TEXT regions of `.mako` files using `TemplateLanguageFileViewProvider`. This makes HTML tag/attribute completion, Emmet expansion, CSS/JS injection inside `<style>`/`<script>` tags, HTML error detection, and HTML code folding all work automatically in `.mako` files — driven by the HTML plugin operating on a parallel HTML PSI tree that the Mako plugin creates.

The recommended approach is the canonical IntelliJ Platform pattern used by Handlebars, Django templates, Velocity, FreeMarker, and all other mature template language plugins: implement `MultiplePsiFilesPerDocumentFileViewProvider` plus `TemplateLanguageFileViewProvider` (`MakoFileViewProvider`), a factory (`MakoFileViewProviderFactory`), a `TemplateDataElementType` constant that bridges `TEMPLATE_TEXT` tokens to the HTML parser, and an `OUTER_ELEMENT_TYPE` constant that represents Mako constructs as opaque placeholders in the HTML PSI tree. Register the factory via the `lang.fileViewProviderFactory` extension point in `plugin.xml`. Three new classes, one `plugin.xml` line, one new constant in `MakoTokenTypes.kt` — nothing else changes. All five research files agree on this scope: no existing classes require logic changes, only defensive audits and guard additions.

The primary risk is not correctness of the new code, but regression in existing features. The introduction of a second PSI root per file (the HTML tree alongside the Mako tree) changes how `containingFile`, `viewProvider.allFiles`, and PSI tree traversal behave throughout the plugin. `MakoPythonInjector` must be defensively updated to obtain the Mako PSI root via `viewProvider.getPsi(MakoLanguage)` rather than `context.containingFile` to survive the dual-tree environment. `MakoAnnotator` needs an `OuterLanguageElement` guard. `MakoCompletionContributor`'s `language="any"` registration must be re-evaluated. All existing tests must pass unchanged after the FileViewProvider is wired in, serving as the regression baseline.

## Key Findings

### Recommended Stack

The existing plugin infrastructure (IntelliJ Platform 2025.2+, build 252, Kotlin, GrammarKit lexer/parser, `MakoLanguage extends TemplateLanguage`) is already correct and requires no additions. The HTML injection mechanism uses only platform APIs present in the Gradle-cached PyCharm 2025.2.6 JARs — verified by `javap` directly against `app-client.jar` and `util-8.jar`. No new Gradle dependencies, no new SDK modules, no new plugin.xml dependencies are required. The existing `com.intellij.modules.platform` dependency already provides `HTMLLanguage`, `HtmlFileType`, and the full HTML plugin infrastructure.

**Core APIs (all verified from PyCharm 2025.2.6 JARs):**
- `TemplateLanguageFileViewProvider` (`util-8.jar`): Interface contract for multi-PSI-tree files — already partially satisfied since `MakoLanguage` extends `TemplateLanguage`
- `MultiplePsiFilesPerDocumentFileViewProvider` (`app-client.jar`): Abstract base class managing the per-language PSI tree cache; provides `getAllFiles()`, `cloneInner()` contract
- `TemplateDataElementType` (`app-client.jar`): Bridges `TEMPLATE_TEXT` tokens to the HTML parser; inserts `OuterLanguageElement` placeholders for Mako constructs
- `OuterLanguageElementType` (`app-client.jar`): IElementType for Mako-construct placeholder nodes in the HTML PSI tree
- `TemplateDataLanguageMappings` (`app-client.jar`): Project service enabling user override of template data language in IDE Settings
- `HTMLLanguage.INSTANCE` (`app-client.jar`): Default template data language for `.mako` files
- `lang.fileViewProviderFactory` EP (`LangExtensionPoints.xml`): Extension point to register `MakoFileViewProviderFactory`

**Critical note on extension point naming:** Use `lang.fileViewProviderFactory` (language-keyed, `LanguageExtensionPoint` bean) — not `fileType.fileViewProviderFactory` (file-type-keyed). Confirmed via `restructuredtext.jar` bundled in PyCharm 2025.2.6 as a canonical RST plugin reference.

**Reference implementation:** The Handlebars plugin (`HbFileViewProvider.java`, `HbFileViewProviderFactory.java` in JetBrains/intellij-plugins) and the bundled RST plugin (`RestFileViewProvider`, `RestFileProviderFactory` in `restructuredtext.jar`) both implement this exact pattern and were used as canonical references.

### Expected Features

The single enabling change — `TemplateLanguageFileViewProvider` + `TemplateDataElementType` — automatically unlocks nearly all HTML features with zero additional code. The distinction between automatic and explicit is the key finding of the features research.

**Must have (P1 — v0.3.0 launch):**
- `TemplateLanguageFileViewProvider` + `TemplateDataElementType` wiring — the single mechanism that creates the HTML PSI tree; all other features depend on it
- `templateDataLanguagePatterns` registration in `plugin.xml` mapping `.mako`/`.mak` to HTML — without this, users must configure HTML manually per project; the feature is invisible to new users
- Regression verification: existing folding, structure view, Python injection, completion, and annotator must all pass existing tests unchanged after the FileViewProvider is active
- False-positive audit: verify `${...}` in attribute values and `%for`/`%if` control lines do not produce HTML error squiggles

**Automatic (free once P1 wiring is done):**
- HTML syntax coloring in TEMPLATE_TEXT regions
- HTML tag and attribute completion (`<div>`, `class=`, `href=`, etc.)
- HTML auto-closing tags and brace/tag matching
- HTML error squiggles for malformed markup (with OuterLanguageElement handling Mako syntax correctly)
- Emmet abbreviation expansion in HTML regions
- CSS injection inside `<style>` tags (HTML plugin handles internally)
- JavaScript injection inside `<script>` tags (HTML plugin handles internally)
- HTML code folding (coexists with existing Mako folding)
- HTML live templates, breadcrumb navigation, HTML inspections

**Defer (v0.3.x after validation):**
- HTML reformatting via `SimpleTemplateLanguageFormattingModelBuilder` — requires explicit registration; risk of mangling Mako control lines; validate before shipping
- `MakoCompletionContributor` `language="any"` guard reassessment — may be resolvable once HTML PSI context is verified working

**Defer (v0.4+):**
- Go-to-definition for `<%inherit>`, `<%include>`, `<%namespace>` file references
- HTML-aware inspections for Mako-specific attribute patterns

**Anti-features (confirmed out of scope):**
- `MultiHostInjector` for HTML injection — produces fragmented HTML PSI, not a continuous tree; tag matching across Mako expression boundaries fails
- `languageInjectionContributor` for HTML — same fragmentation problem
- Custom CSS-in-Python-expression highlighting — not inferable without runtime semantics

### Architecture Approach

The architecture adds exactly three new classes to the existing plugin. The existing `MakoLanguage`, `MakoParserDefinition`, `MakoFile`, `MakoPythonInjector`, and all feature providers remain logically unchanged; only defensive guards are added to existing code.

**New components (must be created):**
1. `MakoFileViewProvider` (`lang/MakoFileViewProvider.kt`) — implements `MultiplePsiFilesPerDocumentFileViewProvider` + `TemplateLanguageFileViewProvider`; creates Mako PSI tree (delegates to existing `MakoParserDefinition`) and HTML PSI tree (delegates to HTML's `ParserDefinition`, then sets `contentElementType` to `TEMPLATE_DATA_ELEMENT_TYPE`); caches `TemplateDataElementType` instances in `ConcurrentHashMap<String, TemplateDataElementType>` keyed by language ID (Handlebars pattern, prevents duplicate `IElementType` registration); overrides `supportsIncrementalReparse = false` to keep both PSI trees consistent
2. `MakoFileViewProviderFactory` (`lang/MakoFileViewProviderFactory.kt`) — implements `FileViewProviderFactory`; reads `TemplateDataLanguageMappings` for user-configured language, falls back to `HTMLLanguage.INSTANCE`; registered in `plugin.xml` via `lang.fileViewProviderFactory language="Mako Template"`
3. `OUTER_ELEMENT_TYPE` constant in `MakoTokenTypes.kt` — `IElementType("MAKO_OUTER_ELEMENT", MakoLanguage)`; 4th argument to `TemplateDataElementType`; marks Mako-construct placeholder leaves in the HTML PSI tree

**Modified components (defensive changes only):**
- `plugin.xml`: add one `lang.fileViewProviderFactory` registration and `templateDataLanguagePatterns` block
- `MakoTokenTypes.kt`: add `OUTER_ELEMENT_TYPE` constant (additive only)
- `MakoPythonInjector`: change `collectCodeAndExpressionHosts` root lookup to use `viewProvider.getPsi(MakoLanguage)` instead of `context.containingFile`
- `MakoAnnotator`: add `if (element is OuterLanguageElement) return` guard

**Build order (strict):**
1. Add `OUTER_ELEMENT_TYPE` to `MakoTokenTypes` (pure additive)
2. Create `MakoFileViewProvider` with `TemplateDataElementType` singleton caching
3. Create `MakoFileViewProviderFactory`
4. Register in `plugin.xml`
5. Add defensive guards to `MakoPythonInjector` and `MakoAnnotator`
6. Verify all existing tests pass; verify HTML PSI tree via PSI Viewer

### Critical Pitfalls

1. **Wrong extension point key for factory registration** — Using `lang.fileViewProviderFactory` vs `fileType.fileViewProviderFactory` is a silent failure: the factory is ignored, no HTML PSI tree is created, no error is shown. Verify: `(file.viewProvider as? TemplateLanguageFileViewProvider) != null`. The correct registration uses `language="Mako Template"` (matches `MakoLanguage.getID()`).

2. **`contentElementType` not set on the HTML PSI file in `createFile`** — Without this, the HTML parser processes the raw Mako file bytes including `<%def`, `%for`, etc., producing a broken HTML PSI tree with false completions and false errors everywhere. `(htmlFile as PsiFileImpl).contentElementType = MAKO_TEMPLATE_DATA_TYPE` must immediately follow `def.createFile(this)` for the HTML language case.

3. **`TemplateDataElementType` instantiated per-file instead of as a singleton** — `IElementType` uses object identity; creating a new instance per `createFile()` call means the `contentElementType` on different files never matches the token type the platform recognizes. `TemplateDataElementType` must be in a companion object `ConcurrentHashMap` cache, initialized once per data language ID.

4. **Python injector receives HTML PSI root instead of Mako PSI root** — After adding `TemplateLanguageFileViewProvider`, `context.containingFile` in `MakoPythonInjector` may return the HTML `PsiFile`. `PsiTreeUtil.findChildrenOfType(htmlFile, MakoCodeBlock::class.java)` returns empty; injection fires zero hosts; all Python expressions turn red. Fix: `val makoFile = context.containingFile.viewProvider.getPsi(MakoLanguage) ?: return emptyList()`.

5. **`MakoAnnotator` encounters `OuterLanguageElementImpl` nodes and throws** — The Mako PSI tree now contains `OuterLanguageElementImpl` nodes at TEMPLATE_TEXT positions. Code that casts these to Mako PSI types throws `ClassCastException`. Add `if (element is OuterLanguageElement) return` as the first line of `annotate()`.

6. **`getTemplateDataLanguage()` NPE during early project initialization** — `TemplateDataLanguageMappings.getInstance(project)` throws if `project` is null (can happen during IDE startup before project is fully loaded). Always guard: `val project = manager.project ?: return HTMLLanguage.INSTANCE`.

7. **`TemplateDataElementType` constructor argument order confusion** — The 3rd argument (`templateElementType`) is NOT the HTML-content token; it is the Mako-syntax token type (the marker to exclude from the HTML tree). The 4th argument (`outerElementType`) is the token that becomes a placeholder in the HTML tree. For Mako: pass `MakoTokenTypes.TEMPLATE_TEXT` as the 3rd argument (it IS the HTML content token that the HTML parser should receive); pass `MakoTokenTypes.OUTER_ELEMENT_TYPE` as the 4th argument. Getting these transposed causes either the Mako tree or the HTML tree to lose all content.

## Implications for Roadmap

Based on combined research, the v0.3.0 milestone has clear phase structure driven by the dependency chain: the FileViewProvider scaffolding must be correct before any HTML features can be verified, existing-feature regressions must be confirmed resolved before the milestone is declared complete, and HTML correctness (false-positive audit) requires both trees stable.

### Phase 1: FileViewProvider Scaffolding

**Rationale:** All HTML features depend on the FileViewProvider creating a valid parallel HTML PSI tree. This is the single enabling change — nothing else in this milestone can be verified until this is in place and verified correct. All pitfalls in the critical list are preventable here by careful construction before wiring in.
**Delivers:** `MakoFileViewProvider`, `MakoFileViewProviderFactory`, `OUTER_ELEMENT_TYPE`, `plugin.xml` registration, `templateDataLanguagePatterns`; PSI Viewer shows two PSI roots for any `.mako` file; `file.viewProvider.allFiles.size == 2`.
**Addresses:** HTML PSI tree construction; default template data language for `.mako` files (no manual configuration required by users).
**Avoids:** Pitfalls 1, 2, 3, 6, 7 (extension point key, `contentElementType`, singleton, NPE, argument order) — all must be correct before the scaffolding is declared done.
**Research flag:** Standard pattern (well-documented by Handlebars and RST reference implementations; HIGH confidence; skip `/gsd:research-phase`).

### Phase 2: Existing Feature Regression Hardening

**Rationale:** The FileViewProvider changes how `containingFile`, `viewProvider.allFiles`, and PSI tree traversal behave. `MakoPythonInjector`, `MakoAnnotator`, and `MakoCompletionContributor` each have known failure modes in the dual-tree environment. These must be addressed and all existing tests must pass before the milestone is declared shippable. Shipping HTML injection that breaks Python injection is worse than shipping no HTML injection.
**Delivers:** `MakoPythonInjector` updated to use `viewProvider.getPsi(MakoLanguage)` as root; `MakoAnnotator` `OuterLanguageElement` guard added; `MakoCompletionContributor` `language="any"` guard verified or updated; `./gradlew check` passes 100%.
**Addresses:** All existing features verified unchanged: Python injection, folding, structure view, completion, annotations.
**Avoids:** Pitfalls 4, 5, and the `ClassCastException` failure mode (Python injector root, annotator `OuterLanguageElement`, completion double-firing).
**Research flag:** Standard pattern for guarding PSI consumers in a multi-tree context; skip `/gsd:research-phase`.

### Phase 3: HTML Feature Verification and False-Positive Audit

**Rationale:** With the scaffolding correct and regressions resolved, this phase verifies that the automatic HTML features actually work as expected and that Mako syntax does not cause false-positive HTML errors. This is verification and test-writing, not new implementation.
**Delivers:** Manual verification in `runIde` that HTML tag completion, Emmet, CSS/JS injection, HTML folding, and HTML error detection all work in TEMPLATE_TEXT regions; false-positive audit confirming `${...}` in attribute values and `%for`/`%if` lines do not produce HTML error squiggles; new tests for `viewProvider.getPsi(HTMLLanguage.INSTANCE)` returning a valid `HtmlFile`.
**Addresses:** False-positive audit (P1 MVP requirement from FEATURES.md); HTML completion works in TEMPLATE_TEXT; Emmet fires.
**Avoids:** Shipping HTML features that produce noise (false HTML errors on Mako syntax).
**Research flag:** Verification phase; no research needed. If false positives are found, targeted suppression strategy is documented in PITFALLS.md.

### Phase 4: HTML Reformatting (v0.3.x, post-validation)

**Rationale:** `SimpleTemplateLanguageFormattingModelBuilder` registration enables Ctrl+Alt+L to apply HTML formatting rules to template body regions. This is an explicit feature (not automatic) and carries risk of mangling Mako control lines — it should be validated independently after core HTML injection is shipped and stable.
**Delivers:** `lang.formatter` registration for Mako pointing to `SimpleTemplateLanguageFormattingModelBuilder`; Ctrl+Alt+L applies HTML indentation in TEMPLATE_TEXT regions without corrupting `%for`, `%if`, `<%def>` lines.
**Addresses:** HTML reformatting from FEATURES.md (P2 — "should have").
**Avoids:** Shipping before validating on templates with complex Mako control flow mixed with HTML.
**Research flag:** May need a targeted spike on `SimpleTemplateLanguageFormattingModelBuilder` behavior with mixed-language content before implementation. Consider `/gsd:research-phase` for this phase.

### Phase Ordering Rationale

- Phase 1 must precede all others: the HTML PSI tree does not exist until the FileViewProvider is wired in; nothing HTML-related can be verified without it.
- Phase 2 must precede Phase 3: verifying HTML features is only meaningful once existing features are confirmed unbroken; a broken Python injection would make HTML completion tests ambiguous.
- Phase 3 closes the v0.3.0 milestone: all P1 requirements from FEATURES.md are satisfied when the false-positive audit is clean and existing tests pass.
- Phase 4 is v0.3.x: deliberately deferred because it carries independent risk (formatting bugs) and does not block the core HTML injection value proposition.

### Research Flags

Phases needing deeper research during planning:
- **Phase 4 (HTML Reformatting):** `SimpleTemplateLanguageFormattingModelBuilder` interaction with Mako control lines (indentation-sensitive `% for`, `% if`) is not fully verified. A spike is recommended before committing to implementation. Candidate for `/gsd:research-phase`.

Phases with standard patterns (skip `/gsd:research-phase`):
- **Phase 1 (FileViewProvider Scaffolding):** Exact implementation verified against Handlebars plugin source and PyCharm 2025.2.6 JARs; HIGH confidence on all API signatures and extension point registration.
- **Phase 2 (Regression Hardening):** PSI consumer guard patterns (`viewProvider.getPsi(MakoLanguage)`, `OuterLanguageElement` guard) are straightforward defensive changes with no API uncertainty.
- **Phase 3 (HTML Verification):** This is verification work, not new implementation; no research needed.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All APIs verified via `javap` against PyCharm 2025.2.6 `app-client.jar` and `util-8.jar`; extension point verified in `LangExtensionPoints.xml` and `PyCharmCorePlugin.xml` extracted from live JARs; RST plugin reference implementation verified from `restructuredtext.jar` |
| Features | MEDIUM | Automatic feature activation (completion, Emmet, CSS/JS) confirmed from JetBrains developer statements and Pebble plugin docs; specific behavior of `OuterLanguageElement` boundary for false-positive suppression is MEDIUM — confirmed architecturally but needs empirical verification per pitfall audit |
| Architecture | HIGH | Implementation verified against Handlebars `HbFileViewProvider.java` and `HbFileViewProviderFactory.java` live source; `TemplateDataElementType` constructor and `TemplateDataLanguageMappings` verified from `intellij-community` source; 5 anti-patterns documented with specific failure modes |
| Pitfalls | MEDIUM-HIGH | 10 specific pitfalls documented with failure modes, warning signs, and recovery steps; most verified from live source (Handlebars, intellij-community); extension point key pitfall verified from `LangExtensionPoints.xml`; some pitfalls (completion double-firing severity) are architectural inference requiring empirical confirmation |

**Overall confidence:** HIGH for the core implementation (FileViewProvider scaffolding); MEDIUM for regression behavior (requires empirical testing in Phase 2).

### Gaps to Address

- **False-positive behavior with `${...}` in attribute values:** The `OuterLanguageElement` mechanism should suppress HTML errors in Mako expression positions, but this depends on how tightly the TEMPLATE_TEXT token boundaries align with HTML attribute value boundaries in the lexer. Needs empirical verification in Phase 3; if false positives appear, targeted suppressor strategy is documented in PITFALLS.md.
- **`MakoCompletionContributor` `language="any"` behavior post-FileViewProvider:** The original reason for `language="any"` was that TEMPLATE_TEXT positions were not reachable with `language="Mako Template"`. With `TemplateLanguageFileViewProvider`, TEMPLATE_TEXT positions are now in the HTML PSI tree. Whether `language="any"` is still necessary or whether it causes double-firing needs empirical testing in Phase 3; the guard change is documented in PITFALLS.md.
- **`templateDataLanguagePatterns` vs `templateDataLanguageProvider` extension point:** PITFALLS.md identifies `<templateDataLanguagePatterns>` as the correct extension for pre-populating the default in Settings; FEATURES.md references `templateDataLanguageProvider`. Verify the correct extension point name against `LangExtensionPoints.xml` during Phase 1 implementation.
- **HTML reformatting with Mako control lines:** Behavior of `SimpleTemplateLanguageFormattingModelBuilder` on `% for`/`% if`/`% endif` indentation-sensitive lines is not empirically verified. Defer to Phase 4; spike before implementation.

## Sources

### Primary (HIGH confidence)
- PyCharm Community 2025.2.6 `app-client.jar` — `javap -p` on `TemplateLanguageFileViewProvider`, `TemplateDataElementType`, `TemplateDataLanguageMappings`, `MultiplePsiFilesPerDocumentFileViewProvider`, `OuterLanguageElementType`, `HTMLLanguage`, `HtmlFileType`
- PyCharm Community 2025.2.6 `util-8.jar` — `javap -p` on `TemplateLanguageFileViewProvider`, `FileViewProviderFactory`
- PyCharm Community 2025.2.6 `restructuredtext.jar` (bundled RST plugin) — `RestFileViewProvider`, `RestFileProviderFactory`, `RestPythonTemplateType`; `META-INF/plugin.xml` confirming `lang.fileViewProviderFactory language="ReST"`
- `META-INF/LangExtensionPoints.xml` extracted from `app-client.jar` — `lang.fileViewProviderFactory` EP definition
- Handlebars `HbFileViewProvider.java` (live source): github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/file/HbFileViewProvider.java
- Handlebars `HbFileViewProviderFactory.java` (live source): github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/file/HbFileViewProviderFactory.java
- `TemplateDataElementType.java` (live source): github.com/JetBrains/intellij-community/blob/master/platform/analysis-impl/src/com/intellij/psi/templateLanguages/TemplateDataElementType.java
- `TemplateDataLanguageMappings.java` (live source): github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/psi/templateLanguages/TemplateDataLanguageMappings.java
- Existing Mako plugin codebase — `MakoLanguage.kt`, `MakoParserDefinition.kt`, `MakoFile.kt`, `MakoTokenTypes.kt`, `MakoPythonInjector.kt`, `plugin.xml`

### Secondary (MEDIUM confidence)
- JetBrains developer statement: "you'll get that for almost free" / "JavaScript gets embedded into the HTML tree automatically" — confirms automatic CSS/JS and Emmet behavior
- Pebble IntelliJ Plugin docs (github.com/bjansen/pebble-intellij) — confirms Emmet activates automatically after Template Data Language = HTML
- JetBrains YouTrack PY-13775 — confirms current Mako plugin lacks `TemplateLanguageFileViewProvider`; Template Data Language setting has no effect on `.mako` files without it
- IntelliJ Platform Plugin SDK: File View Providers (plugins.jetbrains.com/docs/intellij/file-view-providers.html)
- IntelliJ Platform API Changes 2025: confirms no breaking changes to template language APIs in 2025.2

### Tertiary (LOW confidence)
- JetBrains support community tutorial: "Custom templating language plugin" — confirms architectural pattern; community post, not official docs

---
*Research completed: 2026-02-22*
*Ready for roadmap: yes*

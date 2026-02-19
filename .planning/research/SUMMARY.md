# Project Research Summary

**Project:** JetBrains Mako Template Language Plugin
**Domain:** IntelliJ Platform custom language plugin — multi-language template file support (PyCharm)
**Researched:** 2026-02-19
**Confidence:** MEDIUM

## Executive Summary

This project is a JetBrains IDE plugin that adds first-class language support for the Mako template language in PyCharm. Mako files mix three languages in a single file: HTML (the output structure), Mako directives and control constructs (the template logic), and embedded Python (expressions and code blocks). Experts build this type of plugin using the IntelliJ Platform's Template Language API — registering Mako as a `TemplateLanguage` subclass that hosts HTML as its data language, then injecting Python into expression and block regions via `MultiHostInjector`. This approach is used by all mature JetBrains template language plugins (Velocity, Twig, Blade, FreeMarker) and is the only architecture that correctly models Mako's semantics without fighting the platform.

The recommended build sequence is strict: Language/FileType registration first, then JFlex lexer with correct restart-state handling, then GrammarKit-generated parser with a well-structured PSI node hierarchy, then syntax highlighting and Template Language integration, then Python injection, and finally semantic features (completion, navigation, annotations). This order is not arbitrary — each layer is a hard prerequisite for the next, and the most catastrophic mistakes in custom language plugin development come from building in the wrong order or making an irreversible architectural decision early (treating Mako as injected-into-HTML rather than the primary language).

The key risks are well-understood and largely avoidable with the right tooling: using JFlex eliminates the incremental re-lex bug class entirely; using the Template Language API from the start avoids an expensive rewrite; designing distinct PSI node classes for each named Mako construct (def, block, inherit, include, namespace) before writing the grammar enables reference resolution and refactoring without structural rework. Python API stability is a real concern — all Python PSI access must go through the language injection boundary, never through direct imports of internal `com.jetbrains.python.psi` classes.

## Key Findings

### Recommended Stack

The project already has a valid scaffold targeting IntelliJ Platform 2025.2.5 (build 252), Kotlin 2.3.0, and IntelliJ Platform Gradle Plugin 2.11.0. The critical additions needed are the GrammarKit Gradle plugin (for JFlex lexer generation and BNF parser generation) and a `src/main/gen/` source root for generated sources. Plugin dependencies should declare `com.intellij.modules.lang` for language infrastructure and `com.intellij.modules.python` as an optional dependency for PyCharm-specific Python injection. Do not depend on the `com.jetbrains.python` Marketplace plugin — only the bundled module.

**Core technologies:**
- IntelliJ Platform 2025.2.5 (build 252): Plugin host — already configured, provides all Language, PSI, lexer, parser, completion, and reference APIs
- Kotlin 2.3.0: Implementation language — already configured, idiomatic for IntelliJ Platform plugins
- JFlex 1.9.x (via GrammarKit): Lexer generator — generates correct stateful scanner from `.flex` grammar; bundled with GrammarKit, do NOT add separately
- Grammar-Kit 2024.3.4: Parser and PSI generator — generates parser and PSI node classes from `.bnf` grammar; verify current version before pinning
- GrammarKit Gradle Plugin ~2022.3.x: Build orchestration for lexer/parser generation — verify current version at https://plugins.gradle.org/plugin/org.jetbrains.grammarkit

See `STACK.md` for complete extension point list by phase, Gradle configuration snippets, and version compatibility notes.

### Expected Features

All mature JetBrains template language plugins (Django/Jinja2, Thymeleaf, FreeMarker) share the same feature baseline. Users of this plugin will expect parity on table-stakes features before calling the plugin production-ready.

**Must have (table stakes — v1 MVP):**
- File type registration for `.mako` — without this, nothing works; includes content-based sniffing for `.html` files with Mako syntax
- Syntax highlighting for Mako constructs, embedded HTML regions, and embedded Python — the core quality-of-life win
- Code folding for `<%def>`, `<%block>`, and control flow blocks — large templates become unreadable without it
- File structure view showing named defs and blocks — Ctrl+F12 navigation
- Brace/tag matching for `<%def>` / `</%def>` pairs
- Comment/uncomment for `##` line comments
- File icon for `.mako` files

**Should have (v1.x — after core validation):**
- Code completion for Mako tags and attributes
- Go-to-definition for `<%inherit>`, `<%include>`, `<%namespace>` file references — highest-value navigation feature
- Live templates for common Mako patterns
- Basic semantic error annotations (malformed Mako constructs)
- Color scheme customization for Mako-specific token colors
- Settings panel for file associations

**Defer (v2+):**
- Python expression completion inside `${...}` — requires stable Python injection architecture
- Go-to-definition for def call resolution across files (cross-file reference resolution)
- Find usages of template defs
- Rename refactoring
- Inspections for undefined variables or invalid inherited template structure
- Breadcrumb navigation

**Anti-features (do not build):** Template preview/render, full Python analysis without Python plugin dependency, web framework integration, real-time external Mako linter integration. See `FEATURES.md` for detailed rationale.

### Architecture Approach

The architecture centers on four stacked layers: (1) Language/FileType registration establishing Mako as a `TemplateLanguage` subclass, (2) a JFlex-generated lexer with at least 5-6 states handling the mode transitions between HTML content, Mako expressions, Mako tags, Python blocks, control lines, and comments, (3) a GrammarKit-generated parser producing a typed PSI tree with distinct node classes for each addressable Mako construct, and (4) feature providers (highlighter, annotator, completion, navigation, injection, folding) that all read the PSI tree. HTML interop is handled declaratively via `TemplateDataLanguageConfigurable`; Python interop is handled via `MultiHostInjector` that injects `PythonLanguage.INSTANCE` into expression and block content ranges.

**Major components:**
1. `MakoLanguage` (extends `TemplateLanguage`) + `MakoFileType` — language identity; foundation for all extension point registrations
2. `MakoLexer` (JFlex-generated) — tokenizes the full file; multi-state for HTML/expression/tag/block/control-line modes; must implement correct `getState()` for incremental re-lex
3. `MakoParser` + PSI node hierarchy — builds typed tree from tokens; distinct classes for `MakoDefDirective`, `MakoBlockDirective`, `MakoInheritDirective`, `MakoIncludeDirective`, `MakoNamespaceDirective`, `MakoExpression`, `MakoControlLine`
4. `MakoTemplateDataLanguageConfigurable` — declares HTML as the hosted data language; enables HTML plugin features in template body regions
5. `MakoPythonInjector` (implements `MultiHostInjector`) — injects Python into `${...}` and `<% ... %>` regions; delegates all Python analysis to PyCharm's Python plugin
6. Feature providers: `MakoSyntaxHighlighter`, `MakoAnnotator`, `MakoFoldingBuilder`, `MakoCompletionContributor`, `MakoReferenceContributor`

See `ARCHITECTURE.md` for complete component boundary table, data flow diagram, build order, and anti-patterns.

### Critical Pitfalls

1. **Stateful lexer restart bugs** — If `getState()` does not correctly serialize all lexer state, incremental re-lex produces wrong tokens after any edit. Mitigation: use JFlex (it generates correct state machine code), never hand-write the lexer, write `LexerTestCase` tests that restart from mid-file offsets before closing the lexer phase.

2. **Wrong multi-language strategy** — Treating the file as HTML with Mako injections (instead of Mako as primary language) breaks when Mako control flow spans multiple HTML subtrees. Mitigation: register Mako as a `TemplateLanguage`, HTML as the template data language — this decision cannot be reversed cheaply, so it must be made before any lexer work.

3. **PSI tree too coarse for reference resolution** — Using a single generic `MakoElement` for all constructs makes navigation and refactoring impossible to implement later. Mitigation: design distinct PSI node classes for every named Mako construct before writing the BNF grammar; implement `PsiNamedElement` on definition nodes immediately.

4. **Thread safety violations in PSI access** — PSI reads from background threads without read-lock cause intermittent `AssertionError` crashes that only appear in production. Mitigation: establish the `ReadAction.compute` pattern in the first feature implementation; enable `ide.slow.operations.assertion` in the sandbox during development.

5. **Python plugin internal API dependency** — Direct imports of `com.jetbrains.python.psi.*` in non-injection code break on every PyCharm release when internal APIs change. Mitigation: all Python PSI access must go through the injection boundary; never import Python PSI classes outside `MakoPythonInjector`.

6. **`sinceBuild`/`untilBuild` misconfiguration** — Incorrect compatibility range causes Marketplace rejection or user crashes on IDE updates. Mitigation: configure `untilBuild = 252.*` initially; run `runPluginVerifier` as a required CI check before every release.

See `PITFALLS.md` for additional integration gotchas, performance traps, recovery strategies, and a "looks done but isn't" checklist.

## Implications for Roadmap

Based on combined research, the natural phase structure follows the hard dependency chain in the IntelliJ Platform custom language plugin build order. No phase can safely begin before the previous one is stable.

### Phase 1: Language Foundation and File Type Registration
**Rationale:** Everything in the plugin depends on `MakoLanguage`, `MakoFileType`, and `MakoTokenTypes` existing as stable singletons. This is zero-risk, zero-dependency work that unblocks all subsequent phases. Architectural decisions about Template Language API and Python plugin dependency must also be locked in here before any grammar work starts.
**Delivers:** `.mako` files open with a file icon instead of plain text; Mako is recognized as a distinct language; project structure shows `.mako` files; build pipeline includes GrammarKit generation step.
**Features:** File type registration, file icon, GrammarKit/JFlex Gradle integration, plugin.xml dependency declarations.
**Avoids:** Wrong multi-language strategy pitfall — architecture decision locked before any other code is written.

### Phase 2: Lexer
**Rationale:** The lexer is the foundation of all syntax-aware features. It must be correct and must implement restart-state semantics before anything builds on top of it. JFlex grammar for Mako's 5-6 lexer states is well-understood but must be validated with restart tests.
**Delivers:** `.mako` files tokenized correctly; restart-from-middle verified with `LexerTestCase` tests.
**Uses:** JFlex via GrammarKit Gradle plugin; `MakoTokenTypes` constants from Phase 1.
**Avoids:** Stateful lexer restart bug pitfall — correctness verified before parser is built on top.

### Phase 3: Parser and PSI Tree
**Rationale:** The PSI tree shape determines what every subsequent feature can do. Designing `MakoDefDirective`, `MakoBlockDirective`, `MakoInheritDirective`, etc. as distinct typed PSI nodes here eliminates an expensive refactor later when navigation and refactoring are built. This is the highest-leverage architectural investment.
**Delivers:** `MakoParserDefinition` wiring lexer + parser + PSI; complete PSI node hierarchy; root `MakoFile` class; GrammarKit-generated parser with error recovery.
**Implements:** Parser component, PSI element classes, `MakoParserDefinition`.
**Avoids:** PSI tree too coarse for reference resolution pitfall.

### Phase 4: Syntax Highlighting and Template Language Integration
**Rationale:** Syntax highlighting is the most immediately visible user value and is the natural next step after a stable PSI exists. Template Language (HTML) integration is bundled here because both depend on stable token types and can be registered in `plugin.xml` at the same time. This phase makes the plugin usable for daily work.
**Delivers:** Mako constructs highlighted with distinct colors; HTML regions highlighted by the HTML plugin; user-configurable color settings page; `<%def>` / `</%def>` brace matching; `##` comment toggling.
**Features:** Syntax highlighting (Mako constructs + embedded HTML), brace/tag matching, comment/uncomment, color scheme customization, Template Data Language registration.

### Phase 5: Structural Features (Folding, Structure View, File Type Polish)
**Rationale:** Code folding and the file structure view both read the PSI tree but have no dependency on language injection — they can be built before Python injection is stable. Together they complete the MVP and make the plugin feel finished for day-to-day template editing.
**Delivers:** Code folding for defs, blocks, and control structures; Ctrl+F12 structure view showing named defs and blocks; file type detection polish (`.html` sniffing for Mako content).
**Features:** Code folding, file structure view, file association settings panel.
**Avoids:** Folding defaults should not collapse defs by default — collapse only `<%doc>` and `<%!>` blocks by default.

### Phase 6: Python Language Injection
**Rationale:** Python injection is architecturally isolated — it touches the stable PSI from Phase 3 and the Python plugin API, but adding it does not destabilize earlier features. It must be built before completion and annotations because both depend on injected Python PSI being available.
**Delivers:** `${...}`, `<% ... %>`, `<%! ... %>`, and control-line expressions have full Python highlighting, completion, and error checking courtesy of PyCharm's Python plugin.
**Uses:** `MultiHostInjector`, `com.intellij.modules.python` optional dependency.
**Avoids:** Python internal API dependency pitfall — all Python access through injection boundary only.

### Phase 7: Navigation and Completion
**Rationale:** Go-to-definition for file references (`<%inherit>`, `<%include>`, `<%namespace>`) and code completion for Mako tags are the highest-value differentiating features for v1.x. They depend on the stable PSI node types from Phase 3 and the injected Python from Phase 6.
**Delivers:** Ctrl+click on `<%inherit file="..."/>` navigates to the referenced template; `<%include>` and `<%namespace>` file references are navigable; code completion for Mako directives and tag attributes; live templates for common Mako patterns.
**Features:** Go-to-definition (file references), code completion (Mako tags/attributes), live templates.

### Phase 8: Semantic Annotations and Release Readiness
**Rationale:** Semantic error annotations are the final polish layer; they require stable PSI and correct injection. Plugin Verifier configuration and Marketplace submission process must be finalized before this phase closes.
**Delivers:** Red squiggles for definitively malformed Mako syntax; `runPluginVerifier` as CI check; Marketplace-ready build with correct `sinceBuild`/`untilBuild`.
**Features:** Basic error annotations, Plugin Verifier CI integration.
**Avoids:** `sinceBuild`/`untilBuild` misconfiguration pitfall; annotator performance trap (must benchmark on large files).

### Phase Ordering Rationale

- Phases 1-3 are strictly sequential due to hard dependency: token types exist before lexer, lexer exists before parser, parser exists before PSI tree.
- Phase 4 follows Phase 3 because syntax highlighting consumes token types and the PSI tree; Template Language integration requires a stable `ParserDefinition`.
- Phase 5 can technically overlap Phase 6 but is listed separately because folding/structure view do not block on Python injection, and shipping the structural features independently provides testable value.
- Phase 6 (Python injection) must precede Phase 7 (completion) because Python expression completion inside `${...}` depends on injection being active.
- Phase 8 is last because annotations are the most sensitive to PSI stability — premature annotation work tends to produce false errors that undermine user trust.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (Lexer):** JFlex grammar for Mako's filter expression syntax (`${x | h,trim}`) is a known complexity — the `|` character inside expression context must be differentiated from Python's bitwise OR. The lexer state design for this case needs a spike before committing to the grammar.
- **Phase 6 (Python Injection):** MultiHostInjector range calculation for injected Python across multiple `${...}` expressions in the same logical context needs verification against the current platform's injection debug tooling. Fragmented injection files are a known failure mode.
- **Phase 7 (Navigation):** Template path resolution for `<%inherit file="..."/>` when projects use Mako lookup paths (not filesystem-relative paths) requires understanding how the project layout interacts with `PsiReference` resolution. This may need a research spike before implementation.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Foundation):** Registering `Language`, `FileType`, and token type constants is a fully documented, template-driven process. No surprises expected.
- **Phase 4 (Syntax Highlighting):** `SyntaxHighlighter` + `ColorSettingsPage` is one of the best-documented IntelliJ Platform APIs. Follow the Custom Language Support tutorial directly.
- **Phase 5 (Folding/Structure View):** `FoldingBuilder` and `StructureViewBuilder` are standard, stable APIs with complete tutorial coverage.
- **Phase 8 (Release):** Plugin Verifier configuration is already partially scaffolded in the existing `build.gradle.kts`.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Core platform/Kotlin/Gradle versions confirmed from existing scaffold (HIGH); GrammarKit Gradle plugin version LOW — must verify at plugins.gradle.org before implementation |
| Features | MEDIUM | Feature categories (what language plugins provide) are HIGH confidence from IntelliJ Platform docs structure; specific API names need verification against platform 2025.2.5 |
| Architecture | MEDIUM | Template Language + MultiHostInjector pattern is well-documented and consistent across multiple comparable plugins; platform-version-specific API details need verification; research session had no web access |
| Pitfalls | MEDIUM | Pitfall categories (lexer restart, threading, API stability) are stable and well-known; specific recovery procedures may vary by platform version |

**Overall confidence:** MEDIUM

### Gaps to Address

- **GrammarKit Gradle plugin version:** Training data indicates `2022.3.x` as the most recent stable version. Must verify at https://plugins.gradle.org/plugin/org.jetbrains.grammarkit before the build configuration phase. Do not assume this is current.
- **JFlex filter expression handling:** How `${x | h,trim}` should be tokenized (specifically how `|` is disambiguated from Python bitwise OR in expression context) is not fully resolved by research. Requires a lexer design spike in Phase 2.
- **TemplateDataLanguageConfigurable current API:** The exact class names and registration pattern for `TemplateDataLanguage` support in platform build 252 need verification against current documentation. Training data is consistent but not verified against 2025.2.5 specifically.
- **`.html` content-based sniffing:** The mechanism for detecting Mako syntax in `.html` files (to offer Mako file type association) needs a decision: automatic detection vs. user-triggered association. This has UX and implementation tradeoffs to resolve in Phase 1.
- **Python injection coherence for multi-expression contexts:** Whether multiple `${...}` expressions on a page can be injected as a single coherent Python fragment (vs. separate fragments) affects how Python type inference works across expression sites. This needs a spike in Phase 6.

## Sources

### Primary (HIGH confidence)
- Existing scaffold `build.gradle.kts`, `gradle.properties`, `plugin.xml` — confirms IntelliJ Platform 2025.2.5, build 252, Kotlin 2.3.0, Gradle 9.3.1, IntelliJ Platform Gradle Plugin 2.11.0
- IntelliJ Platform Docs — Custom Language Support Tutorial: https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html
- IntelliJ Platform Docs — Extension Point Reference: https://plugins.jetbrains.com/docs/intellij/extension-point-list.html

### Secondary (MEDIUM confidence)
- IntelliJ Platform Docs — Language Injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html
- IntelliJ Platform Docs — Template Languages: https://plugins.jetbrains.com/docs/intellij/template-languages.html
- Grammar-Kit GitHub: https://github.com/JetBrains/Grammar-Kit
- IntelliJ Platform Gradle Plugin 2.x docs: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- Training knowledge of Velocity, Twig, Blade, FreeMarker plugin patterns (training data, August 2025 cutoff)
- Training knowledge of MultiHostInjector, TemplateDataLanguage, JFlex state machine patterns

### Tertiary (LOW confidence)
- GrammarKit Gradle plugin version `2022.3.2` — from training data; must be verified at https://plugins.gradle.org/plugin/org.jetbrains.grammarkit before use
- Grammar-Kit plugin version `2024.3.4` — from training data; verify at https://plugins.jetbrains.com/plugin/6606-grammar-kit
- JFlex version `1.9.2` — bundled with GrammarKit; do not pin separately

---
*Research completed: 2026-02-19*
*Ready for roadmap: yes*

# Mako Template Plugin for JetBrains

## What This Is

A PyCharm plugin providing first-class IDE support for Mako template files (`.mako`, `.html.mako`). Mako is a Python template engine used for generating HTML; this plugin brings syntax highlighting, code folding, structure navigation, tag completion, Python language injection, and error detection to Mako templates in PyCharm — filling a gap where no JetBrains plugin currently exists. Shipped as `com.schtilig.mako` v0.1.0 on JetBrains Marketplace.

## Core Value

Mako template files get the same rich editing experience as native Python and HTML files in PyCharm — syntax is colored, errors are caught before runtime, and navigation works across template boundaries.

## Requirements

### Validated

- ✓ Plugin scaffold with Kotlin/Gradle/IntelliJ Platform 2025.2.5 — existing
- ✓ Build toolchain configured (Gradle 9.3.1, Java 21, plugin signing/publishing pipeline) — existing
- ✓ File type recognition for `.mako` files and `.html.mako` Mako-containing HTML files — v0.1.0 (LANG-01)
- ✓ Mako-specific file icon in the project tree — v0.1.0 (LANG-02)
- ✓ Mako registered as a `TemplateLanguage` subclass in the IntelliJ Platform — v0.1.0 (LANG-03)
- ✓ JFlex-generated lexer tokenizing all Mako constructs with restart-state semantics — v0.1.0 (PARS-01, PARS-02, PARS-03)
- ✓ GrammarKit parser with typed PSI nodes and error recovery — v0.1.0 (PARS-04, PARS-05)
- ✓ Syntax highlighting for all Mako constructs (9 color attributes, configurable in Settings) — v0.1.0 (SYNX-01, SYNX-02, SYNX-03, SYNX-04, SYNX-07)
- ✓ Brace matching for 5 Mako tag pairs — v0.1.0 (SYNX-05)
- ✓ Code folding for `<%def>`, `<%block>`, control flow, `<%doc>`, `<%!>` blocks — v0.1.0 (SYNX-06)
- ✓ `##` line comment toggle (Ctrl+/) and `<%doc>` block comment toggle (Ctrl+Shift+/) — v0.1.0 (EDIT-01, EDIT-02)
- ✓ Structure View panel with navigable `<%def>`/`<%block>` declarations — v0.1.0 (EDIT-03)
- ✓ Python language injection into `${...}`, `<% %>`, `<%! %>` via MultiHostInjector — v0.1.0
- ✓ Tag name completion (7 directives after `<%`) and per-tag attribute completion — v0.1.0 (COMP-01, COMP-02)
- ✓ Error annotations for unclosed tags and invalid directive names — v0.1.0 (COMP-03)
- ✓ Plugin Verifier passing for PC-252/PY-253/PY-261; Marketplace-ready as `com.schtilig.mako` v0.1.0 — v0.1.0

### Active

*(Next milestone requirements — to be defined via `/gsd:new-milestone`)*

### Out of Scope

- Support for non-PyCharm IDEs — focusing on PyCharm where Python integration matters most
- Runtime template rendering/preview — IDE editing support only, not a template engine
- Mako configuration file editing — focus is on template files themselves
- Integration with web frameworks (Pyramid, TurboGears routing) — pure template language support
- HTML language injection — Mako wraps HTML; the platform's default HTML handling covers non-Mako regions adequately for v0.1.0

## Context

**v0.1.0 shipped 2026-02-21.** Implemented across 9 phases in 3 days.

**Codebase state:**
- ~1,276 hand-written Kotlin LOC, 2,302 generated Java LOC, 1,237 test Kotlin LOC
- 22 token types, 12 PSI node types, 9 color attributes, 6 fold construct types
- 68+ unit tests (lexer, parser, folding, structure view, completion, annotator)
- Plugin ID: `com.schtilig.mako`, display name: `Mako`, version: `0.1.0`
- Target: PyCharm Community 2025.2+ (build 252+)

**Tech stack:** Kotlin, Gradle 9.3.1, IntelliJ Platform 2025.2.5, GrammarKit 2023.3.0.2, JFlex 1.9.1

**Known tech debt from v0.1.0:**
- `updateText()` no-op on injection host mixins — injection round-trip editing (typing in injected Python fragment syncing back) not implemented; deferred
- `getNameIdentifier()` skipped in def/block mixins — rename refactoring silently absent; deferred by design
- `FILTER_NAME` token defined but never emitted by lexer — dead constant, harmless
- Orphaned test fixtures and unused token sets (see milestone audit for full list)

**v2 requirements for future milestones:**
- NAVG-01–04: Go-to-definition for inherited templates, included files, namespace files, def navigation
- MLNG-01–04: HTML injection for HTML regions, Python completion in `${...}`
- ADVN-01–04: Cross-file def navigation, find usages, rename refactoring, undefined variable inspection
- PLSH-01–03: Live templates, breadcrumb navigation, settings panel for HTML-Mako detection

## Constraints

- **Platform**: IntelliJ Platform 2025.2.5+ (build 252+) — configured
- **Language**: Kotlin — primary implementation language
- **Runtime**: Java 21 JVM — configured via toolchain
- **org.gradle.java.home**: Must be pinned to JDK 21 — IntelliJ Platform `instrumentCode` fails on Windows with JDK 25+ (missing `Packages/` directory in MSI-installed JDK)
- **Template language complexity**: Mako embeds arbitrary Python; deep analysis requires PyCharm's Python plugin
- **Multi-language**: Templates mix HTML + Mako + Python — language injection handles embedded Python; HTML regions handled by platform default

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| PyCharm-only target | Mako is Python — PyCharm has Python support needed for embedded expression analysis | ✓ Good — PythonCore bundled plugin provides injection host for Python syntax |
| TemplateLanguage subclass (not injection-into-HTML) | Own PSI tree gives full control over folding, structure view, completion | ✓ Good — all IDE features work on custom PSI tree |
| Depend on PythonCore bundled plugin (not Marketplace `com.jetbrains.python`) | Bundled module available in PyCharm Community without extra dependency | ✓ Good — verified with verifyPlugin on PC-252, PY-253, PY-261 |
| JFlex multi-state lexer with brace depth tracking | `${...}` nesting requires stateful tokenization; JFlex states cleanly model this | ✓ Good — 22 token types cover all constructs; restart semantics verified |
| GrammarKit parser with pin=1 + recoverWhile | Standard IntelliJ error recovery pattern; tolerates partial/malformed Mako | ✓ Good — PSI tree survives unclosed tags; partial trees power annotator |
| Per-tag token types (TAG_OPEN_DEF..TAG_OPEN_PAGE) | GrammarKit requires distinct token types to produce distinct PSI node classes | ✓ Good — 12 typed PSI nodes; required for completion, folding, injection |
| generateTokens=false in BNF header | Prevents duplicate token constants that cause silent parse failures | ✓ Good — token delegates hand-added to MakoTypes.java once; stable across parser regenerations |
| PsiLanguageInjectionHost via BNF `implements=` attribute | GrammarKit propagates interface to generated PSI; no manual interface editing needed | ✓ Good — injection host contract flows through generated class hierarchy automatically |
| Python injection via MultiHostInjector (not LanguageInjector) | Multiple disjoint host regions in a single file require MultiHostInjector | ✓ Good — all 3 host types (expression, code_block, module_block) wired correctly |
| Raw text inspection for tag-name completion | Dummy identifier disrupts lexer before TAG_OPEN_xxx tokens appear; PSI patterns unreliable at `<%` position | ✓ Good — completion fires correctly with empty prefix matcher bypass |
| completion.contributor language='any' | TEMPLATE_TEXT tokens fall in template-data-language layer; language='Mako Template' filter prevents firing for those positions | ✓ Good — Mako guard inside contributor prevents false positives |
| structural=false for all BracePair entries | Shared END_TAG token across `<%def>` and `<%block>` causes platform matching conflicts with structural=true | ✓ Good — brace matching works correctly for all 5 pairs |
| Plugin ID `com.schtilig.mako`, display name `Mako` | JetBrains Marketplace naming conventions — no generic terms (Support, Tool, Plugin) in display name | ✓ Good — passes Marketplace plugin ID validation and naming guidelines |
| purgeOldFiles=false on generateMakoLexer | lang/ directory contains committed psi/ and parser/ subdirs; purgeOldFiles=true recursively deletes them on every clean build | ✓ Good — generated lexer regenerates safely without destroying parser files |
| addFileToProject + configureFromExistingVirtualFile for completion tests | configureByText(FileType) creates in-memory file before MakoFileType registered; physical temp file ensures correct file type after registration | ✓ Good — completion tests reliably detect Mako file type |

---
*Last updated: 2026-02-21 after v0.1.0 milestone*

# Mako Template Plugin for JetBrains

## What This Is

A PyCharm plugin that provides first-class IDE support for Mako template files (.mako, .html with Mako syntax). Mako is a Python template engine commonly used for generating HTML, and this plugin brings syntax highlighting, code completion, error detection, navigation, and refactoring to Mako templates — filling a gap where no JetBrains plugin currently exists.

## Core Value

Mako template files get the same rich editing experience as native Python and HTML files in PyCharm — syntax is colored, errors are caught before runtime, and navigation works across template boundaries.

## Requirements

### Validated

- ✓ Plugin scaffold with Kotlin/Gradle/IntelliJ Platform 2025.2.5 — existing
- ✓ Build toolchain configured (Gradle 9.3.1, Java 21, plugin signing/publishing pipeline) — existing

### Active

- [ ] Syntax highlighting for all Mako constructs within HTML context
- [ ] Code completion for Mako tags, expressions, and Python code within templates
- [ ] Error detection for Mako syntax mistakes (malformed tags, unclosed blocks)
- [ ] Go-to-definition for template defs, inherited templates, and included files
- [ ] Refactoring support (rename defs, extract blocks)
- [ ] File type recognition for .mako files and Mako-containing .html files
- [ ] Mako-specific code folding (collapse defs, blocks, control structures)

### Out of Scope

- Support for non-PyCharm IDEs — focusing on PyCharm where Python integration matters most
- Runtime template rendering/preview — IDE editing support only, not a template engine
- Mako configuration file editing — focus is on template files themselves
- Integration with web frameworks (Pyramid, TurboGears routing) — pure template language support

## Context

- Mako is a Python template engine used primarily for HTML generation
- Mako syntax includes: `${...}` expressions, `% for/if/while` control lines, `<%def>`, `<%block>`, `<%inherit>`, `<%include>`, `<%namespace>`, `<%page>`, `<%!>` module-level blocks, `<%doc>` comments, `##` line comments, and filter expressions like `${x | h,trim}`
- Templates mix three languages: HTML structure, Mako template directives, and embedded Python code
- The existing project is scaffolded from the JetBrains Plugin Template with boilerplate (tool window, service, startup activity) that needs to be replaced with Mako-specific functionality
- IntelliJ Platform provides rich APIs for custom language support: Lexer, Parser, PSI tree, annotators, completion contributors, reference resolution, and more
- Package namespace: `com.github.kimuth.jetbrainsmakotemplateplugin`

## Constraints

- **Platform**: IntelliJ Platform 2025.2.5+ (build 252+) — already configured
- **Language**: Kotlin — already set up as primary implementation language
- **Runtime**: Java 21 JVM — configured via toolchain
- **Template language complexity**: Mako embeds arbitrary Python expressions, which limits how deeply we can analyze embedded code without a full Python parser (PyCharm's Python plugin provides this)
- **Multi-language**: Templates mix HTML + Mako + Python, requiring language injection or multi-language file support from IntelliJ Platform

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| PyCharm-only target | Mako is Python — PyCharm has Python support needed for embedded expression analysis | — Pending |
| Custom language vs. injection | Need to decide: register Mako as a custom language with its own PSI, or use language injection into HTML | — Pending |
| Depend on Python plugin | PyCharm's Python plugin could provide expression analysis for ${...} blocks | — Pending |

---
*Last updated: 2026-02-19 after initialization*

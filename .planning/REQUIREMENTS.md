# Requirements: Mako Template Plugin for JetBrains

**Defined:** 2026-02-19
**Core Value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Language Foundation

- [x] **LANG-01**: Plugin registers `.mako` as a recognized file type in PyCharm
- [x] **LANG-02**: `.mako` files display a Mako-specific icon in the project tree
- [x] **LANG-03**: Mako language is registered as a `TemplateLanguage` subclass with the IntelliJ Platform

### Lexer & Parsing

- [x] **PARS-01**: JFlex-generated lexer tokenizes all Mako constructs (`${...}`, `% control`, `<%def>`, `<%block>`, `<%inherit>`, `<%include>`, `<%namespace>`, `<%page>`, `<%! %>`, `<% %>`, `<%doc>`, `##`)
- [x] **PARS-02**: Lexer correctly handles nested constructs (Python expressions inside `${...}`)
- [x] **PARS-03**: Lexer state is serializable for incremental re-lexing (restart from mid-file)
- [x] **PARS-04**: GrammarKit-generated parser builds PSI tree with typed nodes for each Mako construct
- [x] **PARS-05**: Parser recovers gracefully from malformed Mako (partial parses, not full failure)

### Syntax & Visual

- [x] **SYNX-01**: Mako directives (`<%def>`, `<%block>`, etc.) are highlighted with distinct colors
- [x] **SYNX-02**: Mako expressions (`${...}`) are highlighted distinctly from surrounding HTML
- [x] **SYNX-03**: Mako control lines (`% for`, `% if`, `% while`, `% endfor`, etc.) are highlighted
- [x] **SYNX-04**: Mako comments (`##` and `<%doc>`) are highlighted as comments
- [x] **SYNX-05**: Matching Mako tag pairs (`<%def>` / `</%def>`) are highlighted when cursor is on either
- [ ] **SYNX-06**: User can fold/collapse `<%def>`, `<%block>`, and control flow blocks
- [x] **SYNX-07**: User can customize Mako-specific colors via Settings > Editor > Color Scheme

### Editing

- [x] **EDIT-01**: User can toggle line comments (`##`) with Ctrl+/
- [x] **EDIT-02**: User can toggle block comments (`<%doc>...</%doc>`) with Ctrl+Shift+/
- [ ] **EDIT-03**: Structure view panel shows all `<%def>` and `<%block>` declarations as navigable nodes

### Completion & Errors

- [ ] **COMP-01**: User gets autocomplete suggestions for Mako tag names (`<%def`, `<%block`, `<%inherit`, etc.)
- [ ] **COMP-02**: User gets autocomplete suggestions for Mako tag attributes (`name=`, `file=`, `buffered=`)
- [ ] **COMP-03**: Malformed Mako syntax (unclosed tags, invalid directives) shows error annotations in the editor

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Multi-Language Integration

- **MLNG-01**: Embedded HTML regions are syntax-highlighted via language injection
- **MLNG-02**: Embedded Python expressions are syntax-highlighted via language injection
- **MLNG-03**: HTML code completion works inside HTML regions of Mako templates
- **MLNG-04**: Python code completion works inside `${...}` expression blocks

### Navigation

- **NAVG-01**: User can Ctrl+click on `<%inherit file="...">` to navigate to the base template
- **NAVG-02**: User can Ctrl+click on `<%include file="...">` to navigate to the included file
- **NAVG-03**: User can Ctrl+click on `<%namespace file="...">` to navigate to the namespace file
- **NAVG-04**: User can navigate from def calls to def declarations within a file

### Advanced Intelligence

- **ADVN-01**: Cross-file def call navigation (resolve `${lib.foo()}` to `<%def name="foo">` in another file)
- **ADVN-02**: Find usages for `<%def>` declarations across the project
- **ADVN-03**: Rename refactoring for `<%def name="...">` updates all call sites
- **ADVN-04**: Inspection flags undefined variables in `${...}` expressions

### Polish

- **PLSH-01**: Live templates / code snippets for common Mako patterns
- **PLSH-02**: Breadcrumb navigation showing template structure context
- **PLSH-03**: Settings panel for configuring which `.html` files get Mako treatment

## Out of Scope

| Feature | Reason |
|---------|--------|
| Template preview / render output | Requires Mako Python runtime and context data — fundamentally unsafe in IDE |
| Full Python semantic analysis without Python plugin | Reimplementing Python type inference is infeasible |
| Mako configuration file support | Separate domain; config files already handled by other plugins |
| Web framework integration (Pyramid, TurboGears) | Framework-specific; breaks single-responsibility of template plugin |
| Auto-format / prettify Mako templates | Extremely hard for multi-language files; recommend external formatters |
| Non-PyCharm IDE support | Python integration is core to value; PyCharm-only is the right scope |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| LANG-01 | Phase 1 | Complete |
| LANG-02 | Phase 1 | Complete |
| LANG-03 | Phase 1 | Complete |
| PARS-01 | Phase 2 | Complete |
| PARS-02 | Phase 2 | Complete |
| PARS-03 | Phase 2 | Complete |
| PARS-04 | Phase 3 | Complete |
| PARS-05 | Phase 3 | Complete |
| SYNX-01 | Phase 4 | Complete |
| SYNX-02 | Phase 4 | Complete |
| SYNX-03 | Phase 4 | Complete |
| SYNX-04 | Phase 4 | Complete |
| SYNX-05 | Phase 4 | Complete |
| SYNX-06 | Phase 5 | Pending |
| SYNX-07 | Phase 4 | Complete |
| EDIT-01 | Phase 4 | Complete |
| EDIT-02 | Phase 4 | Complete |
| EDIT-03 | Phase 5 | Pending |
| COMP-01 | Phase 7 | Pending |
| COMP-02 | Phase 7 | Pending |
| COMP-03 | Phase 8 | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0

---
*Requirements defined: 2026-02-19*
*Last updated: 2026-02-19 after Phase 3 Plan 1 — PARS-04 and PARS-05 marked complete*

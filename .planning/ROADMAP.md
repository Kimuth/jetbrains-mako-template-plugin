# Roadmap: Mako Template Plugin for JetBrains

## Overview

Starting from an existing Kotlin/Gradle/IntelliJ Platform scaffold, this roadmap builds Mako language support in strict dependency order: language registration first, then lexer, then parser and PSI, then syntax features, then structural features, then Python injection as an enabler, then completion, and finally semantic annotations with release readiness. Each phase is a hard prerequisite for the next. The result is a PyCharm plugin where `.mako` files get first-class syntax highlighting, code folding, structure navigation, tag completion, and error detection.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Language Foundation** - Register Mako as a language and `.mako` as a recognized file type
- [x] **Phase 2: Lexer** - JFlex-generated lexer that correctly tokenizes all Mako constructs with restart-state semantics
- [x] **Phase 3: Parser and PSI Tree** - GrammarKit-generated parser with distinct typed PSI nodes for every Mako construct (completed 2026-02-19)
- [x] **Phase 4: Syntax Highlighting and Comment Support** - Distinct colors for all Mako constructs, brace matching, comment toggling, and color scheme settings (completed 2026-02-19)
- [ ] **Phase 5: Structural Features** - Code folding and structure view panel for navigating defs and blocks
- [ ] **Phase 6: Python Language Injection** - Inject Python into expression and code block regions via MultiHostInjector
- [ ] **Phase 7: Completion** - Autocomplete for Mako tag names and tag attributes
- [ ] **Phase 8: Error Annotations and Release Readiness** - Red squiggles for malformed Mako syntax and Marketplace-ready build

## Phase Details

### Phase 1: Language Foundation
**Goal**: Mako is recognized as a distinct language in PyCharm and `.mako` files are treated as first-class citizens
**Depends on**: Nothing (first phase)
**Requirements**: LANG-01, LANG-02, LANG-03
**Success Criteria** (what must be TRUE):
  1. Opening a `.mako` file in PyCharm shows a Mako-specific file icon in the project tree instead of a generic text icon
  2. PyCharm lists "Mako" as a recognized language in Settings > Languages & Frameworks
  3. The plugin builds cleanly with GrammarKit and JFlex generation steps wired into the Gradle build
  4. The boilerplate tool window, service, and startup activity from the original scaffold are removed
**Plans**: 2 plans
Plans:
- [x] 01-01-PLAN.md — Remove scaffold boilerplate, configure build for PyCharm Community + GrammarKit
- [x] 01-02-PLAN.md — Create MakoLanguage, MakoFileType, MakoIcons, and register fileType in plugin.xml

### Phase 2: Lexer
**Goal**: Every character in a `.mako` file is assigned the correct token type, and the lexer can correctly resume from any mid-file offset
**Depends on**: Phase 1
**Requirements**: PARS-01, PARS-02, PARS-03
**Success Criteria** (what must be TRUE):
  1. A `.mako` file containing all Mako construct types produces the expected token sequence with no misclassified tokens
  2. Nested constructs such as a Python expression inside `${...}` are tokenized correctly (inner expression tokens are distinct from surrounding Mako delimiters)
  3. Editing a line in the middle of a large `.mako` file does not corrupt token coloring below the edit point (restart-state semantics work)
  4. Filter expressions (`${x | h,trim}`) tokenize the `|` as a Mako filter separator, not as Python bitwise OR
**Plans**: 3 plans
Plans:
- [x] 02-01-PLAN.md — JFlex lexer with multi-state tokenization, token types, Gradle generation wiring
- [x] 02-02-PLAN.md — ParserDefinition stub, plugin.xml registration, lexer integration tests
- [x] 02-03-PLAN.md — Gap closure: replace UnsupportedOperationException stubs with no-op parser and ASTWrapperPsiElement

### Phase 3: Parser and PSI Tree
**Goal**: The parser builds a typed PSI tree where every Mako construct has a distinct node class that supports future reference resolution
**Depends on**: Phase 2
**Requirements**: PARS-04, PARS-05
**Success Criteria** (what must be TRUE):
  1. The PSI tree for a `.mako` file contains distinct node types for `<%def>`, `<%block>`, `<%inherit>`, `<%include>`, `<%namespace>`, expressions, and control lines — visible in the PsiViewer
  2. Definition nodes implement `PsiNamedElement` so their names are retrievable without string parsing
  3. A file with a malformed Mako tag (e.g., unclosed `<%def`) still produces a partial PSI tree rather than a complete parse failure — the rest of the file continues to parse
  4. The `MakoParserDefinition` wires the lexer, parser, and PSI node factory together so opening any `.mako` file does not throw exceptions in the IDE log
**Plans**: 3 plans
Plans:
- [x] 03-01-PLAN.md — Split TAG_OPEN into per-tag token types, create Mako.bnf grammar with error recovery, activate GenerateParserTask
- [x] 03-02-PLAN.md — Create PsiNamedElement mixins, wire MakoParserDefinition to generated parser, add parsing tests
- [x] 03-03-PLAN.md — Gap closure: fix CONTROL_LINE token/composite name collision by renaming BNF rule to control_line_stmt

### Phase 4: Syntax Highlighting and Comment Support
**Goal**: Mako constructs are visually distinct from surrounding HTML content and users can comment/uncomment Mako lines with standard keybindings
**Depends on**: Phase 3
**Requirements**: SYNX-01, SYNX-02, SYNX-03, SYNX-04, SYNX-05, SYNX-07, EDIT-01, EDIT-02
**Success Criteria** (what must be TRUE):
  1. Mako directives (`<%def>`, `<%block>`, etc.), expressions (`${...}`), control lines (`% for`, `% if`), and comments (`##`, `<%doc>`) each display in a distinct color that differs from the surrounding HTML text
  2. Placing the cursor on `<%def` highlights its matching `</%def>` closing tag (and vice versa)
  3. Pressing Ctrl+/ on a line containing Mako code inserts a `##` line comment prefix; pressing again removes it
  4. Pressing Ctrl+Shift+/ wraps selected Mako content in `<%doc>...</%doc>` block comment tags; pressing again unwraps it
  5. Mako-specific colors appear as named entries in Settings > Editor > Color Scheme > Mako and can be customized by the user
**Plans**: 2 plans
Plans:
- [x] 04-01-PLAN.md — Create MakoSyntaxHighlighter, ColorSettingsPage, PairedBraceMatcher, Commenter and register in plugin.xml
- [x] 04-02-PLAN.md — Gap closure: fix MAKO_EXPRESSION fallback color from TEMPLATE_LANGUAGE_COLOR to MARKUP_TAG

### Phase 5: Structural Features
**Goal**: Users can collapse large template sections and navigate directly to named defs and blocks without scrolling
**Depends on**: Phase 4
**Requirements**: SYNX-06, EDIT-03
**Success Criteria** (what must be TRUE):
  1. A gutter fold arrow appears on the opening line of every `<%def>`, `<%block>`, and control flow block (`% for`, `% if`, `% while`); clicking it collapses the block to a single line
  2. The Structure view panel (View > Tool Windows > Structure, or Ctrl+F12) shows all `<%def>` and `<%block>` declarations as a navigable tree; clicking a node moves the editor caret to that declaration
  3. `<%doc>` and `<%!>` blocks are collapsed by default when a file is opened; all other blocks start expanded
**Plans**: 2 plans
Plans:
- [ ] 05-01-PLAN.md — MakoFoldingBuilder with sibling-scan control flow folding, folding test
- [ ] 05-02-PLAN.md — Structure View three-class stack (factory, model, element), structure view test

### Phase 6: Python Language Injection
**Goal**: Python syntax highlighting and analysis from PyCharm's Python plugin is active inside Mako expression and code block regions
**Depends on**: Phase 3
**Requirements**: (none — enabler phase for Phase 7)
**Success Criteria** (what must be TRUE):
  1. Python code inside `${...}` is syntax-highlighted with Python colors (strings, keywords, builtins), distinct from the `${` and `}` Mako delimiters
  2. Python code inside `<% ... %>` and `<%! ... %>` blocks is fully highlighted as Python
  3. The injected Python ranges are visible in the Language Injection debug panel, confirming correct offset boundaries
  4. No exceptions related to Python PSI access appear in the IDE event log when editing a `.mako` file
**Plans**: TBD

### Phase 7: Completion
**Goal**: Users receive accurate autocomplete suggestions when typing Mako tag names and attributes, reducing typos and reference lookups
**Depends on**: Phase 6
**Requirements**: COMP-01, COMP-02
**Success Criteria** (what must be TRUE):
  1. Typing `<%` inside a `.mako` file triggers a completion popup listing all Mako directive names (`<%def`, `<%block`, `<%inherit`, `<%include`, `<%namespace`, `<%page`, `<%doc`)
  2. Typing inside the opening tag of a Mako directive (e.g., `<%def `) triggers attribute completions appropriate to that tag (`name=` for `<%def>`, `file=` for `<%inherit>`, `buffered=` for `<%def>`)
  3. Completion suggestions do not appear for plain HTML regions outside Mako constructs (no false positives)
**Plans**: TBD

### Phase 8: Error Annotations and Release Readiness
**Goal**: Definitively malformed Mako syntax is flagged with inline error indicators, and the plugin passes Plugin Verifier and is Marketplace-ready
**Depends on**: Phase 7
**Requirements**: COMP-03
**Success Criteria** (what must be TRUE):
  1. An unclosed Mako tag (e.g., `<%def name="foo">` with no `</%def>`) shows a red error annotation on the opening tag line
  2. An invalid or unrecognized Mako directive name (e.g., `<%bogus>`) shows an error annotation distinguishing it from valid directives
  3. `./gradlew runPluginVerifier` completes without errors against the configured target platform build
  4. The plugin installs and operates on a clean PyCharm 2025.2.x installation without exceptions in the event log
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Language Foundation | 2/2 | Complete    | 2026-02-19 |
| 2. Lexer | 3/3 | Complete   | 2026-02-19 |
| 3. Parser and PSI Tree | 3/3 | Complete    | 2026-02-19 |
| 4. Syntax Highlighting and Comment Support | 2/2 | Complete   | 2026-02-19 |
| 5. Structural Features | 0/2 | Not started | - |
| 6. Python Language Injection | 0/TBD | Not started | - |
| 7. Completion | 0/TBD | Not started | - |
| 8. Error Annotations and Release Readiness | 0/TBD | Not started | - |

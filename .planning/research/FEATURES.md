# Feature Research

**Domain:** JetBrains language support plugin — Mako template language for PyCharm
**Researched:** 2026-02-19
**Confidence:** MEDIUM (IntelliJ Platform API feature categories verified from official documentation structure; specific API names from training data through August 2025 — flag for version verification)

---

## Research Notes

**Sources used:**
- Project documentation: `.planning/PROJECT.md`, `.planning/codebase/` analysis files
- IntelliJ Platform SDK Docs knowledge (training data, August 2025 cutoff)
- Comparable plugins known from training: PyCharm's Jinja2/Django template support, Thymeleaf plugin, FreeMarker plugin, Velocity plugin, IntelliJ IDEA's built-in HTML/XML support
- IntelliJ Platform Custom Language Support Tutorial (training data)

**Confidence notes:**
- Feature categories (what types of features language plugins provide) — HIGH confidence from official IntelliJ Platform documentation structure
- API extension point names (e.g., `SyntaxHighlighter`, `CompletionContributor`) — MEDIUM confidence; stable APIs but verify exact names against current platform version 2025.2.5
- Competitor feature sets (Jinja2, Thymeleaf) — MEDIUM confidence; observed behavior may have changed
- Mako-specific complexity assessments — MEDIUM confidence; based on Mako language spec knowledge

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete. These are standard for any language plugin in JetBrains IDEs.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **File type registration** (.mako, .html with Mako) | Without it, files open as plain text with no features | LOW | Register `FileType` + `FileTypeFactory`; associate `.mako` extension. `.html` detection requires content-based sniffing (checking for Mako markers like `<%` or `${`) — adds complexity |
| **Syntax highlighting** — Mako constructs | Every language plugin provides this; plain text is the baseline users are escaping | MEDIUM | Requires `Lexer` + `SyntaxHighlighter` + color scheme descriptor. Complexity comes from three embedded languages: HTML structure, Mako directives, Python expressions |
| **Syntax highlighting** — embedded HTML | Templates are primarily HTML; users expect HTML colors inside `.mako` files | MEDIUM | Requires language injection or multi-language file support to hand off HTML regions to IDE's HTML highlighter |
| **Syntax highlighting** — embedded Python | `${expr}`, `% for`, `% if` lines contain Python; users expect Python colors | HIGH | Python language injection into Mako expression/control regions; depends on PyCharm Python plugin being present |
| **Code folding** — blocks and defs | Large templates are unreadable without folding `<%def>`, `<%block>`, control structures | MEDIUM | `FoldingBuilder` extension point. Fold regions: `<%def name="...">...</%def>`, `<%block name="...">...</%block>`, `% for/if/while` blocks |
| **Brace/tag matching** | Users expect `<%def>` to highlight its matching `</%def>`; standard IDE behavior | LOW | `BraceMatcher` or `PairedBraceMatcher` extension point |
| **Comment/uncomment** | Ctrl+/ to toggle `##` line comments in Mako | LOW | `Commenter` extension point. Mako uses `## comment` for line comments and `<%doc>...</%doc>` for block comments |
| **Basic error annotation** — malformed Mako | Red squiggle on syntactically invalid Mako (unclosed `<%def>`, etc.) | MEDIUM | `Annotator` or `ExternalAnnotator` extension point. Parser-level errors surface automatically from a correct grammar |
| **File icon** | `.mako` files should show a Mako icon, not a generic file icon | LOW | Register file icon in `FileType` definition; create 16x16 SVG icon |
| **File structure view** | Ctrl+F12 or Structure panel showing defs/blocks in the file | MEDIUM | `StructureViewBuilder` + `TreeElement` hierarchy. Show all `<%def>` and `<%block>` declarations as named nodes |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable — these raise the plugin from "usable" to "excellent."

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Code completion** — Mako tags and attributes | Autocomplete `<%def`, `<%block`, `<%inherit href="...">` etc. | MEDIUM | `CompletionContributor` extension point. Can be done without Python integration for Mako-specific keywords |
| **Code completion** — Python expressions in `${...}` | Reuse PyCharm's Python completion inside expression blocks | HIGH | Requires language injection into Python plugin's scope. Dependent on Python plugin dependency declaration in `plugin.xml` |
| **Go-to-definition** — template inheritance (`<%inherit>`) | Ctrl+click on `<%inherit href="base.html">` navigates to the base template file | MEDIUM | `PsiReference` on the `href` attribute of `<%inherit>` tags; resolves to file path relative to project |
| **Go-to-definition** — include/namespace resolution | Ctrl+click on `<%include file="...">` and `<%namespace file="...">` navigates to referenced file | MEDIUM | Same mechanism as inheritance — `PsiReference` on file path strings |
| **Go-to-definition** — def calls within templates | Navigating from `${self.body()}` or `${next.body()}` to the def declaration | HIGH | Requires cross-file PSI reference resolution; depends on namespace resolution working correctly |
| **Find usages** of template defs | "Find all usages" for a `<%def name="foo">` across the project | HIGH | Requires `PsiNamedElement` + `UsageSearcher`; depends on go-to-definition being implemented first |
| **Rename refactoring** — defs | Rename `<%def name="foo">` and update all call sites | HIGH | Requires `PsiNamedElement` + `RenameHandler`; depends on find usages |
| **Live templates / code snippets** | Snippets for common Mako patterns (`<%def>`, `<%block>`, `% for`) | LOW | `LiveTemplate` registration in plugin.xml. High user value, low implementation cost |
| **Inspections** — undefined variables | Flag `${undefined_var}` where variable is not defined in visible scope | HIGH | Requires Python type inference or at minimum scope tracking for `% for x in y:` bound variables |
| **Inspections** — inherited template structure validation | Detect when a child template references a block `<%block name="x">` that doesn't exist in parent | HIGH | Requires cross-file analysis and understanding `<%inherit>` resolution |
| **Settings panel** — file associations | Let users configure which `.html` extensions also get Mako treatment | LOW | `SearchableConfigurable` implementation; small UI with checkbox/pattern list |
| **Breadcrumb navigation** | Show path like `base.html > content_block > sidebar_def` in editor gutter | MEDIUM | `BreadcrumbsInfoProvider` extension point; shows structural context while editing deep in a template |
| **Color scheme customization** | Users can theme Mako-specific colors (expression delimiters, directive keywords, comments) | LOW | Color scheme descriptor in plugin.xml; the attributes are defined when implementing SyntaxHighlighter |
| **HTML-aware completion inside Mako** | HTML tag/attribute completion works inside Mako template HTML regions | MEDIUM | Automatic if language injection into HTML is implemented correctly — the HTML plugin handles it |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem like good ideas but create disproportionate complexity or maintenance burden.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **Template preview / render output** | "Show me what the HTML looks like" | Requires Mako Python runtime, project dependencies, context data — fundamentally an IDE plugin cannot execute arbitrary templates safely; maintenance nightmare | Out of scope per PROJECT.md. Direct users to run the application |
| **Full Python semantic analysis inside `${...}` without Python plugin dependency** | Users want "intelligent" Python completion even in IntelliJ IDEA (non-PyCharm) | Reimplementing Python type inference is a years-long project; results will always be inferior to PyCharm's Python plugin | Declare hard dependency on Python plugin; PyCharm-only is the correct scope |
| **Mako configuration file support** (`mako.conf`, application settings) | "Complete" support means touching config files too | Separate domain from template editing; config files use Python/INI syntax already supported by other plugins | Out of scope per PROJECT.md |
| **Web framework integration** (Pyramid routes, TurboGears URL generation) | "Jump to route handler from template" | Framework-specific; creates N separate maintenance problems for N frameworks; breaks the single-responsibility of a template language plugin | Out of scope per PROJECT.md; let framework-specific plugins handle this |
| **Auto-format / prettify Mako templates** | "Format Document" (Ctrl+Alt+L) for Mako | Extremely hard to implement correctly for a language that mixes HTML, Python, and Mako directives; formatting one layer breaks another | Implement basic indentation support only; defer full formatting. Recommend external formatters (e.g., `djlint`) as a separate tool |
| **Real-time Mako lint integration** (running `mako` parser as external tool) | Catch all Mako errors, not just lexer-level ones | External process execution on every keystroke is slow and fragile; process lifecycle management is complex | Implement parser-level error detection within the plugin's own grammar instead |

---

## Feature Dependencies

```
[File Type Registration]
    └──required by──> [Syntax Highlighting]
    └──required by──> [Code Folding]
    └──required by──> [Comment/Uncomment]
    └──required by──> [File Icon]
    └──required by──> [File Structure View]
    └──required by──> [All other features]

[Lexer + Grammar (PSI)]
    └──required by──> [Syntax Highlighting]
    └──required by──> [Code Folding]
    └──required by──> [Brace Matching]
    └──required by──> [Basic Error Annotation]
    └──required by──> [File Structure View]
    └──required by──> [Code Completion — Mako tags]
    └──required by──> [Go-to-definition]

[HTML Language Injection]
    └──required by──> [Syntax Highlighting — embedded HTML]
    └──enhances──>    [HTML-aware completion inside Mako]

[Python Language Injection]
    └──required by──> [Syntax Highlighting — embedded Python]
    └──required by──> [Code Completion — Python expressions]
    └──required by──> [Inspections — undefined variables]

[Go-to-definition — file references]
    └──required by──> [Find Usages]
    └──required by──> [Rename Refactoring]
    └──required by──> [Inspections — inherited template structure]

[Find Usages]
    └──required by──> [Rename Refactoring]
```

### Dependency Notes

- **File type registration requires nothing:** It is the foundation; everything else depends on it being correct.
- **Lexer + Grammar is the second foundation:** The PSI tree built from parsing is consumed by highlighting, folding, structure view, completion, and navigation. Investing in a correct grammar early pays dividends across all later features.
- **Language injection (HTML and Python) is the hardest architectural decision:** Decides whether Mako is a standalone language or a "host" language that injects regions into HTML/Python. This choice affects how all downstream features are built. See ARCHITECTURE.md.
- **Python plugin dependency gates several features:** Code completion inside `${...}`, proper Python expression error detection, and variable inspection all require PyCharm's Python plugin. This is the correct design — don't re-implement Python analysis.
- **Go-to-definition is prerequisite for refactoring:** Rename refactoring requires knowing all reference sites; finding reference sites requires the same resolution logic as go-to-definition.

---

## MVP Definition

### Launch With (v1)

Minimum viable product — what users need to stop using plain text editing.

- [ ] **File type registration** for `.mako` — without this, nothing else works and users have to configure it manually
- [ ] **Syntax highlighting — Mako constructs** — the single biggest quality-of-life improvement; distinguishes directives from content
- [ ] **Syntax highlighting — embedded HTML regions** — templates are HTML first; HTML coloring is expected
- [ ] **Syntax highlighting — embedded Python** — `% for`, `% if`, `${...}` blocks look wrong without Python colors
- [ ] **Code folding** for `<%def>`, `<%block>`, control flow blocks — large templates become navigable
- [ ] **Comment/uncomment** (Ctrl+/) for `##` line comments — basic editing ergonomics
- [ ] **File structure view** showing defs and blocks — navigate to any named construct quickly
- [ ] **Brace/tag matching** for Mako tag pairs — standard IDE expectation
- [ ] **File icon** for `.mako` files — visual identification in project tree

### Add After Validation (v1.x)

Features to add once the core highlighting/structure layer is solid and users have validated it.

- [ ] **Code completion — Mako tags and attributes** — add when user feedback confirms highlighting is working; completion requires PSI to be stable
- [ ] **Go-to-definition** for `<%inherit>`, `<%include>`, `<%namespace>` file references — highest-value navigation feature
- [ ] **Live templates** for common Mako constructs — low cost, high user satisfaction
- [ ] **Basic error annotations** — parser-level errors for malformed Mako syntax
- [ ] **Settings panel** for file associations — needed when users report `.html` files aren't being recognized

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Code completion — Python expressions inside `${...}`** — requires stable Python injection architecture; defer until injection is solid
- [ ] **Go-to-definition — def call resolution** (cross-file) — requires complex reference resolution; substantial scope expansion
- [ ] **Find usages of template defs** — depends on go-to-definition being complete
- [ ] **Rename refactoring** — high complexity, depends on find usages
- [ ] **Inspections — undefined variables** — requires Python type inference integration
- [ ] **Inspections — inherited template structure** — requires cross-file analysis
- [ ] **Breadcrumb navigation** — nice-to-have quality-of-life feature

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| File type registration (.mako) | HIGH | LOW | P1 |
| Syntax highlighting — Mako constructs | HIGH | MEDIUM | P1 |
| Syntax highlighting — embedded HTML | HIGH | MEDIUM | P1 |
| Syntax highlighting — embedded Python | HIGH | HIGH | P1 |
| Code folding (defs, blocks) | HIGH | MEDIUM | P1 |
| Comment/uncomment | MEDIUM | LOW | P1 |
| File structure view | HIGH | MEDIUM | P1 |
| Brace/tag matching | MEDIUM | LOW | P1 |
| File icon | LOW | LOW | P1 |
| Code completion — Mako tags | HIGH | MEDIUM | P2 |
| Go-to-definition — file references | HIGH | MEDIUM | P2 |
| Live templates | HIGH | LOW | P2 |
| Basic error annotations | MEDIUM | MEDIUM | P2 |
| Settings panel (file associations) | LOW | LOW | P2 |
| Code completion — Python expressions | HIGH | HIGH | P3 |
| Go-to-definition — def calls | MEDIUM | HIGH | P3 |
| Find usages | MEDIUM | HIGH | P3 |
| Rename refactoring | MEDIUM | HIGH | P3 |
| Inspections — undefined variables | HIGH | HIGH | P3 |
| Inspections — inherited template structure | MEDIUM | HIGH | P3 |
| Breadcrumb navigation | LOW | MEDIUM | P3 |
| Color scheme customization | LOW | LOW | P2 |

**Priority key:**
- P1: Must have for launch (MVP)
- P2: Should have, add when possible (v1.x)
- P3: Nice to have, future consideration (v2+)

---

## Competitor Feature Analysis

Reference plugins analyzed from training data (MEDIUM confidence — behavior may have changed since training cutoff August 2025).

| Feature | PyCharm Jinja2/Django templates | Thymeleaf plugin (JetBrains) | IntelliJ FreeMarker/Velocity | Our Approach |
|---------|--------------------------------|------------------------------|------------------------------|--------------|
| File type registration | Yes — `.html` with Django/Jinja2 mode detection | Yes — `.html` with `th:` namespace detection | Yes — `.ftl`, `.vm` extensions | Register `.mako` + content-based sniffing for `.html` |
| Syntax highlighting | Yes — directives, expressions, HTML coexist | Yes — Thymeleaf attrs highlighted in HTML | Yes — template tags highlighted | Same approach; three-language mixing |
| Embedded Python/Java | Yes (Django templates) — variable/filter syntax highlighted | No Python; Java expressions via EL | FreeMarker/Velocity use their own expression language | Full Python embedding via PyCharm Python plugin injection |
| Code completion | Yes — template tags, filters, variables from context | Yes — Thymeleaf attribute/expression completion | Yes — FTL/Velocity directive completion | Mako-specific completion; Python expressions defer to Python plugin |
| Go-to-definition — file references | Yes — `{% include %}`, `{% extends %}` navigate to files | Yes — Thymeleaf `th:replace` navigates to fragments | Yes — `#include` navigates | `<%inherit>`, `<%include>`, `<%namespace>` file navigation |
| Go-to-definition — defs/fragments | Yes — Django template tags, Jinja2 macros | Yes — Thymeleaf fragment navigation | Partial | `<%def>` navigation within and across files |
| Refactoring | Partial — rename for some constructs | Partial | Partial | MVP defers refactoring; add in v2 |
| Inspections / error detection | Yes — undefined variables, template syntax errors | Yes — Thymeleaf-specific validations | Partial | Start with parser-level; add semantic inspections in v2 |
| Structure view | Yes | Yes | Partial | Show `<%def>` and `<%block>` nodes |
| Code folding | Yes | Yes | Yes | Fold defs, blocks, control structures |
| Live templates | Yes — snippet library for common patterns | Yes | Yes | Include from v1.x |
| Multi-language file (HTML + template) | Yes — language injection approach | Yes | Yes | Core architectural challenge; language injection preferred |

**Key insight from comparison:** All mature template language plugins for JetBrains IDEs share the same architectural pattern — language injection into HTML. Jinja2/Django, Thymeleaf, and FreeMarker all inject their template language tokens into an HTML host, allowing the HTML plugin to handle HTML features and the template plugin to handle template-specific features. This is the proven pattern for Mako.

**Gap identified:** No existing JetBrains plugin for Mako exists (verified by PROJECT.md). The closest analogues are Django template support (bundled in PyCharm) and Jinja2 support (PyCharm). Both handle Python-based templates with Python expression embedding — same problem domain as Mako.

---

## Mako-Specific Syntax Coverage Checklist

Mako has several unique constructs that must all be handled. This drives the grammar/lexer scope.

| Mako Construct | Example | Feature Impact | Complexity |
|----------------|---------|----------------|------------|
| Expression substitution | `${variable}` | Highlighting, completion, Python injection | MEDIUM |
| Expression with filters | `${x \| h,trim}` | Highlighting (pipe syntax is Mako-specific) | LOW |
| Control lines | `% for x in items:` / `% endfor` | Highlighting, folding, indentation | MEDIUM |
| Def blocks | `<%def name="foo(arg)">...</%def>` | Highlighting, structure view, go-to-def, folding | HIGH |
| Named blocks | `<%block name="content">...</%block>` | Highlighting, structure view, go-to-def, folding | HIGH |
| Inheritance | `<%inherit file="base.html"/>` | Highlighting, go-to-def (file reference) | MEDIUM |
| Include | `<%include file="fragment.html"/>` | Highlighting, go-to-def (file reference) | LOW |
| Namespace | `<%namespace file="lib.html" name="lib"/>` | Highlighting, go-to-def, completion for `lib.` calls | HIGH |
| Module-level Python | `<%! import os %>` | Highlighting, Python injection | MEDIUM |
| Page-level Python | `<% x = 1 %>` | Highlighting, Python injection | MEDIUM |
| Page directive | `<%page args="x, y"/>` | Highlighting, argument tracking | MEDIUM |
| Line comments | `## This is a comment` | Comment/uncomment feature | LOW |
| Block comments | `<%doc>...</%doc>` | Highlighting, folding | LOW |
| Tags with attributes | `<%def name="..." buffered="True">` | Attribute highlighting, completion | LOW |
| Self/next/parent | `${self.body()}` | Highlighting, go-to-def (complex) | HIGH |
| Unicode literals | Standard Python unicode in expressions | Handled by Python injection | LOW |

---

## Sources

- Project documentation: `.planning/PROJECT.md` — Mako syntax constructs list, constraints, out-of-scope definitions
- `.planning/codebase/ARCHITECTURE.md` — Existing plugin scaffold structure
- IntelliJ Platform SDK documentation (training data, August 2025): Custom Language Support tutorial, extension point reference, Language Injection API
- Comparable plugins (training data, MEDIUM confidence): PyCharm Django/Jinja2 template support, JetBrains Thymeleaf plugin, IntelliJ FreeMarker/Velocity support
- Mako template language documentation (training data): https://docs.makotemplates.org/

---
*Feature research for: JetBrains Mako template language plugin (PyCharm)*
*Researched: 2026-02-19*

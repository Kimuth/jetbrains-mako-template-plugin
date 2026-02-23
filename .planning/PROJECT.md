# Mako Template Plugin for JetBrains

## What This Is

A PyCharm plugin providing first-class IDE support for Mako template files (`.mako`, `.html.mako`). Mako is a Python template engine used for generating HTML; this plugin brings syntax highlighting, code folding, structure navigation, tag completion, Python and HTML language injection, CSS/JS sub-language injection, and error detection to Mako templates in PyCharm — filling a gap where no JetBrains plugin currently exists. Shipped as `com.schtilig.mako` v0.3.0.

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
- ✓ Plugin Verifier passing for PC-252/PY-253/PY-261; Marketplace-ready as `com.schtilig.mako` v0.1.0 with plugin icon — v0.1.0
- ✓ `getName()` in PSI mixins returns attribute paired with `name=` key via ASTNode walk — v0.2.0 (PSI-01)
- ✓ `setName()` in PSI mixins throws `UnsupportedOperationException` — v0.2.0 (PSI-02)
- ✓ `updateText()` in all three injection host mixins throws `UnsupportedOperationException` — v0.2.0 (INJECT-01)
- ✓ Python injection range stops at first `FILTER_SEP` token — filter names excluded from injected fragment — v0.2.0 (INJECT-02)
- ✓ Recursive fold descent via `walkAllNodes` — nested doc/code/module blocks inside def/block produce fold regions — v0.2.0 (FOLD-01)
- ✓ `MAKO_CODE_CONTENT` default color changed from `STRING` to `IDENTIFIER` — v0.2.0 (VIEW-01)
- ✓ Structure View interleaves defs and blocks in document order (`sortedBy { textOffset }`) — v0.2.0 (VIEW-02)
- ✓ `MODULE_OPEN/CODE_CLOSE` brace pair registered in `MakoPairedBraceMatcher` — v0.2.0 (VIEW-03)
- ✓ Structure View def/block nodes use `AllIcons.Nodes.Function` instead of file icon — v0.2.0 (VIEW-04)
- ✓ `DUMMY_BLOCK` detection uses `element.language == Language.ANY` identity check — v0.2.0 (VIEW-05)
- ✓ `braceDepth` overflow emits `Logger` warning before clamping to 0xF — v0.2.0 (VIEW-06)
- ✓ `<%doc` insert handler uses captured `ltPos` for replacement range — v0.2.0 (COMP-01)
- ✓ Completion uses `document.charsSequence` backward scan instead of full `file.text` copy — v0.2.0 (COMP-02)
- ✓ Language guard uses `MakoLanguage` identity comparison, not string literal `"Mako Template"` — v0.2.0 (ANNOT-01)
- ✓ Parser fixture test covers unknown directive `<%bogus>` to guard against future lexer regressions — v0.2.0 (ANNOT-02)
- ✓ Dead `FILTER_NAME` token and unused `TEMPLATE_CONTENT`/`TAG_OPENS` sets removed — v0.2.0 (CLEAN-01, CLEAN-02, CLEAN-03)
- ✓ HTML language injection in TEMPLATE_TEXT regions via TemplateLanguageFileViewProvider — v0.3.0 (HINJ-01)
- ✓ HTML tag and attribute completion in template body regions — v0.3.0 (HINJ-02)
- ✓ Emmet abbreviation expansion in template body regions — v0.3.0 (HINJ-03)
- ✓ HTML error squiggles for malformed markup in template body — v0.3.0 (HINJ-04)
- ✓ CSS completion and validation inside `<style>` tags — v0.3.0 (HINJ-05)
- ✓ JavaScript completion and validation inside `<script>` tags — v0.3.0 (HINJ-06)
- ✓ `${...}` expressions in HTML attribute values produce no false-positive HTML errors — v0.3.0 (CRCT-01)
- ✓ Mako control lines (`%for`, `%if`, `%endif`) produce no false-positive HTML errors — v0.3.0 (CRCT-02)
- ✓ Python injection continues to work alongside FileViewProvider — v0.3.0 (RGRN-01)
- ✓ Code folding, structure view, and tag completion work alongside HTML injection — v0.3.0 (RGRN-02)
- ✓ All 98 automated tests pass unchanged after FileViewProvider is active — v0.3.0 (RGRN-03)

### Active

- [ ] **RFMT-01**: Ctrl+Alt+L applies HTML indentation rules without corrupting Mako control lines
- [ ] **NAVG-01**: Go To Definition for `<%include file="...">` — navigate to referenced file
- [ ] **NAVG-02**: Go To Definition for `<%inherit file="...">` — navigate to referenced file
- [ ] **NAVG-03**: Go To Definition for `<%namespace file="...">` — navigate to referenced file
- [ ] **NAVG-04**: Go To Definition from def call to `<%def>` definition
- [ ] **PYTH-01**: Template-local variables from `<% %>` blocks appear in `${...}` completion
- [ ] **PYTH-02**: Parameter hints for `<%def name="card(title, body)">` when def is invoked
- [ ] **ADVN-01**: Find usages of `<%def>` or `<%block>` across `.mako` files
- [ ] **ADVN-02**: Rename refactoring for `<%def>` or `<%block>` across files

### Out of Scope

- Support for non-PyCharm IDEs — focusing on PyCharm where Python integration matters most
- Runtime template rendering/preview — IDE editing support only, not a template engine
- Mako configuration file editing — focus is on template files themselves
- Integration with web frameworks (Pyramid, TurboGears routing) — pure template language support
- MultiHostInjector for HTML injection — produces fragmented HTML PSI; tag matching across Mako expression boundaries fails; TemplateLanguageFileViewProvider used instead
- Implement `updateText()` round-trip editing — high complexity; PSI write operations require platform expertise; deferred
- Implement `setName()` rename refactoring — requires cross-file reference resolution; deferred
- Filter-name tokenization in lexer — deferred; CLEAN-01 removes dead code instead

## Context

**v0.3.0 shipped 2026-02-23.** Implemented across 5 phases (18–22) in 2 days.

**Codebase state:**
- ~1,773 hand-written Kotlin LOC (src/main/kotlin), generated Java LOC in src/main/gen/
- 22 token types, 12 PSI node types, 9 color attributes, 6 fold construct types
- 98 unit tests (lexer, parser, folding, structure view, completion, annotator, injection host, injection range, FileViewProvider, CSS injector)
- testData/ contains 15 actively-loaded fixture files
- Plugin ID: `com.schtilig.mako`, display name: `Mako`, version: `0.3.0`
- Target: PyCharm Community 2025.2+ (build 252+)

**Tech stack:** Kotlin, Gradle 9.3.1, IntelliJ Platform 2025.2.5, GrammarKit 2023.3.0.2, JFlex 1.9.1, TemplateLanguageFileViewProvider

**Known tech debt from v0.3.0:**
- `TagAttrCompletionProvider` lacks explicit `language != MakoLanguage` guard — PSI pattern (`WHITE_SPACE`) provides implicit restriction; no functional risk; low priority
- `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java~` editor backup file — not git-tracked; harmless but untidy
- CSS injection via MakoCssInjector requires CSS plugin installed; JavaScript injection requires JavaScript plugin — degrades gracefully when absent but not tested in CI without plugins

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
| ASTNode child walk for getName() in mixins | `findChildByType()` returns first match; name= attribute ordering is not guaranteed; explicit walk pairs TAG_ATTR_NAME with its TAG_ATTR_VALUE sibling | ✓ Good — PSI-01 correctly resolved; 7 tests cover edge cases |
| setName()/updateText() throw UnsupportedOperationException | Silent no-op misleads callers; explicit exception gives actionable failure signal for callers attempting rename/round-trip edit | ✓ Good — consistent contract across PSI-02 and INJECT-01 |
| walkAllNodes Boolean return from visitor controls child recursion | Prevents double-fold when composite element and its child token are both visited | ✓ Good — FOLD-01 correctly handles nested constructs |
| Language.ANY identity check for DUMMY_BLOCK | `node.psi.language == Language.ANY` is stable against JetBrains type renames vs brittle `toString()` string comparison | ✓ Good — VIEW-05 resolved; stable across platform upgrades |
| FILTER_SEP retained; FILTER_NAME removed | Lexer emits FILTER_SEP for `|` inside `${...}`; Python injector uses it for boundary; FILTER_NAME was never emitted | ✓ Good — CLEAN-01 removes dead constant; INJECT-02 boundary preserved |
| IncompleteCodeBlock.mako deleted not completed | File was never git-tracked; MakoParsingTest had no `testIncompleteCodeBlock()` method; fixture was completely unreachable | ✓ Good — CLEAN-03 resolved cleanly without creating misleading test infrastructure |
| Annotator/folding tests migrated to inline configureMakoFile() | Disk fixtures for error-annotation tests create confusion about what is exercised; inline strings co-locate test content with assertions | ✓ Good — 4 orphaned fixtures deleted; 1 disk fixture retained (WellFormedDefTag.mako needed for checkHighlighting negative assertion) |
| LightVirtualFile guard in MakoFileViewProviderFactory | Returns SingleRootFileViewProvider for in-memory/test files — prevents ParsingTestCase from generating per-language fixture files (.Mako Template.txt, .HTML.txt) which break parser fixture tests | ✓ Good — all 98 tests pass with dual-tree active |
| OUTER_ELEMENT_TYPE as 4th arg, TEMPLATE_TEXT as 3rd arg to TemplateDataElementType | Field semantics from `javap`: `myOuterElementType` is the placeholder node in HTML PSI; `myTemplateElementType` is the Mako content type — transposing causes broken HTML PSI trees | ✓ Good — correct HTML PSI tree structure confirmed by MakoFileViewProviderTest |
| viewProvider.baseLanguage for dual-tree language identity checks | In dual-tree environment, `file.language` returns the data language for TEMPLATE_TEXT positions; `baseLanguage` is always MakoLanguage for .mako files | ✓ Good — completion and injection guards use baseLanguage reliably |
| Language.findLanguageByID("CSS") null guard instead of CssLanguage.INSTANCE | Avoids ClassNotFoundException when CSS plugin absent; gracefully degrades when CSS plugin not installed | ✓ Good — MakoCssInjector safe across PyCharm Community and Pro |
| No MakoJsInjector written — platform HtmlScriptLanguageInjector handles JS | Platform's HtmlScriptLanguageInjector already targets `<script>` XmlText in HTML PSI tree; writing a duplicate injector would conflict | ✓ Good — HINJ-06 closed with zero implementation; JS completions confirmed in running IDE |

---
*Last updated: 2026-02-23 after v0.3.0 milestone*

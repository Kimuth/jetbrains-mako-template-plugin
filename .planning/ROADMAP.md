# Roadmap: Mako Template Plugin for JetBrains

## Milestones

- ✅ **v0.1.0 Initial Release** — Phases 1–9 (shipped 2026-02-21)
- ✅ **v0.2.0 Bug Fixing & Cleanup** — Phases 10–17 (shipped 2026-02-22)
- 🚧 **v0.3.0 HTML Language Injection** — Phases 18–22 (in progress)

## Phases

<details>
<summary>✅ v0.1.0 Initial Release (Phases 1–9) — SHIPPED 2026-02-21</summary>

- [x] Phase 1: Language Foundation (2/2 plans) — completed 2026-02-19
- [x] Phase 2: Lexer (3/3 plans) — completed 2026-02-19
- [x] Phase 3: Parser and PSI Tree (3/3 plans) — completed 2026-02-19
- [x] Phase 4: Syntax Highlighting and Comment Support (2/2 plans) — completed 2026-02-19
- [x] Phase 5: Structural Features (3/3 plans) — completed 2026-02-20
- [x] Phase 6: Python Language Injection (2/2 plans) — completed 2026-02-21
- [x] Phase 7: Completion (2/2 plans) — completed 2026-02-21
- [x] Phase 8: Error Annotations and Release Readiness (3/3 plans) — completed 2026-02-21
- [x] Phase 9: Marketplace Branding (3/3 plans) — completed 2026-02-21

Full details: `.planning/milestones/v0.1.0-ROADMAP.md`

</details>

<details>
<summary>✅ v0.2.0 Bug Fixing & Cleanup (Phases 10–17) — SHIPPED 2026-02-22</summary>

- [x] Phase 10: PSI Correctness (1/1 plans) — completed 2026-02-21
- [x] Phase 11: Python Injection Fixes (2/2 plans) — completed 2026-02-21
- [x] Phase 12: Code Folding and Structure View (2/2 plans) — completed 2026-02-21
- [x] Phase 13: Editor Behavior Fixes (1/1 plans) — completed 2026-02-21
- [x] Phase 14: Completion Fixes (1/1 plans) — completed 2026-02-21
- [x] Phase 15: Annotator Fixes (1/1 plans) — completed 2026-02-21
- [x] Phase 16: Dead Code Cleanup (1/1 plans) — completed 2026-02-22
- [x] Phase 17: Clean Up Orphaned Test Fixtures (1/1 plans) — completed 2026-02-22

Full details: `.planning/milestones/v0.2.0-ROADMAP.md`

</details>

### v0.3.0 HTML Language Injection (In Progress)

**Milestone Goal:** Inject real HTML language into Mako TEMPLATE_TEXT regions so PyCharm delivers full HTML editing (coloring, tag/attribute completion, Emmet, error detection) inside `.mako` files — using `TemplateLanguageFileViewProvider` to create a parallel HTML PSI tree alongside the existing Mako PSI tree.

- [x] **Phase 18: FileViewProvider Scaffolding** - Create `MakoFileViewProvider`, `MakoFileViewProviderFactory`, and `OUTER_ELEMENT_TYPE`; register in `plugin.xml`; parallel HTML PSI tree exists and HTML features activate automatically (completed 2026-02-22)
- [x] **Phase 19: Regression Hardening** - Guard `MakoPythonInjector` and `MakoAnnotator` against the dual-tree environment; verify all existing automated tests pass unchanged (completed 2026-02-22)
- [x] **Phase 20: HTML Feature Verification and False-Positive Audit** - Verify HTML completions, Emmet, CSS/JS injection, and error detection work in TEMPLATE_TEXT; confirm `${...}` and Mako control lines produce no false-positive HTML errors (completed 2026-02-22)
- [ ] **Phase 21: CSS and JS Sub-Language Injection** - Implement `MakoCssInjector` (MultiHostInjector) to inject `CSSLanguage` into `<style>` element XmlText nodes in the HTML PSI tree; JavaScript injection handled by platform's `HtmlScriptLanguageInjector`; close HINJ-05 and HINJ-06
- [ ] **Phase 22: Documentation Hygiene** - Fix stale traceability descriptions for HINJ-01/HINJ-04 (still say "Verification failed"/"PARTIAL" after Phase 20 confirmed PASS); correct misleading `[x]` requirement-list checkboxes for HINJ-05/HINJ-06; fix cosmetic unchecked plan checkboxes in ROADMAP.md for 18-03 and 19-01

## Phase Details

### Phase 18: FileViewProvider Scaffolding
**Goal**: A parallel HTML PSI tree exists for every `.mako` file, enabling the full suite of HTML IDE features in template body regions automatically
**Depends on**: Phase 17 (v0.2.0 complete)
**Requirements**: HINJ-01, HINJ-02, HINJ-03, HINJ-04, HINJ-05, HINJ-06
**Success Criteria** (what must be TRUE):
  1. `file.viewProvider.allFiles.size == 2` for any `.mako` file opened in a test or running IDE — both Mako and HTML PSI roots are present
  2. The PSI Viewer shows a valid HTML PSI tree alongside the Mako PSI tree for a `.mako` file containing HTML markup
  3. HTML syntax coloring is visible in TEMPLATE_TEXT regions of `.mako` files in the running IDE (`./gradlew runIde`)
  4. HTML tag and attribute completion suggestions appear when typing `<div`, `class=`, or `href=` in a `.mako` template body region
  5. Emmet abbreviation expansion fires in a TEMPLATE_TEXT region (e.g., `div.container` expands to `<div class="container"></div>`)
**Plans**: 3 plans
Plans:
- [x] 18-01-PLAN.md — Wire TemplateLanguageFileViewProvider: add OUTER_ELEMENT_TYPE, defensive guards, MakoFileViewProvider, MakoFileViewProviderFactory, plugin.xml registration
- [x] 18-02-PLAN.md — Human verification of HTML features and regression check in running IDE (6/9 points passed)
- [x] 18-03-PLAN.md — Gap closure: verify HINJ-04 (HTML error squiggles) in running IDE; explicitly defer HINJ-01/05/06 to Phase 20

### Phase 19: Regression Hardening
**Goal**: All existing plugin features — Python injection, code folding, structure view, tag completion, and error annotations — work correctly alongside the HTML PSI tree; the full automated test suite passes without modification
**Depends on**: Phase 18
**Requirements**: RGRN-01, RGRN-02, RGRN-03
**Success Criteria** (what must be TRUE):
  1. `./gradlew check` reports zero test failures after `MakoFileViewProvider` is active — all existing lexer, parser, folding, structure view, completion, annotator, injection host, and injection range tests pass unchanged
  2. Python syntax highlighting and error detection are active inside `${...}`, `<% %>`, and `<%! %>` regions in the running IDE after the FileViewProvider is wired in
  3. Code folding gutter icons appear for `<%def>`, `<%block>`, and `<%doc>` regions in a `.mako` file that also has HTML content
  4. Structure View shows `<%def>` and `<%block>` nodes in document order for a `.mako` file open in the running IDE
**Plans**: 1 plan
Plans:
- [ ] 19-01-PLAN.md — Apply MakoStructureViewFactory dual-tree guard, add regression test, run ./gradlew check, verify IDE success criteria

### Phase 20: HTML Feature Verification and False-Positive Audit
**Goal**: HTML features are confirmed working in TEMPLATE_TEXT regions and Mako syntax (expressions, control lines) produces zero false-positive HTML error squiggles; the milestone is shippable
**Depends on**: Phase 19
**Requirements**: CRCT-01, CRCT-02
**Success Criteria** (what must be TRUE):
  1. A `.mako` file containing `<div class="${cls}">` shows no red HTML error squiggle on the `${cls}` expression — `OuterLanguageElement` boundary suppresses the false positive
  2. A `.mako` file containing `%for item in items:`, `%endfor`, `%if condition:`, and `%endif` lines shows no red HTML error squiggles on those Mako control lines
  3. A test asserts that `viewProvider.getPsi(HTMLLanguage.INSTANCE)` returns a non-null `HtmlFile` for a `.mako` fixture containing HTML markup
  4. CSS completion or validation is active inside a `<style>` tag in a `.mako` file in the running IDE (e.g., `color:` produces CSS property completions)
  5. JavaScript completion or validation is active inside a `<script>` tag in a `.mako` file in the running IDE (e.g., `document.` produces member completions)
**Plans**: 3 plans
Plans:
- [ ] 20-01-PLAN.md — Implement MakoEditorHighlighter, MakoEditorHighlighterProvider, MakoErrorFilter + plugin.xml registration + CRCT automated tests
- [ ] 20-02-PLAN.md — Run ./gradlew check; verify all tests pass including two new CRCT tests
- [ ] 20-03-PLAN.md — Human IDE verification of CRCT-01/02 false-positive suppression and HINJ-01/04/05/06 feature behavior

### Phase 21: CSS and JS Sub-Language Injection
**Goal**: CSS completion and validation are active inside `<style>` elements and JavaScript completion and validation are active inside `<script>` elements in `.mako` files in the running IDE
**Depends on**: Phase 20
**Requirements**: HINJ-05, HINJ-06
**Gap Closure**: Closes gaps from v0.3.0 milestone audit
**Success Criteria** (what must be TRUE):
  1. Typing `color:` inside `<style>...</style>` in a `.mako` file and pressing Ctrl+Space produces CSS property/value completions (not plain-text word completion)
  2. Typing `document.` inside `<script>...</script>` in a `.mako` file and pressing Ctrl+Space produces JavaScript member completions (e.g., `getElementById`, `querySelector`)
  3. CSS syntax errors inside `<style>` are highlighted with error squiggles
  4. JavaScript syntax errors inside `<script>` are highlighted with error squiggles
**Plans**: 2 plans
Plans:
- [ ] 21-01-PLAN.md — Implement MakoCssInjector (MultiHostInjector for XmlText in <style>), register in plugin.xml, add MakoCssInjectorTest, run ./gradlew check
- [ ] 21-02-PLAN.md — Human IDE verification of CSS completions (HINJ-05) and JavaScript completions (HINJ-06) in running IDE

### Phase 22: Documentation Hygiene
**Goal**: All REQUIREMENTS.md traceability descriptions and checkboxes accurately reflect the actual verification outcome from Phase 20; ROADMAP.md plan checklists are cosmetically correct
**Depends on**: Phase 21
**Requirements**: (documentation only — no new feature requirements)
**Gap Closure**: Addresses tech debt items from v0.3.0 milestone audit
**Success Criteria** (what must be TRUE):
  1. REQUIREMENTS.md traceability description for HINJ-01 no longer says "Verification failed — carry to Phase 20"; reflects Phase 20 PASS
  2. REQUIREMENTS.md traceability description for HINJ-04 no longer says "PARTIAL — carry to Phase 20"; reflects Phase 20 PASS
  3. REQUIREMENTS.md requirement-list checkboxes for HINJ-05 and HINJ-06 are unchecked `[ ]` (not `[x]`) until Phase 21 delivers completion
  4. ROADMAP.md plan checklist checkboxes for 18-03-PLAN.md and 19-01-PLAN.md are checked `[x]`
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Language Foundation | v0.1.0 | 2/2 | Complete | 2026-02-19 |
| 2. Lexer | v0.1.0 | 3/3 | Complete | 2026-02-19 |
| 3. Parser and PSI Tree | v0.1.0 | 3/3 | Complete | 2026-02-19 |
| 4. Syntax Highlighting and Comment Support | v0.1.0 | 2/2 | Complete | 2026-02-19 |
| 5. Structural Features | v0.1.0 | 3/3 | Complete | 2026-02-20 |
| 6. Python Language Injection | v0.1.0 | 2/2 | Complete | 2026-02-21 |
| 7. Completion | v0.1.0 | 2/2 | Complete | 2026-02-21 |
| 8. Error Annotations and Release Readiness | v0.1.0 | 3/3 | Complete | 2026-02-21 |
| 9. Marketplace Branding | v0.1.0 | 3/3 | Complete | 2026-02-21 |
| 10. PSI Correctness | v0.2.0 | 1/1 | Complete | 2026-02-21 |
| 11. Python Injection Fixes | v0.2.0 | 2/2 | Complete | 2026-02-21 |
| 12. Code Folding and Structure View | v0.2.0 | 2/2 | Complete | 2026-02-21 |
| 13. Editor Behavior Fixes | v0.2.0 | 1/1 | Complete | 2026-02-21 |
| 14. Completion Fixes | v0.2.0 | 1/1 | Complete | 2026-02-21 |
| 15. Annotator Fixes | v0.2.0 | 1/1 | Complete | 2026-02-21 |
| 16. Dead Code Cleanup | v0.2.0 | 1/1 | Complete | 2026-02-22 |
| 17. Clean Up Orphaned Test Fixtures | v0.2.0 | 1/1 | Complete | 2026-02-22 |
| 18. FileViewProvider Scaffolding | v0.3.0 | 3/3 | Complete | 2026-02-22 |
| 19. Regression Hardening | 1/1 | Complete    | 2026-02-22 | - |
| 20. HTML Feature Verification and False-Positive Audit | 3/3 | Complete    | 2026-02-22 | - |
| 21. CSS and JS Sub-Language Injection | v0.3.0 | 0/2 | Pending | - |
| 22. Documentation Hygiene | v0.3.0 | 0/? | Pending | - |

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-22 after v0.3.0 roadmap created)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 20 complete — v0.3.0 milestone achieved; CRCT-01/CRCT-02 verified at runtime

## Current Position

Phase: 20 of 20 (HTML Feature Verification and False-Positive Audit)
Plan: 03 of 3 completed (Human IDE verification — CRCT-01 PASS, CRCT-02 PASS, HINJ-01 PASS, HINJ-04 PASS, HINJ-05/HINJ-06 gaps documented; see 20-03-SUMMARY.md)
Status: Complete
Last activity: 2026-02-22 — Phase 20 Plan 03 complete (human IDE verification; all required criteria PASS; CSS/JS injection gaps documented for future phase)

Progress: [██████████] 100% (v0.3.0 — Phase 20 complete, all 3 plans executed)

## Performance Metrics

**v0.1.0 Velocity:**
- Total plans completed: 23
- Timeline: 3 days (2026-02-19 → 2026-02-21)
- Files changed: 222, LOC: ~4,800 (hand-written + generated + tests)

**v0.2.0 Velocity:**
- Total plans completed: 10
- Timeline: 2 days (2026-02-21 → 2026-02-22)
- Files changed: 138, 5,449 insertions, 507 deletions

**By Phase (v0.2.0):**

| Phase | Plans | Duration |
|-------|-------|----------|
| 10-psi-correctness | 1 | 3 min |
| 11-python-injection-fixes | 2 | ~10 min |
| 12-code-folding-and-structure-view | 2 | ~15 min |
| 13-editor-behavior-fixes | 1 | ~5 min |
| 14-completion-fixes | 1 | ~5 min |
| 15-annotator-fixes | 1 | ~5 min |
| 16-dead-code-cleanup | 1 | 15 min |
| 17-clean-up-orphaned-test-fixtures | 1 | 5 min |
| Phase 18-fileviewprovider-scaffolding P01 | 18 | 3 tasks | 8 files |
| Phase 19-regression-hardening P01 | 3 | 1 tasks | 2 files |
| Phase 20-html-feature-verification P01 | 3 min | 3 tasks | 5 files |
| Phase 20-html-feature-verification P02 | 1 min | 1 task | 0 files |
| Phase 20-html-feature-verification P03 | 15 min | 2 tasks | 0 files |

## Accumulated Context

### Decisions

All key decisions logged in PROJECT.md Key Decisions table.

Recent decisions affecting Phase 18:
- Use `lang.fileViewProviderFactory` EP (language-keyed), NOT `fileType.fileViewProviderFactory` — confirmed from `LangExtensionPoints.xml`
- `TemplateDataElementType` must be singleton per data-language ID (ConcurrentHashMap in companion object) — not per-file instance
- `contentElementType` on HTML PSI file must be set immediately after `def.createFile(this)` in `createFile()` override
- `OUTER_ELEMENT_TYPE` is 4th argument to `TemplateDataElementType`; `TEMPLATE_TEXT` is 3rd argument — transposing causes broken trees
- [Phase 18-fileviewprovider-scaffolding]: LightVirtualFile guard in MakoFileViewProviderFactory: return SingleRootFileViewProvider for in-memory files to prevent ParsingTestCase fixture file explosion
- [Phase 18-fileviewprovider-scaffolding]: viewProvider.baseLanguage check in MakoCompletionContributor instead of file.language: in dual-tree, file.language returns HTMLLanguage for TEMPLATE_TEXT positions
- [Phase 18-fileviewprovider-scaffolding]: OUTER_ELEMENT_TYPE is 4th arg to TemplateDataElementType; TEMPLATE_TEXT is 3rd — transposing causes broken HTML PSI trees
- [Phase 18-02 verification]: HINJ-01 (HTML coloring) gap — TemplateLanguageFileViewProvider builds the HTML PSI tree but syntax coloring is not visually applying; likely requires TemplateLanguageSyntaxHighlighter delegation or color scheme mapping
- [Phase 18-02 verification]: HINJ-05/HINJ-06 gaps — CSS/JS sub-injection inside <style>/<script> does not fire automatically from HTML PSI tree alone; platform Emmet handler operates on outer HTML context; requires additional MultiHostInjector or LanguageInjectionContributor wiring (Phase 20 scope)
- [Phase 18-03 verification]: HINJ-04 PARTIAL — HTML error squiggles work for inline elements (span) but not block/root elements (p, html); element-type-dependent annotator coverage; carry inconsistency investigation to Phase 20
- [Phase 18-03]: Phase 18 complete — all 3 plans executed; HINJ-01/04/05/06 deferred to Phase 20 with recorded rationale; HINJ-02 and HINJ-03 verified working
- [Phase 19-regression-hardening]: Use viewProvider.getPsi(MakoLanguage) ?: psiFile (no cast needed) in MakoStructureViewFactory dual-tree guard
- [Phase 19-regression-hardening]: requireNotNull(makoFile) after assertNotNull used in test to satisfy Kotlin null-safety without type mismatch from fail() return type
- [Phase 19-regression-hardening]: Known limitation (carry to Phase 20): os from <%! %> module-level block is not in scope for ${...} expressions — cross-injection scoping gap, not a regression introduced by Phase 18/19
- [Phase 19-regression-hardening]: Known limitation (carry to Phase 20): Single-line inline <%! import os %> triggers Unexpected indent from Python language service — workaround is multi-line syntax; pre-existing behavior, not a regression
- [Phase 20-01 html-coloring-and-false-positive-suppression]: MakoEditorHighlighter null-guards project/file in init block — getEditorHighlighter() called with nulls during Settings color scheme previews and early IDE init
- [Phase 20-01 html-coloring-and-false-positive-suppression]: MakoErrorFilter TokenSet contains EXPR_START, EXPR_END, CONTROL_LINE (not TEMPLATE_TEXT) — these are boundary tokens adjacent to OuterLanguageElement regions; TEMPLATE_TEXT is the HTML content token, not a boundary
- [Phase 20-01 html-coloring-and-false-positive-suppression]: CRCT tests provide structural coverage but may trivially pass if HTML annotator is inactive in BasePlatformTestCase — definitive runtime check is human IDE verification in plan 20-03
- [Phase 20-02 test-suite-verification]: Gradle test caching caused :test UP-TO-DATE on initial ./gradlew check; use --rerun flag to force actual test execution when confirming new tests after code-only plans
- [Phase 20-02 test-suite-verification]: All 95 tests across 11 suites pass including CRCT-01 and CRCT-02; no compilation errors in Phase 20-01 code additions
- [Phase 20]: CRCT-01 and CRCT-02 confirmed PASS at runtime in human IDE verification; Python Unresolved reference warnings on ${cls}/${item} are expected Python-layer validation, not HTML errors
- [Phase 20]: HINJ-05/HINJ-06 confirmed GAP at runtime: CSS/JS sub-language injection requires MultiHostInjector or LanguageInjectionContributor EP registrations beyond editorHighlighterProvider; carry to future phase

### Pending Todos

- [highlighting] Reference TextMate/VS Code Mako bundles for syntax decisions — `.planning/todos/pending/2026-02-20-reference-textmate-vscode-mako-bundles.md`

### Known Tech Debt (carry-forward from v0.2.0)

- `TagAttrCompletionProvider` lacks explicit `language != MakoLanguage` guard — PSI pattern provides implicit restriction; no functional risk
- `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java~` editor backup file — not git-tracked; harmless

### Known Gaps from Phase 19 Verification (carry to Phase 20)

- **Cross-injection scoping:** `os` imported in `<%! import os %>` module-level block is not in scope for `${os.getcwd()}` expressions; Python injection is active (unresolved reference squiggles fire for unknown names) but cross-block variable sharing is not wired up
- **Single-line inline blocks:** `<%! import os %>` on one line triggers "Unexpected indent" from Python language service; multi-line syntax `<%!\nimport os\n%>` is the workaround; pre-existing behavior, not a Phase 18/19 regression

### Known Gaps from Phase 18 Verification (resolved/updated in Phase 20)

- **HINJ-01 (HTML coloring):** RESOLVED in Phase 20 — MakoEditorHighlighter confirmed WORKING at runtime; HTML tags visually colored in TEMPLATE_TEXT regions
- **HINJ-04 (HTML error squiggles):** RESOLVED in Phase 20 — behavior confirmed CORRECT per HTML5 spec: span without close gets squiggle; p without close does not (optional-close element)
- **HINJ-05 (CSS in `<style>`):** CONFIRMED GAP — CSS sub-language injection not active; requires MultiHostInjector or LanguageInjectionContributor EP; carry to future phase
- **HINJ-06 (JS in `<script>`):** CONFIRMED GAP — JS sub-language injection not active; same root cause as HINJ-05; carry to future phase

### Blockers/Concerns

None.

### Quick Tasks Completed

| # | Description | Date | Commit | Status | Directory |
|---|-------------|------|--------|--------|-----------|
| 1 | Fix false-positive Unresolved Reference across mako code blocks | 2026-02-22 | 72a400c | Verified | [1-fix-false-positive-unresolved-reference-](./quick/1-fix-false-positive-unresolved-reference-/) |

## Session Continuity

Last session: 2026-02-22
Stopped at: Completed 20-html-feature-verification-and-false-positive-audit/20-03-PLAN.md (human IDE verification complete; CRCT-01/CRCT-02 PASS; Phase 20 done)
Resume with: Phase 20 complete — plan next phase (CSS/JS injection or new feature)

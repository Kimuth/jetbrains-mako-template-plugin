# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-22 after v0.3.0 roadmap created)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 19 — Regression Hardening (Phase 18 complete)

## Current Position

Phase: 18 of 20 (FileViewProvider Scaffolding)
Plan: 03 of 3 completed (gap closure — HINJ-04 partial, HINJ-01/05/06 deferred; see 18-03-SUMMARY.md)
Status: Complete
Last activity: 2026-02-22 — Phase 18 Plan 03 complete (HINJ-04 partial verification; Phase 18 declared complete)

Progress: [██░░░░░░░░] 20% (v0.3.0 — 2 of ~10 plans complete)

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

### Pending Todos

- [highlighting] Reference TextMate/VS Code Mako bundles for syntax decisions — `.planning/todos/pending/2026-02-20-reference-textmate-vscode-mako-bundles.md`

### Known Tech Debt (carry-forward from v0.2.0)

- `TagAttrCompletionProvider` lacks explicit `language != MakoLanguage` guard — PSI pattern provides implicit restriction; no functional risk
- `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java~` editor backup file — not git-tracked; harmless

### Known Gaps from Phase 18 Verification (carry to Phase 20)

- **HINJ-01 (HTML coloring):** HTML tags in TEMPLATE_TEXT regions not visually colored; TemplateLanguageSyntaxHighlighter delegation likely needed
- **HINJ-04 (HTML error squiggles) — PARTIAL:** Squiggles appear for `<span>` tags missing close tag, but NOT for `<p>` or `<html>` without closing tags; element-type-dependent behavior; inconsistent coverage requires investigation in Phase 20
- **HINJ-05 (CSS in `<style>`):** Emmet fires HTML handler instead of CSS; CSS language sub-injection inside HTML PSI tree not activating automatically
- **HINJ-06 (JS in `<script>`):** Emmet fires HTML handler instead of JS; JS language sub-injection inside HTML PSI tree not activating automatically

### Blockers/Concerns

None.

### Quick Tasks Completed

| # | Description | Date | Commit | Status | Directory |
|---|-------------|------|--------|--------|-----------|
| 1 | Fix false-positive Unresolved Reference across mako code blocks | 2026-02-22 | 72a400c | Verified | [1-fix-false-positive-unresolved-reference-](./quick/1-fix-false-positive-unresolved-reference-/) |

## Session Continuity

Last session: 2026-02-22
Stopped at: Completed 18-fileviewprovider-scaffolding/18-03-PLAN.md (gap closure — HINJ-04 partial; Phase 18 complete)
Resume with: `/gsd:plan-phase 19` (Regression Hardening) or `/gsd:plan-phase 20` (HTML Feature Verification and False-Positive Audit)

---
phase: 18-fileviewprovider-scaffolding
plan: "02"
subsystem: lang
tags: [kotlin, intellij-platform, templateLanguage, fileViewProvider, html-injection, verification]

# Dependency graph
requires:
  - phase: 18-fileviewprovider-scaffolding
    plan: "01"
    provides: MakoFileViewProvider wiring dual PSI tree — required to have HTML features to verify
provides:
  - Human verification results for HINJ-01 through HINJ-06
  - Partial pass: 6 of 9 verification points confirmed working
  - Known gaps: HTML syntax coloring not applying; CSS/JS injection inside <style>/<script> not firing
affects:
  - phase 19-regression-hardening (needs to know which features work vs gap)
  - phase 20-html-feature-verification-and-false-positive-audit (CSS/JS injection gaps carry forward as explicit work items)

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified: []

key-decisions:
  - "Plan 02 closed with partial pass (6/9 verification points). Three failures deferred: HTML syntax coloring (HINJ-01), CSS completion in <style> (HINJ-05), JS completion in <script> (HINJ-06). These require additional investigation beyond FileViewProvider scaffolding."
  - "CSS and JS gaps are Emmet/context mis-fires: platform still treats <style> and <script> content as HTML rather than dispatching to CSS/JS injector — TemplateLanguageFileViewProvider alone is insufficient for sub-language injection."

patterns-established: []

requirements-completed:
  - HINJ-02
  - HINJ-03
  - HINJ-04

# Metrics
duration: human-async
completed: 2026-02-22
---

# Phase 18 Plan 02: Human Verification Summary

**6 of 9 verification points passed; HTML coloring, CSS injection, and JS injection remain unresolved gaps requiring follow-up work**

## Performance

- **Duration:** async (human verification step — no wall-clock duration captured)
- **Started:** 2026-02-22
- **Completed:** 2026-02-22
- **Tasks:** 2 (1 automated IDE launch + 1 human verification checkpoint)
- **Files modified:** 0 (verification-only plan; no source changes)

## Accomplishments

- Confirmed HTML tag and attribute completion (HINJ-02) working: `<di` shows HTML tag popup; `class=` appears from attribute completion
- Confirmed Emmet expansion (HINJ-03) working in TEMPLATE_TEXT: `ul>li*3` + Tab expands correctly
- Confirmed Python injection regression-free: `${title}` retains Python highlighting
- Confirmed code folding regression-free: `<%def` fold icon present
- Confirmed Structure View regression-free: `card` def node visible

## Verification Results

| # | Requirement | Test | Result | Observed |
|---|-------------|------|--------|----------|
| 1 | HINJ-01 | HTML syntax coloring — `<div>` etc. appear different from Mako constructs | FAILED | HTML tags not colored differently from Mako constructs |
| 2 | HINJ-02 | HTML tag completion — `<di` shows popup | PASSED | Tag popup appears; choosing produces `<div></div>` |
| 3 | HINJ-02 | HTML attribute completion — `cl` inside `<div ` suggests `class=` | PASSED | `class=` suggestion appears |
| 4 | HINJ-03 | Emmet — `ul>li*3` + Tab expands | PASSED | Expands correctly to full list structure |
| 5 | Regression | Python injection — `${title}` has Python highlighting | PASSED | Python highlighting active |
| 6 | Regression | Code folding — `<%def` fold icon visible | PASSED | Fold icon present |
| 7 | Regression | Structure View — `card` def node visible | PASSED | Node visible |
| 8 | HINJ-05 | CSS completion in `<style>` — `font-` + Tab suggests CSS properties | FAILED | `font-` + Tab creates `<font-></font->` (Emmet firing in HTML context instead of CSS) |
| 9 | HINJ-06 | JS completion in `<script>` — `doc` + Tab suggests JS members | FAILED | `doc` + Tab creates full HTML document (Emmet firing in HTML context instead of JS) |

**Summary: 6 passed, 3 failed.**

## Task Commits

This plan has no code commits — it was a human verification checkpoint only.

No task commits.

No plan metadata commit (documentation-only plan with partial results; tracking files updated separately).

## Files Created/Modified

None — this plan produced no source changes. All wiring was done in Plan 01.

## Decisions Made

- Plan closed as partial pass. Three failures are not regressions introduced by Plan 01 code; they are gaps in what `TemplateLanguageFileViewProvider` alone delivers.
- HINJ-01 failure (HTML syntax coloring): the `TemplateDataElementType` bridge exists and the HTML PSI tree is confirmed active (Plan 01 tests pass), but the coloring is not visually applying in the editor. This likely requires either a color scheme mapping or an additional syntax highlighter delegation step.
- HINJ-05/HINJ-06 failures (CSS/JS inside `<style>`/`<script>`): the platform Emmet handler is operating on the outer HTML context rather than dispatching into the CSS or JS injected language. `TemplateLanguageFileViewProvider` builds one HTML tree for TEMPLATE_TEXT; sub-language injection inside `<style>`/`<script>` within that HTML tree requires an additional language injection configuration (e.g., `MultiHostInjector` or platform's built-in HTML language injection for CSS/JS). This is architectural work beyond scaffolding.

## Deviations from Plan

None — plan executed as written. The human verification step produced partial results as documented above; this is an outcome, not a deviation.

## Issues Encountered

Three verification points failed:

**HINJ-01 — HTML syntax coloring not applying:**
- HTML tags (`<div>`, `<h1>`, etc.) are not visually colored differently from Mako constructs in the editor.
- The dual PSI tree is active (Plan 01 tests confirm this), so the HTML tree exists. The gap is in color scheme / highlighter dispatch — the HTML coloring logic is not being triggered for TEMPLATE_TEXT tokens.
- Likely cause: `MakoSyntaxHighlighter` handles TEMPLATE_TEXT with a plain/no-color attribute; the HTML syntax highlighter needs to be consulted for tokens within TEMPLATE_TEXT regions. May require `SyntaxHighlighterFactory` delegation or a `TemplateLanguageSyntaxHighlighter` approach.

**HINJ-05 — CSS completion/Emmet in `<style>` not working:**
- Typing `font-` + Tab inside a `<style>` block produces `<font-></font->` — the HTML Emmet handler fires, not the CSS one.
- The platform's built-in CSS language injection into `<style>` tags in HTML files is not activating inside the Mako HTML PSI tree. The HTML PSI tree exists, but the secondary sub-language dispatch (CSS inside `<style>`) requires additional wiring.

**HINJ-06 — JS completion/Emmet in `<script>` not working:**
- Typing `doc` + Tab inside a `<script>` block produces a full HTML document — the HTML Emmet handler fires, not a JS one.
- Same root cause as HINJ-05: the platform's JavaScript injection into `<script>` tags is not activating in the Mako context.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**What is confirmed working and stable:**
- HTML tag/attribute completion (HINJ-02): production-ready
- Emmet in TEMPLATE_TEXT (HINJ-03): production-ready
- Python injection, code folding, Structure View: no regressions

**Known gaps that require follow-up:**
1. **HINJ-01 (HTML coloring):** Investigate `TemplateLanguageSyntaxHighlighter` or syntax highlighter delegation for TEMPLATE_TEXT regions. Possibly a separate plan in Phase 19 or 20.
2. **HINJ-05 / HINJ-06 (CSS/JS sub-injection):** Determine whether platform HTML-to-CSS/JS injection activates automatically once HTML PSI is present, or whether an explicit `MultiHostInjector` or `LanguageInjectionContributor` is required for `<style>` and `<script>` content in the Mako HTML tree. This is the primary scope of Phase 20.

Phase 18 is functionally complete for scaffolding (all code in place, dual tree active, core HTML features working). The three failing items are product gaps, not implementation regressions, and are appropriate to carry into Phase 19/20 planning.

---
*Phase: 18-fileviewprovider-scaffolding*
*Completed: 2026-02-22*

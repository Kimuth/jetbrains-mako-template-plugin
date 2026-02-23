---
phase: 20-html-feature-verification-and-false-positive-audit
plan: "03"
subsystem: testing
tags: [intellij-platform, html-injection, ide-verification, crct, hinj]

# Dependency graph
requires:
  - phase: 20-01
    provides: MakoEditorHighlighter, MakoEditorHighlighterProvider, MakoErrorFilter with plugin.xml registrations
  - phase: 20-02
    provides: Full test suite confirmation — 95 tests pass, CRCT-01/CRCT-02 verified in MakoFileViewProviderTest

provides:
  - Human-verified runtime IDE behavior for all Phase 20 criteria
  - CRCT-01 PASS: ${cls} expression in div class attribute shows no HTML error squiggle
  - CRCT-02 PASS: %for/%endfor control lines show no HTML error squiggles
  - HINJ-01 PASS: HTML tags visually colored in TEMPLATE_TEXT regions
  - HINJ-04 PASS: span without close tag shows squiggle; p without close tag shows no squiggle (HTML5 spec)
  - HINJ-05 GAP documented: CSS sub-language completion not active inside <style> blocks
  - HINJ-06 GAP documented: JavaScript member completion not active inside <script> blocks

affects: [future-css-js-injection-phase]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "editorHighlighterProvider EP activates MakoEditorHighlighter for .mako files — confirmed working at runtime"
    - "highlightErrorFilter EP via MakoErrorFilter suppresses PsiErrorElement false positives near OuterLanguageElement boundaries — confirmed working at runtime"
    - "CSS/JS sub-language injection requires additional MultiHostInjector or LanguageInjectionContributor EP registrations beyond editorHighlighterProvider — not provided by Phase 20"

key-files:
  created:
    - hinj04_test.mako — verification test fixture at project root (created in Task 1, not committed to src/)
  modified: []

key-decisions:
  - "CRCT-01 and CRCT-02 confirmed PASS at runtime: Python 'Unresolved reference' warnings on ${cls} and ${item} are expected and correct (Python identifiers validated by PyCharm); they are NOT HTML errors and do not constitute CRCT failures"
  - "HINJ-05 GAP: CSS property completions not active inside <style> blocks — only plain-text word completion fires (color: + Ctrl+Space yields no suggestions; typing 'colo' + Ctrl+Space shows word matches only). Root cause: editorHighlighterProvider wires syntax coloring but does not register MultiHostInjector or LanguageInjectionContributor for CSS sub-language injection inside HTML <style> elements. Carry to future phase."
  - "HINJ-06 GAP: JavaScript member completions not active inside <script> blocks — no document.getElementById etc. Root cause: same as HINJ-05; JS sub-language injection not wired via MultiHostInjector or LanguageInjectionContributor EP. Carry to future phase."
  - "Phase 20 milestone scope boundary: editorHighlighterProvider + highlightErrorFilter EPs fully address false-positive suppression (CRCT) and HTML syntax coloring (HINJ-01). CSS/JS sub-injection is a separate, non-trivial feature requiring additional EP work scoped to a future phase."

patterns-established: []

requirements-completed: [CRCT-01, CRCT-02]

# Metrics
duration: 15min
completed: 2026-02-22
---

# Phase 20 Plan 03: Human IDE Verification Summary

**CRCT-01 and CRCT-02 confirmed PASS at runtime; HINJ-01 and HINJ-04 pass; CSS/JS sub-injection gaps (HINJ-05/HINJ-06) documented with root cause for future phase**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-02-22T21:30:00Z
- **Completed:** 2026-02-22T21:44:33Z
- **Tasks:** 2 (Task 1 auto, Task 2 human-verify checkpoint)
- **Files modified:** 0 (verification-only plan)

## Accomplishments

- CRCT-01 PASS: `<div class="${cls}">` produces no HTML error squiggle on `${cls}` — MakoErrorFilter successfully suppresses false positives on expression boundaries
- CRCT-02 PASS: `%for item in items:` / `%endfor` control lines produce no HTML error squiggles — MakoErrorFilter suppresses false positives on CONTROL_LINE tokens
- HINJ-01 PASS: HTML tags in TEMPLATE_TEXT regions are visually colored (different color from Mako constructs) — MakoEditorHighlighter activating correctly via editorHighlighterProvider EP
- HINJ-04 PASS: `<span>unclosed span` shows error squiggle (correct — span requires closing); `<p>unclosed paragraph` shows no squiggle (correct — HTML5 optional-close element)
- HINJ-05 GAP documented: CSS property completions not active inside `<style>` blocks — only plain-text word completion fires
- HINJ-06 GAP documented: JavaScript member completions not active inside `<script>` blocks

## Verification Results

| Criterion | ID | Result | Notes |
|-----------|-----|--------|-------|
| No HTML squiggle on `${cls}` | CRCT-01 | PASS | Python "Unresolved reference 'cls'" warning is expected; that is PyCharm Python validation, NOT an HTML error |
| No HTML squiggle on `%for`/`%endfor` | CRCT-02 | PASS | Python "Unresolved reference 'item'" warning is expected; same reasoning as CRCT-01 |
| HTML tag coloring in TEMPLATE_TEXT | HINJ-01 | PASS | HTML tags visually colored differently from Mako constructs |
| HTML5 error squiggle correctness | HINJ-04 | PASS | `<span>` without close gets squiggle; `<p>` without close does not (spec-correct) |
| CSS completions inside `<style>` | HINJ-05 | GAP | `color:` + Ctrl+Space = no suggestions; `colo` + Ctrl+Space = plain-text word matches only. CSS sub-language injection not active. |
| JS completions inside `<script>` | HINJ-06 | GAP | No `document.getElementById` etc. JS sub-language injection not active. |

## Task Commits

1. **Task 1: Launch runIde and prepare test file** - `d5954a2` (chore)

No Task 2 commit — checkpoint task produces no source changes; human-verified behavior is recorded in this SUMMARY.

## Files Created/Modified

- `hinj04_test.mako` — verification test fixture created at project root during Task 1 (untracked, not part of src/)

## Decisions Made

- Python "Unresolved reference" warnings on `${cls}` and `${item}` are expected and correct: these are Python identifiers that PyCharm validates through the Python injection layer. They are not HTML errors. CRCT criteria specifically guard against HTML error squiggles — both pass.
- HINJ-05 and HINJ-06 remain as documented gaps. The `editorHighlighterProvider` EP (which wires `MakoEditorHighlighter`) handles syntax highlighting delegation but does NOT activate CSS/JS sub-language injection inside `<style>` and `<script>` blocks. That requires separate `multiHostInjector` or `languageInjectionContributor` EP registrations with injection logic to tell the platform "inside this HTML element, use CSS/JS language". This is a non-trivial feature scoped to a future phase.

## Deviations from Plan

None - plan executed exactly as written. The checkpoint was resolved by human verification. HINJ-05 and HINJ-06 gaps were anticipated in the plan's must_haves (explicit OR-clause: "CSS completion is active OR this gap is explicitly documented").

## Issues Encountered

None. All verification results were unambiguous. The Python "Unresolved reference" warnings required clarification that they are expected Python-layer validation, not HTML false positives — this distinction confirms CRCT-01 and CRCT-02 pass rather than fail.

## Gap Analysis: HINJ-05 and HINJ-06 Root Cause

**Root cause:** The `editorHighlighterProvider` extension point wires `MakoEditorHighlighter` (a `LayeredLexerEditorHighlighter`) for syntax coloring. It does not register any injection contributors. For CSS/JS completions to fire inside `<style>`/`<script>` blocks, the IntelliJ platform needs:

1. A `MultiHostInjector` (or `LanguageInjectionContributor`) that identifies `<style>` PSI elements within the Mako HTML PSI tree and declares "inject CSSLanguage here"
2. Similarly for `<script>` → JavaScript language injection

Without these registrations, Ctrl+Space inside `<style>` falls through to plain-text word completion, and inside `<script>` no JavaScript member completions fire.

**Phase 20 scope boundary:** Phase 20 addressed false-positive suppression (CRCT) and HTML syntax coloring (HINJ-01). CSS/JS sub-injection is a separate, independent feature requiring dedicated EP work.

**Carry-forward:** HINJ-05 and HINJ-06 gaps are documented in STATE.md for a future CSS/JS injection phase.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 20 is complete. All required criteria (CRCT-01, CRCT-02) confirmed PASS at runtime.
- MakoEditorHighlighter + MakoErrorFilter are production-ready and verified in a live IDE session.
- v0.3.0 milestone is shippable with documented known gaps (HINJ-05/HINJ-06).
- Future work: CSS/JS sub-language injection via MultiHostInjector or LanguageInjectionContributor EPs.

---
*Phase: 20-html-feature-verification-and-false-positive-audit*
*Completed: 2026-02-22*

## Self-Check: PASSED

- FOUND: .planning/phases/20-html-feature-verification-and-false-positive-audit/20-03-SUMMARY.md
- FOUND: Task 1 commit d5954a2 verified via git log
- CRCT-01 PASS recorded
- CRCT-02 PASS recorded
- HINJ-01 PASS recorded
- HINJ-04 PASS recorded
- HINJ-05 GAP documented with root cause
- HINJ-06 GAP documented with root cause

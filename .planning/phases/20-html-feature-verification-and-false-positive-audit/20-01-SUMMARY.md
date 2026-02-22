---
phase: 20-html-feature-verification-and-false-positive-audit
plan: "01"
subsystem: highlighting
tags: [intellij-platform, layered-highlighter, template-language, error-filter, html-coloring]

# Dependency graph
requires:
  - phase: 18-fileviewprovider-scaffolding
    provides: MakoFileViewProvider (TemplateLanguageFileViewProvider) and dual PSI tree
  - phase: 20-research
    provides: Verified patterns for LayeredLexerEditorHighlighter and TemplateLanguageErrorFilter

provides:
  - MakoEditorHighlighter extends LayeredLexerEditorHighlighter — HTML coloring in TEMPLATE_TEXT regions
  - MakoEditorHighlighterProvider implements EditorHighlighterProvider — factory registered via editorHighlighterProvider EP
  - MakoErrorFilter extends TemplateLanguageErrorFilter — suppresses false-positive HTML errors at Mako boundaries
  - Two automated CRCT tests in MakoFileViewProviderTest confirming no ERROR-severity HTML highlights

affects: [20-html-feature-verification-and-false-positive-audit, 21-css-js-injection-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - LayeredLexerEditorHighlighter with registerLayer(TEMPLATE_TEXT, htmlHighlighter) for HTML coloring in template files
    - TemplateLanguageErrorFilter with EXPR_START/EXPR_END/CONTROL_LINE TokenSet for false-positive suppression
    - editorHighlighterProvider EP with filetype="Mako Template" (all lowercase attribute name)
    - highlightErrorFilter EP via com.intellij.highlightErrorFilter for HTML error suppression

key-files:
  created:
    - src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighter.kt
    - src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighterProvider.kt
    - src/main/kotlin/com/schtilig/mako/lang/annotation/MakoErrorFilter.kt
  modified:
    - src/main/resources/META-INF/plugin.xml
    - src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt

key-decisions:
  - "MakoEditorHighlighter null-guards project and file: getEditorHighlighter() is called with null args during color scheme previews and early IDE init"
  - "MakoErrorFilter TokenSet contains EXPR_START, EXPR_END, CONTROL_LINE — not TEMPLATE_TEXT — because these are the tokens at OuterLanguageElement boundaries in the HTML PSI tree"
  - "TemplateLanguageErrorFilter 3-arg constructor with 'HTML' sub-language ID matches HbErrorFilter canonical pattern"
  - "CRCT tests trivially pass if HTML annotator is inactive in BasePlatformTestCase — documented as structural coverage; definitive check is human IDE verification in plan 20-03"

patterns-established:
  - "Template language editor highlighter: extend LayeredLexerEditorHighlighter, registerLayer(templateTextToken, dataLanguageHighlighter) in null-guarded init block"
  - "False-positive suppression: extend TemplateLanguageErrorFilter with boundary-token TokenSet and FileViewProvider class"

requirements-completed: [CRCT-01, CRCT-02]

# Metrics
duration: 3min
completed: 2026-02-22
---

# Phase 20 Plan 01: HTML Coloring and False-Positive Suppression Summary

**LayeredLexerEditorHighlighter for HTML coloring in TEMPLATE_TEXT plus TemplateLanguageErrorFilter suppressing false-positive squiggles at ${...} and %for/%if boundaries**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-22T21:23:50Z
- **Completed:** 2026-02-22T21:26:34Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- MakoEditorHighlighter registers HTML SyntaxHighlighter as a layer for TEMPLATE_TEXT tokens via LayeredLexerEditorHighlighter, enabling HTML syntax coloring in template regions (HINJ-01)
- MakoErrorFilter extends TemplateLanguageErrorFilter with EXPR_START/EXPR_END/CONTROL_LINE TokenSet, suppressing false-positive HTML error squiggles at Mako expression and control line boundaries (CRCT-01, CRCT-02)
- Both extension points registered in plugin.xml: editorHighlighterProvider with filetype="Mako Template" and highlightErrorFilter with MakoErrorFilter
- Two CRCT automated tests added to MakoFileViewProviderTest (6 total, all passing)

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement MakoEditorHighlighter and MakoEditorHighlighterProvider** - `100962d` (feat)
2. **Task 2: Implement MakoErrorFilter and register both extensions in plugin.xml** - `44961f6` (feat)
3. **Task 3: Add CRCT automated tests to MakoFileViewProviderTest** - `210f067` (test)

## Files Created/Modified

- `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighter.kt` - Extends LayeredLexerEditorHighlighter; registers HTML SyntaxHighlighter for TEMPLATE_TEXT tokens with null-guard for project/file
- `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighterProvider.kt` - Implements EditorHighlighterProvider; returns MakoEditorHighlighter for all Mako Template file opens
- `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoErrorFilter.kt` - Extends TemplateLanguageErrorFilter; TokenSet contains EXPR_START, EXPR_END, CONTROL_LINE; binds to MakoFileViewProvider::class.java
- `src/main/resources/META-INF/plugin.xml` - Added editorHighlighterProvider (filetype="Mako Template") and highlightErrorFilter EP registrations
- `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` - Added testNoFalsePositiveHtmlErrorOnMakoExpression and testNoFalsePositiveHtmlErrorOnMakoControlLines (CRCT-01, CRCT-02)

## Decisions Made

- Null-guard in MakoEditorHighlighter init block: `getEditorHighlighter()` is called with null `project` and `file` during IDE startup and Settings color scheme previews. Only calling `registerLayer` when both are non-null prevents NPE and matches the RST plugin's verified pattern.
- MakoErrorFilter TokenSet uses EXPR_START, EXPR_END, CONTROL_LINE (not TEMPLATE_TEXT): these are the Mako tokens adjacent to OuterLanguageElement placeholder regions in the HTML PSI tree. TEMPLATE_TEXT is the token for content that IS HTML — not a boundary token.
- TemplateLanguageErrorFilter 3-arg constructor with "HTML" as the known sub-language ID: matches the Handlebars HbErrorFilter canonical pattern verified from the intellij-plugins repository.
- CRCT tests documented as structural coverage: BasePlatformTestCase.doHighlighting() may not run the HTML annotator that produces false positives. Tests confirm zero ERROR-severity highlights in the test environment; definitive runtime verification is the human IDE check in plan 20-03.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The XmlTagNameSynchronizer disposal warning in test stderr is a pre-existing platform test teardown timing issue (present before this plan, not caused by new code). All 6 tests pass with BUILD SUCCESSFUL.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- HINJ-01 code is in place — MakoEditorHighlighter provides the layered highlighter for HTML coloring in TEMPLATE_TEXT regions; visual effect requires runIde verification (plan 20-03)
- CRCT-01 and CRCT-02 are suppressed at the code level — automated tests pass; runtime false-positive verification requires runIde (plan 20-03)
- CSS/JS injection (HINJ-05, HINJ-06) may automatically resolve once the layered editor highlighter is in place — empirical runIde verification required before writing any additional code

---
*Phase: 20-html-feature-verification-and-false-positive-audit*
*Completed: 2026-02-22*

## Self-Check: PASSED

- FOUND: src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighter.kt
- FOUND: src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoEditorHighlighterProvider.kt
- FOUND: src/main/kotlin/com/schtilig/mako/lang/annotation/MakoErrorFilter.kt
- FOUND: .planning/phases/20-html-feature-verification-and-false-positive-audit/20-01-SUMMARY.md
- FOUND: commit 100962d (feat(20-01): add MakoEditorHighlighter and MakoEditorHighlighterProvider)
- FOUND: commit 44961f6 (feat(20-01): add MakoErrorFilter and register both extensions in plugin.xml)
- FOUND: commit 210f067 (test(20-01): add CRCT-01 and CRCT-02 automated tests to MakoFileViewProviderTest)

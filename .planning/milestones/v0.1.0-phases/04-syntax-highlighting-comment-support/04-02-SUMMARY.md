---
phase: 04-syntax-highlighting-comment-support
plan: "02"
subsystem: syntax-highlighting
tags: [kotlin, intellij-platform, text-attributes-key, expression-highlighting, markup-tag]

# Dependency graph
requires:
  - phase: 04-syntax-highlighting-comment-support/04-01
    provides: MakoSyntaxHighlighter with MAKO_EXPRESSION TextAttributesKey using TEMPLATE_LANGUAGE_COLOR fallback
provides:
  - MAKO_EXPRESSION TextAttributesKey with MARKUP_TAG fallback — ${...} expressions display in distinct color from surrounding template text
affects:
  - 05-template-language
  - UAT expression highlighting verification

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Use DefaultLanguageHighlighterColors.MARKUP_TAG for Mako expression tokens to guarantee visual distinction in all built-in color schemes

key-files:
  created: []
  modified:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt

key-decisions:
  - "MARKUP_TAG fallback for MAKO_EXPRESSION: DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR inherits from HighlighterColors.TEXT which has no visible foreground color, making ${...} expressions indistinguishable from plain text; MARKUP_TAG provides a distinct color in all standard schemes (dark cyan/teal in Darcula, dark blue/navy in Light, visible foreground in High Contrast)"

patterns-established:
  - "Expression token fallback pattern: use MARKUP_TAG (not TEMPLATE_LANGUAGE_COLOR) for Mako expression syntax so ${...} constructs are always visually distinct from surrounding template text regardless of color scheme"

requirements-completed: [SYNX-02]

# Metrics
duration: 1min
completed: 2026-02-20
---

# Phase 4 Plan 02: Expression Highlighting Fix Summary

**MAKO_EXPRESSION fallback changed from TEMPLATE_LANGUAGE_COLOR to MARKUP_TAG, making ${...} expressions visually distinct from surrounding template text in all standard color schemes**

## Performance

- **Duration:** 1 min
- **Started:** 2026-02-19T23:38:56Z
- **Completed:** 2026-02-19T23:40:12Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Fixed expression highlighting gap identified in UAT test 1: ${...} expressions are now visually distinct from plain template text
- MARKUP_TAG fallback renders as dark cyan/teal in Darcula, dark blue/navy in Light/IntelliJ, and a visible foreground in High Contrast schemes
- All 22 tests remain green; compilation passes with no regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: Change MAKO_EXPRESSION fallback from TEMPLATE_LANGUAGE_COLOR to MARKUP_TAG** - `e72d3b2` (fix)

**Plan metadata:** (docs commit — this summary)

## Files Created/Modified
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt` - Changed MAKO_EXPRESSION TextAttributesKey fallback from TEMPLATE_LANGUAGE_COLOR to MARKUP_TAG (single line change, line 22)

## Decisions Made
- MARKUP_TAG chosen over TEMPLATE_LANGUAGE_COLOR because TEMPLATE_LANGUAGE_COLOR inherits from HighlighterColors.TEXT which carries no foreground color — expressions were invisible against the template background. MARKUP_TAG is a semantic match (markup/template expression syntax) and guaranteed to be colored distinctly in all built-in JetBrains color schemes.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

During `./gradlew build`, the `:prepareSandbox` task failed with a Windows file-lock error because a running IDE instance had the sandbox JAR memory-mapped. This is an environment issue, not a code issue. The compilation, instrumentation, and all 22 tests passed successfully as verified by running `./gradlew test` directly.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Expression highlighting gap is now closed; UAT test 1 (expressions display in distinct color) should pass on next manual verification
- All syntax highlighting TextAttributesKey constants are stable; Phase 5 template language integration can proceed
- No blockers

---
*Phase: 04-syntax-highlighting-comment-support*
*Completed: 2026-02-20*

## Self-Check: PASSED

- FOUND: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt`
- FOUND: `.planning/phases/04-syntax-highlighting-comment-support/04-02-SUMMARY.md`
- FOUND commit: `e72d3b2`
- CONFIRMED: `MARKUP_TAG` appears 1 time in MakoSyntaxHighlighter.kt
- CONFIRMED: `TEMPLATE_LANGUAGE_COLOR` appears 0 times in MakoSyntaxHighlighter.kt

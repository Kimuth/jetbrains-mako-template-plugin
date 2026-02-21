---
phase: 08-error-annotations-and-release-readiness
plan: 01
subsystem: language-support
tags: [annotator, intellij, psi, error-highlighting, squiggle]

# Dependency graph
requires:
  - phase: 07-completion
    provides: MakoCompletionContributor, 64 green tests baseline
  - phase: 03-parser
    provides: MakoDefTag, MakoBlockTag, MakoTemplateTextContent PSI interfaces; MakoTokenTypes.END_TAG
provides:
  - MakoAnnotator.kt in lang.annotation package — error squiggles for unclosed tags and unknown directives
  - annotator extension point registered in plugin.xml for "Mako Template" language
affects: [09-marketplace-branding]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "IntelliJ Annotator API pattern: implement Annotator, use holder.newAnnotation(HighlightSeverity.ERROR).range(...).create()"
    - "PSI interface is-check pattern: use Kotlin 'is MakoDefTag' (interface), NOT element.javaClass == MakoDefTagImpl::class.java"
    - "END_TAG child detection: tag.node.findChildByType(MakoTokenTypes.END_TAG) != null for structural well-formedness"

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/annotation/MakoAnnotator.kt
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "language='Mako Template' (not 'any') for annotator registration — unlike completion contributors, annotators registered with the specific language ID receive correct PSI elements for that language"
  - "Annotate openToken.textRange (firstChild = <%def/<%block keyword) for unclosed tags, not tag.textRange which spans potentially hundreds of lines"
  - "VALID_DIRECTIVES set contains 7 names: def, block, inherit, include, namespace, page, doc"
  - "MakoTemplateTextContent nodes carry unrecognized directive-like text because unrecognized <%name sequences fall through to TEMPLATE_TEXT in lexer"

patterns-established:
  - "lang.annotation subpackage follows same naming convention as lang.completion, lang.injection, lang.folding, lang.structure"
  - "Annotator guards with element.containingFile.language.id check before processing"

requirements-completed: [COMP-03]

# Metrics
duration: 1min
completed: 2026-02-21
---

# Phase 8 Plan 01: MakoAnnotator Summary

**MakoAnnotator providing real-time red squiggles for unclosed <%def>/<%block> tags (END_TAG child absence) and unrecognized Mako directives (<%bogus>) via IntelliJ Annotator API**

## Performance

- **Duration:** 1 min
- **Started:** 2026-02-21T12:58:12Z
- **Completed:** 2026-02-21T12:59:30Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Created `MakoAnnotator.kt` in new `lang.annotation` package implementing `Annotator` interface
- Unclosed tag detection: checks `tag.node.findChildByType(MakoTokenTypes.END_TAG) != null`; flags the opening token range only (not the full multi-line body)
- Invalid directive detection: regex `<%([a-zA-Z]+)` scans `MakoTemplateTextContent` nodes; unknown names flagged with `HighlightSeverity.ERROR`
- Registered annotator in `plugin.xml` with `language="Mako Template"` extension point
- All 64 existing tests remain green; `./gradlew check` passes without compilation errors

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement MakoAnnotator and register in plugin.xml** - `a6d7920` (feat)

**Plan metadata:** (see final commit below)

## Files Created/Modified
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/annotation/MakoAnnotator.kt` - Annotator class with unclosed tag check and invalid directive check
- `src/main/resources/META-INF/plugin.xml` - Added `<annotator language="Mako Template" implementationClass="...MakoAnnotator"/>` entry

## Decisions Made
- Used `language="Mako Template"` (not `"any"`) for the annotator registration — annotators receive correct PSI elements when scoped to the target language, unlike completion contributors which need `"any"` to handle TEMPLATE_TEXT positions
- Annotate `openToken.textRange` (the `firstChild`) for unclosed tags to avoid red underlining multi-line content bodies
- `MakoTemplateTextContent` is the correct PSI type for invalid directive detection because the lexer emits `TEMPLATE_TEXT` for any `<%name` sequence that doesn't match a valid grammar rule

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- MakoAnnotator is complete and registered; COMP-03 satisfied
- Phase 8 Plan 02 (annotator tests) can proceed immediately — MakoAnnotator is stable and testable
- No blockers

---
*Phase: 08-error-annotations-and-release-readiness*
*Completed: 2026-02-21*

## Self-Check: PASSED

- MakoAnnotator.kt: FOUND at src/main/kotlin/.../lang/annotation/MakoAnnotator.kt
- 08-01-SUMMARY.md: FOUND at .planning/phases/08-error-annotations-and-release-readiness/08-01-SUMMARY.md
- Task commit a6d7920: FOUND in git log
- class MakoAnnotator : Annotator — confirmed in source
- annotator language="Mako Template" — confirmed in plugin.xml

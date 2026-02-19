---
phase: 02-lexer
plan: 03
subsystem: parser
tags: [intellij-platform, kotlin, psi, parserdefinition, astnode]

# Dependency graph
requires:
  - phase: 02-lexer-02
    provides: "MakoParserDefinition with UnsupportedOperationException stubs for createParser/createElement"
provides:
  - "Functional no-op PsiParser in createParser() that wraps all tokens in a single root marker"
  - "ASTWrapperPsiElement fallback in createElement() for any AST node type"
  - "MakoParserDefinition fully satisfies IntelliJ Platform file-open pipeline without exceptions"
affects: [03-parser]

# Tech tracking
tech-stack:
  added: [com.intellij.extapi.psi.ASTWrapperPsiElement]
  patterns:
    - "No-op PsiParser pattern: wrap all tokens in single root marker using builder.mark() / advanceLexer loop / marker.done(root)"
    - "ASTWrapperPsiElement as generic PSI fallback before typed PSI node classes are generated in Phase 3"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt

key-decisions:
  - "No-op parser uses single root marker to wrap all tokens — sufficient for Phase 2 file-open pipeline; Phase 3 will replace with GrammarKit-generated parser"
  - "ASTWrapperPsiElement used as createElement() fallback — generic PSI wrapper until Phase 3 generates typed PSI node factory"

patterns-established:
  - "Gap closure pattern: stubs that throw in one phase are replaced with minimal working implementations before advancing, preventing platform exceptions"

requirements-completed: [PARS-01, PARS-02, PARS-03]

# Metrics
duration: 1min
completed: 2026-02-19
---

# Phase 2 Plan 03: Gap Closure — MakoParserDefinition No-Op Parser Summary

**No-op PsiParser with ASTWrapperPsiElement fallback replaces UnsupportedOperationException stubs so .mako files open in the IDE without exceptions**

## Performance

- **Duration:** 1 min
- **Started:** 2026-02-19T19:57:43Z
- **Completed:** 2026-02-19T19:58:58Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Replaced `createParser()` stub with a working no-op PsiParser that advances through all tokens and wraps them in a single root marker
- Replaced `createElement()` stub with `ASTWrapperPsiElement(node)` generic PSI fallback
- Build passes with zero compilation errors and all 26 tasks succeed
- Opening a .mako file in the IDE will no longer produce UnsupportedOperationException in the event log

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace UnsupportedOperationException stubs with minimal working implementations** - `7f8c58e` (feat)

**Plan metadata:** (docs commit — see final commit)

## Files Created/Modified
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt` - Added ASTWrapperPsiElement import, replaced createParser() stub with no-op PsiParser, replaced createElement() stub with ASTWrapperPsiElement(node)

## Decisions Made
- No-op parser wraps all tokens in a single root marker — the simplest implementation that satisfies the platform's file-open pipeline without building a real PSI tree. Phase 3 (GrammarKit) will replace this.
- ASTWrapperPsiElement used as generic createElement() fallback — allows platform to navigate PSI nodes without needing typed classes. Phase 3 will replace with a factory dispatching to generated typed PSI node classes.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 2 is now fully complete: lexer tokenizes correctly, ParserDefinition satisfies platform file-open pipeline without exceptions
- Phase 3 (Parser) can proceed: replace the no-op PsiParser with a GrammarKit-generated parser and replace ASTWrapperPsiElement with a typed PSI factory
- Next plan: .planning/phases/03-parser/03-01-PLAN.md

---
*Phase: 02-lexer*
*Completed: 2026-02-19*

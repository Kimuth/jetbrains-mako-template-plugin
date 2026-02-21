---
phase: 06-python-language-injection
plan: 02
subsystem: injection
tags: [injection, MultiHostInjector, Python, kotlin, plugin-xml]

# Dependency graph
requires:
  - phase: 06-01
    provides: PsiLanguageInjectionHost mixin classes on MakoExpression, MakoCodeBlock, MakoModuleBlock
provides:
  - MakoPythonInjector — MultiHostInjector wiring Python into all three Mako PSI host node types
  - plugin.xml multiHostInjector registration
affects: [07-completion]

# Tech tracking
tech-stack:
  added: [MultiHostInjector, MultiHostRegistrar, Language.findLanguageByID]
  patterns:
    - MultiHostInjector.elementsToInjectIn() lists PSI interface classes (not Impl classes)
    - getLanguagesToInject() uses guarded when-branches with end > start safety check
    - TextRange offsets relative to host node text — platform handles document coordinate mapping
    - Language.findLanguageByID("Python") called per-invocation (no caching)

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/injection/MakoPythonInjector.kt
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "elementsToInjectIn() must list the PSI interface classes (MakoExpression::class.java), NOT the generated *Impl classes — the platform dispatches on interface type"
  - "end > start guard prevents injection into empty or malformed nodes (e.g. ${} with no content, truncated <% with no closing %>)"
  - "Language.findLanguageByID('Python') called per-invocation — no caching; cheap lookup; avoids initialization-order issues"
  - "Filter expressions (${x | h}) inject full content between ${ and } including filter part; Python parser sees x | h as valid bitwise-OR — filter refinement deferred to Phase 7"

requirements-completed: []

# Metrics
duration: ~3 min
completed: 2026-02-21
---

# Phase 6 Plan 02: Python Language Injector Summary

**MultiHostInjector wiring Python syntax highlighting into ${...}, <% %>, and <%! %> regions via TextRange-based injection into the three PsiLanguageInjectionHost PSI nodes established in Plan 01**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-02-21T09:37:09Z
- **Completed:** 2026-02-21
- **Tasks:** 2 (both auto)
- **Files modified:** 2

## Accomplishments

- Created `lang/injection/` package (new directory) under the Kotlin sources
- Implemented `MakoPythonInjector` class with `elementsToInjectIn()` returning all three PSI interface types and `getLanguagesToInject()` with per-type guarded when-branches covering `${...}`, `<% %>`, and `<%! %>` syntax
- Registered `multiHostInjector` extension point in plugin.xml with Phase 6 comment block
- `./gradlew check` passes with 57 tests, zero compilation errors

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MakoPythonInjector** - `97d3dc2` (feat)
2. **Task 2: Register in plugin.xml and verify build** - `3c0ba22` (feat)

**Plan metadata:** _(this summary commit)_ (docs: complete plan)

## Files Created/Modified

- `src/main/kotlin/.../lang/injection/MakoPythonInjector.kt` - MultiHostInjector for Python injection into expression, code block, and module block PSI nodes
- `src/main/resources/META-INF/plugin.xml` - Added `multiHostInjector` extension point registration for MakoPythonInjector

## Decisions Made

- `elementsToInjectIn()` uses PSI interface classes (`MakoExpression::class.java`), not the generated `*Impl` classes — the platform's injection dispatch mechanism works on interface types.
- The `end > start` guard in each when-branch prevents calling `addPlace()` on empty or malformed host nodes (e.g., `${}` with no content between delimiters, or a truncated tag).
- `Language.findLanguageByID("Python")` is called per-invocation (not cached as a field) to avoid initialization-order issues with language registry population.
- Filter expression content (e.g., `${x | h}`) is injected as-is; Python sees `x | h` as valid bitwise-OR syntax. Proper filter handling is deferred to Phase 7.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The `LiteralTextEscaper.createSimple()` contingency from Plan 01 was not needed here (it was already resolved in Plan 01). The `./gradlew check` contingency (replacing simple escaper with anonymous class) was not triggered — all 57 tests passed immediately.

## User Setup Required

None - no external service configuration required. Functional verification of Python syntax highlighting in the IDE is done via `./gradlew runIde` (out of scope for this automated plan execution).

## Next Phase Readiness

- Phase 6 is complete — both plans executed
- Python injection is wired end-to-end: host nodes (Plan 01) + injector registration (Plan 02)
- Phase 7 (completion) can proceed: injection host contract enables completion contributor to participate in injected Python fragments

## Self-Check: PASSED

- FOUND: `src/main/kotlin/.../lang/injection/MakoPythonInjector.kt`
- FOUND: commit `97d3dc2` (Task 1)
- FOUND: commit `3c0ba22` (Task 2)
- FOUND: `multiHostInjector` entry in plugin.xml
- BUILD: `./gradlew check` PASSED, 57 tests green

---
*Phase: 06-python-language-injection*
*Completed: 2026-02-21*

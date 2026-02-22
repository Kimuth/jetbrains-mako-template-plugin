---
phase: 21-css-js-sub-language-injection
plan: "01"
subsystem: injection
tags: [kotlin, intellij-platform, multi-host-injector, css-injection, language-injection, html-psi]

# Dependency graph
requires:
  - phase: 18-fileviewprovider-scaffolding
    provides: MakoFileViewProvider creates dual HTML/Mako PSI tree — the substrate MakoCssInjector targets
  - phase: 20-html-feature-verification
    provides: Gap analysis confirming CSS injection requires explicit MultiHostInjector (no built-in for PyCharm Community)

provides:
  - MakoCssInjector: MultiHostInjector that injects CSS into XmlText nodes inside <style> elements in HTML PSI tree
  - plugin.xml multiHostInjector registration for MakoCssInjector
  - MakoCssInjectorTest: 3 structural tests covering host type, null guard, and HTML file guard

affects:
  - Human IDE verification: CSS completions in <style> blocks require IDE with CSS plugin installed
  - Phase 22 (if any): JS injection verification via HtmlScriptLanguageInjector

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "MultiHostInjector on XmlText for sub-language injection into HTML PSI tree elements"
    - "Language.findLanguageByID null guard for optional plugin dependencies (no CssLanguage.INSTANCE)"
    - "localName.lowercase() string comparison instead of HtmlUtil.isStyleTag (safe fallback for unconfirmed method)"

key-files:
  created:
    - src/main/kotlin/com/schtilig/mako/lang/injection/MakoCssInjector.kt
    - src/test/kotlin/com/schtilig/mako/lang/MakoCssInjectorTest.kt
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "Use Language.findLanguageByID('CSS') null guard — not CssLanguage.INSTANCE — to avoid ClassNotFoundException when CSS plugin absent"
  - "Use parentTag.localName.lowercase() != 'style' string comparison instead of HtmlUtil.isStyleTag — isStyleTag not confirmed via javap (safe fallback)"
  - "No MakoJsInjector written — platform HtmlScriptLanguageInjector already handles <script> XmlText in our HTML PSI tree (HINJ-06)"

patterns-established:
  - "Pattern: MultiHostInjector targeting XmlText for sub-language injection — analogous to MakoPythonInjector but targets HTML PSI tree XmlText nodes"
  - "Pattern: Mock MultiHostRegistrar in tests with throw-on-call startInjecting to assert null guard fires"

requirements-completed: [HINJ-05, HINJ-06]

# Metrics
duration: 2min
completed: 2026-02-22
---

# Phase 21 Plan 01: CSS Sub-Language Injection Summary

**MakoCssInjector (MultiHostInjector) injects CSS into `<style>` XmlText nodes in Mako's HTML PSI tree via Language.findLanguageByID null guard; platform HtmlScriptLanguageInjector handles `<script>` automatically; 98 tests pass**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-22T22:42:48Z
- **Completed:** 2026-02-22T22:45:32Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Implemented `MakoCssInjector : MultiHostInjector` targeting `XmlText` nodes in `<style>` elements of the HTML PSI tree created by `MakoFileViewProvider` (Phase 18)
- Registered `MakoCssInjector` as `<multiHostInjector>` in `plugin.xml` — no optional dependency declaration needed since CSS class is not referenced at compile time
- Added 3 structural tests in `MakoCssInjectorTest` covering injection host type declaration, CSS null guard behavior, and HTML file restriction guard
- Full test suite expanded from 95 to 98 tests, all passing; `./gradlew check --rerun` BUILD SUCCESSFUL
- Confirmed no `MakoJsInjector` written — `HtmlScriptLanguageInjector` (registered in `PyCharmCorePlugin.xml`) handles `<script>` automatically once JavaScript plugin is installed (HINJ-06)

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement MakoCssInjector and register in plugin.xml** - `60efb69` (feat)
2. **Task 2: Add MakoCssInjectorTest and run full test suite** - `c9f856d` (feat)

**Plan metadata:** (docs commit below)

## Files Created/Modified

- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoCssInjector.kt` - CSS MultiHostInjector targeting XmlText inside `<style>` elements in HTML PSI tree; null guard for CSS plugin absence; HTML file guard; localName string comparison for style tag
- `src/main/resources/META-INF/plugin.xml` - Added `<multiHostInjector implementation="com.schtilig.mako.lang.injection.MakoCssInjector"/>` with Phase 21 comments explaining HINJ-05/HINJ-06 approach
- `src/test/kotlin/com/schtilig/mako/lang/MakoCssInjectorTest.kt` - 3 structural tests: elementsToInjectIn check, null guard verification, HTML-only restriction check

## Decisions Made

- **`Language.findLanguageByID("CSS")` over `CssLanguage.INSTANCE`:** Direct class reference causes `ClassNotFoundException` at IDE startup when CSS plugin not installed; ID-based lookup returns null safely and is the correct pattern for optional language dependencies (same as `MakoPythonInjector` uses for Python)
- **`localName.lowercase() != "style"` over `HtmlUtil.isStyleTag()`:** Research confirmed `HtmlUtil.isScriptTag(XmlTag)` via javap but `isStyleTag` was not explicitly confirmed in the truncated output; string comparison is the safe fallback that avoids potential `NoSuchMethodError`
- **No `MakoJsInjector`:** `HtmlScriptLanguageInjector` (already registered platform-wide in `PyCharmCorePlugin.xml`) fires on `XmlText` nodes in HTML-containing files via `HtmlUtil.isHtmlTagContainingFile()` check — our `MakoFileViewProvider` HTML PSI tree qualifies; writing a second injector would cause double-injection and duplicate completions

## Deviations from Plan

None — plan executed exactly as written. The `localName.lowercase()` string comparison vs `HtmlUtil.isStyleTag` was explicitly specified by the plan as the required approach per research open questions.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

CSS completions in `<style>` blocks will be active when the CSS plugin is installed (available in PyCharm Professional, IntelliJ IDEA Ultimate, or from JetBrains Marketplace for PyCharm Community). JavaScript completions in `<script>` blocks are handled by the platform's `HtmlScriptLanguageInjector` and activate when the JavaScript plugin is present.

## Self-Check

Created files verified:

- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoCssInjector.kt` — exists (created in Task 1)
- `src/test/kotlin/com/schtilig/mako/lang/MakoCssInjectorTest.kt` — exists (created in Task 2)

Commits verified:
- `60efb69` — feat(21-01): implement MakoCssInjector and register in plugin.xml
- `c9f856d` — feat(21-01): add MakoCssInjectorTest with 3 structural tests

Test results: 98 total tests, 0 failures, BUILD SUCCESSFUL.

## Self-Check: PASSED

## Next Phase Readiness

- HINJ-05 closed: `MakoCssInjector` is wired and will activate CSS completions in `<style>` elements when CSS plugin is present
- HINJ-06 closed: No additional code needed; `HtmlScriptLanguageInjector` handles `<script>` elements automatically
- Human IDE verification (Phase 22 or separate plan) recommended to confirm CSS completions in `<style>` blocks with CSS plugin installed
- No blockers for next phase

---
*Phase: 21-css-js-sub-language-injection*
*Completed: 2026-02-22*

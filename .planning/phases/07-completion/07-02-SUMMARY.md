---
phase: 07-completion
plan: 02
subsystem: testing
tags: [completion-tests, basePlatformTestCase, intellij-platform, kotlin, junit4]

# Dependency graph
requires:
  - phase: 07-01
    provides: MakoCompletionContributor with TagNameCompletionProvider (COMP-01) and TagAttrCompletionProvider (COMP-02)
provides:
  - MakoCompletionTest with 7 passing tests covering COMP-01 (tag-name), COMP-02 (attributes), and anti-regression (no HTML false positives)
  - Executable proof that both completion requirements are met end-to-end
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - addFileToProject + configureFromExistingVirtualFile pattern for BasePlatformTestCase completion tests in plugin projects
    - setUp() FileTypeManager lookup + PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() to force eager file type registration
    - platformBundledPlugins = com.intellij.modules.json required to load PythonCore (and hence our plugin) in tests

key-files:
  created:
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoCompletionTest.kt
  modified:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/completion/MakoCompletionContributor.kt
    - src/main/resources/META-INF/plugin.xml
    - gradle.properties

key-decisions:
  - "addFileToProject + configureFromExistingVirtualFile used instead of configureByText(FileType) — the latter creates an in-memory file before MakoFileType is registered in the test JVM, resulting in PLAIN_TEXT; addFileToProject creates a physical temp file whose extension is recognized after file type registration completes"
  - "completion.contributor language='any' required — TEMPLATE_TEXT tokens fall in the template-data-language layer; language='Mako Template' filter prevented contributor from firing for those positions"
  - "attrsForTag() uses Kotlin is (instanceof) checks — original Class equality via element.javaClass vs MakoDefTag::class.java failed because impl classes (MakoDefTagImpl) differ from interface classes (MakoDefTag)"
  - "raw text inspection enhanced to search backward for '<%%' + optional partial letters — original offset-2 check only handled '<%<caret>' but not '<%d<caret>' partial typing"
  - "com.intellij.modules.json added to platformBundledPlugins — PythonCore depends on intellij.json.backend module (from JSON plugin); without it PythonCore fails to load, preventing our plugin (which depends on PythonCore) from loading in tests"

patterns-established:
  - "Completion test pattern for IntelliJ plugin projects with TemplateLanguage: use addFileToProject('test.mako', content) + configureFromExistingVirtualFile; never configureByText(FileType) when file type registration may be deferred"
  - "Test setUp pattern: FileTypeManager.getInstance().getFileTypeByExtension('mako') + PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() to warm up extension registry"

requirements-completed: [COMP-01, COMP-02]

# Metrics
duration: 27min
completed: 2026-02-21
---

# Phase 7 Plan 02: MakoCompletionTest Summary

**7-test BasePlatformTestCase suite verifying COMP-01 tag-name popup and COMP-02 per-tag attribute completion with three auto-fixed contributor bugs discovered during test execution**

## Performance

- **Duration:** 27 min
- **Started:** 2026-02-21T10:29:43Z
- **Completed:** 2026-02-21T10:57:40Z
- **Tasks:** 1 (TDD — write tests, fix contributor bugs to achieve green)
- **Files modified:** 4

## Accomplishments

- `MakoCompletionTest.kt` with 7 tests: `testTagNameCompletionAfterLt`, `testTagNameCompletionPartiallyTyped`, `testDefAttrCompletion`, `testInheritAttrCompletion`, `testBlockAttrCompletion`, `testNoCompletionInPlainHtml`, `testAttrInsertHandlerProducesQuotedValue`
- Discovered and fixed 3 bugs in `MakoCompletionContributor.kt` that prevented completion from working in practice:
  - Contributor not firing for TEMPLATE_TEXT positions (language filter too strict)
  - TAG_ATTRIBUTES map key mismatch (interface vs Impl class identity)
  - Partial tag-name typing not handled (raw text inspection only checked 2 chars back)
- Identified and fixed root cause of plugin not loading in test environment (missing `com.intellij.modules.json` bundled plugin causing PythonCore to fail)
- All 64 tests green (57 pre-existing + 7 new)

## Task Commits

TDD execution committed as single task (write tests + fix bugs to achieve green):

1. **Task 1: MakoCompletionTest + contributor fixes** - `5ea9406` (feat)

## Files Created/Modified

- `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoCompletionTest.kt` — 7-method BasePlatformTestCase test class; uses addFileToProject + configureFromExistingVirtualFile pattern; setUp() forces eager file type registration
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/completion/MakoCompletionContributor.kt` — Fixed 3 bugs: language="any" for contributor registration, attrsForTag() with `is` instanceof checks, backward-scanning raw text inspection for partial tag names
- `src/main/resources/META-INF/plugin.xml` — completion.contributor changed from language="Mako Template" to language="any"
- `gradle.properties` — added com.intellij.modules.json to platformBundledPlugins

## Decisions Made

- `addFileToProject + configureFromExistingVirtualFile` used instead of `configureByText(FileType, text)` because the file type is registered lazily in the test JVM. `configureByText` creates an in-memory file BEFORE MakoFileType is registered, so the file gets `PLAIN_TEXT` language. `addFileToProject` creates a physical temp file; by the time `configureFromExistingVirtualFile` opens it, the type is registered.
- `language="any"` required for completion.contributor because Mako's `TEMPLATE_TEXT` tokens (where the caret sits during `<%<caret>`) fall in the TemplateLanguage's template-data-language layer. The platform's `language="Mako Template"` filter skips the contributor for those positions.
- `attrsForTag()` function with Kotlin `is` checks replaces the original `TAG_ATTRIBUTES[element.javaClass]` map lookup. In Kotlin/JVM, `MakoDefTag::class.java` is the interface Class, while `element.javaClass` for a `MakoDefTagImpl` instance is the concrete Impl Class — map lookup with interface key and Impl key never matches.
- `com.intellij.modules.json` added to `platformBundledPlugins`: the PythonCore plugin has a module dependency on `intellij.json.backend` (provided by the JSON plugin). In the `TestFrameworkType.Platform` test setup, PythonCore fails to load if `intellij.json.backend` is unavailable, causing our plugin (which `<depends>` on PythonCore) to also fail to load and not register any extensions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] completion.contributor registration language changed from "Mako Template" to "any"**
- **Found during:** Task 1 (writing and running completion tests)
- **Issue:** Contributor was not being invoked for caret positions on TEMPLATE_TEXT tokens because those elements have TemplateDataLanguage, not MakoLanguage. The `language="Mako Template"` filter on the extension point prevented the contributor from firing.
- **Fix:** Changed `plugin.xml` registration to `language="any"`. Internal language guard (`file.language.id != "Mako Template"`) prevents Mako completions from appearing in non-Mako files.
- **Files modified:** src/main/resources/META-INF/plugin.xml, src/main/kotlin/.../completion/MakoCompletionContributor.kt (comment update)
- **Verification:** testTagNameCompletionAfterLt, testTagNameCompletionPartiallyTyped pass
- **Committed in:** 5ea9406 (task commit)

**2. [Rule 1 - Bug] TAG_ATTRIBUTES map key mismatch fixed via attrsForTag() with instanceof checks**
- **Found during:** Task 1 (attribute completion tests returning empty items)
- **Issue:** `TAG_ATTRIBUTES: Map<Class<out PsiElement>, List<String>>` was keyed by PSI interface types (`MakoDefTag::class.java`) but looked up by concrete Impl types (`element.javaClass` = `MakoDefTagImpl.class`). In JVM, interface class ≠ implementation class, so map lookup always returned null.
- **Fix:** Replaced map-based lookup with `attrsForTag(element: PsiElement): List<String>?` function using Kotlin `is` (JVM instanceof) checks. `element is MakoDefTag` correctly matches `MakoDefTagImpl` instances.
- **Files modified:** src/main/kotlin/.../completion/MakoCompletionContributor.kt
- **Verification:** testDefAttrCompletion, testInheritAttrCompletion, testBlockAttrCompletion, testAttrInsertHandlerProducesQuotedValue pass
- **Committed in:** 5ea9406 (task commit)

**3. [Rule 1 - Bug] Raw text inspection enhanced for partial tag name typing**
- **Found during:** Task 1 (testTagNameCompletionPartiallyTyped failing)
- **Issue:** Original check `file.text.substring(offset - 2, offset) != "<%"` failed for `"<%d<caret>"` (offset=3): `file.text.substring(1, 3) = "%d"` which is not `"<%"`. Only `"<%<caret>"` (offset=2) worked.
- **Fix:** Changed to backward scan: `textBefore.lastIndexOf("<%")`, extract partial text after `<%`, accept if partial is empty or all-letters (no whitespace = not a code block). The insert handler already used `ctx.startOffset - 2` to find the `<%` position, so this fixes the detection symmetrically.
- **Files modified:** src/main/kotlin/.../completion/MakoCompletionContributor.kt
- **Verification:** testTagNameCompletionPartiallyTyped passes
- **Committed in:** 5ea9406 (task commit)

**4. [Rule 3 - Blocking] Added com.intellij.modules.json to platformBundledPlugins**
- **Found during:** Task 1 (all tests returning empty completion items; IDE log showing plugin failed to load)
- **Issue:** PythonCore (PythonCore) failed to load in test environment because its module dependency `intellij.json.backend` (from the JSON bundled plugin) was not available. Our plugin declares `<depends>com.intellij.modules.python</depends>` which resolves to PythonCore. Since PythonCore failed, our plugin was also disabled, preventing MakoFileType and MakoCompletionContributor from being registered.
- **Fix:** Added `com.intellij.modules.json` to `platformBundledPlugins` in `gradle.properties`. This makes the JSON plugin available in the test sandbox, providing `intellij.json.backend`.
- **Files modified:** gradle.properties
- **Verification:** Plugin loads successfully in tests; IDE log no longer shows "Problems found loading plugins"
- **Committed in:** 5ea9406 (task commit)

---

**Total deviations:** 4 auto-fixed (3 Rule 1 bugs, 1 Rule 3 blocking issue)
**Impact on plan:** All fixes were necessary for the completion feature to work correctly. No scope creep. The tests accurately describe the intended behavior; the implementation had bugs that prevented it from working in practice.

## Issues Encountered

- Test sandbox file type registration was deferred until after `configureByText` was called, making the first test attempt create a PlainText file. Resolved by switching to `addFileToProject + configureFromExistingVirtualFile` which avoids the timing issue.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 7 is complete: both COMP-01 (tag-name completion) and COMP-02 (attribute completion) are implemented and verified by tests
- 64 tests green total
- Ready for Phase 8 (Marketplace Branding / ROADMAP.md final phase)

---
*Phase: 07-completion*
*Completed: 2026-02-21*

## Self-Check: PASSED

- FOUND: `.planning/phases/07-completion/07-02-SUMMARY.md`
- FOUND: commit `5ea9406` (feat(07-02): MakoCompletionTest + contributor fixes)

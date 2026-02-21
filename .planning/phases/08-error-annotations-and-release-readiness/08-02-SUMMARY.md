---
phase: 08-error-annotations-and-release-readiness
plan: 02
subsystem: testing
tags: [annotator, intellij, psi, highlighting, test, basePlatformTestCase]

# Dependency graph
requires:
  - phase: 08-error-annotations-and-release-readiness
    provides: MakoAnnotator.kt with unclosed-tag and invalid-directive detection
  - phase: 07-completion
    provides: MakoCompletionTest addFileToProject+doHighlighting test patterns

provides:
  - MakoAnnotatorTest.kt — 4-test BasePlatformTestCase suite verifying COMP-03
  - annotator testData fixtures in src/test/testData/annotator/
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "doHighlighting() + filtered message assertion: preferred over checkHighlighting() when parser also generates ERROR-severity highlights (e.g., unclosed tags with missing END_TAG)"
    - "checkHighlighting() for negative annotator tests: verifies no annotations on well-formed input with no fixture error markers"
    - "addFileToProject pattern for annotator tests: same file-type detection workaround as completion tests"

key-files:
  created:
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoAnnotatorTest.kt
    - src/test/testData/annotator/UnclosedDefTag.mako
    - src/test/testData/annotator/UnclosedBlockTag.mako
    - src/test/testData/annotator/WellFormedDefTag.mako
    - src/test/testData/annotator/InvalidDirective.mako
  modified:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/annotation/MakoAnnotator.kt

key-decisions:
  - "doHighlighting() over checkHighlighting() for error tests: unclosed tags produce both annotator annotations and PSI parse-error annotations; checkHighlighting() sees both as 'extra' unless all error markers are in the fixture; doHighlighting() + targeted filter is cleaner"
  - "Invalid directive detection was broken: regex searched for '<%name' in a single TEMPLATE_TEXT token but the Mako lexer emits '<', '%', and 'name...' as three separate tokens; fixed via sibling-traversal on the '<' node"
  - "Fixture descr attribute uses literal <> not &lt;&gt;: the IntelliJ test framework does NOT unescape XML entities in descr attribute values when comparing against annotation messages"

patterns-established:
  - "Annotator test split: doHighlighting()+filter for positive error tests; checkHighlighting()+empty fixture for negative (no-error) tests"
  - "Sibling-traversal pattern for multi-token detection in annotators: when lexer emits adjacent single-char tokens for a multi-char pattern, traverse nextSibling chain in the annotator"

requirements-completed: [COMP-03]

# Metrics
duration: 10min
completed: 2026-02-21
---

# Phase 8 Plan 02: MakoAnnotatorTest Summary

**BasePlatformTestCase suite with 4 green annotator tests (doHighlighting+filter pattern) plus Rule 1 fix to MakoAnnotator.checkForInvalidDirective for multi-token <%bogus detection**

## Performance

- **Duration:** 10 min
- **Started:** 2026-02-21T13:11:22Z
- **Completed:** 2026-02-21T13:21:22Z
- **Tasks:** 1
- **Files modified:** 6

## Accomplishments
- Created `MakoAnnotatorTest.kt` with all 4 required tests: testUnclosedDefTagShowsError, testUnclosedBlockTagShowsError, testWellFormedDefTagNoError, testInvalidDirectiveShowsError
- All 4 new tests pass; total test suite is 68 tests, all green
- Fixed annotator bug: `checkForInvalidDirective` was using a regex expecting `<%name` in a single token, but the lexer emits `<`, `%`, and `name...` as three separate TEMPLATE_TEXT tokens — fixed via sibling traversal
- Established the `doHighlighting()` + targeted-message-filter pattern for annotator tests where the parser also generates error-severity highlights alongside annotator annotations
- Created 4 test fixture files in `src/test/testData/annotator/`

## Task Commits

Each task was committed atomically:

1. **Task 1: MakoAnnotatorTest + annotator bug fix** - `b81de46` (test)

**Plan metadata:** (see final commit below)

## Files Created/Modified
- `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoAnnotatorTest.kt` - 4-method annotator test suite using BasePlatformTestCase
- `src/test/testData/annotator/UnclosedDefTag.mako` - Fixture: unclosed def tag (no closing tag)
- `src/test/testData/annotator/UnclosedBlockTag.mako` - Fixture: unclosed block tag (no closing tag)
- `src/test/testData/annotator/WellFormedDefTag.mako` - Fixture: well-formed def+close tag (no errors expected)
- `src/test/testData/annotator/InvalidDirective.mako` - Fixture: unrecognized directive name
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/annotation/MakoAnnotator.kt` - Fixed checkForInvalidDirective via sibling traversal

## Decisions Made
- Used `doHighlighting()` + message filter instead of `checkHighlighting()` for the error tests: `checkHighlighting` requires ALL error-severity highlights to be accounted for in fixture markers; for unclosed tags, both the annotator and the PSI parser generate ERROR highlights, making fixture calibration complex. `doHighlighting()` lets the test target just the annotator's message.
- Used `checkHighlighting(true, false, false)` for testWellFormedDefTagNoError: this test verifies absence of annotations, has no parse errors (file is well-formed), and the fixture file approach works cleanly.
- `addFileToProject` + `configureFromExistingVirtualFile` for the doHighlighting tests: matches the proven MakoCompletionTest pattern for reliable file type detection.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed MakoAnnotator.checkForInvalidDirective multi-token detection**
- **Found during:** Task 1 (writing and running MakoAnnotatorTest)
- **Issue:** The original annotator searched for `<%([a-zA-Z]+)` regex within a single `MakoTemplateTextContent` node's text. The Mako lexer (YYINITIAL state) emits `<`, `%`, and `bogus attr="x">` as three separate TEMPLATE_TEXT tokens via the `[$<%#]` single-char rule. So no single node ever contains `<%bogus`, and the regex never matched.
- **Fix:** Changed `checkForInvalidDirective` to detect when the element text is `<`, then traverse `nextSibling` for a `%` node and another sibling with a leading `[a-zA-Z]+` name. Annotates the `<` node's textRange.
- **Files modified:** `src/main/kotlin/.../lang/annotation/MakoAnnotator.kt`
- **Verification:** testInvalidDirectiveShowsError passes; `./gradlew check` exits 0
- **Committed in:** b81de46 (task commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Bug)
**Impact on plan:** Necessary correctness fix — the annotator's invalid-directive detection was non-functional as written; fix was required to satisfy COMP-03 for the <%bogus case.

## Issues Encountered
- IntelliJ test framework does NOT unescape XML entities in `<error descr="...">` attributes: `&lt;%def&gt;` in a fixture file is compared literally (as `&lt;%def&gt;`) against the annotation message (`<%def>`), causing a mismatch. Resolved by switching to `doHighlighting()` approach for error tests.
- Parser-generated error highlights (missing END_TAG) appear alongside annotator errors in `checkHighlighting`, requiring fixture markers for both. Resolved by using `doHighlighting()` + targeted filter.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All annotator tests pass; COMP-03 fully satisfied
- Phase 8 Plan 03 (README/CHANGELOG/pluginIcon/verifyPlugin) has already been completed
- No blockers

---
*Phase: 08-error-annotations-and-release-readiness*
*Completed: 2026-02-21*

## Self-Check: PASSED

- MakoAnnotatorTest.kt: FOUND at src/test/kotlin/.../lang/MakoAnnotatorTest.kt
- UnclosedDefTag.mako: FOUND at src/test/testData/annotator/UnclosedDefTag.mako
- UnclosedBlockTag.mako: FOUND at src/test/testData/annotator/UnclosedBlockTag.mako
- WellFormedDefTag.mako: FOUND at src/test/testData/annotator/WellFormedDefTag.mako
- InvalidDirective.mako: FOUND at src/test/testData/annotator/InvalidDirective.mako
- 08-02-SUMMARY.md: FOUND at .planning/phases/08-error-annotations-and-release-readiness/08-02-SUMMARY.md
- Task commit b81de46: FOUND in git log
- class MakoAnnotatorTest: 4 tests, all passing — confirmed by gradlew test output (4/4 green)
- Total test suite: 68 tests, 0 failures — confirmed by gradlew check

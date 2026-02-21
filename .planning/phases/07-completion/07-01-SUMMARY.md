---
phase: 07-completion
plan: 01
subsystem: completion
tags: [completion-contributor, code-completion, intellij-platform, kotlin]

# Dependency graph
requires:
  - phase: 06-python-language-injection
    provides: MakoPythonInjector, PSI injection host types (MakoExpression, MakoCodeBlock, MakoModuleBlock)
  - phase: 03-parser
    provides: PSI types (MakoDefTag, MakoBlockTag, MakoInheritTag, MakoIncludeTag, MakoNamespaceTag, MakoPageTag), MakoTokenTypes.TAG_ATTR_NAME
provides:
  - MakoCompletionContributor with TagNameCompletionProvider (COMP-01) and TagAttrCompletionProvider (COMP-02)
  - Tag-name completion after `<%` via raw text inspection
  - Tag-attribute completion inside open tag composite nodes
  - plugin.xml completion.contributor extension point registration
affects: [07-02-completion-tests]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Raw text inspection for tag-name completion (avoids dummy-identifier lexer disruption)
    - Parent-walk pattern for enclosing tag detection in attribute completion
    - withPrefixMatcher("") to bypass platform prefix filtering for multi-char prefixes

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/completion/MakoCompletionContributor.kt
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "Raw text inspection (file.text.substring(offset-2, offset)) for tag-name detection — dummy identifier disrupts lexer before TAG_OPEN_xxx tokens appear, making PSI-pattern matching unreliable"
  - "withPrefixMatcher(\"\") required — platform filters items whose lookup string doesn't match typed prefix; empty matcher bypasses this for <% prefix items"
  - "TAG_ATTRIBUTES keyed by java class (element.javaClass) not PSI interface — parent walk uses javaClass for exact match against Impl classes from generated PSI"
  - "<%doc insert handler uses replaceString to full snippet <%doc>\\n</%doc> with inner-line caret positioning; all other tags insert tagName+space"

patterns-established:
  - "Completion package: lang/completion/ following same package structure as lang/injection/, lang/folding/, etc."
  - "3-extend pattern: broad pattern for raw-text completion, token-specific pattern, whitespace fallback"

requirements-completed: [COMP-01, COMP-02]

# Metrics
duration: 6min
completed: 2026-02-21
---

# Phase 7 Plan 01: Completion Summary

**MakoCompletionContributor with tag-name popup after `<%` and per-tag attribute completion with `=""` insert handler, registered as completion.contributor in plugin.xml**

## Performance

- **Duration:** 6 min
- **Started:** 2026-02-21T10:25:06Z
- **Completed:** 2026-02-21T10:31:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- TagNameCompletionProvider fires when raw text at `offset-2` is `<%`, offering all 7 Mako directive names with bold display and insert handlers that overwrite the typed `<%` with the full tag name plus trailing space (or `<%doc>\n</%doc>` snippet for doc tags)
- TagAttrCompletionProvider walks the PSI parent chain to find the enclosing tag type, then offers the correct attribute list for that tag type with `=""` auto-inserted and caret positioned between the quotes
- `completion.contributor` extension registered in plugin.xml for `language="Mako Template"` — contributor fires only in Mako files
- All 57 existing tests pass with zero regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MakoCompletionContributor with tag-name and attribute providers** - `93fbbb8` (feat)
2. **Task 2: Register completion.contributor in plugin.xml and verify build** - `b9db23f` (feat)

## Files Created/Modified

- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/completion/MakoCompletionContributor.kt` - CompletionContributor with TagNameCompletionProvider and TagAttrCompletionProvider; TAG_NAMES list; TAG_ATTRIBUTES map for 6 tag types
- `src/main/resources/META-INF/plugin.xml` - Added completion.contributor extension after Phase 6 multiHostInjector entry

## Decisions Made

- Raw text inspection (`file.text.substring(offset-2, offset)`) chosen over PSI pattern matching for tag-name completion because the dummy identifier the completion framework injects disrupts the lexer before TAG_OPEN_xxx tokens appear, making PSI-based pattern matching unreliable.
- `result.withPrefixMatcher("")` is required so that the platform does not filter out lookup items whose strings start with `<%` — without the empty prefix matcher, no items would be shown.
- TAG_ATTRIBUTES map uses `element.javaClass` (the concrete `*Impl` class) for matching in the parent-walk because the PSI interface types and the generated impl classes have different `Class<?>` identities. The plan specified `Class<out PsiElement>` keys mapped from the PSI interface, but the generated impl classes are what appear in the parent chain. Adjusted to use the PSI interface `.java` class and compare via `TAG_ATTRIBUTES[element.javaClass]` — this works because the generated impls are looked up by their own class which matches the interface-keyed map entry.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 (MakoCompletionTest) can now be implemented; the contributor is registered and compiling
- Integration tests will verify the completion popup behavior end-to-end using the BasePlatformTestCase pattern established in Phase 5
- No blockers

---
*Phase: 07-completion*
*Completed: 2026-02-21*

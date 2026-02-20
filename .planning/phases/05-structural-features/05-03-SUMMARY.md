---
phase: 05-structural-features
plan: 03
subsystem: parser
tags: [grammarkit, bnf, psi, folding, parser-recovery, error-state]

# Dependency graph
requires:
  - phase: 05-structural-features
    provides: "MakoFoldingBuilder for def/block/doc/code folding; Phase 05-01 AST scanning"
  - phase: 03-parser
    provides: "GrammarKit-generated MakoParser.java, Mako.bnf grammar, MakoTypes.java token delegates"
provides:
  - "Grammar fix: template_text_content named BNF rule wrapping TEMPLATE_TEXT"
  - "Reordered item_ alternatives: template_text_content FIRST prevents GrammarKit recovery from consuming TEMPLATE_TEXT before item_() matches"
  - "5 regression tests: TEMPLATE_TEXT-only body folding, sibling tag cascading fix, fold range verification"
  - "Updated WellFormedFile.txt and MalformedTag.txt snapshots: TEMPLATE_TEXT_CONTENT composite nodes"
affects:
  - "Future grammar changes in Mako.bnf must maintain template_text_content ordering in item_ rule"
  - "Any rule that adds TEMPLATE_TEXT alternatives must be aware of GrammarKit recovery/rollback interaction"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "GrammarKit item_ alternative ordering: token-matching alternatives must come before pinned rules with recoverWhile to prevent recovery from consuming target tokens before the matching alternative fires"
    - "GrammarKit token wrapping: wrap bare consumeToken(TOKEN) in a named rule to create enter_section_/exit_section_ composite, but ordering in alternatives list is the critical fix"

key-files:
  created:
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoTemplateTextContent.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoTemplateTextContentImpl.java
  modified:
    - src/main/grammars/Mako.bnf
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoTypes.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoDefTag.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoBlockTag.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoVisitor.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoDefTagImpl.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoBlockTagImpl.java
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoFoldingTest.kt
    - src/test/testData/parser/WellFormedFile.txt
    - src/test/testData/parser/MalformedTag.txt

key-decisions:
  - "template_text_content FIRST in item_ alternatives: root fix is alternative ordering, not just naming the rule; named rule alone does not fix rollback issue"
  - "Bare consumeToken(TEMPLATE_TEXT) in item_ caused GrammarKit recovery from failed def_tag/block_tag alternatives to consume TEMPLATE_TEXT via reportError; item_() marker rollback un-consumed it; outer def_tag then saw TEMPLATE_TEXT instead of END_TAG"
  - "By checking template_text_content FIRST in item_, TEMPLATE_TEXT is consumed before any pinned rule's recovery can grab it"

patterns-established:
  - "GrammarKit alternative ordering: low-ambiguity token alternatives (single-token rules) should precede high-impact pinned rules with recoverWhile"
  - "After generateMakoParser: always restore MakoTokenTypes token delegates manually (generateTokens=false pattern)"

requirements-completed: [SYNX-06]

# Metrics
duration: 20min
completed: 2026-02-20
---

# Phase 5 Plan 03: Grammar Fix for TEMPLATE_TEXT-body Folding Summary

**GrammarKit item_ alternative reordering and template_text_content named rule fix the parser so <%def>/<%block> END_TAG is always consumed into DEF_TAG/BLOCK_TAG PSI composite regardless of body content**

## Performance

- **Duration:** 20 min
- **Started:** 2026-02-20T22:06:32Z
- **Completed:** 2026-02-20T22:26:00Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments

- Parser grammar fixed: TEMPLATE_TEXT-only tag bodies no longer cause END_TAG to be orphaned at file level
- Sibling <%def>/<%block> tags after a TEMPLATE_TEXT-only body now produce proper DEF_TAG/BLOCK_TAG PSI nodes
- 5 regression tests added covering single def/block with text body, sibling tags cascading fix, and fold range verification
- PSI snapshots (WellFormedFile.txt, MalformedTag.txt) updated to reflect TEMPLATE_TEXT_CONTENT composite nodes
- Total test count: 52 tests, all passing (previously 47 = 52 - 5 new regression tests)

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix Mako.bnf grammar and regenerate parser** - `a2563cf` (fix)
2. **Task 2: Add regression tests for TEMPLATE_TEXT-body folding** - `c6eca29` (test)

## Files Created/Modified

- `src/main/grammars/Mako.bnf` - Added template_text_content rule; reordered item_ to put it FIRST
- `src/main/gen/.../parser/MakoParser.java` - Regenerated: template_text_content first in item_(), new template_text_content() method
- `src/main/gen/.../psi/MakoTypes.java` - Added TEMPLATE_TEXT_CONTENT composite type; restored token delegates
- `src/main/gen/.../psi/MakoTemplateTextContent.java` - New PSI interface for TEMPLATE_TEXT_CONTENT composite
- `src/main/gen/.../psi/impl/MakoTemplateTextContentImpl.java` - New PSI impl
- `src/main/gen/.../psi/MakoDefTag.java` - Updated with getTemplateTextContentList()
- `src/main/gen/.../psi/MakoBlockTag.java` - Updated with getTemplateTextContentList()
- `src/main/gen/.../psi/MakoVisitor.java` - Updated with visitTemplateTextContent()
- `src/main/gen/.../psi/impl/MakoDefTagImpl.java` - Updated implementation
- `src/main/gen/.../psi/impl/MakoBlockTagImpl.java` - Updated implementation
- `src/test/kotlin/.../MakoFoldingTest.kt` - 5 new regression tests
- `src/test/testData/parser/WellFormedFile.txt` - Updated PSI snapshot (TEMPLATE_TEXT_CONTENT composites)
- `src/test/testData/parser/MalformedTag.txt` - Updated PSI snapshot

## Decisions Made

- **Alternative ordering is the real fix**: The initial plan described adding the `template_text_content` named rule as sufficient. Analysis of GrammarKit internals revealed that the ORDERING of alternatives in `item_` is the critical fix. When `def_tag()` (pinned rule) fails inside `item_()`, its `exit_section_impl_` calls `reportError` which consumes the current token (TEMPLATE_TEXT). When `item_()` subsequently fails (all alternatives exhausted), its marker rollback un-consumes TEMPLATE_TEXT. Putting `template_text_content` FIRST means TEMPLATE_TEXT is consumed and committed before any pinned rule's recovery can grab it.

- **Named rule is still needed**: Even with reordering, the named `template_text_content` rule is kept because it wraps TEMPLATE_TEXT in a proper TEMPLATE_TEXT_CONTENT composite (via enter_section_/exit_section_), making the PSI tree cleaner and more explicit than a bare consumeToken.

- **Parser snapshot updates are expected**: WellFormedFile.txt and MalformedTag.txt snapshots are updated to reflect the new PSI tree where TEMPLATE_TEXT tokens are properly wrapped in TEMPLATE_TEXT_CONTENT composites instead of appearing in PsiErrorElement nodes (for the well-formed case).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Initial fix approach was incorrect; root cause required deeper analysis**
- **Found during:** Task 1 (grammar fix and parser regeneration)
- **Issue:** The plan described adding `template_text_content` named rule (with enter_section_/exit_section_) as sufficient to fix the ErrorState tracking issue. After implementation, 3 of 5 regression tests failed: `testDefFoldRangeIncludesEndTag` (endOffset=30 vs expected 38), `testSiblingDefsWithPlainTextBodiesFold` (1 fold vs 2), `testSiblingDefAndBlockWithPlainTextBodiesFold` (1 fold vs 2).
- **Root cause**: Deep analysis of GrammarKit's `exit_section_impl_` revealed that when a pinned rule (def_tag, block_tag) fails inside item_(), its `reportError` call with `advance=true` consumes the TEMPLATE_TEXT token. When item_() subsequently fails (template_text_content finds nothing to consume since TEMPLATE_TEXT was already taken), item_()'s marker rollback undoes the reportError consumption. The outer def_tag_3 loop then sees TEMPLATE_TEXT instead of END_TAG, causing consumeToken(END_TAG) to fail.
- **Fix**: Reordered item_ alternatives to put `template_text_content` FIRST. When TEMPLATE_TEXT is the current token, template_text_content matches immediately before any pinned rule's recovery can consume it. template_text_content succeeds and item_() returns true with a proper drop (no rollback).
- **Files modified:** `src/main/grammars/Mako.bnf`, regenerated `MakoParser.java`, `MakoTypes.java`
- **Verification:** All 5 regression tests pass; all 52 tests pass; ./gradlew build succeeds
- **Committed in:** a2563cf (Task 1 commit, part of the same task)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Bug: root cause was alternative ordering not just named rule)
**Impact on plan:** Root cause required deeper GrammarKit analysis than planned. The actual fix (reordering) is simpler than any proposed alternative. No scope creep.

## Issues Encountered

- **GrammarKit alternative ordering interaction with recovery**: The fundamental issue was that GrammarKit's recovery from failed pinned rules (reportError + tokenAdvancer) consumes tokens even when those tokens are needed by later alternatives in the same item_() call. This interaction is not documented in the GrammarKit docs and required reading GeneratedParserUtilBase.java source to understand.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 5 Plan 03 gap closure complete: all three UAT issues resolved
- SYNX-06 requirement satisfied: <%def> and <%block> tags fold consistently regardless of body content
- 52 tests passing, no regressions
- Phase 5 (Structural Features) now has all 3 plans complete

---
*Phase: 05-structural-features*
*Completed: 2026-02-20*

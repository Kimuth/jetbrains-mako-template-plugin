---
status: resolved
trigger: "TEMPLATE_TEXT that follows closing structural tags (<%def>, <%block>, and likely <%doc>, <%namespace>, etc.) is consumed inside the tag's PSI node as PsiErrorElement instead of becoming a sibling template_text_content at file level."
created: 2026-02-21T00:00:00Z
updated: 2026-02-21T00:00:00Z
symptoms_prefilled: true
---

## Current Focus

hypothesis: CONFIRMED - tag_recover in both Mako.bnf and MakoParser.java is missing TEMPLATE_TEXT from its negative-lookahead stop set. All structural tags (def_tag, block_tag, include_tag, inherit_tag, namespace_tag, page_tag, code_block, module_block, doc_comment) share the same tag_recover predicate. Adding TEMPLATE_TEXT to tag_recover will fix the issue for all of them simultaneously.
test: Fix Mako.bnf and MakoParser.java (tag_recover / tag_recover_0), run tests
expecting: Tests pass; TEMPLATE_TEXT after any structural tag becomes a sibling node, not absorbed into the tag
next_action: Apply fix to Mako.bnf and MakoParser.java, add regression tests, run tests

## Symptoms

expected: After a `<%def>...</%def>` block, any following TEMPLATE_TEXT like `bar` should parse as a top-level `MakoTemplateTextContentImpl(TEMPLATE_TEXT_CONTENT)` sibling in the FILE node.
actual: `bar` ends up INSIDE `MakoDefTagImpl(DEF_TAG)` as `PsiErrorElement:'bar' unexpected`. The DEF_TAG node spans the entire content including `bar` (0,49) even though the END_TAG token is at (38,45).
errors: |
  Input: <%def name="my_function(arg)">\n${foo}\n</%def>\nbar
  Bad PSI tree: DEF_TAG(0,49) ... PsiErrorElement:'bar' unexpected(46,49) containing PsiElement(TEMPLATE_TEXT)('bar')(46,49)
reproduction: Create .mako file with <%def> or <%block> tag, close it with </%def> or </%block>, then add plain text on the next line
started: Has never worked

## Eliminated

(none yet)

## Evidence

- timestamp: 2026-02-21T00:01:00Z
  checked: Mako.bnf lines 141-144 (tag_recover rule)
  found: |
    private tag_recover ::= !(TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE
                            | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
                            | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
                            | EXPR_START | LINE_COMMENT)
    TEMPLATE_TEXT is NOT in the stop set.
  implication: When any structural tag (def_tag, block_tag, etc.) finishes parsing (even successfully), the recoverWhile loop runs tag_recover to consume "junk" tokens. Since TEMPLATE_TEXT is not in the stop set, the loop consumes following TEMPLATE_TEXT and wraps it in a PsiErrorElement inside the tag node.

- timestamp: 2026-02-21T00:01:30Z
  checked: MakoParser.java lines 479-509 (tag_recover and tag_recover_0 methods)
  found: |
    tag_recover_0 checks: TAG_OPEN_DEF, TAG_OPEN_BLOCK, TAG_OPEN_INHERIT, TAG_OPEN_INCLUDE,
    TAG_OPEN_NAMESPACE, TAG_OPEN_PAGE, END_TAG, CONTROL_LINE, CODE_OPEN, MODULE_OPEN, DOC_OPEN,
    EXPR_START, LINE_COMMENT. TEMPLATE_TEXT is absent.
  implication: The Java parser exactly mirrors the BNF. Both need TEMPLATE_TEXT added to the stop set. The file header says "This is a generated file. Not intended for manual editing." but the expression_recover fix was applied directly to the .java file (confirmed: TEMPLATE_TEXT appears at line 260 in expression_recover_0). Therefore MakoParser.java IS being maintained by hand, and we must update tag_recover_0 directly.

- timestamp: 2026-02-21T00:02:00Z
  checked: expression_recover in MakoParser.java (lines 241-274) vs expression_recover in Mako.bnf (lines 146-150)
  found: Both contain TEMPLATE_TEXT in their stop sets. The pattern of the fix is clear - add `if (!result_) result_ = consumeToken(builder_, TEMPLATE_TEXT);` to tag_recover_0.
  implication: The fix for tag_recover is exactly analogous to the expression_recover fix.

## Resolution

root_cause: |
  The `tag_recover` predicate in Mako.bnf (and its generated counterpart `tag_recover_0` in
  MakoParser.java) was missing TEMPLATE_TEXT from its negative-lookahead stop token set.
  GrammarKit's `recoverWhile` mechanism runs after every pinned rule - even after a SUCCESSFUL
  parse. So after `def_tag` or `block_tag` (and all other structural tags using `tag_recover`)
  successfully consumed their END_TAG, the recovery loop continued running and consumed the next
  TEMPLATE_TEXT token into the tag's PSI node as a PsiErrorElement instead of leaving it as a
  top-level sibling. This is the exact same class of bug that was fixed for `expression_recover`
  the previous day (that fix added TEMPLATE_TEXT to expression_recover's stop set).
fix: |
  Added `TEMPLATE_TEXT` to the negative-lookahead stop set in `tag_recover` (Mako.bnf line 144)
  and to `tag_recover_0` (MakoParser.java line 508). Because all structural tags (def_tag,
  block_tag, inherit_tag, include_tag, namespace_tag, page_tag, code_block, module_block,
  doc_comment) share the single `tag_recover` predicate, a single two-file change fixes all of
  them simultaneously.
verification: |
  All 58 tests pass (./gradlew test BUILD SUCCESSFUL).
  New regression tests PARS-08 (testDefTagFollowedByText) and PARS-09 (testBlockTagFollowedByText)
  confirm that `bar` after `</%def>` and `</%block>` respectively parses as a top-level
  MakoTemplateTextContentImpl sibling in the FILE node with no PsiErrorElements.
  The MalformedTag.txt expected output was also updated: it now correctly shows `valid content`
  as a top-level MakoTemplateTextContentImpl sibling instead of being absorbed into the
  malformed DEF_TAG node.
files_changed:
  - src/main/grammars/Mako.bnf
  - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
  - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParsingTest.kt
  - src/test/testData/parser/DefTagFollowedByText.mako
  - src/test/testData/parser/DefTagFollowedByText.txt
  - src/test/testData/parser/BlockTagFollowedByText.mako
  - src/test/testData/parser/BlockTagFollowedByText.txt
  - src/test/testData/parser/MalformedTag.txt

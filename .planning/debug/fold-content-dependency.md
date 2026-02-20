---
status: diagnosed
trigger: "Code Folding Depends on Tag Body Content"
created: 2026-02-20T21:15:00Z
updated: 2026-02-20T21:30:00Z
---

## Current Focus

hypothesis: CONFIRMED - GrammarKit parser fails to include END_TAG inside DEF_TAG/BLOCK_TAG PSI nodes when the tag body contains only TEMPLATE_TEXT (no pinned constructs like expressions or code blocks). This causes the folding builder to create fold regions that exclude the closing tag, and for sibling tags, prevents the second tag from being recognized as a MakoDefTag/MakoBlockTag PSI node at all.
test: Diagnostic test dumped PSI trees and fold regions for 14 different body content patterns
expecting: END_TAG inside DEF_TAG node for all patterns
next_action: Root cause found. Report diagnosis.

## Symptoms

expected: Fold gutter arrows appear for all <%def> and <%block> tags regardless of body content
actual: Fold arrows only appear for certain tag body contents; some content prevents arrows
errors: No runtime errors, but PSI tree structure is malformed (END_TAG orphaned at file level)
reproduction: Create <%def> tag with only plain text body (no expressions); fold arrow may appear but fold range excludes closing tag. Create sibling <%def> tags with plain text bodies; second tag gets no fold arrow at all.
started: Since parser was first implemented with pin=1 and recoverWhile=tag_recover on def_tag/block_tag

## Eliminated

- hypothesis: expression_recover absorbs END_TAG
  evidence: END_TAG is in expression_recover's stop set. Test confirms END_TAG is preserved.
  timestamp: 2026-02-20T21:20:00Z

- hypothesis: tag_recover absorbs END_TAG of sibling tags
  evidence: END_TAG is in tag_recover's stop set. Recovery stops before END_TAG.
  timestamp: 2026-02-20T21:20:00Z

- hypothesis: Lexer produces wrong token types for body content
  evidence: Lexer correctly produces TEMPLATE_TEXT for plain text and END_TAG for closing tags
  timestamp: 2026-02-20T21:18:00Z

- hypothesis: FoldingDescriptor rejected due to single-line range
  evidence: All test cases are multi-line; folds ARE created but with wrong ranges
  timestamp: 2026-02-20T21:22:00Z

- hypothesis: findClosingTokenEnd fallback returns wrong offset
  evidence: The fallback to composite.textRange.endOffset works correctly; the issue is that END_TAG is not in the composite at all
  timestamp: 2026-02-20T21:25:00Z

## Evidence

- timestamp: 2026-02-20T21:18:00Z
  checked: WellFormedFile.txt PSI tree output from existing parser test
  found: |
    TEMPLATE_TEXT tokens inside def/block tag bodies are wrapped in PsiErrorElement nodes.
    Expression nodes absorb trailing TEMPLATE_TEXT via expression_recover (e.g., ${name|h}! absorbs the ! into the expression node).
    END_TAG is sometimes inside DEF_TAG and sometimes orphaned at file level.
  implication: Parser error recovery behavior varies based on body content composition

- timestamp: 2026-02-20T21:22:00Z
  checked: Generated MakoParser.java - def_tag() function
  found: |
    def_tag rule: TAG_OPEN_DEF tag_attribute* TAG_CLOSE item_* END_TAG
    pin=1 on TAG_OPEN_DEF, recoverWhile=tag_recover
    item_* calls item_() which tries 13 alternatives, TEMPLATE_TEXT is last
    consumeToken(TEMPLATE_TEXT) has no pin, no recoverWhile
  implication: TEMPLATE_TEXT consumption in item_* does not trigger any recovery mechanism

- timestamp: 2026-02-20T21:23:00Z
  checked: GrammarKit documentation on recoverWhile behavior
  found: "recoverWhile matches any number of tokens after the rule matching completes with any result"
  implication: Recovery runs even on successful matches when rule is pinned. This explains why expressions absorb trailing TEMPLATE_TEXT.

- timestamp: 2026-02-20T21:25:00Z
  checked: Diagnostic test with 14 content patterns - PSI tree structure and fold regions
  found: |
    CRITICAL FINDINGS:

    1. Body = only TEMPLATE_TEXT ("plain text"):
       DEF_TAG(0,26) has NO END_TAG child. END_TAG(27,34) is orphaned at file level.
       Fold(0,26) - does NOT include </%def>.

    2. Body = expression then text ("${value} text"):
       DEF_TAG(0,37) HAS END_TAG child at offset 30. Expression absorbs trailing text.
       Fold(0,37) - correctly includes </%def>.

    3. Body = text then expression ("text ${value}"):
       DEF_TAG(0,37) HAS END_TAG child at offset 30.
       Fold(0,37) - correctly includes </%def>.

    4. Body = empty:
       DEF_TAG(0,23) HAS END_TAG child at offset 16.
       Fold(0,23) - correctly includes </%def>.

    5. Sibling defs with plain text bodies:
       First DEF_TAG(0,25) - NO END_TAG. Fold(0,25) excludes </%def>.
       Second def - NOT wrapped in DEF_TAG node at all! Its tokens are flat file-level elements.
       Only 1 fold region instead of expected 2.

    6. Sibling def + block with plain text:
       DEF_TAG(0,25) - NO END_TAG. Second block also not wrapped.
       Only 1 fold region.
  implication: |
    The parser's exit_section_impl_ with tag_recover does NOT consume END_TAG into the
    DEF_TAG node when the body contains only unpinned TEMPLATE_TEXT tokens. This causes:
    (a) Fold range excludes closing tag
    (b) Orphaned END_TAG triggers makoFile() item_* loop exit, causing subsequent
        sibling tags to be parsed as flat file-level tokens without composite nodes

- timestamp: 2026-02-20T21:27:00Z
  checked: PSI tree for body with HTML content (TEMPLATE_TEXT only - <div>hello</div>)
  found: |
    DEF_TAG(0,45) has NO END_TAG child. END_TAG at 46 is file-level.
    Multiple TEMPLATE_TEXT tokens (<, div..., <, /div>) are consumed by item_* loop.
    Same pattern as plain text: TEMPLATE_TEXT-only body -> END_TAG orphaned.
  implication: Confirms the pattern is NOT about specific text content but about the ABSENCE of pinned constructs in the body

- timestamp: 2026-02-20T21:28:00Z
  checked: PSI tree for body with code_block (<% x=1 %>)
  found: |
    DEF_TAG(0,39) HAS END_TAG child. CODE_BLOCK parsed correctly inside body.
    Fold(0,39) is correct.
  implication: Any pinned construct (expression, code_block, control_line) in the body fixes the issue because its recoverWhile absorbs subsequent TEMPLATE_TEXT and preserves END_TAG for the parent def_tag

## Resolution

root_cause: |
  The GrammarKit parser's `exit_section_impl_` method, when processing `def_tag` and `block_tag`
  rules with `pin=1` and `recoverWhile=tag_recover`, fails to consume END_TAG into the composite
  PSI node when the tag body contains ONLY TEMPLATE_TEXT tokens (consumed via unpinned
  `consumeToken(TEMPLATE_TEXT)` in the `item_*` loop).

  The mechanism:
  1. def_tag matches TAG_OPEN_DEF (pinned), attributes, TAG_CLOSE
  2. item_* loop: TEMPLATE_TEXT is consumed by `consumeToken(TEMPLATE_TEXT)` (no pin, no recovery)
  3. item_* loop breaks when END_TAG is encountered (no alternative matches END_TAG)
  4. def_tag step 5 attempts `consumeToken(END_TAG)` but FAILS
  5. exit_section_ with tag_recover runs; tag_recover stops before END_TAG (it's in the stop set)
  6. DEF_TAG node is created WITHOUT END_TAG; END_TAG becomes an orphaned file-level token

  When the body contains a PINNED construct (expression, code_block, etc.), that construct's
  `recoverWhile` absorbs trailing TEMPLATE_TEXT into itself. This changes the parser state
  (specifically the ErrorState frame's lastVariantAt/errorReportedAt fields) in a way that
  allows the subsequent `consumeToken(END_TAG)` to succeed.

  The root cause is in the GRAMMAR design, not in the folding builder:
  - `item_` treats TEMPLATE_TEXT as a raw token match (`consumeToken(TEMPLATE_TEXT)`)
  - This unpinned consumption does not update GrammarKit's error recovery state properly
  - The mismatch between the item_* loop's expectations and GrammarKit's internal state
    causes END_TAG consumption to fail

  CASCADING EFFECTS:
  - Orphaned END_TAG causes makoFile()'s item_* loop to fail on the next iteration
  - Subsequent sibling tags (def, block) are parsed as flat file-level tokens without
    composite DEF_TAG/BLOCK_TAG nodes
  - PsiTreeUtil.collectElementsOfType() cannot find these flat tokens as MakoDefTag/MakoBlockTag
  - No fold regions are created for affected sibling tags

fix: (not applied - diagnosis only)
verification: (not applied - diagnosis only)
files_changed: []

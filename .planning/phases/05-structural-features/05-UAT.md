---
status: diagnosed
phase: 05-structural-features
source: 05-01-SUMMARY.md, 05-02-SUMMARY.md
started: 2026-02-20T20:00:00Z
updated: 2026-02-20T20:15:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Code Folding - Def Tags
expected: Open a .mako file with a <%def name="foo">...</%def> block (multi-line content). A fold gutter arrow appears on the <%def> opening line. Clicking collapses the block to a single line with [...] placeholder.
result: pass

### 2. Code Folding - Block Tags
expected: In the same or another .mako file, a <%block name="header">...</%block> with multi-line content shows a fold gutter arrow on the opening line. Clicking collapses it.
result: issue
reported: "It actually depends on what is inside that <%block />, it's the same with previous checkpoint verification for <%def /> block."
severity: major

### 3. Code Folding - Control Flow
expected: A % for, % if, or % while control line with a matching % endfor/endif/endwhile shows a fold gutter arrow on the opening % line. Clicking collapses the control flow block.
result: pass

### 4. Doc Comment Default Collapse
expected: When opening a .mako file containing a <%doc>...</%doc> block, the doc comment block is collapsed by default (you see the fold placeholder, not the expanded content).
result: pass

### 5. Module Block Default Collapse
expected: When opening a .mako file containing a <%! ... %> module-level code block, it is collapsed by default on file open.
result: pass

### 6. Structure View - Def and Block Tree
expected: Open a .mako file with <%def> and <%block> tags. Open Structure view (View > Tool Windows > Structure, or Ctrl+F12). The panel shows a tree listing each <%def> and <%block> by name (e.g., "foo", "header").
result: pass

### 7. Structure View - Navigate to Declaration
expected: In the Structure view panel, click on a <%def> or <%block> node. The editor caret moves to the line where that tag is declared.
result: pass

## Summary

total: 7
passed: 6
issues: 1
pending: 0
skipped: 0

## Gaps

- truth: "<%def> and <%block> fold arrows appear consistently regardless of tag body content"
  status: failed
  reason: "User reported: It actually depends on what is inside that <%block />, it's the same with previous checkpoint verification for <%def /> block."
  severity: major
  test: 2
  root_cause: "GrammarKit parser fails to include END_TAG inside DEF_TAG/BLOCK_TAG PSI nodes when tag body contains only TEMPLATE_TEXT (no pinned constructs). Unpinned consumeToken(TEMPLATE_TEXT) in item_ rule does not update ErrorState, causing subsequent consumeToken(END_TAG) to fail. END_TAG becomes orphaned at file level, breaking fold region calculation and cascading to break all subsequent sibling def/block tags."
  artifacts:
    - path: "src/main/grammars/Mako.bnf"
      issue: "def_tag and block_tag rules use item_* for body; TEMPLATE_TEXT matched by unpinned consumeToken fails to advance error recovery state"
    - path: "src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java"
      issue: "Generated def_tag parser step 5 (consumeToken END_TAG) fails when body has only unpinned TEMPLATE_TEXT items"
    - path: "src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/folding/MakoFoldingBuilder.kt"
      issue: "findClosingTokenEnd falls back to composite.textRange.endOffset when END_TAG absent — fold region excludes closing tag"
  missing:
    - "Grammar fix: ensure TEMPLATE_TEXT in tag bodies is handled by a pinned rule or add folding builder fallback to scan file-level siblings for orphaned END_TAG"
  debug_session: ".planning/debug/fold-content-dependency.md"

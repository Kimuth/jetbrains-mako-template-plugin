---
status: diagnosed
phase: 04-syntax-highlighting-comment-support
source: [04-01-SUMMARY.md]
started: 2026-02-20T00:00:00Z
updated: 2026-02-20T00:01:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Distinct Colors for Mako Constructs
expected: Open a .mako file containing directives (<%def>, <%block>), expressions (${...}), control lines (% for, % if), and comments (##, <%doc>). Each construct type should display in a visually distinct color from the surrounding HTML/template text.
result: issue
reported: "`<%def name=\"foo()\">`, `% if x:`, `## comment` and `<%doc>` do have distinct colors from plain text (or HTML), but `${foo}` or `${\"foo\"}` just looks like plain text"
severity: major

### 2. Brace Matching on Mako Tags
expected: Place cursor on <%def and the matching </%def> closing tag highlights (and vice versa). Same for <%block>/</%block>, <%doc>/</%doc>, ${ / }, and <% / %>.
result: pass

### 3. Line Comment Toggle (Ctrl+/)
expected: Place cursor on a line containing Mako code and press Ctrl+/. A "## " prefix is inserted at the beginning of the line. Pressing Ctrl+/ again removes it.
result: pass

### 4. Block Comment Toggle (Ctrl+Shift+/)
expected: Select a region of Mako content and press Ctrl+Shift+/. The selection is wrapped in <%doc>...</%doc> block comment tags. Pressing Ctrl+Shift+/ again unwraps it.
result: pass

### 5. Color Settings Page
expected: Navigate to Settings > Editor > Color Scheme > Mako. Nine named color entries appear (e.g., Tag, Expression Delimiter, Control Keyword, Comment, etc.) and each can be customized. A demo Mako snippet previews the colors.
result: pass

## Summary

total: 5
passed: 4
issues: 1
pending: 0
skipped: 0

## Gaps

- truth: "Expression constructs (${...}) display in a distinct color from surrounding template text"
  status: failed
  reason: "User reported: `<%def name=\"foo()\">`, `% if x:`, `## comment` and `<%doc>` do have distinct colors from plain text (or HTML), but `${foo}` or `${\"foo\"}` just looks like plain text"
  severity: major
  test: 1
  root_cause: "MAKO_EXPRESSION TextAttributesKey falls back to TEMPLATE_LANGUAGE_COLOR which inherits from HighlighterColors.TEXT (plain text color with no visible foreground)"
  artifacts:
    - path: "src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt"
      issue: "Line 22: MAKO_EXPRESSION fallback is TEMPLATE_LANGUAGE_COLOR which resolves to plain text"
  missing:
    - "Change MAKO_EXPRESSION fallback to a visually distinct color (e.g., MARKUP_TAG or NUMBER)"
  debug_session: ".planning/debug/expr-highlight-issue.md"

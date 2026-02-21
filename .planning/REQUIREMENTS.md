# Requirements: Mako Template Plugin

**Defined:** 2026-02-21
**Core Value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm

## v0.2.0 Requirements

All requirements derived from the v0.1.0 code review (`.docs/code-review.md`, reviewed at commit `cbb336d`).

### PSI Correctness

- [ ] **PSI-01**: `getName()` in `MakoDefTagMixin` and `MakoBlockTagMixin` returns the value paired with the `name=` attribute, not the first `TAG_ATTR_VALUE` found
- [ ] **PSI-02**: `setName()` in `MakoDefTagMixin` and `MakoBlockTagMixin` throws `UnsupportedOperationException` instead of silently returning `this`

### Code Folding

- [ ] **FOLD-01**: `buildDocCommentFolds()`, `buildModuleBlockFolds()`, and `buildCodeBlockFolds()` use recursive descent (via `PsiTreeUtil`) so that doc comments, code blocks, and module blocks nested inside `<%def>` or `<%block>` tags produce fold regions

### Python Injection

- [ ] **INJECT-01**: `updateText()` in `MakoExpressionMixin`, `MakoCodeBlockMixin`, and `MakoModuleBlockMixin` throws `UnsupportedOperationException` instead of silently returning `this`
- [ ] **INJECT-02**: Python injection range in `MakoPythonInjector` stops at the first `FILTER_SEP` token, so filter names (e.g., `h`, `trim` in `${x | h, trim}`) are excluded from the injected Python fragment

### Editor Views

- [ ] **VIEW-01**: `MAKO_CODE_CONTENT` token attribute key defaults to `DefaultLanguageHighlighterColors.IDENTIFIER` instead of `STRING`
- [ ] **VIEW-02**: `MakoStructureViewElement` collects defs and blocks together sorted by source offset, preserving document order instead of showing all defs before all blocks
- [ ] **VIEW-03**: `MakoPairedBraceMatcher` includes a `BracePair(MODULE_OPEN, CODE_CLOSE, false)` entry so `<%!...%>` blocks get bracket highlighting
- [ ] **VIEW-04**: `MakoStructureViewElement` fallback `ItemPresentation` returns `AllIcons.Nodes.Function` (or equivalent method icon) instead of `MakoIcons.FILE` for def/block nodes
- [ ] **VIEW-05**: `MakoFoldingBuilder` DUMMY_BLOCK detection uses a language-based check (e.g., `element.language == Language.ANY`) instead of brittle `toString()` / `javaClass.simpleName` string comparison
- [ ] **VIEW-06**: `MakoLexerAdapter` logs a warning via `Logger` when `braceDepth` exceeds 15 before clamping to 0xF

### Code Completion

- [ ] **COMP-01**: `<%doc` insert handler in `MakoCompletionContributor` computes the replacement start position from `ltPos` (already computed earlier in the method) instead of using `ctx.startOffset - 2`, so partial text already typed is correctly accounted for
- [ ] **COMP-02**: `MakoCompletionContributor` uses `parameters.editor.document.charsSequence` and scans backward from `offset` instead of copying `file.text` and taking a substring

### Annotator

- [ ] **ANNOT-01**: `MakoAnnotator` and `MakoCompletionContributor` language guard uses `element.containingFile.language != MakoLanguage.INSTANCE` identity comparison instead of `language.id != "Mako Template"` string literal
- [ ] **ANNOT-02**: A parser fixture test covers an unknown directive (e.g., `<%bogus>`) to guard against future lexer changes silently breaking `checkForInvalidDirective` detection

### Cleanup

- [ ] **CLEAN-01**: `FILTER_NAME` token type removed from `MakoTokenTypes`; unreachable highlighter branch for `FILTER_NAME` removed from `MakoSyntaxHighlighter`
- [ ] **CLEAN-02**: `TEMPLATE_CONTENT` and `TAG_OPENS` token sets removed from `MakoTokenSets` (confirmed unused in codebase)
- [ ] **CLEAN-03**: `IncompleteCodeBlock.mako` test fixture either gets a verified `.txt` companion committed, or the fixture file is deleted

## Future Requirements

*(None identified — v0.2.0 is cleanup-only. New features tracked in PROJECT.md v2 requirements.)*

## Out of Scope

| Feature | Reason |
|---------|--------|
| Implement `updateText()` round-trip editing | High complexity; PSI write operations require platform expertise; deferred to future milestone |
| Implement `setName()` rename refactoring | Requires cross-file reference resolution; deferred to future milestone |
| Filter-name tokenization in lexer | Lexer change needed to emit `FILTER_NAME` tokens; deferred — CLEAN-01 removes dead code instead |
| HTML language injection | Deferred from v0.1.0; not part of bug-fix scope |

## Traceability

*(Populated during roadmap creation)*

| Requirement | Phase | Status |
|-------------|-------|--------|
| PSI-01 | — | Pending |
| PSI-02 | — | Pending |
| FOLD-01 | — | Pending |
| INJECT-01 | — | Pending |
| INJECT-02 | — | Pending |
| VIEW-01 | — | Pending |
| VIEW-02 | — | Pending |
| VIEW-03 | — | Pending |
| VIEW-04 | — | Pending |
| VIEW-05 | — | Pending |
| VIEW-06 | — | Pending |
| COMP-01 | — | Pending |
| COMP-02 | — | Pending |
| ANNOT-01 | — | Pending |
| ANNOT-02 | — | Pending |
| CLEAN-01 | — | Pending |
| CLEAN-02 | — | Pending |
| CLEAN-03 | — | Pending |

**Coverage:**
- v0.2.0 requirements: 18 total
- Mapped to phases: 0
- Unmapped: 18 ⚠️

---
*Requirements defined: 2026-02-21*
*Last updated: 2026-02-21 after initial definition*

# Roadmap: Mako Template Plugin for JetBrains

## Milestones

- ✅ **v0.1.0 Initial Release** — Phases 1–9 (shipped 2026-02-21)
- 🚧 **v0.2.0 Bug Fixing & Cleanup** — Phases 10–16 (in progress)

## Phases

<details>
<summary>✅ v0.1.0 Initial Release (Phases 1–9) — SHIPPED 2026-02-21</summary>

- [x] Phase 1: Language Foundation (2/2 plans) — completed 2026-02-19
- [x] Phase 2: Lexer (3/3 plans) — completed 2026-02-19
- [x] Phase 3: Parser and PSI Tree (3/3 plans) — completed 2026-02-19
- [x] Phase 4: Syntax Highlighting and Comment Support (2/2 plans) — completed 2026-02-19
- [x] Phase 5: Structural Features (3/3 plans) — completed 2026-02-20
- [x] Phase 6: Python Language Injection (2/2 plans) — completed 2026-02-21
- [x] Phase 7: Completion (2/2 plans) — completed 2026-02-21
- [x] Phase 8: Error Annotations and Release Readiness (3/3 plans) — completed 2026-02-21
- [x] Phase 9: Marketplace Branding (3/3 plans) — completed 2026-02-21

Full details: `.planning/milestones/v0.1.0-ROADMAP.md`

</details>

### 🚧 v0.2.0 Bug Fixing & Cleanup (In Progress)

**Milestone Goal:** Fix all known bugs from the v0.1.0 code review and eliminate dead code/housekeeping gaps so the codebase is correct and clean.

- [x] **Phase 10: PSI Correctness** — Fix getName/setName mixin bugs so named element contract is correct (completed 2026-02-21)
- [x] **Phase 11: Python Injection Fixes** — Fix updateText no-op and filter-name injection boundary (completed 2026-02-21)
- [x] **Phase 12: Code Folding and Structure View** — Fix nested folding and structure view ordering, icon, and DUMMY_BLOCK detection (completed 2026-02-21)
- [ ] **Phase 13: Editor Behavior Fixes** — Fix code content color, MODULE_OPEN brace pair, and braceDepth overflow warning
- [ ] **Phase 14: Completion Fixes** — Fix doc-insert offset math and eliminate full-file text allocation
- [ ] **Phase 15: Annotator Fixes** — Fix language guard and add unknown-directive regression test
- [ ] **Phase 16: Dead Code Cleanup** — Remove FILTER_NAME token, unused token sets, and resolve orphaned fixture

## Phase Details

### Phase 10: PSI Correctness
**Goal**: Named PSI elements (def/block tags) expose correct names and explicitly reject unsupported mutations
**Depends on**: Nothing (first v0.2.0 phase)
**Requirements**: PSI-01, PSI-02
**Success Criteria** (what must be TRUE):
  1. `MakoDefTagMixin.getName()` returns the value of the attribute paired with the `name=` key, not the first `TAG_ATTR_VALUE` found in the tree
  2. `MakoBlockTagMixin.getName()` behaves identically to the def mixin fix
  3. `setName()` in both mixins throws `UnsupportedOperationException` so callers receive a clear failure signal instead of a silent no-op
  4. Existing PSI tests pass without regression
**Plans**: 1 plan

Plans:
- [x] 10-01-PLAN.md — TDD: Fix getName attribute pairing and setName throws in PSI mixins

### Phase 11: Python Injection Fixes
**Goal**: Injection host mixins fail loudly on unsupported round-trip edits, and injection ranges exclude filter names
**Depends on**: Nothing (independent of Phase 10)
**Requirements**: INJECT-01, INJECT-02
**Success Criteria** (what must be TRUE):
  1. `updateText()` in `MakoExpressionMixin`, `MakoCodeBlockMixin`, and `MakoModuleBlockMixin` throws `UnsupportedOperationException` instead of returning `this` silently
  2. Python injection range for an expression like `${x | h, trim}` ends at the first `FILTER_SEP` token, so `h` and `trim` are not presented to the Python language service as Python code
  3. Injection tests confirm correct range boundaries with and without filter clauses
**Plans**: 2 plans

Plans:
- [x] 11-01-PLAN.md — TDD: Make updateText() throw UnsupportedOperationException in injection host mixins
- [ ] 11-02-PLAN.md — Fix MakoPythonInjector injection range to exclude filter names

### Phase 12: Code Folding and Structure View
**Goal**: Folding covers nested constructs inside def/block, and structure view displays elements in document order with correct icons
**Depends on**: Nothing (independent)
**Requirements**: FOLD-01, VIEW-02, VIEW-04, VIEW-05
**Success Criteria** (what must be TRUE):
  1. A `<%doc>` block nested inside a `<%def>` produces a visible fold region in the editor
  2. A `<% %>` code block or `<%! %>` module block nested inside a `<%block>` produces a visible fold region
  3. Structure View lists `<%def>` and `<%block>` entries interleaved in document order, not all defs before all blocks
  4. Def and block entries in Structure View display a function/method icon, not the Mako file icon
  5. `DUMMY_BLOCK` detection in `MakoFoldingBuilder` uses a language identity check (`element.language == Language.ANY`) rather than a string comparison against class name
**Plans**: 2 plans

Plans:
- [ ] 12-01-PLAN.md — Fix MakoFoldingBuilder: recursive nested fold descent and Language.ANY DUMMY_BLOCK check
- [ ] 12-02-PLAN.md — Fix MakoStructureViewElement: document-order children and AllIcons.Nodes.Function icon

### Phase 13: Editor Behavior Fixes
**Goal**: Code content tokens use the correct default color, the module block brace pair is matched, and brace depth overflow is logged
**Depends on**: Nothing (independent)
**Requirements**: VIEW-01, VIEW-03, VIEW-06
**Success Criteria** (what must be TRUE):
  1. `MAKO_CODE_CONTENT` tokens render with the `IDENTIFIER` color attribute by default (not `STRING`), so code block content is visually distinct from string literals
  2. Placing the cursor on `<%!` highlights its matching `%>` (MODULE_OPEN is registered in `MakoPairedBraceMatcher`)
  3. When the lexer encounters a `braceDepth` value exceeding 15, a warning is emitted via `Logger` before clamping, so abnormal nesting is observable in IDE logs
**Plans**: TBD

### Phase 14: Completion Fixes
**Goal**: Doc-tag insert handler computes the correct replacement range, and completion does not allocate a full file text copy per keystroke
**Depends on**: Nothing (independent)
**Requirements**: COMP-01, COMP-02
**Success Criteria** (what must be TRUE):
  1. Accepting the `<%doc` completion item when partial text (e.g., `<%d`) is already typed replaces the correct range and does not leave stale characters
  2. Completion contribution reads `document.charsSequence` (a view) instead of copying the full file text, so no unnecessary string allocation occurs per keystroke
  3. Existing completion tests pass without regression
**Plans**: TBD

### Phase 15: Annotator Fixes
**Goal**: Language guards use type-safe identity comparison, and unknown-directive detection is covered by a regression test
**Depends on**: Nothing (independent)
**Requirements**: ANNOT-01, ANNOT-02
**Success Criteria** (what must be TRUE):
  1. `MakoAnnotator` and `MakoCompletionContributor` guard against non-Mako files using `element.containingFile.language != MakoLanguage.INSTANCE` (not a string literal comparison against `"Mako Template"`)
  2. A parser fixture test exists for a `.mako` file containing `<%bogus>`, and the PSI tree produced includes an error node or invalid-directive marker
  3. The regression test is committed so future lexer changes that break `checkForInvalidDirective` are caught automatically
**Plans**: TBD

### Phase 16: Dead Code Cleanup
**Goal**: Dead token constants, unused token sets, and orphaned test fixtures are removed from the codebase
**Depends on**: Phase 15 (ANNOT-01 confirms no remaining string-literal uses of FILTER_NAME-adjacent identifiers before removal)
**Requirements**: CLEAN-01, CLEAN-02, CLEAN-03
**Success Criteria** (what must be TRUE):
  1. `FILTER_NAME` is absent from `MakoTokenTypes` and no unreachable branch for it exists in `MakoSyntaxHighlighter`
  2. `TEMPLATE_CONTENT` and `TAG_OPENS` are absent from `MakoTokenSets` and no reference to them remains in the codebase
  3. `IncompleteCodeBlock.mako` either has a verified `.txt` companion committed alongside it, or the fixture file is deleted — no orphaned fixture exists
  4. `./gradlew check` passes with zero errors after all removals
**Plans**: TBD

## Progress

**Execution Order:** 10 → 11 → 12 → 13 → 14 → 15 → 16

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Language Foundation | v0.1.0 | 2/2 | Complete | 2026-02-19 |
| 2. Lexer | v0.1.0 | 3/3 | Complete | 2026-02-19 |
| 3. Parser and PSI Tree | v0.1.0 | 3/3 | Complete | 2026-02-19 |
| 4. Syntax Highlighting and Comment Support | v0.1.0 | 2/2 | Complete | 2026-02-19 |
| 5. Structural Features | v0.1.0 | 3/3 | Complete | 2026-02-20 |
| 6. Python Language Injection | v0.1.0 | 2/2 | Complete | 2026-02-21 |
| 7. Completion | v0.1.0 | 2/2 | Complete | 2026-02-21 |
| 8. Error Annotations and Release Readiness | v0.1.0 | 3/3 | Complete | 2026-02-21 |
| 9. Marketplace Branding | v0.1.0 | 3/3 | Complete | 2026-02-21 |
| 10. PSI Correctness | v0.2.0 | 1/1 | Complete | 2026-02-21 |
| 11. Python Injection Fixes | 2/2 | Complete    | 2026-02-21 | - |
| 12. Code Folding and Structure View | 2/2 | Complete   | 2026-02-21 | - |
| 13. Editor Behavior Fixes | v0.2.0 | 0/TBD | Not started | - |
| 14. Completion Fixes | v0.2.0 | 0/TBD | Not started | - |
| 15. Annotator Fixes | v0.2.0 | 0/TBD | Not started | - |
| 16. Dead Code Cleanup | v0.2.0 | 0/TBD | Not started | - |

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-21 after v0.2.0 milestone start)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 17: Clean Up Orphaned Test Fixtures (COMPLETE)

## Current Position

Phase: 17 of 17 (Clean Up Orphaned Test Fixtures)
Plan: 1 of 1 in current phase (COMPLETE)
Status: Phase 17 Plan 01 complete — 5 orphaned test fixtures deleted, testData/ clean
Last activity: 2026-02-22 — Phase 17 Plan 01 complete (deleted rename/ scaffold, 3 annotator fixtures, 1 folding fixture; ./gradlew check passes)

Progress: [██████████] 100% (Phase 17 complete — all orphaned fixtures removed)

## Performance Metrics

**v0.1.0 Velocity:**
- Total plans completed: 23
- Timeline: 3 days (2026-02-19 → 2026-02-21)
- Files changed: 222, LOC: ~4,800 (hand-written + generated + tests)

**By Phase (v0.1.0):**

| Phase | Plans | Duration (min) | Avg/Plan |
|-------|-------|----------------|----------|
| 01-language-foundation | 2 | 11 | 5.5 |
| 02-lexer | 3 | 7 | 2.3 |
| 03-parser | 3 | 15 | 5 |
| 04-syntax-highlighting | 2 | 3 | 1.5 |
| 05-structural-features | 3 | 78 | 26 |
| 06-python-language-injection | 2 | 33 | 16.5 |
| 07-completion | 2 | 33 | 16.5 |
| 08-error-annotations | 3 | 76 | 25.3 |
| 09-marketplace-branding | 3 | 9 | 3 |
| Phase 10-psi-correctness P01 | 3 | 2 tasks | 3 files |
| Phase 11-python-injection-fixes P01 | 2 | 5 min | 4 files |
| Phase 11-python-injection-fixes P02 | 5 | 2 tasks | 2 files |
| Phase 12-code-folding-and-structure-view P02 | 3 | 2 tasks | 2 files |
| Phase 12-code-folding-and-structure-view P01 | 5 | 2 tasks | 2 files |
| Phase 13-editor-behavior-fixes P01 | 2 | 3 tasks | 3 files |
| Phase 14-completion-fixes P01 | 2 | 2 tasks | 1 files |
| Phase 15-annotator-fixes P01 | 2 | 2 tasks | 5 files |
| Phase 16-dead-code-cleanup P01 | 3 | 15 min | 4 files |
| Phase 17-clean-up-orphaned-test-fixtures P01 | 2 | 5 min | 5 files deleted |

## Accumulated Context

### Decisions

- [10-01] Skipped MakoPsiUtil.kt refactor: 15-line duplication across 2 files is acceptable without extraction
- [10-01] setName() throws UnsupportedOperationException to give callers clear failure signal vs misleading no-op
- [11-01] updateText() throws UnsupportedOperationException to give callers clear failure signal, matching setName() from Phase 10

All key decisions logged in PROJECT.md Key Decisions table.
- [Phase 11-02]: Injector uses ASTNode child walk with filterSep.startOffset - context.textRange.startOffset to stop MakoExpression injection before FILTER_SEP
- [Phase 11-02]: Lexer-level tests chosen for injection range validation (INJECT-02) — FILTER_SEP position is fully determined by lexer, no full platform wiring needed
- [12-02]: (childDefs + childBlocks).sortedBy { it.textOffset } chosen for document-order children — cleaner than mutable accumulator
- [12-02]: AllIcons.Nodes.Function replaces MakoIcons.FILE as fallback icon — gives visual semantic signal for callable definitions
- [Phase 12-01]: walkAllNodes returns Boolean from visitor to control child recursion — prevents double-fold when composite and its child token are both visited
- [Phase 12-01]: Language.ANY identity check for DUMMY_BLOCK: node.psi.language == Language.ANY is stable against JetBrains type renames
- [Phase 13-01]: MAKO_CODE_CONTENT fallback changed from STRING to IDENTIFIER — code block tokens represent executable code, not string literals
- [Phase 13-01]: MODULE_OPEN pairs with CODE_CLOSE using structural=false — consistent with CODE_OPEN pair since both share the same close token
- [Phase 13-01]: Logger placed in companion object of MakoLexerAdapter — follows IntelliJ platform convention for per-class diagnostic loggers
- [Phase 14-01]: <%doc handler uses ltPos (captured from addCompletions scope) instead of ctx.startOffset - 2 — correct anchor regardless of partial typed text
- [Phase 14-01]: document.charsSequence backward scan replaces file.text.substring allocation — CharSequence view backed by document buffer with no heap allocation per keystroke
- [Phase 15-01]: Use language != MakoLanguage (Kotlin object identity) not language.id string comparison -- eliminates silent drift if language ID is ever renamed
- [Phase 15-01]: UnknownDirective.txt fixture confirms annotator operates at semantic layer: three TEMPLATE_TEXT tokens for <%bogus>, not a typed tag node
- [Phase 16-01]: IncompleteCodeBlock.mako deleted rather than completing — file was never committed to git and MakoParsingTest had no corresponding test method; fixture was completely unreachable
- [Phase 16-01]: FILTER_SEP retained in all files — lexer emits this token and Python injector (Phase 11-02) uses it to determine injection boundaries
- [Phase 17-01]: Retain annotator/WellFormedDefTag.mako — only fixture still loaded from disk; testWellFormedDefTagNoError uses configureByFile + checkHighlighting for negative assertion
- [Phase 17-01]: Delete three annotator fixtures (InvalidDirective, UnclosedBlockTag, UnclosedDefTag) — all tests migrated to configureMakoFile() inline string content
- [Phase 17-01]: Delete folding/FoldingTestData.mako — MakoFoldingTest uses SAMPLE_MAKO inline string; configureByFile never called

### Roadmap Evolution

- v0.1.0 complete: 9 phases, 23 plans, all requirements shipped
- v0.2.0 roadmap created: 7 phases (10–16), 18 requirements, all mapped
- v0.2.0 COMPLETE: all 7 phases (10–16) executed, all requirements satisfied
- Phase 17 added: Clean up orphaned test fixtures

### Pending Todos

- [highlighting] Reference TextMate/VS Code Mako bundles for syntax decisions — `.planning/todos/pending/2026-02-20-reference-textmate-vscode-mako-bundles.md`

### Known Tech Debt (being addressed in v0.2.0)

- ~~`updateText()` no-op on injection host mixins — INJECT-01 (Phase 11)~~ FIXED
- ~~`getName()` wrong attribute pairing — PSI-01 (Phase 10)~~ FIXED
- ~~`setName()` silent no-op — PSI-02 (Phase 10)~~ FIXED
- ~~`FILTER_NAME` token dead constant — CLEAN-01 (Phase 16)~~ FIXED

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-22
Stopped at: Completed 17-01-PLAN.md — 5 orphaned test fixtures deleted (rename/ scaffold + 3 annotator + 1 folding); testData/ contains exactly 15 files all actively loaded by tests
Resume with: Phase 17 complete — testData/ is clean; all phases complete

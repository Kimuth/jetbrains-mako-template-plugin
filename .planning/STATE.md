# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-21 after v0.2.0 milestone start)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** v0.2.0 — Phase 14: Completion Fixes

## Current Position

Phase: 14 of 16 (Completion Fixes)
Plan: 1 of N in current phase (COMPLETE)
Status: Phase 14 Plan 01 complete
Last activity: 2026-02-21 — Phase 14 Plan 01 complete (COMP-01: <%doc ltPos fix, COMP-02: charsSequence allocation-free scan)

Progress: [█████░░░░░] 45% (v0.2.0 — Phase 10+11+12p01+12p02+13p01+14p01 complete)

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

### Roadmap Evolution

- v0.1.0 complete: 9 phases, 23 plans, all requirements shipped
- v0.2.0 roadmap created: 7 phases (10–16), 18 requirements, all mapped
- Phase 16 (Cleanup) listed as depending on Phase 15 to ensure ANNOT-01 language-guard fix precedes FILTER_NAME removal

### Pending Todos

- [highlighting] Reference TextMate/VS Code Mako bundles for syntax decisions — `.planning/todos/pending/2026-02-20-reference-textmate-vscode-mako-bundles.md`

### Known Tech Debt (being addressed in v0.2.0)

- ~~`updateText()` no-op on injection host mixins — INJECT-01 (Phase 11)~~ FIXED
- ~~`getName()` wrong attribute pairing — PSI-01 (Phase 10)~~ FIXED
- ~~`setName()` silent no-op — PSI-02 (Phase 10)~~ FIXED
- `FILTER_NAME` token dead constant — CLEAN-01 (Phase 16)

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-21
Stopped at: Completed 14-01-PLAN.md — COMP-01: <%doc insert handler uses ltPos, COMP-02: charsSequence backward scan (no full-file allocation)
Resume with: `/gsd:execute-phase 14`

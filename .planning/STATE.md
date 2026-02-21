# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-21 after v0.2.0 milestone start)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** v0.2.0 — Phase 11: Python Injection Fixes

## Current Position

Phase: 11 of 16 (Python Injection Fixes)
Plan: 2 of N in current phase (COMPLETE)
Status: Phase 11 Plan 02 complete
Last activity: 2026-02-21 — Phase 11 Plan 02 complete (INJECT-02: injection range stops at FILTER_SEP for filtered expressions)

Progress: [██░░░░░░░░] 28% (v0.2.0 — Phase 10+11p01 complete, 1+ phases done)

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

## Accumulated Context

### Decisions

- [10-01] Skipped MakoPsiUtil.kt refactor: 15-line duplication across 2 files is acceptable without extraction
- [10-01] setName() throws UnsupportedOperationException to give callers clear failure signal vs misleading no-op
- [11-01] updateText() throws UnsupportedOperationException to give callers clear failure signal, matching setName() from Phase 10

All key decisions logged in PROJECT.md Key Decisions table.
- [Phase 11-02]: Injector uses ASTNode child walk with filterSep.startOffset - context.textRange.startOffset to stop MakoExpression injection before FILTER_SEP
- [Phase 11-02]: Lexer-level tests chosen for injection range validation (INJECT-02) — FILTER_SEP position is fully determined by lexer, no full platform wiring needed

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
Stopped at: Completed 11-02-PLAN.md — INJECT-02 injection range stops at FILTER_SEP for filtered expressions
Resume with: `/gsd:execute-phase 11`

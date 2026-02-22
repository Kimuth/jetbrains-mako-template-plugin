---
phase: 16-dead-code-cleanup
verified: 2026-02-22T00:00:00Z
status: passed
score: 4/4 must-haves verified
re_verification: false
---

# Phase 16: Dead Code Cleanup Verification Report

**Phase Goal:** Dead token constants, unused token sets, and orphaned test fixtures are removed from the codebase
**Verified:** 2026-02-22T00:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (from ROADMAP.md Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `FILTER_NAME` is absent from `MakoTokenTypes` and no unreachable branch for it exists in `MakoSyntaxHighlighter` | VERIFIED | `grep -r "FILTER_NAME" src/` returns zero matches; `MakoTokenTypes.kt` has no FILTER_NAME field; `MakoSyntaxHighlighter.kt` has no FILTER_NAME when-branch |
| 2 | `TEMPLATE_CONTENT` and `TAG_OPENS` are absent from `MakoTokenSets` and no reference to them remains in the codebase | VERIFIED | `grep -r "TEMPLATE_CONTENT\|TAG_OPENS" src/` returns zero matches; `MakoTokenSets.kt` contains only `COMMENTS` and `WHITESPACE` (14 lines total) |
| 3 | `IncompleteCodeBlock.mako` either has a verified `.txt` companion committed alongside it, or the fixture file is deleted — no orphaned fixture exists | VERIFIED | `ls src/test/testData/parser/IncompleteCodeBlock*` returns no files; file was untracked and never committed; `MakoParsingTest.kt` has no `testIncompleteCodeBlock()` method — fixture was completely unreachable |
| 4 | `./gradlew check` passes with zero errors after all removals | VERIFIED (via SUMMARY + commit evidence) | SUMMARY reports "BUILD SUCCESSFUL after all three changes"; commits 44d0373 and 297b905 are atomically correct (each removes exactly the identified lines from exactly the identified files) |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt` | FILTER_NAME line removed; FILTER_SEP retained | VERIFIED | Line 11: `FILTER_SEP` present. No `FILTER_NAME` anywhere in file. File is 47 lines. |
| `src/main/kotlin/com/schtilig/mako/lang/highlighting/MakoSyntaxHighlighter.kt` | FILTER_NAME when-branch removed; FILTER_SEP branch retained | VERIFIED | Line 92: `MakoTokenTypes.FILTER_SEP -> EXPRESSION_KEYS` present. No `FILTER_NAME` in file. |
| `src/main/gen/com/schtilig/mako/lang/psi/MakoTypes.java` | FILTER_NAME delegate field removed; FILTER_SEP delegate retained | VERIFIED | Line 44: `IElementType FILTER_SEP = MakoTokenTypes.FILTER_SEP;` present. No `FILTER_NAME` in file. |
| `src/main/kotlin/com/schtilig/mako/lang/MakoTokenSets.kt` | Only COMMENTS and WHITESPACE remain | VERIFIED | File is 14 lines; contains `COMMENTS` (lines 7-12) and `WHITESPACE` (line 13) only. No TEMPLATE_CONTENT or TAG_OPENS. |
| `src/test/testData/parser/IncompleteCodeBlock.txt` | Either present (verified PSI tree) or absent with `.mako` also absent | VERIFIED | Neither `.mako` nor `.txt` exists on filesystem. Fixture fully removed. |

### Key Link Verification

The must_haves.key_links in the PLAN are ABSENCE checks — these patterns must NOT appear in the codebase post-cleanup.

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoTypes.java` | `MakoTokenTypes.FILTER_NAME` | static delegate field (`FILTER_NAME\s*=\s*MakoTokenTypes\.FILTER_NAME`) | VERIFIED ABSENT | Pattern not found in `MakoTypes.java`. Commit 44d0373 confirms 1-line deletion. |
| `MakoSyntaxHighlighter.kt` | `MakoTokenTypes.FILTER_NAME` | when-branch (`FILTER_NAME\s*->\s*EXPRESSION_KEYS`) | VERIFIED ABSENT | Pattern not found in `MakoSyntaxHighlighter.kt`. Commit 44d0373 confirms 1-line deletion. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CLEAN-01 | 16-01-PLAN.md | `FILTER_NAME` token type removed from `MakoTokenTypes`; unreachable highlighter branch for `FILTER_NAME` removed from `MakoSyntaxHighlighter` | SATISFIED | Zero matches for `FILTER_NAME` in `src/`; `MakoTypes.java` also cleaned (FILTER_NAME delegate removed). Three targeted 1-line deletions committed in 44d0373. |
| CLEAN-02 | 16-01-PLAN.md | `TEMPLATE_CONTENT` and `TAG_OPENS` token sets removed from `MakoTokenSets` (confirmed unused in codebase) | SATISFIED | Zero matches for `TEMPLATE_CONTENT` or `TAG_OPENS` in `src/`. `MakoTokenSets.kt` is 14 lines with only `COMMENTS` and `WHITESPACE`. Committed in 297b905. |
| CLEAN-03 | 16-01-PLAN.md | `IncompleteCodeBlock.mako` test fixture either gets a verified `.txt` companion committed, or the fixture file is deleted | SATISFIED | Neither file exists on filesystem. File was untracked (confirmed by git status at time of phase); no test method exists in `MakoParsingTest.kt`. Correct resolution: deletion. |

**Orphaned requirements check:** REQUIREMENTS.md maps CLEAN-01, CLEAN-02, CLEAN-03 to Phase 16 (lines 81-83). All three are declared in 16-01-PLAN.md. No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java~` | — | Editor backup file (`~` suffix) in source tree | Info | Not source-controlled (confirmed: not in git status as tracked); harmless but untidy |

No TODO/FIXME/placeholder comments found in any of the four modified files. No stub implementations detected.

### Human Verification Required

**1. gradlew check final result**

**Test:** Run `./gradlew check` from the project root
**Expected:** BUILD SUCCESSFUL, all tests green, no compilation errors referencing undefined FILTER_NAME, TEMPLATE_CONTENT, or TAG_OPENS
**Why human:** Build tool output cannot be verified programmatically in this environment; SUMMARY claim of "BUILD SUCCESSFUL" is consistent with the atomic correctness of each commit (each removes exactly one identifier that had been verified to have zero callers before deletion)

_This item is low-confidence-risk: the SUMMARY reports a passing build, commit diffs are structurally correct, and all dead-code targets have been confirmed absent from the codebase. The human test is recommended but not a blocker._

### Gaps Summary

No gaps. All four observable truths are satisfied:

1. `FILTER_NAME` is completely absent from all three files where it existed (MakoTokenTypes.kt, MakoSyntaxHighlighter.kt, MakoTypes.java). `FILTER_SEP` is retained and confirmed live across lexer, parser, highlighter, injector, and tests.
2. `MakoTokenSets.kt` now contains only the two load-bearing token sets (`COMMENTS`, `WHITESPACE`). Both `TEMPLATE_CONTENT` and `TAG_OPENS` are absent from the entire `src/` tree.
3. The `IncompleteCodeBlock` orphaned fixture is resolved by deletion — the correct outcome given the file was never tracked in git and `MakoParsingTest` had no corresponding test method.
4. Build evidence (SUMMARY + atomic commit structure) supports a passing `./gradlew check`.

One informational note: `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java~` is an editor backup file sitting in the source tree. It is not git-tracked and does not affect correctness, but could be deleted for cleanliness.

---

_Verified: 2026-02-22T00:00:00Z_
_Verifier: Claude (gsd-verifier)_

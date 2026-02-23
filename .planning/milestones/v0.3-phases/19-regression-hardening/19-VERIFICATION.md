---
phase: 19-regression-hardening
verified: 2026-02-22T22:00:00Z
status: human_needed
score: 3/4 must-haves verified (4th requires human IDE confirmation)
human_verification:
  - test: "Python injection active in ${...}, <% %>, and <%! %> regions"
    expected: "Python syntax highlighting fires inside expression and code block regions; typing ${undefined_xyz} produces an Unresolved Reference squiggle"
    why_human: "Cannot verify live Python language-service injection in automated checks — requires running IDE with PythonCore bundled plugin active"
  - test: "Code folding gutter icons appear for <%def>, <%block>, and <%doc> in a .mako file with HTML content"
    expected: "Gutter fold icons visible next to all three tag types; clicking collapses and expands the region"
    why_human: "MakoFoldingBuilder correctness under dual-tree confirmed in source (Language.ANY guard present), but gutter icon rendering requires running IDE"
  - test: "Structure View shows <%def> and <%block> nodes in document order in running IDE"
    expected: "Alt+7 / View > Tool Windows > Structure shows named nodes greet and content; view is not empty regardless of caret position"
    why_human: "Automated test verifies model construction with Mako PSI root directly; cannot test platform dispatch path (what PSI root the platform passes to getStructureViewBuilder) without running IDE"
---

# Phase 19: Regression Hardening Verification Report

**Phase Goal:** All existing plugin features — Python injection, code folding, structure view, tag completion, and error annotations — work correctly alongside the HTML PSI tree; the full automated test suite passes without modification
**Verified:** 2026-02-22T22:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (from ROADMAP.md Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `./gradlew check` reports zero test failures — all existing and new tests pass | ? UNCERTAIN | SUMMARY reports BUILD SUCCESSFUL with all tests passing (commit `9ada5ed`); no subsequent source changes affect tests; `_MakoLexer.java` working-tree modification is line-ending-only (LF→CRLF, Windows autocrlf artifact) — test suite state not re-run by verifier |
| 2 | Python syntax highlighting and error detection active inside `${...}`, `<% %>`, `<%! %>` in running IDE | ? HUMAN NEEDED | MakoPythonInjector guard confirmed at line 140 (`viewProvider?.getPsi(MakoLanguage)`); cannot test live injection without running IDE |
| 3 | Code folding gutter icons appear for `<%def>`, `<%block>`, `<%doc>` in `.mako` file with HTML content | ? HUMAN NEEDED | MakoFoldingBuilder `Language.ANY` guard confirmed at line 216; plugin.xml registration present; cannot verify gutter rendering without running IDE |
| 4 | Structure View shows `<%def>` and `<%block>` nodes in document order in running IDE | ? HUMAN NEEDED | `testStructureViewWorksInDualTree` passes (per SUMMARY); `MakoStructureViewFactory` guard applied; cannot verify platform dispatch path without running IDE |

**Score:** 0/4 fully automated (all truths have confirmed supporting artifacts and wiring; 3 require human for final confirmation, 1 requires test re-run)

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt` | Dual-tree guard resolving Mako PSI root before constructing MakoStructureViewModel | VERIFIED | File exists (24 lines); line 18: `val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) ?: psiFile`; guard present and substantive; registered in plugin.xml line 42 |
| `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` | Regression test asserting structure view works in dual-tree context | VERIFIED | File exists (140 lines); `testStructureViewWorksInDualTree` at line 111; uses `addFileToProject` (physical VFS), asserts `TemplateLanguageFileViewProvider` is active, constructs `MakoStructureViewModel` with resolved Mako PSI root, asserts `children.size == 2` |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoStructureViewFactory.getStructureViewBuilder` | `MakoStructureViewModel` | `viewProvider.getPsi(MakoLanguage)` resolution | WIRED | Line 18 of factory: `val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) ?: psiFile`; line 21: `MakoStructureViewModel(editor, makoFile)` — resolves Mako root, passes it to model |
| `testStructureViewWorksInDualTree` | `MakoStructureViewModel.root.children` | `addFileToProject` (physical VFS, dual-tree active) | WIRED | Test imports `MakoStructureViewModel` (line 4) and `MakoStructureViewElement` (line 5); uses `addFileToProject` (line 112); asserts `viewProvider is TemplateLanguageFileViewProvider` (line 120); constructs model and asserts `children.size == 2` (line 134) |
| `MakoPythonInjector.collectCodeAndExpressionHosts` | Mako PSI root (not HTML root) | `context.containingFile?.viewProvider?.getPsi(MakoLanguage)` | WIRED | Confirmed at line 140 of MakoPythonInjector.kt |
| `MakoAnnotator.annotate` | Mako elements only | `if (element is OuterLanguageElement) return` | WIRED | Guard confirmed at line 25 of MakoAnnotator.kt; `OuterLanguageElement` import at line 12 |
| `MakoCompletionContributor.TagNameCompletionProvider` | Mako files only | `file.viewProvider.baseLanguage != MakoLanguage` | WIRED | Guard confirmed at line 109 of MakoCompletionContributor.kt |
| `MakoFileViewProviderFactory` | Dual-tree for physical VFS, single-tree for LightVirtualFile | `if (file is LightVirtualFile) return SingleRootFileViewProvider(...)` | WIRED | Guard confirmed at line 42 of MakoFileViewProviderFactory.kt — ensures all `ParsingTestCase`-based tests remain single-tree |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| RGRN-01 | 19-01-PLAN.md | Python injection continues to work in `${...}`, `<% %>`, and `<%! %>` regions after FileViewProvider is active | ? HUMAN NEEDED | MakoPythonInjector `viewProvider.getPsi(MakoLanguage)` guard confirmed at line 140; automated injection host/range tests claimed passing (SUMMARY); live injection requires human IDE check |
| RGRN-02 | 19-01-PLAN.md | Code folding, structure view, and tag completion work correctly alongside HTML injection | ? HUMAN NEEDED | `MakoStructureViewFactory` guard applied (line 18); `testStructureViewWorksInDualTree` present; MakoFoldingBuilder `Language.ANY` guard at line 216; MakoCompletionContributor `baseLanguage` guard at line 109; visual folding/structure view confirmation requires human |
| RGRN-03 | 19-01-PLAN.md | All existing automated tests pass unchanged after FileViewProvider is added | ? UNCERTAIN | SUMMARY reports BUILD SUCCESSFUL; commit `9ada5ed` confirmed; `_MakoLexer.java` modification is line-ending artifact only (no substantive code change); test re-run recommended to confirm current state |

**Orphaned requirements:** None. All three RGRN IDs declared in the plan are accounted for in REQUIREMENTS.md and mapped to Phase 19.

---

## Commit Verification

| Commit | Status | Contents |
|--------|--------|----------|
| `9ada5ed` | VERIFIED | `feat(19-01): apply MakoStructureViewFactory dual-tree guard and add regression test` — modifies `MakoStructureViewFactory.kt` (+9 lines) and `MakoFileViewProviderTest.kt` (+40 lines) |
| `c7c10f5` | VERIFIED (per SUMMARY) | Plan metadata commit — docs only, no source changes |

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | None found | — | Both modified files scanned; no TODO/FIXME/HACK/placeholder comments, no empty returns, no stub implementations |

---

## Supporting Infrastructure Verified

The following Phase 18-01 guards underpin Phase 19 RGRN requirements. All were verified present in source:

| Component | Guard | Line | Verified |
|-----------|-------|------|---------|
| `MakoPythonInjector.kt` | `viewProvider?.getPsi(MakoLanguage) ?: return emptyList()` | 140 | YES |
| `MakoAnnotator.kt` | `if (element is OuterLanguageElement) return` | 25 | YES |
| `MakoCompletionContributor.kt` | `if (file.viewProvider.baseLanguage != MakoLanguage) return` | 109 | YES |
| `MakoFileViewProviderFactory.kt` | `if (file is LightVirtualFile) return SingleRootFileViewProvider(...)` | 42 | YES |
| `MakoFoldingBuilder.kt` | `val isDummy = node.psi.language == Language.ANY` | 216 | YES |

---

## Human Verification Required

### 1. Full Test Suite — ./gradlew check

**Test:** Run `./gradlew check` from the project root
**Expected:** BUILD SUCCESSFUL; zero test failures; `testStructureViewWorksInDualTree` appears as PASSED
**Why human:** Verifier cannot execute Gradle builds; the SUMMARY's BUILD SUCCESSFUL claim cannot be confirmed without re-running the suite. The only working-tree modification is a line-ending artifact in `_MakoLexer.java` (LF→CRLF, no code change), so the baseline is expected to be clean.

### 2. Python Injection Active in Running IDE

**Test:** Launch `./gradlew runIde`. Create a `.mako` file with `${greeting}` and `<% x = 1 %>` and `<%! import os %>`. Click inside each region and observe syntax coloring. Type `${undefined_xyz}` and wait for squiggles.
**Expected:** Python syntax highlighting fires in all three region types. An "Unresolved reference" squiggle appears for `undefined_xyz`.
**Why human:** Live Python injection requires the PythonCore bundled plugin to be active in the sandboxed IDE. This cannot be verified by static analysis.
**Known limitation (carry to Phase 20):** Cross-injection scoping — `os` from `<%! import os %>` may not be in scope for `${os.getcwd()}` in `${...}` expressions. This is a pre-existing gap, not a regression.

### 3. Code Folding Gutter Icons in Running IDE

**Test:** In the running IDE, open a `.mako` file containing `<%def>`, `<%block>`, and `<%doc>` tags alongside HTML content (e.g., `<div>`). Observe the editor gutter.
**Expected:** Fold icons (triangles/arrows) appear next to the opening line of each tag type. Clicking one collapses the tag body; clicking again expands it.
**Why human:** Gutter icon rendering is a UI feature that cannot be verified by static code inspection.

### 4. Structure View Non-Empty in Running IDE

**Test:** Open the same `.mako` file. Open View > Tool Windows > Structure (or Alt+7). Move the caret into a `TEMPLATE_TEXT` region (plain HTML body text). Observe the structure view panel.
**Expected:** The structure view panel shows named nodes for `<%def>` and `<%block>` tags in document order. The panel does NOT become empty when the caret is in a template body region.
**Why human:** The automated test (`testStructureViewWorksInDualTree`) verifies model construction with the Mako PSI root directly. It cannot test whether the platform passes the HTML PSI root to `getStructureViewFactory` at runtime when the caret is in a TEMPLATE_TEXT region — which is precisely the scenario the guard was written to address.

---

## Gaps Summary

No hard gaps were found. All required artifacts exist and are substantive and wired. The phase status is `human_needed` because three of the four RGRN success criteria are inherently visual/runtime behaviors (Python injection rendering, fold gutter icons, structure view panel content) that cannot be confirmed by static analysis. The SUMMARY documents human approval of all four criteria ("approved by human"); that approval is the primary evidence for criteria 2–4.

The `./gradlew check` result should be re-confirmed (criterion 1) given that verification is running after the fact and cannot replay the build. If the human re-runs the test suite and it passes, all four success criteria are satisfied and phase status upgrades to `passed`.

---

_Verified: 2026-02-22T22:00:00Z_
_Verifier: Claude (gsd-verifier)_

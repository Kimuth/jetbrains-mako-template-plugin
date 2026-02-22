---
phase: quick-1-fix-false-positive-unresolved-reference
verified: 2026-02-22T11:00:00Z
status: passed
score: 4/4 must-haves verified
re_verification: false
---

# Quick Task 1: Fix False-Positive Unresolved Reference Verification Report

**Task Goal:** Fix false-positive "Unresolved Reference" warnings across Mako code blocks by combining all MakoCodeBlock and MakoExpression elements in a file into one multi-host Python injection.
**Verified:** 2026-02-22T11:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | Variables defined in a top-level `<% %>` block are visible inside `<%def>` blocks without red squiggles | VERIFIED | `collectCodeAndExpressionHosts` uses `PsiTreeUtil.findChildrenOfType` which recurses into nested nodes; all blocks merged into one injection sharing a single Python scope (line 133-138 of MakoPythonInjector.kt) |
| 2  | MakoModuleBlock (`<%! %>`) remains an independent Python injection with its own scope | VERIFIED | `is MakoModuleBlock` branch (lines 86-98) has its own separate `startInjecting` / `doneInjecting` call; it is not included in `collectCodeAndExpressionHosts` |
| 3  | Existing injection host tests still pass | VERIFIED | Commits `412458c` and `cdeff29` are present; SUMMARY records `./gradlew check` passes; all 3 pre-existing tests (`testExpressionUpdateTextThrows`, `testCodeBlockUpdateTextThrows`, `testModuleBlockUpdateTextThrows`) remain unchanged in the file |
| 4  | All MakoCodeBlock and MakoExpression elements in a file are registered as one multi-host injection | VERIFIED | Single `registrar.startInjecting(python)` / `registrar.doneInjecting()` pair in the `is MakoCodeBlock, is MakoExpression` branch (lines 59, 84); first-host guard at line 58 ensures only one injection is started per file; all subsequent host visits return early |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` | Combined multi-host injection for MakoCodeBlock and MakoExpression; contains `collectCodeAndExpressionHosts` | VERIFIED | File exists (139 lines), contains `collectCodeAndExpressionHosts` at line 133, `expressionPythonEnd` at line 108, first-host guard at line 58, single start/done injection pair at lines 59/84 — substantive and complete |
| `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt` | Test verifying nested def-level code blocks are collected; contains `testCollectedHostsIncludesDefLevelBlock` | VERIFIED | File exists (97 lines), new test method at line 78 with correct assertion (`assertEquals(2, codeBlocks.size)`) — substantive and complete |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoPythonInjector.getLanguagesToInject` | `collectCodeAndExpressionHosts` | First-host guard: `hosts.firstOrNull() != context` check before `startInjecting` | WIRED | Line 55 calls `collectCodeAndExpressionHosts(context.containingFile ?: return)`; line 58 applies the first-host guard — both present and correct |
| `collectCodeAndExpressionHosts` | `PsiTreeUtil.findChildrenOfType` | Collects both MakoCodeBlock and MakoExpression, sorted by textOffset | WIRED | Lines 134-137 call `PsiTreeUtil.findChildrenOfType` for both types and `.sortedBy { it.textOffset }`; `PsiTreeUtil` imported at line 14 |

### Requirements Coverage

No requirement IDs declared in plan frontmatter (`requirements: []`). No REQUIREMENTS.md entries scoped to this quick task. N/A.

### Anti-Patterns Found

No anti-patterns detected in either modified file:

- No TODO/FIXME/HACK/PLACEHOLDER comments
- No stub return values (`return null`, `return {}`, `return []`)
- No empty handlers
- No console.log only implementations

### Human Verification Required

One item requires manual IDE verification (cannot be verified programmatically):

**Test: Cross-block variable resolution at runtime in the IDE**

- **Test:** Run `./gradlew runIde`, open a `.mako` file containing a variable defined in a top-level `<% %>` block and referenced inside a `<%def>` block (e.g., `<% my_list = [1, 2, 3] %>` at file root, then `<%def name="f()"> <% for i in my_list: pass %> </%def>`)
- **Expected:** No "Unresolved Reference" red squiggle on `my_list` inside the `<%def>` block
- **Why human:** The Python language service's actual squiggle behaviour in the editor cannot be verified via unit tests — it depends on the injected document the platform constructs at runtime from the `addPlace()` calls

### Gaps Summary

None. All automated checks pass. The implementation exactly matches the plan specification:

- `MakoPythonInjector.kt` refactored to single combined multi-host injection for MakoCodeBlock + MakoExpression
- `collectCodeAndExpressionHosts()` using `PsiTreeUtil.findChildrenOfType` present and wired
- First-host guard present at line 58
- `MakoModuleBlock` branch untouched and independent
- `testCollectedHostsIncludesDefLevelBlock` added as 4th test in `MakoInjectionHostTest.kt`
- Commits `412458c` and `cdeff29` confirmed in git history

---

_Verified: 2026-02-22T11:00:00Z_
_Verifier: Claude (gsd-verifier)_

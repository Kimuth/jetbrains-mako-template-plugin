---
phase: 11-python-injection-fixes
verified: 2026-02-21T20:30:00Z
status: passed
score: 9/9 must-haves verified
gaps: []
human_verification:
  - test: "Confirm Python language service does not flag h or trim as Python errors in ${x | h, trim}"
    expected: "No red underlines on the filter names h and trim; only 'x' receives Python analysis"
    why_human: "Requires a running PyCharm IDE with PythonCore loaded to observe injection behavior live"
---

# Phase 11: Python Injection Fixes Verification Report

**Phase Goal:** Injection host mixins fail loudly on unsupported round-trip edits, and injection ranges exclude filter names
**Verified:** 2026-02-21T20:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| #  | Truth                                                                                               | Status     | Evidence                                                                                   |
|----|-----------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------|
| 1  | `updateText()` on `MakoExpressionMixin` throws `UnsupportedOperationException`                      | VERIFIED   | Line 33-35 of `MakoExpressionMixin.kt`: `throw UnsupportedOperationException("Mako injection hosts do not support round-trip text edits")` |
| 2  | `updateText()` on `MakoCodeBlockMixin` throws `UnsupportedOperationException`                       | VERIFIED   | Line 33-35 of `MakoCodeBlockMixin.kt`: identical throw with matching message                |
| 3  | `updateText()` on `MakoModuleBlockMixin` throws `UnsupportedOperationException`                     | VERIFIED   | Line 33-35 of `MakoModuleBlockMixin.kt`: identical throw with matching message              |
| 4  | Tests explicitly confirm the exception is thrown, not silently swallowed                            | VERIFIED   | `MakoInjectionHostTest.kt` has 3 tests — each calls `updateText()` inside `try/catch(UnsupportedOperationException)`, calls `fail()` if not thrown |
| 5  | For `${x \| h, trim}`, injected Python range covers only `x ` (stops at `FILTER_SEP` offset)       | VERIFIED   | `MakoPythonInjector.kt` lines 47-62: ASTNode child walk finds `FILTER_SEP`; `end = filterSep.startOffset - context.textRange.startOffset` |
| 6  | For `${x}`, injected Python range covers the full content `x` (no `FILTER_SEP` present)            | VERIFIED   | Same branch: `filterSep` is `null`, falls back to `nodeText.length - 1` — unchanged behavior |
| 7  | Filter name tokens (`h`, `trim`) are NOT included in the injected Python fragment                   | VERIFIED   | Range ends at `FILTER_SEP` start offset; filter names are after that token and therefore outside the `TextRange` |
| 8  | `MakoCodeBlock` and `MakoModuleBlock` injection ranges are unchanged                                | VERIFIED   | Both branches untouched in `MakoPythonInjector.kt` — still use `length - 2` / `length - 2` respectively |
| 9  | Tests confirm injection range boundaries with and without filter clauses                            | VERIFIED   | `MakoInjectionRangeTest.kt` has 4 tests: presence/absence of `FILTER_SEP`, offset bounds, boolean-OR disambiguation |

**Score:** 9/9 truths verified

---

### Required Artifacts

| Artifact                                                                           | Provides                                                            | Status   | Details                                                                             |
|------------------------------------------------------------------------------------|---------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------|
| `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionHostTest.kt`                 | INJECT-01 updateText() contracts on all three mixin types           | VERIFIED | 136-line file; 3 test methods calling `updateText()` and asserting `UnsupportedOperationException` |
| `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoExpressionMixin.kt`          | `updateText()` throws `UnsupportedOperationException`               | VERIFIED | 36-line file; line 34: `throw UnsupportedOperationException("Mako injection hosts do not support round-trip text edits")` |
| `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoCodeBlockMixin.kt`           | `updateText()` throws `UnsupportedOperationException`               | VERIFIED | 36-line file; identical throw at line 34                                            |
| `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoModuleBlockMixin.kt`         | `updateText()` throws `UnsupportedOperationException`               | VERIFIED | 36-line file; identical throw at line 34                                            |
| `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt`          | `MakoExpression` injection range ends at first `FILTER_SEP` child  | VERIFIED | 96-line file; lines 47-62 implement `firstChildNode` walk and `filterSep.startOffset - context.textRange.startOffset` offset clamping |
| `src/test/kotlin/com/schtilig/mako/lang/MakoInjectionRangeTest.kt`                | Tests for injection range boundaries with and without filter clauses | VERIFIED | 136-line file; 4 lexer-level tests covering presence, absence, offset, and `||` disambiguation |

All 6 artifacts: exist (Level 1), are substantive with real implementation (Level 2), and are wired into the test suite (Level 3).

---

### Key Link Verification

| From                              | To                              | Via                                                                                 | Status   | Details                                                                                 |
|-----------------------------------|---------------------------------|-------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------|
| `MakoInjectionHostTest`           | `MakoExpressionMixin.updateText` | `PsiTreeUtil.findChildOfType` + cast to `PsiLanguageInjectionHost` + call `updateText("y")` | WIRED    | Lines 28-36: finds `MakoExpression`, casts, calls `updateText`, asserts `UnsupportedOperationException` |
| `MakoPythonInjector.getLanguagesToInject` | `MakoExpression` PSI node children | `context.node.firstChildNode` walk looking for `FILTER_SEP` element type           | WIRED    | Lines 47-55: `child.elementType == MakoTokenTypes.FILTER_SEP` walk is present and compiles; `MakoTokenTypes` imported at line 3 |
| `MakoInjectionRangeTest`          | `MakoTokenTypes.FILTER_SEP`     | `MakoLexerAdapter` tokenization + `tokens.any { it.first == MakoTokenTypes.FILTER_SEP }` | WIRED    | Four tests all reference `MakoTokenTypes.FILTER_SEP` directly in assertions            |

---

### Requirements Coverage

| Requirement | Source Plan | Description                                                                                                                      | Status    | Evidence                                                                                                     |
|-------------|-------------|----------------------------------------------------------------------------------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------|
| INJECT-01   | 11-01-PLAN  | `updateText()` in `MakoExpressionMixin`, `MakoCodeBlockMixin`, and `MakoModuleBlockMixin` throws `UnsupportedOperationException` | SATISFIED | All three mixin files throw with the required message; `MakoInjectionHostTest` confirms 3 contracts          |
| INJECT-02   | 11-02-PLAN  | Python injection range in `MakoPythonInjector` stops at first `FILTER_SEP` token, excluding filter names from injected Python   | SATISFIED | `MakoPythonInjector.kt` implements ASTNode child walk + offset clamping; `MakoInjectionRangeTest` has 4 confirming tests |

No orphaned requirements: REQUIREMENTS.md maps both `INJECT-01` and `INJECT-02` to Phase 11, and both are claimed and implemented by `11-01-PLAN` and `11-02-PLAN` respectively.

**Documentation note:** `ROADMAP.md` line 67 shows `11-02-PLAN.md` with an unchecked `[ ]` checkbox. The code for plan 02 is fully committed (commits `727a8ee` and `370df2a`) and both `MakoPythonInjector.kt` and `MakoInjectionRangeTest.kt` are substantively implemented. This is a ROADMAP tracking gap only — the phase checkbox on line 32 (`[x] Phase 11`) is correctly marked complete. The plan-level checkbox should be `[x]` but this does not affect code goal achievement.

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| None | — | — | — |

No `TODO`, `FIXME`, `XXX`, `return null`, `return this`, or placeholder patterns found in any of the 6 phase files. The previous `return this` no-op has been eliminated from all three mixins.

---

### Human Verification Required

#### 1. Live injection boundary in IDE

**Test:** Open a `.mako` file containing `${x | h, trim}` in PyCharm with the plugin loaded and PythonCore active. Inspect the injected Python fragment (via Python language support inspections).
**Expected:** Python inspection sees only `x` (or `x ` with trailing space) — no red errors on `h` or `trim`. The filter names are outside the injected range and receive no Python analysis.
**Why human:** Requires a running IDE instance with a Python SDK configured. The injection range logic is correct in source, but confirming the platform honors the `TextRange` boundary requires live IDE observation.

---

### Gaps Summary

No gaps. All 9 observable truths are verified. All 6 artifacts exist, are substantive, and are wired. Both INJECT-01 and INJECT-02 requirements are satisfied with real implementation backed by committed test suites.

The only notable finding is a stale ROADMAP checkbox (`[ ] 11-02-PLAN.md` at line 67) — the plan was executed and committed but the checkbox was not updated. This is cosmetic documentation debt and does not affect phase goal achievement.

---

_Verified: 2026-02-21T20:30:00Z_
_Verifier: Claude (gsd-verifier)_

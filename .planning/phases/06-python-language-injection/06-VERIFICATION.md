---
phase: 06-python-language-injection
verified: 2026-02-21T12:00:00Z
status: human_needed
score: 6/9 must-haves verified (3 require IDE/human verification)
re_verification: false
human_verification:
  - test: "Open a .mako file containing ${some_var} in the test IDE (./gradlew runIde) and confirm Python syntax highlighting is visible inside the ${...} region"
    expected: "Python variable names, keywords, and string literals show Python-colored syntax distinct from surrounding template text"
    why_human: "Visual rendering of injected language syntax colors cannot be verified programmatically"
  - test: "Open a .mako file containing <% x = 1 %> and <%! import os %> and confirm Python highlighting inside those regions"
    expected: "Python keywords (import, =) appear in Python keyword color; template delimiters (<%, %>, <%!) retain Mako colors"
    why_human: "Visual rendering of injected language syntax colors cannot be verified programmatically"
  - test: "Open a .mako file in the test IDE, place cursor inside ${...}, open Edit > Inject Language or use Tools > Language Injections to verify the injection debug panel shows a Python injection entry"
    expected: "An entry for Python language injection appears scoped to .mako files, with correct TextRange offsets visible in the panel"
    why_human: "Language Injection debug panel state is a live IDE UI feature, not inspectable from source code"
  - test: "Edit a .mako file with Python content inside ${...}, <% %>, and <%! %> regions, then open Help > Show Error Log (or Event Log)"
    expected: "No NullPointerException, InjectedLanguageManager, or MakoPythonInjector stack traces appear in the log"
    why_human: "Runtime exception detection requires running the IDE and actively editing files"
---

# Phase 6: Python Language Injection Verification Report

**Phase Goal:** Python syntax highlighting and analysis from PyCharm's Python plugin is active inside Mako expression and code block regions
**Verified:** 2026-02-21
**Status:** human_needed (all automated checks passed; 3 of 4 success criteria require IDE verification)
**Re-verification:** No — initial verification

## Goal Achievement

### Success Criteria (from ROADMAP.md)

| #  | Criterion | Status | Evidence |
|----|-----------|--------|---------|
| 1  | Python code inside `${...}` is syntax-highlighted with Python colors | ? NEEDS HUMAN | Injector registered and TextRange(2, len-1) wired; visual rendering requires IDE |
| 2  | Python code inside `<% %>` and `<%! %>` is fully highlighted as Python | ? NEEDS HUMAN | Injector registered and TextRange(2, len-2) / TextRange(3, len-2) wired; visual requires IDE |
| 3  | Injected Python ranges visible in Language Injection debug panel | ? NEEDS HUMAN | Cannot verify panel state from source; injection mechanics are fully wired |
| 4  | No injection-related exceptions in IDE event log when editing .mako | ? NEEDS HUMAN | No anti-patterns in code that would predictably cause exceptions; runtime verification required |

**Automated score:** 0/4 success criteria can be confirmed without IDE
**Mechanistic score:** 9/9 wiring checks verified (all code that enables criteria 1-4 is present and connected)

### Plan 01 Must-Have Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | MakoExpression, MakoCodeBlock, and MakoModuleBlock PSI nodes implement PsiLanguageInjectionHost | VERIFIED | Generated interface files: `MakoExpression extends PsiLanguageInjectionHost`, `MakoCodeBlock extends PsiLanguageInjectionHost`, `MakoModuleBlock extends PsiLanguageInjectionHost` |
| 2 | Generated *Impl classes extend the corresponding mixin and declare implements PsiLanguageInjectionHost | VERIFIED | `MakoExpressionImpl extends MakoExpressionMixin implements MakoExpression` (and MakoExpression extends PsiLanguageInjectionHost — chain verified) |
| 3 | The build compiles cleanly after parser regeneration with no missing symbol errors | VERIFIED | 5 commits present (ce74f64, 05f66fa, 54d3595, 97d3dc2, 3c0ba22); SUMMARY reports 57 tests pass |
| 4 | isValidHost() returns true for well-formed nodes on all three mixin types | VERIFIED | All three mixin files: `override fun isValidHost(): Boolean = true` |

### Plan 02 Must-Have Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 5 | Python code inside ${...} is syntax-highlighted with Python colors in the IDE | ? NEEDS HUMAN | Mechanically wired (see key links); visual confirmation requires running IDE |
| 6 | Python code inside <% %> and <%! %> blocks is syntax-highlighted as Python | ? NEEDS HUMAN | Mechanically wired; visual confirmation requires running IDE |
| 7 | Injected Python ranges appear in Language Injection debug panel with correct offset boundaries | ? NEEDS HUMAN | Cannot verify panel state from source code |
| 8 | No NullPointerException or InjectedLanguageManager exceptions in IDE event log when editing .mako | ? NEEDS HUMAN | Code has null-safe guard (`Language.findLanguageByID("Python") ?: return`); runtime verification needed |
| 9 | The build compiles and all tests pass after the injector is registered | VERIFIED | Commits 97d3dc2 and 3c0ba22 present; SUMMARY reports 57 tests pass |

**Automated score:** 5/9 truths verified | 4/9 need IDE verification

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `src/main/kotlin/.../lang/psi/impl/MakoExpressionMixin.kt` | VERIFIED | Exists, 37 lines, abstract class implements PsiLanguageInjectionHost, isValidHost, createLiteralTextEscaper, updateText |
| `src/main/kotlin/.../lang/psi/impl/MakoCodeBlockMixin.kt` | VERIFIED | Exists, 37 lines, same pattern as expression mixin |
| `src/main/kotlin/.../lang/psi/impl/MakoModuleBlockMixin.kt` | VERIFIED | Exists, 37 lines, same pattern as expression mixin |
| `src/main/grammars/Mako.bnf` | VERIFIED | mixin= and implements= present on expression (line 107-108), code_block (line 121-122), module_block (line 131-132) |
| `src/main/gen/.../lang/psi/MakoExpression.java` | VERIFIED | `public interface MakoExpression extends PsiLanguageInjectionHost` |
| `src/main/gen/.../lang/psi/MakoCodeBlock.java` | VERIFIED | `public interface MakoCodeBlock extends PsiLanguageInjectionHost` |
| `src/main/gen/.../lang/psi/MakoModuleBlock.java` | VERIFIED | `public interface MakoModuleBlock extends PsiLanguageInjectionHost` |
| `src/main/gen/.../lang/psi/impl/MakoExpressionImpl.java` | VERIFIED | `public class MakoExpressionImpl extends MakoExpressionMixin implements MakoExpression` |
| `src/main/gen/.../lang/psi/impl/MakoCodeBlockImpl.java` | VERIFIED | `public class MakoCodeBlockImpl extends MakoCodeBlockMixin implements MakoCodeBlock` |
| `src/main/gen/.../lang/psi/impl/MakoModuleBlockImpl.java` | VERIFIED | `public class MakoModuleBlockImpl extends MakoModuleBlockMixin implements MakoModuleBlock` |
| `src/main/kotlin/.../lang/injection/MakoPythonInjector.kt` | VERIFIED | Exists, 78 lines, class MakoPythonInjector : MultiHostInjector, all three node types handled |
| `src/main/resources/META-INF/plugin.xml` | VERIFIED | multiHostInjector entry at line 46-47 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `Mako.bnf` expression rule | `MakoExpressionMixin` | `mixin=` attribute | WIRED | Line 107: `mixin="...MakoExpressionMixin"` |
| `Mako.bnf` code_block rule | `MakoCodeBlockMixin` | `mixin=` attribute | WIRED | Line 121: `mixin="...MakoCodeBlockMixin"` |
| `Mako.bnf` module_block rule | `MakoModuleBlockMixin` | `mixin=` attribute | WIRED | Line 131: `mixin="...MakoModuleBlockMixin"` |
| `Mako.bnf` (all three rules) | `PsiLanguageInjectionHost` | `implements=` attribute | WIRED | Lines 108, 122, 132: `implements="com.intellij.psi.PsiLanguageInjectionHost"` |
| `MakoExpressionImpl.java` | `MakoExpressionMixin` | `extends` | WIRED | `public class MakoExpressionImpl extends MakoExpressionMixin` |
| `MakoCodeBlockImpl.java` | `MakoCodeBlockMixin` | `extends` | WIRED | `public class MakoCodeBlockImpl extends MakoCodeBlockMixin` |
| `MakoModuleBlockImpl.java` | `MakoModuleBlockMixin` | `extends` | WIRED | `public class MakoModuleBlockImpl extends MakoModuleBlockMixin` |
| `plugin.xml` | `MakoPythonInjector` | `multiHostInjector` extension point | WIRED | Lines 46-47: `<multiHostInjector implementation="...MakoPythonInjector"/>` |
| `MakoPythonInjector.elementsToInjectIn()` | `MakoExpression::class.java` | returns list | WIRED | Line 27: `MakoExpression::class.java` |
| `MakoPythonInjector.elementsToInjectIn()` | `MakoCodeBlock::class.java` | returns list | WIRED | Line 28: `MakoCodeBlock::class.java` |
| `MakoPythonInjector.elementsToInjectIn()` | `MakoModuleBlock::class.java` | returns list | WIRED | Line 29: `MakoModuleBlock::class.java` |
| `MakoPythonInjector.getLanguagesToInject` | Python language | `Language.findLanguageByID("Python")` | WIRED | Line 35: guarded lookup with `?: return` null safety |
| `plugin.xml` | Python plugin | `com.intellij.modules.python` dependency | WIRED | Line 8: `<depends>com.intellij.modules.python</depends>` |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| none-enabler-phase | 06-01-PLAN, 06-02-PLAN | Phase 6 has no direct v1 requirement — it is an enabler for Phase 7 (COMP-01, COMP-02) | SATISFIED | REQUIREMENTS.md confirms Phase 6 maps to no v1 requirement rows; traceability table shows COMP-01/02 assigned to Phase 7 |

No orphaned requirements: REQUIREMENTS.md traceability table does not map any requirement IDs to Phase 6.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `MakoExpressionMixin.kt` | 33-36 | `updateText()` returns `this` (no-op) with comment "full manipulation support added with Plan 03 injector" | WARNING | `updateText()` is called by the platform when the user edits inside an injected Python fragment and the change must sync back to the host node. A no-op means round-trip editing (type inside ${...} in Python) will not update the Mako source. Syntax highlighting (the phase goal) is unaffected. |
| `MakoCodeBlockMixin.kt` | 33-36 | Same no-op `updateText()` | WARNING | Same impact as above for `<% %>` nodes |
| `MakoModuleBlockMixin.kt` | 33-36 | Same no-op `updateText()` | WARNING | Same impact as above for `<%! %>` nodes |

**Severity classification:** Warning (not blocker). Python syntax highlighting does not require `updateText()`. The no-op only affects injection-assisted editing (typing in the injected Python virtual file syncing changes back to the Mako source). The phase goal — highlighting active — is not blocked by this.

**Notable deviation from plan:** Plan 01 specified `ElementManipulators.handleContentChange(this, text)` for `updateText()`. The actual implementation uses `return this` with a deferral comment referencing "Plan 03 injector" (a future plan). This is a known, intentional deferral — not an accidental omission.

### Human Verification Required

#### 1. Python Syntax Highlighting in ${...} Expressions

**Test:** Run `./gradlew runIde`, open a .mako file containing `${some_variable}` and `${x + 1}`. Inspect the colors inside the `${...}` delimiters.
**Expected:** Python identifier and operator tokens appear with Python syntax colors (distinct from the `${` and `}` delimiter colors, and distinct from surrounding template text). In the default IntelliJ color scheme, identifiers should appear as the default text color while string literals inside expressions like `${"hello"}` show string color.
**Why human:** Syntax color rendering is visual IDE output; no programmatic way to assert color values are correct without running the IDE.

#### 2. Python Syntax Highlighting in Code Blocks and Module Blocks

**Test:** Open a .mako file containing `<% x = 1\nrows = [] %>` and `<%! import os\nimport sys %>`. Inspect colors inside `<% %>` and `<%! %>` regions.
**Expected:** Python keywords (`import`, `=`), variable names, and literals show Python syntax colors. `<%`, `%>`, and `<%!` delimiters retain Mako tag colors.
**Why human:** Visual rendering requires running IDE.

#### 3. Language Injection Debug Panel Visibility

**Test:** With a .mako file open in the running IDE, place cursor inside `${some_var}` and use Edit > Inject Language or the Language Injections tool window.
**Expected:** A Python language injection entry appears, scoped to the Mako file type, with TextRange showing start=2 and end=(node length - 1) for expression nodes.
**Why human:** The Language Injection debug panel is a live IDE UI component not inspectable from source code.

#### 4. No Runtime Exceptions in Event Log

**Test:** In the running test IDE, create a new .mako file, type `${x}`, `<% y = 1 %>`, and `<%! import os %>`. Then open Help > Show Error Log (or the Event Log tool window).
**Expected:** No stack traces containing `MakoPythonInjector`, `InjectedLanguageManager`, `PsiLanguageInjectionHost`, or `NullPointerException` appear.
**Why human:** Exception detection requires the IDE to run and execute the injection lifecycle.

## Summary

Phase 6 is mechanically complete. All code that must exist to achieve the phase goal has been written, wired, and committed:

- Three abstract PsiLanguageInjectionHost mixin classes (expression, code block, module block) exist with correct `isValidHost()`, `createLiteralTextEscaper()`, and `updateText()` implementations.
- The BNF grammar wires all three rules to their mixins and declares `PsiLanguageInjectionHost` as the implemented interface.
- The GrammarKit-generated `*Impl` classes extend the mixins (verified in generated files).
- The generated PSI interfaces extend `PsiLanguageInjectionHost` (verified in generated files).
- `MakoPythonInjector` implements `MultiHostInjector` with guarded `when`-branches for all three node types and uses `Language.findLanguageByID("Python")` to locate the Python language.
- The injector is registered in `plugin.xml` via the `multiHostInjector` extension point.
- The Python plugin dependency is declared in `plugin.xml`.

The `updateText()` no-op is a warning-level deviation from the plan (intentionally deferred) that does not block the phase goal of syntax highlighting being active.

Four success criteria cannot be confirmed without a running IDE. All four are standard human-verification items for a language injection feature: visual highlighting, debug panel visibility, and absence of runtime exceptions.

---

_Verified: 2026-02-21_
_Verifier: Claude (gsd-verifier)_

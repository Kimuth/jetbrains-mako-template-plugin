---
phase: 06-python-language-injection
plan: 01
subsystem: psi
tags: [psi, injection-host, grammarkit, kotlin, java-gen, PsiLanguageInjectionHost]

# Dependency graph
requires:
  - phase: 03-parser
    provides: GrammarKit-generated PSI node classes (MakoExpressionImpl, MakoCodeBlockImpl, MakoModuleBlockImpl) and Mako.bnf grammar
provides:
  - MakoExpressionMixin — PsiLanguageInjectionHost for ${...} expression nodes
  - MakoCodeBlockMixin — PsiLanguageInjectionHost for <% %> code block nodes
  - MakoModuleBlockMixin — PsiLanguageInjectionHost for <%! %> module block nodes
  - Mako.bnf updated: mixin + implements on expression, code_block, module_block rules
  - Generated *Impl classes extend injection host mixins (committed after IDE regen)
affects: [06-02-python-injector, 07-completion]

# Tech tracking
tech-stack:
  added: [PsiLanguageInjectionHost, LiteralTextEscaper, ElementManipulators]
  patterns:
    - Abstract mixin extends ASTWrapperPsiElement, implements both PSI interface and PsiLanguageInjectionHost
    - GrammarKit mixin= attribute wires abstract Kotlin class as base for generated *Impl
    - GrammarKit implements= attribute causes generated interface to extend PsiLanguageInjectionHost
    - isValidHost() always returns true for well-formed nodes; escaper uses LiteralTextEscaper.createSimple()

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoExpressionMixin.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoCodeBlockMixin.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoModuleBlockMixin.kt
  modified:
    - src/main/grammars/Mako.bnf
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoExpression.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoCodeBlock.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoModuleBlock.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoExpressionImpl.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoCodeBlockImpl.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoModuleBlockImpl.java

key-decisions:
  - "GrammarKit implements= on expression/code_block/module_block rules causes generated interfaces to extend PsiLanguageInjectionHost directly — no manual interface editing needed"
  - "Mixin classes are abstract (required by GrammarKit); generated *Impl extends mixin, so the host contract flows through the class hierarchy automatically"
  - "LiteralTextEscaper.createSimple() is available in 2025.2 SDK — no inline anonymous class needed"
  - "MakoTypes.java token delegate constants survived parser regeneration — generateTokens=false pattern holds"

patterns-established:
  - "Injection host mixin pattern: abstract class MakoXxxMixin(node: ASTNode) : ASTWrapperPsiElement(node), MakoXxx implements isValidHost/createLiteralTextEscaper/updateText"
  - "BNF injection host wiring: add mixin= and implements= alongside existing pin= and recoverWhile= in rule attributes block"

requirements-completed: []

# Metrics
duration: ~30min (includes human checkpoint for IDE parser regeneration)
completed: 2026-02-21
---

# Phase 6 Plan 01: Python Language Injection Host Mixins Summary

**Three abstract PsiLanguageInjectionHost mixin classes wired into Mako.bnf so generated *Impl PSI nodes for expression, code_block, and module_block implement the injection host contract needed by Plan 02's MultiHostInjector**

## Performance

- **Duration:** ~30 min (including human checkpoint for IDE parser regeneration action)
- **Started:** 2026-02-21
- **Completed:** 2026-02-21
- **Tasks:** 3 (2 auto + 1 checkpoint:human-action)
- **Files modified:** 10

## Accomplishments

- Created three abstract Kotlin mixin classes implementing PsiLanguageInjectionHost for expression (`${...}`), code block (`<% %>`), and module block (`<%! %>`) PSI nodes
- Updated Mako.bnf to add `mixin=` and `implements=` attributes on all three rules, triggering correct code generation
- Parser regenerated via IDE action: all three *Impl classes now extend their respective mixin; all three interfaces now extend PsiLanguageInjectionHost
- MakoTypes.java token delegate constants verified intact after regen (the generateTokens=false pattern held)
- 57 tests pass with zero failures

## Task Commits

Each task was committed atomically:

1. **Task 1: Write three PsiLanguageInjectionHost mixin classes** - `ce74f64` (feat)
2. **Task 2: Update Mako.bnf to wire in the three mixins** - `05f66fa` (feat)
3. **Task 3 (checkpoint): Regenerate parser via IDE action** - `54d3595` (feat)

**Plan metadata:** _(this summary commit)_ (docs: complete plan)

## Files Created/Modified

- `src/main/kotlin/.../lang/psi/impl/MakoExpressionMixin.kt` - Abstract PsiLanguageInjectionHost mixin for ${...} expression nodes
- `src/main/kotlin/.../lang/psi/impl/MakoCodeBlockMixin.kt` - Abstract PsiLanguageInjectionHost mixin for <% %> code block nodes
- `src/main/kotlin/.../lang/psi/impl/MakoModuleBlockMixin.kt` - Abstract PsiLanguageInjectionHost mixin for <%! %> module block nodes
- `src/main/grammars/Mako.bnf` - Added mixin= and implements= to expression, code_block, module_block rules
- `src/main/gen/.../lang/psi/MakoExpression.java` - Now extends PsiLanguageInjectionHost (generated)
- `src/main/gen/.../lang/psi/MakoCodeBlock.java` - Now extends PsiLanguageInjectionHost (generated)
- `src/main/gen/.../lang/psi/MakoModuleBlock.java` - Now extends PsiLanguageInjectionHost (generated)
- `src/main/gen/.../lang/psi/impl/MakoExpressionImpl.java` - Now extends MakoExpressionMixin (generated)
- `src/main/gen/.../lang/psi/impl/MakoCodeBlockImpl.java` - Now extends MakoCodeBlockMixin (generated)
- `src/main/gen/.../lang/psi/impl/MakoModuleBlockImpl.java` - Now extends MakoModuleBlockMixin (generated)

## Decisions Made

- GrammarKit's `implements=` attribute in BNF rule attributes causes the generated PSI interface to extend the specified interface (PsiLanguageInjectionHost). No manual interface file editing is required after generation.
- LiteralTextEscaper.createSimple() is available in the 2025.2 SDK — the fallback anonymous class described in the plan was not needed.
- MakoTypes.java token delegate constants survived parser regeneration unchanged — the generateTokens=false pattern established in Phase 3 continues to hold correctly.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The checkpoint (Task 3) required user action to run the IDE "Generate Parser Code" action, which is the expected flow. After regeneration, all generated files matched expected output and ./gradlew check passed immediately.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 06-02 (MakoPythonInjector) can proceed immediately
- MakoExpression, MakoCodeBlock, and MakoModuleBlock PSI nodes now implement PsiLanguageInjectionHost — the injector can cast any of these three node types and call getInjectionHosts()
- The injection offset boundaries (EXPR_CONTENT, CODE_CONTENT, MODULE_CONTENT token ranges) are defined in the BNF and will be used by Plan 02's injector to specify injection ranges

---
*Phase: 06-python-language-injection*
*Completed: 2026-02-21*

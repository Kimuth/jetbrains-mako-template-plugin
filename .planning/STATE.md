# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 2 - Lexer and Parser

## Current Position

Phase: 1 of 8 (Language Foundation) — COMPLETE
Plan: 2 of 2 in phase (phase complete — advance to Phase 2)
Status: Phase 1 complete
Last activity: 2026-02-19 — Plan 02 complete (MakoLanguage + MakoFileType registered, build passes)

Progress: [██░░░░░░░░] 13%

## Performance Metrics

**Velocity:**
- Total plans completed: 2
- Average duration: 5.5 min
- Total execution time: 0.18 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-language-foundation | 2 | 11 min | 5.5 min |

**Recent Trend:**
- Last 5 plans: 9 min, 2 min
- Trend: accelerating

*Updated after each plan completion*
| Phase 01-language-foundation P02 | 2 | 2 tasks | 5 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Pre-Phase 1]: Architecture decision (TemplateLanguage vs. injection-into-HTML) must be locked in Phase 1 before any grammar work — wrong choice is expensive to reverse
- [Pre-Phase 1]: Python plugin dependency should use `com.intellij.modules.python` (bundled module), NOT `com.jetbrains.python` (Marketplace plugin)
- [Pre-Phase 1]: GrammarKit Gradle plugin version must be verified at plugins.gradle.org before pinning — training data version may be stale
- [01-01]: Use pycharmCommunity() target — IDE is PyCharm Community, not IntelliJ IDEA
- [01-01]: GrammarKit tasks are commented stubs — .flex/.bnf files don't exist until Phase 2; activating now causes Gradle config errors
- [01-01]: org.gradle.java.home pinned to JDK 21 — IntelliJ Platform instrumentCode fails on Windows with JDK 25 (no Packages/ dir in MSI-installed JDK)
- [01-01]: platformBundledPlugins = PythonCore for bundled Python support in PyCharm Community
- [01-01]: plugin.xml name = Mako Template Support (marketplace branding)
- [01-02]: MakoLanguage implements TemplateLanguage (not plain Language) — enables Phase 4 TemplateDataLanguage integration; this was the pre-Phase 1 architecture lock-in
- [01-02]: fieldName=INSTANCE in plugin.xml fileType — correct for Kotlin object singletons (synthetic INSTANCE field)
- [01-02]: compound extension .html.mako uses patterns= not extensions= — glob matching required for multi-dot extensions
- [01-02]: Language ID "Mako Template" must match getName() and plugin.xml name/language attributes exactly — string identity contract enforced to prevent silent failures
- [Phase 01-02]: MakoLanguage implements TemplateLanguage (not plain Language) — enables Phase 4 TemplateDataLanguage integration; architecture lock-in completed
- [Phase 01-02]: fieldName=INSTANCE in plugin.xml fileType — correct for Kotlin object singletons which expose synthetic INSTANCE field to Java
- [Phase 01-02]: compound extension .html.mako uses patterns= not extensions= — glob matching required for multi-dot extensions in fileType registration

### Pending Todos

None.

### Blockers/Concerns

- [Research]: JFlex filter expression handling (`${x | h,trim}`) needs a lexer design spike in Phase 2 — `|` disambiguation from Python bitwise OR not fully resolved
- [Research]: TemplateDataLanguageConfigurable exact API for platform build 252 needs verification against current documentation before Phase 4

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 01-language-foundation/01-02-PLAN.md (Phase 1 complete)
Resume file: .planning/phases/02-lexer-parser/ (Phase 2 plans)

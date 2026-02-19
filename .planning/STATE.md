# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 3 - Parser (GrammarKit BNF + MakoParser)

## Current Position

Phase: 2 of 8 (Lexer) — Complete
Plan: 2 of 2 complete (Phase 2 fully done)
Status: Phase 2 complete — lexer pipeline fully wired, all PARS-01/02/03 requirements verified
Last activity: 2026-02-19 — Plan 02 complete (MakoParserDefinition, MakoFile, plugin.xml registered, 18 lexer tests passing)

Progress: [████░░░░░░] 25%

## Performance Metrics

**Velocity:**
- Total plans completed: 4
- Average duration: 3.75 min
- Total execution time: 0.25 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-language-foundation | 2 | 11 min | 5.5 min |
| 02-lexer | 2 | 6 min | 3 min |

**Recent Trend:**
- Last 5 plans: 9 min, 2 min, 3 min, 3 min
- Trend: stable at 3 min/plan

*Updated after each plan completion*
| Phase 01-language-foundation P02 | 2 | 2 tasks | 5 files |
| Phase 02-lexer P01 | 3 | 2 tasks | 6 files |
| Phase 02-lexer P02 | 3 | 2 tasks | 6 files |

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
- [Phase 02-01]: MakoLexer.flex placed in src/main/grammars/ (not src/main/kotlin/lang/) — cleaner convention separating grammar files from Kotlin sources
- [Phase 02-01]: CONTROL_LINE uses single token for entire % line — sufficient for Phase 2 restart anchors; Phase 3 parser can split if needed
- [Phase 02-01]: Only generateMakoLexer activated in Phase 2 — generateMakoParser stays commented, no .bnf file exists until Phase 3
- [Phase 02-02]: createParser() and createElement() throw UnsupportedOperationException — Phase 3 stubs; platform never calls these during lexer-only operation
- [Phase 02-02]: MakoFile stub delegates getFileType() only — Phase 3 extends with createElement factory when GrammarKit parser generates PSI nodes
- [Phase 02-02]: TAG_ATTRS > close rule added to MakoLexer.flex — named block tags (<%def name='foo'>) end with > not %>

### Pending Todos

None.

### Blockers/Concerns

- [RESOLVED - Phase 02-01]: JFlex filter expression handling `|` disambiguation — resolved using EXPRESSION state; `FILTER_SEP` only emitted inside EXPRESSION state, `||` returns `EXPR_CONTENT`
- [RESOLVED - Phase 02-02]: TAG_ATTRS state missing `>` close rule — added `">"` rule returning TAG_CLOSE; named block tags now tokenize correctly
- [Research]: TemplateDataLanguageConfigurable exact API for platform build 252 needs verification against current documentation before Phase 4

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 02-lexer/02-02-PLAN.md (Phase 2 complete — all 2 plans done)
Resume file: .planning/phases/03-parser/03-01-PLAN.md

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 3 - Parser (GrammarKit BNF + MakoParser) — COMPLETE

## Current Position

Phase: 3 of 8 (Parser) — Complete
Plan: 2 of 2 complete
Status: Phase 3 complete — PSI mixin classes, MakoParserDefinition wired to MakoParser + MakoTypes.Factory, parsing tests passing
Last activity: 2026-02-19 — Plan 02 complete (mixin classes, token delegates, parser definition wired, parsing tests)

Progress: [█████░░░░░] 38%

## Performance Metrics

**Velocity:**
- Total plans completed: 7
- Average duration: 3 min
- Total execution time: 0.40 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-language-foundation | 2 | 11 min | 5.5 min |
| 02-lexer | 3 | 7 min | 2.3 min |
| 03-parser | 2 | 12 min | 6 min |

**Recent Trend:**
- Last 5 plans: 3 min, 3 min, 1 min, 4 min, 8 min
- Trend: stable at ~2-4 min/plan

*Updated after each plan completion*
| Phase 01-language-foundation P02 | 2 | 2 tasks | 5 files |
| Phase 02-lexer P01 | 3 | 2 tasks | 6 files |
| Phase 02-lexer P02 | 3 | 2 tasks | 6 files |
| Phase 02-lexer P03 | 1 | 1 task | 1 file |
| Phase 03-parser P01 | 4 | 2 tasks | 28 files |
| Phase 03-parser P02 | 8 | 2 tasks | 10 files |

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
- [Phase 02-03]: No-op parser wraps all tokens in single root marker — sufficient for file-open pipeline; Phase 3 replaces with GrammarKit-generated parser
- [Phase 02-03]: ASTWrapperPsiElement used as createElement() fallback — generic PSI wrapper until Phase 3 generates typed PSI node factory
- [Phase 03-01]: Per-tag token types (TAG_OPEN_DEF..TAG_OPEN_PAGE) replace generic TAG_OPEN — distinct token types required for GrammarKit to produce distinct PSI node types per Mako tag (PARS-04)
- [Phase 03-01]: generateTokens=false in BNF header prevents silent parse failures from duplicate token constants (research Pitfall 2)
- [Phase 03-01]: No yypushback in per-tag flex rules — tag keyword fully consumed in TAG_OPEN_xxx, TAG_ATTRS reads first attribute name directly
- [Phase 03-01]: line_comment_rule name avoids BNF rule/token name collision with LINE_COMMENT token
- [Phase 03-01]: def_tag and block_tag have mixin+implements in BNF — mixin classes created in Plan 02
- [Phase 03-parser]: MakoTypes.java token delegates: generateTokens=false requires hand-adding token constants to MakoTypes.java that delegate to MakoTokenTypes.kt
- [Phase 03-parser]: generateMakoParser removed from compileKotlin auto-run to prevent purgeOldFiles=true from wiping token delegates; run manually when BNF changes
- [Phase 03-parser]: Mixin classes are abstract and extend ASTWrapperPsiElement; getNameIdentifier is PsiNameIdentifierOwner not PsiNamedElement -- omitted from mixin

### Pending Todos

None.

### Blockers/Concerns

- [RESOLVED - Phase 02-01]: JFlex filter expression handling `|` disambiguation — resolved using EXPRESSION state; `FILTER_SEP` only emitted inside EXPRESSION state, `||` returns `EXPR_CONTENT`
- [RESOLVED - Phase 02-02]: TAG_ATTRS state missing `>` close rule — added `">"` rule returning TAG_CLOSE; named block tags now tokenize correctly
- [RESOLVED - Phase 03-02]: `./gradlew build` was failing due to missing mixin classes — resolved, build is now green with all 22 tests passing
- [Research]: TemplateDataLanguageConfigurable exact API for platform build 252 needs verification against current documentation before Phase 4

## Session Continuity

Last session: 2026-02-19
Stopped at: Completed 03-parser/03-02-PLAN.md (PSI mixin classes, parser definition wired, parsing tests, Phase 3 complete)
Resume file: .planning/phases/04-template-language/ (Phase 4)

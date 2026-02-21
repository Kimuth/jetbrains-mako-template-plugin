# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 5 - Structural Features — ALL 3 PLANS COMPLETE

## Current Position

Phase: 5 of 8 (Structural Features) — COMPLETE
Plan: 3 of 3 complete
Status: Phase 5 Plan 3 complete — Grammar fix for TEMPLATE_TEXT-body folding; template_text_content rule + item_ reordering; 52 tests green; SYNX-06 satisfied
Last activity: 2026-02-20 — Plan 03 complete (grammar fix: END_TAG now always consumed in def/block PSI composites regardless of body content; 5 regression tests added)

Progress: [████████░░] 75%

## Performance Metrics

**Velocity:**
- Total plans completed: 11
- Average duration: 8 min
- Total execution time: ~91 min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-language-foundation | 2 | 11 min | 5.5 min |
| 02-lexer | 3 | 7 min | 2.3 min |
| 03-parser | 3 | 15 min | 5 min |
| 04-syntax-highlighting | 2 | 3 min | 1.5 min |
| 05-structural-features | 3 | 78 min | 26 min |

**Recent Trend:**
- Last 5 plans: 1 min, 4 min, 45 min, 13 min, 20 min
- Trend: 20 min for grammar fix (GrammarKit internals analysis, alternative ordering discovery)

*Updated after each plan completion*
| Phase 01-language-foundation P02 | 2 | 2 tasks | 5 files |
| Phase 02-lexer P01 | 3 | 2 tasks | 6 files |
| Phase 02-lexer P02 | 3 | 2 tasks | 6 files |
| Phase 02-lexer P03 | 1 | 1 task | 1 file |
| Phase 03-parser P01 | 4 | 2 tasks | 28 files |
| Phase 03-parser P02 | 8 | 2 tasks | 10 files |
| Phase 03-parser P03 | 3 | 2 tasks | 8 files |
| Phase 04-syntax-highlighting P01 | 2 | 3 tasks | 6 files |
| Phase 04-syntax-highlighting-comment-support P02 | 1 | 1 tasks | 1 files |
| Phase 05-structural-features P01 | 45 | 2 tasks | 5 files |
| Phase 05-structural-features P02 | 13 | 2 tasks | 5 files |
| Phase 05-structural-features P03 | 20 | 2 tasks | 11 files |

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
- [Phase 03-parser]: BNF rule renamed control_line -> control_line_stmt to fix CONTROL_LINE composite/token name collision (GAP-01): CONTROL_LINE is now exclusively a token delegate in MakoTypes.java, CONTROL_LINE_STMT is the composite
- [Phase 03-parser]: purgeOldFiles=true in GrammarKit config automatically purges old PSI files when BNF rules are renamed -- MakoControlLine.java and MakoControlLineImpl.java were auto-deleted when control_line was renamed to control_line_stmt
- [Phase 04-01]: structural=false for ALL BracePair entries — shared END_TAG token across <%def> and <%block> causes platform matching conflicts with structural=true
- [Phase 04-01]: colorSettingsPage uses 'implementation' attribute (not 'implementationClass') and has no 'language' attribute — different from other language-scoped extensions
- [Phase 04-01]: All MAKO_ TextAttributesKey constants prefixed with MAKO_ to avoid global name collisions with other language highlighters
- [Phase 04-01]: TEMPLATE_TEXT and TAG_ATTR_EQ both return EMPTY_KEYS — HTML layer handles template text; TAG_ATTR_EQ is plain punctuation needing no special color
- [Phase 04-02]: MARKUP_TAG fallback for MAKO_EXPRESSION: TEMPLATE_LANGUAGE_COLOR inherits from HighlighterColors.TEXT with no visible foreground; MARKUP_TAG provides distinct teal/Darcula, blue/Light colors for ${...} expressions
- [Phase 05-01]: AST-level scanning for doc_comment folds: DOC_OPEN/CLOSE in getCommentTokens() means PsiBuilder skips them; doc_comment PSI rule never matches; scan root.node.firstChildNode/treeNext directly
- [Phase 05-01]: DUMMY_BLOCK transparent descent: GeneratedParserUtilBase wraps unconsumed tokens in DUMMY_BLOCK when makoFile() loop exits early; control flow tokens inside must be recursively collected
- [Phase 05-01]: ASTNode element type check for control lines: CONTROL_LINE tokens may appear as raw LeafPsiElement (not MakoControlLineStmtImpl) inside DUMMY_BLOCK; check node.elementType directly
- [Phase 05-01]: purgeOldFiles=false on generateMakoLexer: lang/ directory contains committed psi/ and parser/ subdirs; purgeOldFiles=true recursively deletes them on every clean build
- [Phase 05-01]: ParsingTestCase for folding tests: BasePlatformTestCase cannot register MakoFileType so testFolding() opens .mako as PlainText; use ParsingTestCase with explicit MakoParserDefinition()
- [Phase 05-structural-features]: ASTWrapperPsiElement implements NavigatablePsiElement (via PsiElementBase) so navigate() delegation is free in structure view elements
- [Phase 05-structural-features]: Parser flattens nested <%def> tags into file-level siblings — structure view reflects actual PSI tree (no artificial nesting added)
- [Phase 05-structural-features]: Test content for structure view must use empty tag bodies — TEMPLATE_TEXT inside tags triggers pin=1 error recovery that consumes END_TAG of subsequent sibling tags [RESOLVED by 05-03: grammar fix means this is no longer required; TEMPLATE_TEXT bodies now parse correctly]
- [Phase 05-structural-features]: template_text_content FIRST in item_ alternatives: root fix is alternative ordering not just naming the rule; bare consumeToken in later position causes GrammarKit recovery from failed pinned rules to consume TEMPLATE_TEXT before item_() alternative can match it

### Roadmap Evolution

- Phase 9 added: Marketplace Branding — rename pluginGroup, pluginName, and related metadata to follow JetBrains Marketplace naming best practices (no generic terms like "Support", "Tool", "Integration")

### Pending Todos

- [highlighting] Reference TextMate/VS Code Mako bundles for syntax decisions (Phase 5+) — `.planning/todos/pending/2026-02-20-reference-textmate-vscode-mako-bundles.md`

### Blockers/Concerns

- [RESOLVED - Phase 02-01]: JFlex filter expression handling `|` disambiguation — resolved using EXPRESSION state; `FILTER_SEP` only emitted inside EXPRESSION state, `||` returns `EXPR_CONTENT`
- [RESOLVED - Phase 02-02]: TAG_ATTRS state missing `>` close rule — added `">"` rule returning TAG_CLOSE; named block tags now tokenize correctly
- [RESOLVED - Phase 03-02]: `./gradlew build` was failing due to missing mixin classes — resolved, build is now green with all 22 tests passing
- [RESOLVED - Phase 03-03]: GAP-01 CONTROL_LINE name collision — BNF rule renamed control_line -> control_line_stmt; CONTROL_LINE is now exclusively a token delegate; CONTROL_LINE_STMT is the composite; control lines produce MakoControlLineStmtImpl(CONTROL_LINE_STMT) PSI nodes as top-level FILE children
- [Research]: TemplateDataLanguageConfigurable exact API for platform build 252 needs verification against current documentation before Phase 4

## Session Continuity

Last session: 2026-02-20
Stopped at: Completed 05-structural-features/05-03-PLAN.md (Grammar fix for TEMPLATE_TEXT-body folding; 52 tests green; SYNX-06 satisfied; Phase 5 ALL PLANS COMPLETE)
Resume file: .planning/phases/06-navigation/ (Phase 6, Plan 1)

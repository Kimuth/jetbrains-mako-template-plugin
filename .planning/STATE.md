# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-19)

**Core value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm
**Current focus:** Phase 8 - Error Annotations and Release Readiness (Plan 01 complete)

## Current Position

Phase: 8 of 8 (Error Annotations and Release Readiness) — IN PROGRESS
Plan: 1 of 2 complete
Status: Phase 8 Plan 01 complete — MakoAnnotator implemented; unclosed tag detection + invalid directive detection; 64 tests green; COMP-03 verified
Last activity: 2026-02-21 — Plan 01 complete (MakoAnnotator.kt created; annotator registered in plugin.xml; ./gradlew check 64 tests green)

Progress: [██████████] 100%

## Performance Metrics

**Velocity:**
- Total plans completed: 12
- Average duration: 9 min
- Total execution time: ~121 min

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
| Phase 06-python-language-injection P01 | 30 | 3 tasks | 10 files |
| Phase 06-python-language-injection P02 | 3 | 2 tasks | 2 files |
| Phase 07-completion P01 | 6 | 2 tasks | 2 files |
| Phase 07-completion P02 | 27 | 1 tasks | 4 files |
| Phase 08-error-annotations-and-release-readiness P01 | 1 | 1 tasks | 2 files |

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
- [Phase 06-01]: GrammarKit implements= attribute on BNF rules causes generated PSI interfaces to extend PsiLanguageInjectionHost — no manual interface file editing needed after regen
- [Phase 06-01]: Injection host mixin classes must be abstract (required by GrammarKit); generated *Impl extends mixin so host contract flows through class hierarchy automatically
- [Phase 06-01]: LiteralTextEscaper.createSimple() is available in 2025.2 SDK — fallback anonymous class implementation not needed
- [Phase 06-01]: MakoTypes.java token delegate constants survived parser regeneration unchanged — generateTokens=false pattern established in Phase 3 continues to hold
- [Phase 06-02]: elementsToInjectIn() must use PSI interface classes (MakoExpression::class.java), not generated *Impl classes — platform injection dispatch works on interface types
- [Phase 06-02]: end > start guard prevents addPlace() calls on empty/malformed host nodes (${} with no content, truncated <%)
- [Phase 06-02]: Language.findLanguageByID("Python") called per-invocation — cheap lookup, avoids initialization-order issues with language registry population
- [Phase 06-02]: Filter expressions (${x | h}) inject full content as-is; Python sees x | h as valid bitwise-OR — filter refinement deferred to Phase 7
- [Phase 07-01]: Raw text inspection (file.text.substring(offset-2, offset)) chosen for tag-name completion — dummy identifier disrupts lexer before TAG_OPEN_xxx tokens appear; PSI-pattern matching unreliable for this use case
- [Phase 07-01]: result.withPrefixMatcher("") required for tag-name completion — platform filters items whose lookup string doesn't match typed prefix; empty matcher bypasses this for <% prefix items
- [Phase 07-01]: TAG_ATTRIBUTES map uses PSI interface .java class; parent walk uses element.javaClass against the interface-keyed map — works because impl class identity matches the interface key in Kotlin's mapOf()
- [Phase 07-02]: addFileToProject + configureFromExistingVirtualFile used instead of configureByText(FileType) — the latter creates an in-memory file before MakoFileType is registered in the test JVM, resulting in PLAIN_TEXT; addFileToProject creates a physical temp file whose extension is recognized after file type registration completes
- [Phase 07-02]: completion.contributor language='any' required — TEMPLATE_TEXT tokens fall in the template-data-language layer; language='Mako Template' filter prevented contributor from firing for those positions
- [Phase 07-02]: attrsForTag() uses Kotlin is (instanceof) checks — original Class equality via element.javaClass vs MakoDefTag::class.java failed because impl classes (MakoDefTagImpl) differ from interface classes (MakoDefTag)
- [Phase 07-02]: com.intellij.modules.json added to platformBundledPlugins — PythonCore depends on intellij.json.backend module (from JSON plugin); without it PythonCore fails to load, preventing our plugin (which depends on PythonCore) from loading in tests
- [Phase 08-error-annotations-and-release-readiness]: language='Mako Template' (not 'any') for annotator registration — annotators receive correct PSI elements when scoped to specific language ID
- [Phase 08-error-annotations-and-release-readiness]: Annotate openToken.textRange for unclosed tags (firstChild = <%def/<%block keyword) to avoid red underlining multi-line content bodies
- [Phase 08]: pluginIcon uses teal #2B6B6B rounded square with 'M' — matches file type icon color family for brand consistency
- [Phase 08]: verifyPlugin recommended() checks PC-252, PY-253, PY-261 — all Compatible; no binary errors on any build

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

Last session: 2026-02-21
Stopped at: Completed 08-error-annotations-and-release-readiness/08-01-PLAN.md (MakoAnnotator; 64 tests green; COMP-03 satisfied)
Resume file: .planning/phases/08-error-annotations-and-release-readiness/ (Phase 8 Plan 02 — annotator tests)

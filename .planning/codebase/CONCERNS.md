# Codebase Concerns

**Analysis Date:** 2026-02-21

## Build Artifacts

**Backup Lexer File:**
- Issue: `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java~` (backup file generated during last lexer regeneration)
- Files: `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java~`
- Impact: Not committed, but clutters the generated directory and should be gitignored
- Fix approach: Add `.java~` pattern to `.gitignore` or use lexer generation tool that doesn't create backups

**Untracked Test Data File:**
- Issue: `src/test/testData/parser/IncompleteCodeBlock.mako` lacks corresponding `.txt` expected output
- Files: `src/test/testData/parser/IncompleteCodeBlock.mako`
- Impact: Test fixture is incomplete—when `MakoParsingTest` runs this file, it will auto-generate the `.txt` but it was never committed as verified
- Fix approach: Either delete the fixture (if unneeded) or run test once, verify the generated `.txt` output is correct, then commit it

## Fragile Areas

**Control Flow Extraction via Regex in Folding:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/folding/MakoFoldingBuilder.kt` (lines 179–181)
- Why fragile: The `extractKeyword` function uses `trimStart().removePrefix("%").trimStart().split(Regex("\\s+"))[0].trimEnd(':')` to parse control keywords from CONTROL_LINE tokens. If a control line has unexpected format (e.g., unusual spacing, non-ASCII characters), `split(...)[0]` could fail or return wrong keyword
- Safe modification: Add null-safety checks, unit tests for edge cases (line with only whitespace, multiple `%` chars, missing colons), and consider parsing via token type instead
- Test coverage: Control flow folding tested in `MakoFoldingTest.testForLoopFolded()`, `testIfBlockFolded()`, `testWhileLoopFolded()` but only with well-formed inputs

**DUMMY_BLOCK Detection in Control Line Collection:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/folding/MakoFoldingBuilder.kt` (lines 191–196)
- Why fragile: The `collectControlLineNodes` function checks for DummyBlockElementType by comparing `et.toString() == "DUMMY_BLOCK"` and `et.javaClass.simpleName == "DummyBlockElementType"`. This is a string-based type check that's brittle across IntelliJ platform versions
- Safe modification: Consider using `instanceof` checks with properly imported types or cache the DummyBlockElementType reference
- Test coverage: No explicit tests for malformed files triggering dummy block recovery

**PSI Name Extraction via Token Search:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoDefTagMixin.kt` (lines 17–22), `MakoBlockTagMixin.kt` (lines 17–20)
- Why fragile: Both mixins search for the first TAG_ATTR_VALUE token without verifying it's actually the `name=` attribute. If a tag has multiple attributes and `name=` isn't first, `getName()` will return the wrong attribute value
- Safe modification: Add proper attribute name checking; search for both TAG_ATTR_NAME and TAG_ATTR_VALUE pairs, matching on name="..." pattern
- Test coverage: No unit tests for `getName()` with reordered attributes; parser tests don't cover attribute ordering

**Error Recovery and Parser Composition Boundaries:**
- Files: `src/main/grammars/Mako.bnf` (lines 141–150), `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/folding/MakoFoldingBuilder.kt` (lines 69–91)
- Why fragile: Parser uses `recoverWhile=tag_recover` and `recoverWhile=expression_recover` predicates that absorb TEMPLATE_TEXT tokens when recovery fails. The `findClosingTokenEnd` method in folding works around this by scanning backward for the real closing token, but this is a grammar-level asymmetry that could break if recovery rules change
- Safe modification: Consider restructuring recovery predicates to not consume TEMPLATE_TEXT, or maintain comprehensive fold range tests covering all error cases
- Test coverage: Regression tests in `MakoFoldingTest` cover `TEMPLATE_TEXT`-body folding (lines 189–227) but don't test error recovery edge cases where both END_TAG and TEMPLATE_TEXT are orphaned

## Missing Critical Features

**Rename Refactoring (def/block names):**
- Problem: `setName()` methods in `MakoDefTagMixin` and `MakoBlockTagMixin` are no-op stubs (return `this`)
- Blocks: Cannot rename def/block definitions and have usages updated (if that ever becomes a feature)
- Priority: Low (refactoring not yet implemented per Phase roadmap)

**No inline folding for unclosed tags:**
- Problem: If a def or block tag is malformed (missing END_TAG), the entire file becomes a single error recovery scope and folding doesn't work correctly
- Blocks: Large template files with syntax errors cannot be code-folded, forcing users to scroll through unfolded content
- Priority: Medium (affects usability for buggy templates during development)

## Platform Version Constraints

**PyCharm Community 2025.2+ Requirement:**
- Files: `gradle.properties` (line 13), `build.gradle.kts` (line 51), `plugin.xml` (line 8)
- Issue: Plugin targets PyCharm 2025.2+ (build 252+) and requires bundled `PythonCore` plugin. If users have PyCharm 2024.x or earlier, plugin won't load
- Current state: No `untilBuild` limit set, so plugin appears compatible with future versions, but no upgrade testing done
- Risk: Silent incompatibility if IntelliJ Platform API breaks between 2025.2 and future releases
- Recommendation: Document compatibility range clearly; add upgrade testing to CI/CD pipeline

**JVM Toolchain 21 Requirement:**
- Files: `build.gradle.kts` (line 22), `gradle.properties` (line 31)
- Issue: Plugin requires JDK 21 (`jvmToolchain(21)`). Users with JDK 11/17 cannot build locally
- Impact: Higher barrier for community contributions
- Recommendation: Document in README; consider reducing JDK requirement to 17 if platform allows

## Test Coverage Gaps

**Syntax Highlighter Attribute Mapping:**
- What's not tested: `MakoSyntaxHighlighter.getTokenHighlights()` (lines 75–128) maps all token types to color keys, but no tests verify correct color assignment for each token type
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt`
- Risk: Silent color attribute mismatches could cause incorrect highlighting in certain IDE themes
- Priority: Low (visual; caught by manual testing)

**Brace Matcher Edge Cases:**
- What's not tested: `MakoPairedBraceMatcher` logic for matching `<%...%>`, `${...}`, `</%...>` pairs not unit tested
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoPairedBraceMatcher.kt`
- Risk: Brace highlighting/navigation may fail on nested or malformed constructs
- Priority: Low (UI feature; can be tested manually)

**Structure View Deep Nesting:**
- What's not tested: `MakoStructureViewElement.getChildren()` (lines 39–63) recursively collects defs and blocks but doesn't test deeply nested templates (def > block > def > block...)
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/structure/MakoStructureViewElement.kt`
- Risk: Performance degradation or incorrect tree rendering for complex templates
- Priority: Medium (found in `MakoStructureViewTest` but limited depth coverage)

**Parser Error Recovery Asymmetry:**
- What's not tested: Recovery predicates (`tag_recover`, `expression_recover`) don't have unit tests; only integration tests via parser fixtures
- Files: `src/main/grammars/Mako.bnf` (lines 141–150)
- Risk: Recovery behavior may become incorrect after grammar changes without detection
- Priority: High (affects parser reliability)

## Version and Dependency Concerns

**Pre-Release (v0.0.1):**
- Issue: Plugin is at version 0.0.1 with "Unreleased" changelog; not yet published to JetBrains Marketplace
- Files: `gradle.properties` (line 7), `CHANGELOG.md` (lines 5–7)
- Impact: No real-world usage feedback; breaking changes possible in upcoming versions
- Recommendation: Establish versioning strategy and initial feature completeness before v1.0.0

**IntelliJ Platform Gradle Plugin Version Pinning:**
- Issue: Plugin uses version catalog (`libs.plugins.intelliJPlatform`) from Gradle versionCatalog, but the actual version is not visible in this codebase
- Files: `build.gradle.kts` (line 10), `build.gradle.kts` (line 45–64)
- Impact: Difficult to audit dependency versions and potential security updates
- Recommendation: Document the resolved version or expose it in `gradle.properties`

## Documentation Gaps

**Grammar Maintenance Manual:**
- Issue: Lexer generation requires manual `./gradlew generateMakoLexer` after editing `MakoLexer.flex`. Parser generation requires IDE action via GrammarKit plugin
- Files: `CLAUDE.md` (documented), `build.gradle.kts` (lines 152–175)
- Impact: Developers unfamiliar with IntelliJ's GrammarKit may accidentally commit parser changes without regenerating, or regenerate incorrectly
- Risk: Silent parser bugs from stale generated code
- Recommendation: Add pre-commit hook to verify `_MakoLexer.java` and parser files are up-to-date with grammar sources

**Color Scheme Files Require Manual Sync:**
- Issue: `MakoDarcula.xml` and `MakoDefault.xml` color definitions are separate from `MakoSyntaxHighlighter.kt` token mappings; changes to one must be manually synced to the other
- Files: `src/main/resources/colorSchemes/MakoDarcula.xml`, `src/main/resources/colorSchemes/MakoDefault.xml`, `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/highlighting/MakoSyntaxHighlighter.kt`
- Impact: Color schemes drift from actual token types, causing mismatches in editor display
- Recommendation: Consider adding a test that validates all TextAttributesKey names in highlighter are present in color scheme files

## Known Quirks & Workarounds

**JFlex Combined DFA Fix:**
- Issue: Multiple rules in lexer states split lookahead alternatives (e.g., `[^%]+` and `"%" / [^>]` in CODE_BLOCK) to avoid JFlex generating a non-accepting state for single-char-at-EOF scenarios
- Files: `src/main/grammars/MakoLexer.flex` (lines 154–162, 169–177, 184–198)
- Workaround: This is intentional; documented in comments
- Risk: If JFlex version changes, this pattern may no longer be necessary or may break
- Recommendation: Add comment referencing the original issue; include regression test for single-char-at-EOF

**GrammarKit purgeOldFiles=false Asymmetry:**
- Issue: Lexer task has `purgeOldFiles=false` (line 159 in build.gradle.kts) to preserve parser/PSI subdirectories, but this is unusual and could cause issues if old generated files remain after grammar refactoring
- Files: `build.gradle.kts` (lines 155–159)
- Workaround: Works as designed per comments (lines 156–158)
- Risk: Accidental commits of orphaned generated files
- Recommendation: Document in README; ensure CI enforces clean builds before publishing

## Scaling Limits

**No Incremental Parsing Hints:**
- Issue: Parser generates full PSI tree for entire file on each edit; no incremental re-parse optimization
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt`
- Limit: May degrade editor responsiveness for very large Mako templates (5000+ lines)
- Scaling path: Implement incremental lexer state encoding (already started via `braceDepth` in lexer) and lazy PSI construction (IntelliJ feature)

**Folding Descriptor Collection Not Lazy:**
- Issue: `MakoFoldingBuilder.buildFoldRegions()` collects all def/block/code/doc folds via full tree traversal on each fold query
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/folding/MakoFoldingBuilder.kt` (lines 26–67)
- Limit: O(n) complexity for each fold query; noticeable on templates with 100+ foldable regions
- Scaling path: Cache fold descriptors or use iterator-based traversal instead of `PsiTreeUtil.collectElementsOfType`

---

*Concerns audit: 2026-02-21*

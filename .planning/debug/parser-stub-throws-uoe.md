---
status: resolved
trigger: "MakoParser not yet implemented - Phase 3 UnsupportedOperationException when opening .mako file"
created: 2026-02-19T00:00:00Z
updated: 2026-02-19T00:00:00Z
---

## Current Focus

hypothesis: createParser() and createElement() throw UnsupportedOperationException because they were left as Phase 3 stubs, but IntelliJ platform ALWAYS calls the parser pipeline when opening any file with a registered ParserDefinition
test: confirmed by reading MakoParserDefinition.kt - both methods throw
expecting: replacing with minimal implementations will allow file opening
next_action: report root cause and fix specification

## Symptoms

expected: Opening a .mako file in runIde should display the file with syntax highlighting
actual: Platform throws java.lang.UnsupportedOperationException: MakoParser not yet implemented - Phase 3
errors: "Caused by: java.lang.UnsupportedOperationException: MakoParser not yet implemented - Phase 3"
reproduction: ./gradlew runIde, open any .mako file
started: Since Phase 2 implementation (ParserDefinition was registered but parser stubs throw)

## Eliminated

(none - hypothesis confirmed on first investigation)

## Evidence

- timestamp: 2026-02-19T00:00:00Z
  checked: MakoParserDefinition.kt lines 29-30 and 36-37
  found: createParser() throws UnsupportedOperationException("MakoParser not yet implemented - Phase 3"); createElement() also throws UnsupportedOperationException("MakoElement factory not yet implemented - Phase 3")
  implication: Both methods are called by the platform when opening any file that has a registered lang.parserDefinition in plugin.xml

- timestamp: 2026-02-19T00:00:00Z
  checked: plugin.xml line 18-19
  found: lang.parserDefinition is registered for language "Mako Template" pointing to MakoParserDefinition
  implication: Platform will invoke the full parser pipeline (createParser -> parse -> createElement) for every .mako file opened

- timestamp: 2026-02-19T00:00:00Z
  checked: IntelliJ Platform SDK docs and PsiParser.java source
  found: PsiParser.parse(IElementType root, PsiBuilder builder) must consume all tokens and return an ASTNode tree. Platform calls createParser().parse() to build AST, then createElement() for each composite AST node to build PSI tree.
  implication: A minimal parser that wraps all tokens in a single root marker is the standard pattern for "lexer-only" phases

## Resolution

root_cause: MakoParserDefinition.createParser() throws UnsupportedOperationException. The platform ALWAYS calls the full parser pipeline (createParser -> parse -> createElement) when a file has a registered lang.parserDefinition, even if the intent is lexer-only highlighting. The assumption that createParser() would not be called during lexer-only operation was incorrect.

fix: Replace both stub methods with minimal working implementations:
  1. createParser() should return a PsiParser that wraps all lexer tokens into a single file-level root node
  2. createElement() should return ASTWrapperPsiElement for any AST node

verification: (pending implementation)

files_changed:
  - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt

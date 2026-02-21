---
status: resolved
trigger: "space-after-code-open-index-out-of-bounds"
created: 2026-02-21T00:00:00Z
updated: 2026-02-21T00:35:00Z
---

## Current Focus

hypothesis: CONFIRMED - JFlex combined rule generates non-accepting intermediate state for single non-% char at EOF in CODE_BLOCK
test: Verified by splitting rules, regenerating lexer, running full test suite
expecting: All tests pass, no BAD_CHARACTER for <% followed by single char
next_action: RESOLVED

## Symptoms

expected: `<% ` (space after code open `<%`) should parse without crashing — either produce a valid partial parse or a graceful error element, but never throw a Java exception
actual: `java.lang.IndexOutOfBoundsException: Index out of range: -1; length: 3` is thrown
errors: |
  Caused by: java.lang.IndexOutOfBoundsException: Index out of range: -1; length: 3

  PSI tree for `<% ` (3 chars: `<`, `%`, ` `):
  FILE(0,3)
    MakoCodeBlockImpl(CODE_BLOCK)(0,3)
      PsiElement(CODE_OPEN)('<%')(0,2)
      PsiErrorElement:CODE_CONTENT or CODE_CLOSE expected(2,3)
        PsiElement(BAD_CHARACTER)(' ')(2,3)

reproduction: Open a .mako file in PyCharm, type `<% ` (less-than, percent, space)
started: Unknown — discovered during testing

## Eliminated

- hypothesis: Bug is in MakoFoldingBuilder computing negative TextRange offset
  evidence: FoldingBuilder uses composite.textRange.endOffset which is non-negative; crash is earlier in lexer
  timestamp: 2026-02-21T00:00:00Z

- hypothesis: Bug is in MakoParser or GrammarKit error recovery
  evidence: Parser correctly creates PsiErrorElement with BAD_CHARACTER; the crash happens when IDE processes the BAD_CHARACTER token, tracing back to FlexAdapter catching a ZZ_NO_MATCH Error from the lexer
  timestamp: 2026-02-21T00:00:00Z

## Evidence

- timestamp: 2026-02-21T00:00:00Z
  checked: _MakoLexer.java ZZ_TRANS/ZZ_ROWMAP/ZZ_ACTION/ZZ_ATTRIBUTE packed arrays
  found: State 4 (CODE_BLOCK, non-BOL) + space (char class 1) → state 30 (ZZ_ATTRIBUTE[30]=0, NON-accepting). State 30 + any char → state 51 (ZZ_ATTRIBUTE[51]=1, accepting, ZZ_ACTION[51]=19)
  implication: Single space at EOF in CODE_BLOCK hits non-accepting state 30, then ZZ_NO_MATCH is thrown

- timestamp: 2026-02-21T00:00:00Z
  checked: JFlex DFA action case 19
  found: case 19 = "lookahead expression with fixed base length; zzMarkedPos = Character.offsetByCodePoints(zzBufferL, zzMarkedPos, -1)" — backup from zzMarkedPos=0 → IndexOutOfBoundsException
  implication: When zzMarkedPos=0 (single char at start of input segment), backing up by 1 gives -1, causing IndexOutOfBoundsException

- timestamp: 2026-02-21T00:00:00Z
  checked: FlexAdapter.class (decompiled from PyCharm JARs)
  found: locateToken() catches Throwable (not just Exception) from flex.advance(); on Throwable sets myFailed=true, myTokenType=BAD_CHARACTER, myTokenEnd=myBufferEnd
  implication: ZZ_NO_MATCH Error from zzScanError() is caught, returns BAD_CHARACTER with end=bufferEnd; then some downstream code does index arithmetic on -1

- timestamp: 2026-02-21T00:00:00Z
  checked: MakoLexer.flex CODE_BLOCK, MODULE_BLOCK, DOC_COMMENT rules
  found: Combined rule `[^%]+ | "%" / [^>]` — JFlex cannot determine whether to use `[^%]+` or the lookahead alternative until it sees a second character, so a single non-% char at EOF leaves the DFA in a non-accepting intermediate state
  implication: The fix is to separate the rules: `[^%]+` on its own line, then `"%" / [^>]` on its own line

## Resolution

root_cause: |
  In MakoLexer.flex, the CODE_BLOCK rule `[^%]+ | "%" / [^>]` causes JFlex to generate a
  combined DFA where a single non-`%` character at EOF (e.g., a space) hits a non-accepting
  intermediate DFA state. When JFlex cannot advance further (EOF) from a non-accepting state,
  it calls zzScanError(ZZ_NO_MATCH), throwing java.lang.Error("Error: could not match input").

  IntelliJ's FlexAdapter.locateToken() catches this Throwable and returns BAD_CHARACTER with
  myTokenEnd = myBufferEnd. Some downstream IDE code then computes an offset from this BAD_CHARACTER
  token, arriving at -1, causing IndexOutOfBoundsException.

  The root is in the JFlex grammar: the combined alternative rule forces a lookahead DFA that
  needs a second character to confirm `[^%]+` acceptance. The same issue existed in MODULE_BLOCK
  and DOC_COMMENT states with analogous combined rules.

fix: |
  Split combined rules into separate rules in all three affected states:
  - CODE_BLOCK: `[^%]+ | "%" / [^>]` → two rules: `[^%]+` then `"%" / [^>]`
  - MODULE_BLOCK: same split applied
  - DOC_COMMENT: `[^<]+ | "<" / [^/]` → two rules: `[^<]+` then `"<" / [^/]`

  After splitting, `[^%]+` gets its own simple accepting DFA state — a single non-% char at EOF
  immediately reaches an accepting state (CODE_CONTENT) with no lookahead needed.

  Regenerated _MakoLexer.java via `./gradlew generateMakoLexer`.
  Added regression tests: `testCodeBlockWithOnlyOpen` (strengthened) and `testCodeBlockSingleCharContentAtEof`.

verification: |
  All 58 lexer tests pass including two new regression tests:
  - testCodeBlockWithOnlyOpen: verifies `<% ` produces CODE_CONTENT, not BAD_CHARACTER
  - testCodeBlockSingleCharContentAtEof: verifies single trailing content chars produce CODE_CONTENT

files_changed:
  - src/main/grammars/MakoLexer.flex
  - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java
  - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerTest.kt

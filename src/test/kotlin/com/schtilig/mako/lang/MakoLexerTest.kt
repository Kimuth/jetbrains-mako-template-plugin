package com.schtilig.mako.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Lexer tests verifying correct tokenization for all Mako construct types.
 *
 * Covers PARS-01 (all construct types), PARS-02 (nested braces), PARS-03 (restart state).
 * Uses MakoLexerAdapter directly — no IntelliJ platform wiring required.
 */
class MakoLexerTest : BasePlatformTestCase() {

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = MakoLexerAdapter()
        lexer.start(text)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            tokens.add(lexer.tokenType!! to lexer.tokenText)
            lexer.advance()
        }
        return tokens
    }

    private fun tokenTypes(text: String): List<IElementType> = tokenize(text).map { it.first }

    private fun assertNoBADCharacter(tokens: List<Pair<IElementType, String>>) {
        val bad = tokens.filter { it.first == TokenType.BAD_CHARACTER }
        assertTrue("Unexpected BAD_CHARACTER tokens: $bad", bad.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // 1. Template text — plain HTML should produce only TEMPLATE_TEXT (and whitespace)
    // ---------------------------------------------------------------------------

    fun testTemplateTextSimple() {
        // Simple non-special text produces TEMPLATE_TEXT only
        val tokens = tokenize("Hello world")
        assertNoBADCharacter(tokens)
        assertTrue("Expected all TEMPLATE_TEXT", tokens.all { it.first == MakoTokenTypes.TEMPLATE_TEXT })
    }

    fun testTemplateTextHtml() {
        // HTML content — < and > are individually tokenized as TEMPLATE_TEXT by the fallback rules
        val tokens = tokenize("<html>Hello</html>")
        assertNoBADCharacter(tokens)
        val types = tokens.map { it.first }.toSet()
        assertEquals("Should only contain TEMPLATE_TEXT", setOf(MakoTokenTypes.TEMPLATE_TEXT), types)
    }

    // ---------------------------------------------------------------------------
    // 2. Expression basic: ${x}
    // ---------------------------------------------------------------------------

    fun testExpressionBasic() {
        val tokens = tokenize("\${x}")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("\${x}")
        assertTrue("Must start with EXPR_START", types.first() == MakoTokenTypes.EXPR_START)
        assertTrue("Must end with EXPR_END", types.last() == MakoTokenTypes.EXPR_END)
        // No FILTER_SEP in plain expression
        assertFalse("Should not contain FILTER_SEP", MakoTokenTypes.FILTER_SEP in types)
    }

    // ---------------------------------------------------------------------------
    // 3. Expression with filter: ${x | h,trim} — PARS-01 filter disambiguation
    // ---------------------------------------------------------------------------

    fun testExpressionWithFilter() {
        val tokens = tokenize("\${x | h,trim}")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("\${x | h,trim}")

        assertEquals("Must start with EXPR_START", MakoTokenTypes.EXPR_START, types.first())
        assertEquals("Must end with EXPR_END", MakoTokenTypes.EXPR_END, types.last())
        assertTrue("Must contain FILTER_SEP for the | character",
            MakoTokenTypes.FILTER_SEP in types)
    }

    // ---------------------------------------------------------------------------
    // 4. Nested braces (PARS-02): ${{'key': 'val'}} — EXPR_END only at outermost }
    // ---------------------------------------------------------------------------

    fun testNestedBraces() {
        val tokens = tokenize("\${{'key': 'val'}}")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("\${{'key': 'val'}}")

        assertEquals("Must start with EXPR_START", MakoTokenTypes.EXPR_START, types.first())
        assertEquals("Must end with EXPR_END", MakoTokenTypes.EXPR_END, types.last())

        // Count EXPR_END occurrences — must be exactly 1 (only the outermost })
        val exprEndCount = types.count { it == MakoTokenTypes.EXPR_END }
        assertEquals("EXPR_END must appear exactly once (not for inner braces)", 1, exprEndCount)
    }

    // ---------------------------------------------------------------------------
    // 5. Control line: % for i in items:
    // ---------------------------------------------------------------------------

    fun testControlLine() {
        val tokens = tokenize("% for i in items:")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("% for i in items:")
        assertTrue("Must contain CONTROL_LINE", MakoTokenTypes.CONTROL_LINE in types)
        // Entire line is one CONTROL_LINE token
        assertEquals("Control line must be a single token", 1, types.size)
    }

    // ---------------------------------------------------------------------------
    // 6. Escaped percent: %% at line start → TEMPLATE_TEXT
    // ---------------------------------------------------------------------------

    fun testEscapedPercent() {
        val tokens = tokenize("%% not a control line")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("%% not a control line")
        assertFalse("Escaped %% must NOT produce CONTROL_LINE", MakoTokenTypes.CONTROL_LINE in types)
        // All tokens should be TEMPLATE_TEXT
        assertTrue("Escaped %% produces TEMPLATE_TEXT",
            types.all { it == MakoTokenTypes.TEMPLATE_TEXT })
    }

    // ---------------------------------------------------------------------------
    // 7. Line comment: ## this is a comment
    // ---------------------------------------------------------------------------

    fun testLineComment() {
        val tokens = tokenize("## this is a comment")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("## this is a comment")
        assertTrue("Must contain LINE_COMMENT", MakoTokenTypes.LINE_COMMENT in types)
        assertEquals("Line comment must be a single token", 1, types.size)
    }

    // ---------------------------------------------------------------------------
    // 8. Doc comment: <%doc>comment body</%doc>
    // ---------------------------------------------------------------------------

    fun testDocComment() {
        val tokens = tokenize("<%doc>comment body</%doc>")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("<%doc>comment body</%doc>")
        assertTrue("Must contain DOC_OPEN", MakoTokenTypes.DOC_OPEN in types)
        assertTrue("Must contain DOC_CONTENT", MakoTokenTypes.DOC_CONTENT in types)
        assertTrue("Must contain DOC_CLOSE", MakoTokenTypes.DOC_CLOSE in types)
        // Order must be DOC_OPEN ... DOC_CONTENT ... DOC_CLOSE
        val openIdx = types.indexOf(MakoTokenTypes.DOC_OPEN)
        val closeIdx = types.lastIndexOf(MakoTokenTypes.DOC_CLOSE)
        assertTrue("DOC_OPEN must precede DOC_CLOSE", openIdx < closeIdx)
    }

    // ---------------------------------------------------------------------------
    // 9. Named tag: <%def name="foo"> → TAG_OPEN_DEF, TAG_ATTR_NAME, TAG_ATTR_EQ, TAG_ATTR_VALUE, TAG_CLOSE
    // ---------------------------------------------------------------------------

    fun testNamedTag() {
        val tokens = tokenize("<%def name=\"foo\">")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("<%def name=\"foo\">")
        assertTrue("Must contain TAG_OPEN_DEF", MakoTokenTypes.TAG_OPEN_DEF in types)
        assertTrue("Must contain TAG_ATTR_NAME", MakoTokenTypes.TAG_ATTR_NAME in types)
        assertTrue("Must contain TAG_ATTR_EQ", MakoTokenTypes.TAG_ATTR_EQ in types)
        assertTrue("Must contain TAG_ATTR_VALUE", MakoTokenTypes.TAG_ATTR_VALUE in types)
        assertTrue("Must contain TAG_CLOSE", MakoTokenTypes.TAG_CLOSE in types)
        // TAG_OPEN_DEF must come first, TAG_CLOSE must come last
        assertEquals("TAG_OPEN_DEF must be first token", MakoTokenTypes.TAG_OPEN_DEF, types.first())
        assertEquals("TAG_CLOSE must be last token", MakoTokenTypes.TAG_CLOSE, types.last())
    }

    // ---------------------------------------------------------------------------
    // 9b. Per-tag token types: each tag keyword emits its specific TAG_OPEN_xxx token
    // ---------------------------------------------------------------------------

    fun testTagOpenTypes() {
        assertEquals(MakoTokenTypes.TAG_OPEN_DEF, tokenTypes("<%def name=\"x\">").first())
        assertEquals(MakoTokenTypes.TAG_OPEN_BLOCK, tokenTypes("<%block name=\"x\">").first())
        assertEquals(MakoTokenTypes.TAG_OPEN_INHERIT, tokenTypes("<%inherit file=\"base.mako\"/>").first())
        assertEquals(MakoTokenTypes.TAG_OPEN_INCLUDE, tokenTypes("<%include file=\"header.mako\"/>").first())
        assertEquals(MakoTokenTypes.TAG_OPEN_NAMESPACE, tokenTypes("<%namespace name=\"util\" file=\"util.mako\"/>").first())
        assertEquals(MakoTokenTypes.TAG_OPEN_PAGE, tokenTypes("<%page expression_filter=\"h\"/>").first())
    }

    // ---------------------------------------------------------------------------
    // 10. End tag: </%def>
    // ---------------------------------------------------------------------------

    fun testEndTag() {
        val tokens = tokenize("</%def>")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("</%def>")
        assertEquals("Must be exactly one END_TAG token", listOf(MakoTokenTypes.END_TAG), types)
    }

    // ---------------------------------------------------------------------------
    // 11. Code block: <% x = 1 %>
    // ---------------------------------------------------------------------------

    fun testCodeBlock() {
        val tokens = tokenize("<% x = 1 %>")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("<% x = 1 %>")
        assertTrue("Must contain CODE_OPEN", MakoTokenTypes.CODE_OPEN in types)
        assertTrue("Must contain CODE_CONTENT", MakoTokenTypes.CODE_CONTENT in types)
        assertTrue("Must contain CODE_CLOSE", MakoTokenTypes.CODE_CLOSE in types)
        assertEquals("CODE_OPEN must be first", MakoTokenTypes.CODE_OPEN, types.first())
        assertEquals("CODE_CLOSE must be last", MakoTokenTypes.CODE_CLOSE, types.last())
    }

    // ---------------------------------------------------------------------------
    // 12. Module block: <%! import os %>
    // ---------------------------------------------------------------------------

    fun testModuleBlock() {
        val tokens = tokenize("<%! import os %>")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("<%! import os %>")
        assertTrue("Must contain MODULE_OPEN", MakoTokenTypes.MODULE_OPEN in types)
        assertTrue("Must contain MODULE_CONTENT", MakoTokenTypes.MODULE_CONTENT in types)
        assertTrue("Must contain CODE_CLOSE (end of module block)", MakoTokenTypes.CODE_CLOSE in types)
        assertEquals("MODULE_OPEN must be first", MakoTokenTypes.MODULE_OPEN, types.first())
        assertEquals("CODE_CLOSE must be last", MakoTokenTypes.CODE_CLOSE, types.last())
    }

    // ---------------------------------------------------------------------------
    // 13. Boolean OR vs filter: ${a || b} — || must NOT produce FILTER_SEP
    // ---------------------------------------------------------------------------

    fun testBooleanOrNotFilterSep() {
        val tokens = tokenize("\${a || b}")
        assertNoBADCharacter(tokens)
        val types = tokenTypes("\${a || b}")

        assertEquals("Must start with EXPR_START", MakoTokenTypes.EXPR_START, types.first())
        assertEquals("Must end with EXPR_END", MakoTokenTypes.EXPR_END, types.last())
        assertFalse("|| must NOT produce FILTER_SEP", MakoTokenTypes.FILTER_SEP in types)
    }

    // ---------------------------------------------------------------------------
    // 14. Restart semantics (PARS-03): after expression, state returns to 0
    // ---------------------------------------------------------------------------

    fun testRestartStateAfterExpression() {
        val lexer = MakoLexerAdapter()
        val text = "\${x} more text"
        lexer.start(text)

        // Consume tokens until we have passed EXPR_END
        var seenExprEnd = false
        while (lexer.tokenType != null) {
            if (lexer.tokenType == MakoTokenTypes.EXPR_END) {
                lexer.advance()
                seenExprEnd = true
                break
            }
            lexer.advance()
        }

        assertTrue("Must have encountered EXPR_END", seenExprEnd)
        // After EXPR_END, lexer must be in state 0 (YYINITIAL)
        assertEquals("Lexer state must be 0 (YYINITIAL) after expression ends", 0, lexer.state)
    }

    fun testRestartStateAfterCodeBlock() {
        val lexer = MakoLexerAdapter()
        val text = "<% x = 1 %> more"
        lexer.start(text)

        var seenCodeClose = false
        while (lexer.tokenType != null) {
            if (lexer.tokenType == MakoTokenTypes.CODE_CLOSE) {
                lexer.advance()
                seenCodeClose = true
                break
            }
            lexer.advance()
        }

        assertTrue("Must have encountered CODE_CLOSE", seenCodeClose)
        assertEquals("Lexer state must be 0 (YYINITIAL) after code block ends", 0, lexer.state)
    }

    fun testRestartStateAfterDocComment() {
        val lexer = MakoLexerAdapter()
        val text = "<%doc>body</%doc> more"
        lexer.start(text)

        var seenDocClose = false
        while (lexer.tokenType != null) {
            if (lexer.tokenType == MakoTokenTypes.DOC_CLOSE) {
                lexer.advance()
                seenDocClose = true
                break
            }
            lexer.advance()
        }

        assertTrue("Must have encountered DOC_CLOSE", seenDocClose)
        assertEquals("Lexer state must be 0 (YYINITIAL) after doc comment ends", 0, lexer.state)
    }

    // ---------------------------------------------------------------------------
    // 15. Multi-construct file — no BAD_CHARACTER across a mixed Mako file
    // ---------------------------------------------------------------------------

    fun testMixedMakoFile() {
        val text = """<%doc>Documentation</%doc>
<%! import os %>
<%def name="greet">
Hello ${'$'}{name | h}!
% for i in items:
    ${'$'}{i}
% endfor
</%def>"""
        val tokens = tokenize(text)
        assertNoBADCharacter(tokens)
    }

    // ---------------------------------------------------------------------------
    // 16. Unclosed constructs — lexer must not crash at EOF
    // ---------------------------------------------------------------------------

    fun testUnclosedCodeBlock() {
        // <%\nblabla = "foo" without closing %>
        val tokens = tokenize("<%\nblabla = \"foo\"")
        assertTrue("Must contain CODE_OPEN", tokens.any { it.first == MakoTokenTypes.CODE_OPEN })
        assertTrue("Must contain CODE_CONTENT", tokens.any { it.first == MakoTokenTypes.CODE_CONTENT })
        assertFalse("Must NOT contain CODE_CLOSE", tokens.any { it.first == MakoTokenTypes.CODE_CLOSE })
    }

    fun testUnclosedModuleBlock() {
        // <%!\nimport os without closing %>
        val tokens = tokenize("<%!\nimport os")
        assertTrue("Must contain MODULE_OPEN", tokens.any { it.first == MakoTokenTypes.MODULE_OPEN })
        assertTrue("Must contain MODULE_CONTENT", tokens.any { it.first == MakoTokenTypes.MODULE_CONTENT })
        assertFalse("Must NOT contain CODE_CLOSE", tokens.any { it.first == MakoTokenTypes.CODE_CLOSE })
    }

    fun testUnclosedExpression() {
        // ${foo without closing }
        val tokens = tokenize("\${foo")
        assertTrue("Must contain EXPR_START", tokens.any { it.first == MakoTokenTypes.EXPR_START })
        assertTrue("Must contain EXPR_CONTENT", tokens.any { it.first == MakoTokenTypes.EXPR_CONTENT })
        assertFalse("Must NOT contain EXPR_END", tokens.any { it.first == MakoTokenTypes.EXPR_END })
    }

    fun testUnclosedDocComment() {
        // <%doc>comment body without closing </%doc>
        val tokens = tokenize("<%doc>comment body")
        assertTrue("Must contain DOC_OPEN", tokens.any { it.first == MakoTokenTypes.DOC_OPEN })
        assertTrue("Must contain DOC_CONTENT", tokens.any { it.first == MakoTokenTypes.DOC_CONTENT })
        assertFalse("Must NOT contain DOC_CLOSE", tokens.any { it.first == MakoTokenTypes.DOC_CLOSE })
    }

    fun testUnclosedTagAttrs() {
        // <%def name="foo without closing >
        val tokens = tokenize("<%def name=\"foo")
        assertTrue("Must contain TAG_OPEN_DEF", tokens.any { it.first == MakoTokenTypes.TAG_OPEN_DEF })
        assertFalse("Must NOT contain TAG_CLOSE", tokens.any { it.first == MakoTokenTypes.TAG_CLOSE })
    }

    fun testCodeBlockWithOnlyOpen() {
        // Regression test for IndexOutOfBoundsException: `<% ` (space after CODE_OPEN) at EOF.
        //
        // The JFlex combined rule `[^%]+ | "%" / [^>]` generated a DFA where a single non-`%`
        // char at EOF hit a non-accepting intermediate state, causing ZZ_NO_MATCH (then
        // FlexAdapter caught the Error and returned BAD_CHARACTER with myTokenEnd=bufferEnd).
        // Fix: split into two separate rules so `[^%]+` gets its own simple accepting DFA.
        val tokens = tokenize("<% ")
        assertTrue("Must contain CODE_OPEN", tokens.any { it.first == MakoTokenTypes.CODE_OPEN })
        // The space must tokenize as CODE_CONTENT, never as BAD_CHARACTER.
        assertFalse(
            "Space inside CODE_BLOCK must NOT produce BAD_CHARACTER (was crashing with IndexOutOfBoundsException)",
            tokens.any { it.first == TokenType.BAD_CHARACTER }
        )
        assertTrue("Space inside CODE_BLOCK must produce CODE_CONTENT", tokens.any { it.first == MakoTokenTypes.CODE_CONTENT })
    }

    fun testCodeBlockSingleCharContentAtEof() {
        // Regression: single non-% character at EOF inside CODE_BLOCK must not throw.
        // The CODE_OPEN rule requires `<%` followed by whitespace/newline, so we put a
        // valid CODE_OPEN first (`<% `) and then a trailing content char at EOF.
        // The critical case is a single trailing char that follows CODE_OPEN with no `%>`.
        for (ch in listOf("a", "x", "z", "\t", "!")) {
            val input = "<% $ch"
            val tokens = tokenize(input)
            assertFalse(
                "Single char '$ch' at EOF inside CODE_BLOCK must NOT produce BAD_CHARACTER (input: '$input')",
                tokens.any { it.first == TokenType.BAD_CHARACTER }
            )
            assertTrue(
                "Single char '$ch' at EOF inside CODE_BLOCK must produce CODE_CONTENT (input: '$input')",
                tokens.any { it.first == MakoTokenTypes.CODE_CONTENT }
            )
        }
    }

    fun testCodeBlockPercentAtEof() {
        // Code block ending with lone % at EOF
        val tokens = tokenize("<% foo %")
        assertTrue("Must contain CODE_OPEN", tokens.any { it.first == MakoTokenTypes.CODE_OPEN })
        assertTrue("Must contain CODE_CONTENT", tokens.any { it.first == MakoTokenTypes.CODE_CONTENT })
    }

    // ---------------------------------------------------------------------------
    // 17. braceDepth encoding — state preserves nesting depth for incremental re-lex
    // ---------------------------------------------------------------------------

    fun testBraceDepthEncodedInState() {
        val lexer = MakoLexerAdapter()
        val text = "\${{'key': 'val'}}"
        lexer.start(text)

        // Advance past EXPR_START
        assertEquals(MakoTokenTypes.EXPR_START, lexer.tokenType)
        lexer.advance()

        // Now inside expression with nested braces — state should encode braceDepth
        // After the inner { the braceDepth should be 1, encoded in bits 4+
        // Advance through tokens until we hit the first inner {
        while (lexer.tokenType != null) {
            val state = lexer.state
            val jflexState = state and 0xF
            val depth = (state ushr 4) and 0xF

            // In EXPRESSION state (JFlex state 2), depth should reflect nesting
            if (jflexState == 2 && lexer.tokenText == "{") {
                // After consuming {, braceDepth was incremented
                assertTrue("braceDepth should be > 0 after inner {", depth > 0)
                break
            }
            lexer.advance()
        }
    }

    fun testBraceDepthRestoredOnRestart() {
        val lexer = MakoLexerAdapter()
        val text = "\${{'key': 'val'}}"
        lexer.start(text)

        // Advance past EXPR_START to get into EXPRESSION state with braceDepth > 0
        lexer.advance() // past EXPR_START

        // Find the position after the inner { where braceDepth > 0
        var savedState = -1
        var savedOffset = -1
        while (lexer.tokenType != null) {
            if (lexer.tokenText == "{") {
                lexer.advance() // consume the {
                savedState = lexer.state
                savedOffset = lexer.tokenStart
                break
            }
            lexer.advance()
        }

        assertTrue("Should have found inner { and saved state", savedState > 0)

        // Restart lexer from saved state — simulates incremental re-lexing
        lexer.start(text, savedOffset, text.length, savedState)

        // The restarted lexer should correctly handle the remaining tokens
        // without prematurely closing the expression at the inner }
        val remainingTokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            remainingTokens.add(lexer.tokenType!! to lexer.tokenText)
            lexer.advance()
        }

        // The inner } should be EXPR_CONTENT (not EXPR_END) because braceDepth was restored
        val innerClose = remainingTokens.find { it.second == "}" }
        assertNotNull("Should find a } token", innerClose)

        // Should have exactly one EXPR_END at the outermost }
        val exprEndCount = remainingTokens.count { it.first == MakoTokenTypes.EXPR_END }
        assertEquals("EXPR_END must appear exactly once (outermost })", 1, exprEndCount)
    }
}

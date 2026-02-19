package com.github.kimuth.jetbrainsmakotemplateplugin.lang

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
}

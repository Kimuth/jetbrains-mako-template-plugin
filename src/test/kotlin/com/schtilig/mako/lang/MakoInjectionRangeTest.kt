package com.schtilig.mako.lang

import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Lexer-level tests confirming the token structure that MakoPythonInjector relies on
 * when walking ASTNode children to determine the Python injection range.
 *
 * The injector's range logic depends on FILTER_SEP token position, which is fully
 * determined by the lexer. These tests validate that:
 *   - FILTER_SEP appears in filtered expressions (e.g., ${x | h, trim})
 *   - FILTER_SEP is absent in plain expressions (e.g., ${x})
 *   - FILTER_SEP position is after the Python expression and before the closing }
 *   - Boolean OR (||) does NOT produce FILTER_SEP
 *
 * Covers requirement INJECT-02.
 */
class MakoInjectionRangeTest : BasePlatformTestCase() {

    // ---------------------------------------------------------------------------
    // Helper — mirrors the private tokenize() helper in MakoLexerTest
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

    // ---------------------------------------------------------------------------
    // Test 1: ${x | h, trim} must have FILTER_SEP after EXPR_CONTENT and before EXPR_END
    // ---------------------------------------------------------------------------

    fun testExpressionWithFilterHasFilterSepInTree() {
        val tokens = tokenize("\${x | h, trim}")

        // FILTER_SEP must be present
        assertTrue(
            "FILTER_SEP must appear in token stream for '\${x | h, trim}'",
            tokens.any { it.first == MakoTokenTypes.FILTER_SEP }
        )

        val types = tokens.map { it.first }
        val filterSepIdx = types.indexOf(MakoTokenTypes.FILTER_SEP)
        assertTrue("FILTER_SEP index must be > 0", filterSepIdx > 0)

        // FILTER_SEP must come AFTER EXPR_START (index 0)
        val exprStartIdx = types.indexOf(MakoTokenTypes.EXPR_START)
        assertTrue("FILTER_SEP must come after EXPR_START", filterSepIdx > exprStartIdx)

        // FILTER_SEP must come BEFORE EXPR_END
        val exprEndIdx = types.lastIndexOf(MakoTokenTypes.EXPR_END)
        assertTrue("FILTER_SEP must come before EXPR_END", filterSepIdx < exprEndIdx)

        // EXPR_CONTENT for "x " must appear before FILTER_SEP
        val exprContentIdx = types.indexOf(MakoTokenTypes.EXPR_CONTENT)
        assertTrue(
            "EXPR_CONTENT for 'x ' must appear before FILTER_SEP",
            exprContentIdx in 0 until filterSepIdx
        )
    }

    // ---------------------------------------------------------------------------
    // Test 2: ${x} must NOT have FILTER_SEP
    // ---------------------------------------------------------------------------

    fun testExpressionWithoutFilterHasNoFilterSep() {
        val types = tokenTypes("\${x}")

        assertFalse(
            "FILTER_SEP must NOT appear for plain expression '\${x}'",
            MakoTokenTypes.FILTER_SEP in types
        )
        assertTrue("Must start with EXPR_START", types.first() == MakoTokenTypes.EXPR_START)
        assertTrue("Must end with EXPR_END", types.last() == MakoTokenTypes.EXPR_END)
    }

    // ---------------------------------------------------------------------------
    // Test 3: FILTER_SEP offset is after the Python expression and before EXPR_END
    // ---------------------------------------------------------------------------

    fun testFilterSepOffsetIsBeforeFilterName() {
        val input = "\${x | h}"
        val tokens = tokenize(input)

        val filterSepToken = tokens.find { it.first == MakoTokenTypes.FILTER_SEP }
        assertNotNull("FILTER_SEP must be present in '\${x | h}'", filterSepToken)

        // Compute cumulative character offset of FILTER_SEP from the start of input
        var offset = 0
        for ((type, text) in tokens) {
            if (type == MakoTokenTypes.FILTER_SEP) break
            offset += text.length
        }

        // Offset must be > 2 (past "${") — the Python expression starts after "${", so
        // the filter separator must appear somewhere after the 2-char prefix.
        assertTrue("FILTER_SEP offset ($offset) must be > 2", offset > 2)

        // Offset must be < (input.length - 1) — before the closing "}"
        assertTrue(
            "FILTER_SEP offset ($offset) must be before closing } at ${input.length - 1}",
            offset < input.length - 1
        )

        // Text from offset onward must NOT start with "x" (the Python expression is before it)
        val textFromFilterSep = input.substring(offset)
        assertFalse(
            "Text from FILTER_SEP offset must not start with 'x' (the Python expr precedes it)",
            textFromFilterSep.startsWith("x")
        )
    }

    // ---------------------------------------------------------------------------
    // Test 4: ${a || b} must NOT produce FILTER_SEP (boolean OR disambiguation)
    // ---------------------------------------------------------------------------

    fun testBooleanOrDoesNotProduceFilterSep() {
        val types = tokenTypes("\${a || b}")

        assertFalse(
            "|| in '\${a || b}' must NOT produce FILTER_SEP",
            MakoTokenTypes.FILTER_SEP in types
        )
        assertTrue("Must start with EXPR_START", types.first() == MakoTokenTypes.EXPR_START)
        assertTrue("Must end with EXPR_END", types.last() == MakoTokenTypes.EXPR_END)
    }
}

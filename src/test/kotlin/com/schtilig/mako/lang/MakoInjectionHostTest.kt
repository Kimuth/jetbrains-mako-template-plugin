package com.schtilig.mako.lang

import com.schtilig.mako.lang.psi.MakoCodeBlock
import com.schtilig.mako.lang.psi.MakoExpression
import com.schtilig.mako.lang.psi.MakoModuleBlock
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase

/**
 * Unit tests for PsiLanguageInjectionHost.updateText() contracts on Mako injection host mixins.
 *
 * INJECT-01 — updateText() must throw UnsupportedOperationException on all three
 *             injection host mixin types (Expression, CodeBlock, ModuleBlock).
 *             The old implementation silently returned `this`, which is misleading.
 *             This matches the setName() precedent from Phase 10.
 */
class MakoInjectionHostTest : ParsingTestCase("injection", "mako", MakoParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"

    /**
     * INJECT-01a — MakoExpressionMixin.updateText() must throw UnsupportedOperationException.
     * Parse: "${x}", retrieve MakoExpression, cast to PsiLanguageInjectionHost, call updateText("y").
     */
    fun testExpressionUpdateTextThrows() {
        val file = parseFile("expressionUpdateText", "\${x}")
        val expression = PsiTreeUtil.findChildOfType(file, MakoExpression::class.java)
        assertNotNull("Should have a MakoExpression", expression)
        val host = expression as PsiLanguageInjectionHost
        try {
            host.updateText("y")
            fail("updateText() should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            // Expected — test passes
        }
    }

    /**
     * INJECT-01b — MakoCodeBlockMixin.updateText() must throw UnsupportedOperationException.
     * Parse: "<% x = 1 %>", retrieve MakoCodeBlock, cast to PsiLanguageInjectionHost, call updateText("y = 2").
     */
    fun testCodeBlockUpdateTextThrows() {
        val file = parseFile("codeBlockUpdateText", "<% x = 1 %>")
        val codeBlock = PsiTreeUtil.findChildOfType(file, MakoCodeBlock::class.java)
        assertNotNull("Should have a MakoCodeBlock", codeBlock)
        val host = codeBlock as PsiLanguageInjectionHost
        try {
            host.updateText("y = 2")
            fail("updateText() should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            // Expected — test passes
        }
    }

    /**
     * INJECT-01c — MakoModuleBlockMixin.updateText() must throw UnsupportedOperationException.
     * Parse: "<%! import os %>", retrieve MakoModuleBlock, cast to PsiLanguageInjectionHost, call updateText("import sys").
     */
    fun testModuleBlockUpdateTextThrows() {
        val file = parseFile("moduleBlockUpdateText", "<%! import os %>")
        val moduleBlock = PsiTreeUtil.findChildOfType(file, MakoModuleBlock::class.java)
        assertNotNull("Should have a MakoModuleBlock", moduleBlock)
        val host = moduleBlock as PsiLanguageInjectionHost
        try {
            host.updateText("import sys")
            fail("updateText() should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            // Expected — test passes
        }
    }

    /**
     * Verifies that PsiTreeUtil.findChildrenOfType recurses into nested <%def> blocks,
     * so collectCodeAndExpressionHosts() will find code blocks at all nesting levels.
     * This is the prerequisite for cross-block variable resolution working correctly.
     */
    fun testCollectedHostsIncludesDefLevelBlock() {
        val src = """
            <%
            my_list = [1, 2, 3]
            %>
            <%def name="my_func()">
            <%
            for i in my_list:
                pass
            %>
            </%def>
        """.trimIndent()
        val file = parseFile("collectHosts", src)
        val codeBlocks = PsiTreeUtil.findChildrenOfType(file, MakoCodeBlock::class.java)
        assertEquals(
            "Should find both the top-level and the def-level <% %> blocks",
            2, codeBlocks.size
        )
    }
}

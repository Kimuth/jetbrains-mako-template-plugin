package com.github.kimuth.jetbrainsmakotemplateplugin.lang

import com.intellij.testFramework.ParsingTestCase

/**
 * Parser integration tests using fixture-based PSI tree comparison.
 *
 * Each test method name maps to a test data file pair:
 * - src/test/testData/parser/{TestName}.mako (input)
 * - src/test/testData/parser/{TestName}.txt (expected PSI tree, auto-generated on first run)
 *
 * PARS-04: testWellFormedFile verifies distinct typed nodes for all Mako constructs.
 * PARS-05: testMalformedTag verifies partial PSI tree on malformed input.
 */
class MakoParsingTest : ParsingTestCase("", "mako", MakoParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData/parser"

    override fun skipSpaces(): Boolean = false

    override fun includeRanges(): Boolean = true

    /**
     * PARS-04: Well-formed file produces typed PSI nodes for every Mako construct.
     * On first run, this generates the expected .txt file. Review the generated .txt
     * to confirm it contains: MakoDefTag, MakoBlockTag, MakoInheritTag, MakoIncludeTag,
     * MakoNamespaceTag, MakoExpression, MakoControlLine, MakoCodeBlock, MakoModuleBlock,
     * MakoDocComment, MakoLineCommentRule nodes.
     */
    fun testWellFormedFile() {
        doTest(true)
    }

    /**
     * PARS-05: Malformed tag produces a partial PSI tree -- error node for the broken tag,
     * but subsequent constructs parse correctly as typed nodes.
     */
    fun testMalformedTag() {
        doTest(true)
    }

    /**
     * PARS-06: Two consecutive expressions on separate lines parse as two sibling
     * MakoExpressionImpl nodes with no PSI errors.
     *
     * Regression test for the bug where the second `${` was consumed inside the first
     * MakoExpressionImpl because `expression_recover` was missing EXPR_START in its
     * stop-token set. GrammarKit's recoverWhile runs even on successful matches, so
     * the recovery loop would greedily consume the second expression start.
     */
    fun testConsecutiveExpressions() {
        doTest(true)
    }
}

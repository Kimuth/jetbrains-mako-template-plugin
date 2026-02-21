package com.schtilig.mako.lang

import com.schtilig.mako.lang.folding.MakoFoldingBuilder
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.testFramework.ParsingTestCase

/**
 * Folding builder tests covering all 6 foldable construct types:
 *   def_tag, block_tag, doc_comment, module_block, code_block, and control flow (for/if/while).
 *
 * Extends ParsingTestCase with MakoParserDefinition so PsiFileFactory is properly set up.
 * Uses parseFile() to create Mako PSI files and calls MakoFoldingBuilder directly.
 */
class MakoFoldingTest : ParsingTestCase("folding", "mako", MakoParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"

    private val SAMPLE_MAKO = """<%doc>
This documentation should be collapsed by default.
</%doc>

<%!
    import os
%>

<%
    rows = []
%>

<%def name="greet">
Hello ${"$"}{name}!
</%def>

<%block name="header">
<h1>Title</h1>
</%block>

% for item in items:
    ${"$"}{item}
% endfor

% if show:
    <p>Visible</p>
% endif

% while count > 0:
    <p>Counting</p>
% endwhile"""

    private fun buildFolds(content: String): Int {
        val file = parseFile("test", content)
        val doc = DocumentImpl(content)
        val builder = MakoFoldingBuilder()
        return builder.buildFoldRegions(file, doc, false).size
    }

    /** All 8 fold regions: def, block, doc, module, code, for, if, while */
    fun testAllConstructsFolded() {
        val count = buildFolds(SAMPLE_MAKO)
        assertEquals("Expected 8 fold regions (def+block+doc+module+code+for+if+while)", 8, count)
    }

    /** Def tag combined with for loop */
    fun testDefWithForLoop() {
        val content = """<%def name="greet">
Hello!
</%def>

% for item in items:
    content
% endfor"""
        assertEquals("Expected 2 fold regions (def + for loop)", 2, buildFolds(content))
    }

    /** def_tag folds the entire <%def ...>...</%def> span */
    fun testDefTagFolded() {
        val content = """<%def name="greet">
Hello!
</%def>"""
        assertEquals("Expected 1 fold region for def_tag", 1, buildFolds(content))
    }

    /** block_tag folds the entire <%block ...>...</%block> span */
    fun testBlockTagFolded() {
        val content = """<%block name="header">
<h1>Title</h1>
</%block>"""
        assertEquals("Expected 1 fold region for block_tag", 1, buildFolds(content))
    }

    /** module_block folds <%! ... %> */
    fun testModuleBlockFolded() {
        val content = """<%!
    import os
%>"""
        assertEquals("Expected 1 fold region for module_block", 1, buildFolds(content))
    }

    /** code_block folds <% ... %> */
    fun testCodeBlockFolded() {
        val content = """<%
    rows = [[v for v in range(0,10)] for row in range(0,10)]
%>"""
        assertEquals("Expected 1 fold region for code_block", 1, buildFolds(content))
    }

    /** % for ... % endfor control flow folds */
    fun testForLoopFolded() {
        val content = """% for item in items:
    content
% endfor"""
        assertEquals("Expected 1 fold region for for loop", 1, buildFolds(content))
    }

    /** % if ... % endif control flow folds */
    fun testIfBlockFolded() {
        val content = """% if show:
    <p>Visible</p>
% endif"""
        assertEquals("Expected 1 fold region for if block", 1, buildFolds(content))
    }

    /** % while ... % endwhile control flow folds */
    fun testWhileLoopFolded() {
        val content = """% while count > 0:
    <p>Counting</p>
% endwhile"""
        assertEquals("Expected 1 fold region for while loop", 1, buildFolds(content))
    }

    /** code_block fold range does NOT bleed into subsequent template text */
    fun testCodeBlockFoldRange() {
        val content = """<%
    rows = []
%>
This text should NOT be inside the fold
% for item in items:
    content
% endfor"""
        val file = parseFile("codeblock_range", content)
        val doc = DocumentImpl(content)
        val builder = MakoFoldingBuilder()
        val regions = builder.buildFoldRegions(file, doc, false)

        // Should have 2 folds: code_block + for loop
        assertEquals("Expected 2 fold regions (code_block + for)", 2, regions.size)

        // Find the code_block fold (the one that starts at offset 0)
        val codeBlockFold = regions.find { it.range.startOffset == 0 }
        assertNotNull("Should have a fold starting at offset 0 (code_block)", codeBlockFold)

        // The code_block fold should end at %>  (offset of "%>" + 2)
        val closeIdx = content.indexOf("%>")
        assertEquals("code_block fold should end right after %>",
            closeIdx + 2, codeBlockFold!!.range.endOffset)
    }

    /** module_block fold range does NOT bleed into subsequent template text */
    fun testModuleBlockFoldRange() {
        val content = """<%!
    import os
%>
This text should NOT be inside the fold"""
        val file = parseFile("moduleblock_range", content)
        val doc = DocumentImpl(content)
        val builder = MakoFoldingBuilder()
        val regions = builder.buildFoldRegions(file, doc, false)

        assertEquals("Expected 1 fold region for module_block", 1, regions.size)

        val closeIdx = content.indexOf("%>")
        assertTrue("module_block fold should end at or before %>",
            regions[0].range.endOffset <= closeIdx + 2)
    }

    /** Malformed file with orphaned % endfor does not crash (empty stack guard) */
    fun testMalformedControlFlowNoCrash() {
        val content = """% endfor
some content
% endif"""
        assertEquals("Malformed file should produce 0 folds", 0, buildFolds(content))
    }

    // ---- TEMPLATE_TEXT-body regression tests (GAP-02 / SYNX-06) ----
    // These test cases previously produced incorrect fold regions because END_TAG was orphaned
    // at file level when the def/block body contained only TEMPLATE_TEXT tokens. The grammar
    // fix (template_text_content named rule) ensures END_TAG is consumed into the composite.

    /** Single <%def> with plain-text-only body should fold (regression: END_TAG was orphaned) */
    fun testDefWithPlainTextBodyFolds() {
        val content = "<%def name=\"foo\">\nHello world!\n</%def>"
        assertEquals("Expected 1 fold region for def with plain-text body", 1, buildFolds(content))
    }

    /** Single <%block> with plain-text-only body should fold (regression: END_TAG was orphaned) */
    fun testBlockWithPlainTextBodyFolds() {
        val content = "<%block name=\"header\">\n<h1>Title</h1>\n</%block>"
        assertEquals("Expected 1 fold region for block with plain-text body", 1, buildFolds(content))
    }

    /** Two consecutive <%def> tags with plain-text-only bodies should each fold.
     *  Regression: orphaned END_TAG caused second def to be parsed as flat file-level tokens. */
    fun testSiblingDefsWithPlainTextBodiesFold() {
        val content = "<%def name=\"foo\">\nHello!\n</%def>\n\n<%def name=\"bar\">\nWorld!\n</%def>"
        assertEquals("Expected 2 fold regions for sibling defs with plain-text bodies", 2, buildFolds(content))
    }

    /** <%def> followed by <%block>, both with plain-text-only bodies, should each fold.
     *  Regression: orphaned END_TAG caused second tag to be parsed as flat file-level tokens. */
    fun testSiblingDefAndBlockWithPlainTextBodiesFold() {
        val content = "<%def name=\"foo\">\nHello!\n</%def>\n\n<%block name=\"header\">\n<h1>Title</h1>\n</%block>"
        assertEquals("Expected 2 fold regions for sibling def+block with plain-text bodies", 2, buildFolds(content))
    }

    /** Fold range for <%def> with plain-text body must include the closing </%def> tag.
     *  Regression: fold endOffset excluded END_TAG when END_TAG was orphaned at file level. */
    fun testDefFoldRangeIncludesEndTag() {
        val content = "<%def name=\"foo\">\nHello world!\n</%def>"
        val file = parseFile("def_range", content)
        val doc = DocumentImpl(content)
        val builder = MakoFoldingBuilder()
        val regions = builder.buildFoldRegions(file, doc, false)

        assertEquals("Expected exactly 1 fold region", 1, regions.size)
        assertEquals("Fold endOffset must equal content.length (END_TAG included in DEF_TAG composite)",
            content.length, regions[0].range.endOffset)
    }
}

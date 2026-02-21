package com.schtilig.mako.lang

import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.schtilig.mako.lang.structure.MakoStructureViewElement
import com.schtilig.mako.lang.structure.MakoStructureViewModel
import com.intellij.testFramework.ParsingTestCase

/**
 * Structure view tests verifying that <%def> and <%block> declarations appear
 * as navigable tree nodes with correct names and hierarchy.
 *
 * Uses ParsingTestCase (same approach as MakoFoldingTest) so MakoFileType
 * is properly registered and the PSI tree is correctly built.
 *
 * Note: The GrammarKit parser flattens nested <%def> tags — both the outer and
 * inner def appear as file-level siblings. The structure view reflects the actual
 * PSI tree; nesting in the structure view requires nesting in the PSI, which is
 * a parser-level concern. Tests here verify what the structure view shows given
 * the actual parsed PSI tree.
 */
class MakoStructureViewTest : ParsingTestCase("structure", "mako", MakoParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"

    /**
     * Tests that:
     * - Root has exactly 2 top-level children: a "greet" def and a "header" block
     * - suitableClasses are correctly set (model implements ElementInfoProvider)
     */
    fun testStructureViewBasicNodes() {
        // Use minimal inline content (no TEMPLATE_TEXT inside tags) to avoid parser error recovery
        val content = "<%def name=\"greet\"></%def>\n<%block name=\"header\"></%block>"
        val file = parseFile("basic", content)
        val model = MakoStructureViewModel(null, file)

        // Verify the model implements ElementInfoProvider (which exposes getSuitableClasses)
        assertTrue(
            "MakoStructureViewModel must implement ElementInfoProvider",
            model is com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
        )

        // Verify root element children
        val root = model.root as MakoStructureViewElement
        val topChildren = root.children
        assertEquals("Root should have exactly 2 top-level children (greet def + header block)", 2, topChildren.size)

        // Find the "greet" def and "header" block by presentation text
        val greetNode = topChildren.find { it.presentation.presentableText == "greet" }
        val headerNode = topChildren.find { it.presentation.presentableText == "header" }

        assertNotNull("Root should have a child named 'greet'", greetNode)
        assertNotNull("Root should have a child named 'header'", headerNode)

        // Verify greet node has no nested structure view children
        // (it contains template text and expressions — not structured def/block children)
        val greetChildren = greetNode!!.children
        assertEquals("The 'greet' def should have 0 structure-view children (plain content only)", 0, greetChildren.size)

        // Verify "header" block has no children
        val headerChildren = headerNode!!.children
        assertEquals("The 'header' block should have 0 children", 0, headerChildren.size)
    }

    /**
     * Tests that multiple def and block nodes appear correctly.
     */
    fun testStructureViewMultipleNodes() {
        // Use minimal inline content to avoid parser error recovery issues with TEMPLATE_TEXT inside tags
        val content = "<%def name=\"greet\"></%def>\n<%def name=\"footer\"></%def>\n<%block name=\"header\"></%block>"
        val file = parseFile("multiple", content)
        val model = MakoStructureViewModel(null, file)
        val root = model.root as MakoStructureViewElement
        val children = root.children
        assertEquals("Should have 3 children: 2 defs + 1 block", 3, children.size)

        val names = children.map { it.presentation.presentableText }.toSet()
        assertTrue("Should contain 'greet'", names.contains("greet"))
        assertTrue("Should contain 'footer'", names.contains("footer"))
        assertTrue("Should contain 'header'", names.contains("header"))
    }

    /**
     * Tests that blocks without a name attribute use the "<unnamed>" fallback.
     */
    fun testUnnamedBlock() {
        // An unnamed block has no name= attribute
        val content = "<%block>\n<h2>Section</h2>\n</%block>"
        val file = parseFile("unnamed", content)
        val model = MakoStructureViewModel(null, file)
        val root = model.root as MakoStructureViewElement
        val children = root.children
        assertEquals("Should have 1 child for the unnamed block", 1, children.size)
        val blockNode = children[0]
        val text = blockNode.presentation.presentableText
        assertEquals("Unnamed block should display '<unnamed>'", "<unnamed>", text)
    }

    /**
     * Tests that defs and blocks appear interleaved in document order.
     * File order: def "first", block "second", def "third" must produce children
     * in that exact sequence in the structure view.
     */
    fun testChildrenInDocumentOrder() {
        val content = "<%def name=\"first\"></%def>\n<%block name=\"second\"></%block>\n<%def name=\"third\"></%def>"
        val file = parseFile("doc_order", content)
        val model = MakoStructureViewModel(null, file)
        val root = model.root as MakoStructureViewElement
        val children = root.children

        assertEquals("Should have 3 children", 3, children.size)
        assertEquals("First child should be 'first'", "first", children[0].presentation.presentableText)
        assertEquals("Second child should be 'second'", "second", children[1].presentation.presentableText)
        assertEquals("Third child should be 'third'", "third", children[2].presentation.presentableText)
    }

    /**
     * Tests that the fallback icon for a def node is AllIcons.Nodes.Function,
     * not MakoIcons.FILE.
     */
    fun testDefNodeUsesFunctionIcon() {
        val content = "<%def name=\"greet\"></%def>"
        val file = parseFile("icon_check", content)
        val model = MakoStructureViewModel(null, file)
        val root = model.root as MakoStructureViewElement
        val children = root.children

        assertEquals("Should have exactly 1 child", 1, children.size)
        val icon = children[0].presentation.getIcon(false)
        assertEquals(
            "Def node icon should be AllIcons.Nodes.Function",
            com.intellij.icons.AllIcons.Nodes.Function,
            icon
        )
    }

    /**
     * Tests that only def_tag and block_tag appear in the structure view —
     * template text, control flow, expressions and code blocks are excluded.
     */
    fun testOnlyDefAndBlockNodesAppear() {
        val content = """Hello template text
% for item in items:
    content
% endfor
<% x = 1 %>
<%def name="only_def">
Content
</%def>"""
        val file = parseFile("filtered", content)
        val model = MakoStructureViewModel(null, file)
        val root = model.root as MakoStructureViewElement
        val children = root.children
        assertEquals("Only the def node should appear in structure view", 1, children.size)
        assertEquals("The def node should be named 'only_def'", "only_def", children[0].presentation.presentableText)
    }
}

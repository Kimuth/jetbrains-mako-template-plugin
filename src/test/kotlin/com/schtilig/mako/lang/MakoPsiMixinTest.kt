package com.schtilig.mako.lang

import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase

/**
 * Unit tests for PSI mixin getName() and setName() contracts.
 *
 * PSI-01 — getName() must find the TAG_ATTR_NAME with text "name" and return its
 *           paired TAG_ATTR_VALUE, NOT simply the first TAG_ATTR_VALUE child.
 *           Covers the bug case: <%def args="()" name="foo"> returning "()" instead of "foo".
 *
 * PSI-02 — setName() must throw UnsupportedOperationException so callers receive a
 *           clear failure signal rather than a silent no-op.
 */
class MakoPsiMixinTest : ParsingTestCase("psi", "mako", MakoParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"

    // ---- PSI-01: getName() attribute pairing ----

    /**
     * <%def name="foo"> — name attribute is first (and only) attribute.
     * getName() must return "foo".
     */
    fun testDefGetNameNameFirst() {
        val content = """<%def name="foo"></%def>"""
        val file = parseFile("defNameFirst", content)
        val defTag = PsiTreeUtil.findChildOfType(file, MakoDefTag::class.java)
        assertNotNull("Should have a MakoDefTag", defTag)
        assertEquals("getName() should return 'foo'", "foo", defTag!!.name)
    }

    /**
     * <%def name="bar" args="()"> — name attribute is first, args is second.
     * getName() must return "bar" (not "()" which would be wrong).
     */
    fun testDefGetNameNameBeforeArgs() {
        val content = """<%def name="bar" args="()"></%def>"""
        val file = parseFile("defNameBeforeArgs", content)
        val defTag = PsiTreeUtil.findChildOfType(file, MakoDefTag::class.java)
        assertNotNull("Should have a MakoDefTag", defTag)
        assertEquals("getName() should return 'bar' when name comes before args", "bar", defTag!!.name)
    }

    /**
     * <%def args="()" name="foo"> — args attribute is first, name is second.
     * This is THE BUG CASE: the old findChildByType returns "()" instead of "foo".
     * getName() must return "foo".
     */
    fun testDefGetNameArgsBeforeName() {
        val content = """<%def args="()" name="foo"></%def>"""
        val file = parseFile("defArgsBeforeName", content)
        val defTag = PsiTreeUtil.findChildOfType(file, MakoDefTag::class.java)
        assertNotNull("Should have a MakoDefTag", defTag)
        assertEquals("getName() should return 'foo' even when args attribute appears first", "foo", defTag!!.name)
    }

    /**
     * <%block name="header"> — block tag with name attribute.
     * getName() must return "header".
     */
    fun testBlockGetNameWithNameAttr() {
        val content = """<%block name="header"></%block>"""
        val file = parseFile("blockWithName", content)
        val blockTag = PsiTreeUtil.findChildOfType(file, MakoBlockTag::class.java)
        assertNotNull("Should have a MakoBlockTag", blockTag)
        assertEquals("getName() should return 'header'", "header", blockTag!!.name)
    }

    /**
     * <%block> — block tag with NO name attribute.
     * getName() must return null (no name= attribute present).
     */
    fun testBlockGetNameNoNameAttr() {
        val content = "<%block>\n<h2>Section</h2>\n</%block>"
        val file = parseFile("blockNoName", content)
        val blockTag = PsiTreeUtil.findChildOfType(file, MakoBlockTag::class.java)
        assertNotNull("Should have a MakoBlockTag", blockTag)
        assertNull("getName() should return null when no name attribute exists", blockTag!!.name)
    }

    // ---- PSI-02: setName() throws UnsupportedOperationException ----

    /**
     * MakoDefTagMixin.setName() must throw UnsupportedOperationException.
     * The old implementation silently returned `this`, which is misleading.
     */
    fun testDefSetNameThrows() {
        val content = """<%def name="foo"></%def>"""
        val file = parseFile("defSetName", content)
        val defTag = PsiTreeUtil.findChildOfType(file, MakoDefTag::class.java)
        assertNotNull("Should have a MakoDefTag", defTag)
        try {
            defTag!!.setName("newName")
            fail("setName() should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            // Expected — test passes
        }
    }

    /**
     * MakoBlockTagMixin.setName() must throw UnsupportedOperationException.
     * The old implementation silently returned `this`, which is misleading.
     */
    fun testBlockSetNameThrows() {
        val content = """<%block name="header"></%block>"""
        val file = parseFile("blockSetName", content)
        val blockTag = PsiTreeUtil.findChildOfType(file, MakoBlockTag::class.java)
        assertNotNull("Should have a MakoBlockTag", blockTag)
        try {
            blockTag!!.setName("newName")
            fail("setName() should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            // Expected — test passes
        }
    }
}

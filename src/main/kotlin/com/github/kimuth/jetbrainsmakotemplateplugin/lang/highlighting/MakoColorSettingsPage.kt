package com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class MakoColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Directive (<%def>, <%block>, ...)", MakoSyntaxHighlighter.MAKO_DIRECTIVE),
            AttributesDescriptor("Expression (\${...})", MakoSyntaxHighlighter.MAKO_EXPRESSION),
            AttributesDescriptor("Control line (% for, % if, ...)", MakoSyntaxHighlighter.MAKO_CONTROL_LINE),
            AttributesDescriptor("Line comment (## ...)", MakoSyntaxHighlighter.MAKO_LINE_COMMENT),
            AttributesDescriptor("Block comment (<%doc>)", MakoSyntaxHighlighter.MAKO_BLOCK_COMMENT),
            AttributesDescriptor("Tag attribute name", MakoSyntaxHighlighter.MAKO_TAG_ATTR_NAME),
            AttributesDescriptor("Tag attribute value", MakoSyntaxHighlighter.MAKO_TAG_ATTR_VALUE),
            AttributesDescriptor("Tag close (%>, </%...>)", MakoSyntaxHighlighter.MAKO_TAG_CLOSE),
            AttributesDescriptor("Code block content", MakoSyntaxHighlighter.MAKO_CODE_CONTENT)
        )
    }

    override fun getIcon(): Icon = MakoIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = MakoSyntaxHighlighter()

    override fun getDisplayName(): String = "Mako"

    override fun getDemoText(): String = """
        ## Line comment: this line is a Mako comment
        <%doc>
            Block comment: multiple lines of documentation.
            Used for longer explanations.
        </%doc>
        <%def name="my_function(arg)">
            <p>Hello, ${'$'}{arg | h}!</p>
            % if arg:
                <span>Truthy value: ${'$'}{arg}</span>
            % endif
        </%def>
        <%block name="content">
            <h1>Page content goes here</h1>
        </%block>
        <%inherit file="base.html"/>
        <%include file="header.html"/>
        <%namespace name="helpers" file="helpers.html"/>
        <%page args="title='Default'"/>
        <%
            x = 1 + 2
            result = x * 3
        %>
        <%!
            import os
            MODULE_LEVEL = True
        %>
        <p>Template text with ${'$'}{result} expression.</p>
        % for item in items:
            <li>${'$'}{item}</li>
        % endfor
    """.trimIndent()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
}

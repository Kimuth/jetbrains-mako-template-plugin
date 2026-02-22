package com.schtilig.mako.lang

import com.schtilig.mako.MakoLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.OuterLanguageElementType

object MakoTokenTypes {
    // Expression tokens
    @JvmField val EXPR_START     = IElementType("EXPR_START",     MakoLanguage) // ${
    @JvmField val EXPR_END       = IElementType("EXPR_END",       MakoLanguage) // }
    @JvmField val EXPR_CONTENT   = IElementType("EXPR_CONTENT",   MakoLanguage) // Python expression text
    @JvmField val FILTER_SEP     = IElementType("FILTER_SEP",     MakoLanguage) // | inside ${...}

    // Control lines
    @JvmField val CONTROL_LINE   = IElementType("CONTROL_LINE",   MakoLanguage) // % keyword ... (entire line)

    // Block tags — per-tag opening token types (split from TAG_OPEN for distinct PSI nodes)
    @JvmField val TAG_OPEN_DEF       = IElementType("TAG_OPEN_DEF",       MakoLanguage) // <%def
    @JvmField val TAG_OPEN_BLOCK     = IElementType("TAG_OPEN_BLOCK",     MakoLanguage) // <%block
    @JvmField val TAG_OPEN_INHERIT   = IElementType("TAG_OPEN_INHERIT",   MakoLanguage) // <%inherit
    @JvmField val TAG_OPEN_INCLUDE   = IElementType("TAG_OPEN_INCLUDE",   MakoLanguage) // <%include
    @JvmField val TAG_OPEN_NAMESPACE = IElementType("TAG_OPEN_NAMESPACE", MakoLanguage) // <%namespace
    @JvmField val TAG_OPEN_PAGE      = IElementType("TAG_OPEN_PAGE",      MakoLanguage) // <%page
    @JvmField val TAG_CLOSE      = IElementType("TAG_CLOSE",      MakoLanguage) // %>
    @JvmField val END_TAG        = IElementType("END_TAG",        MakoLanguage) // </%def>, etc.
    @JvmField val TAG_ATTR_NAME  = IElementType("TAG_ATTR_NAME",  MakoLanguage) // attribute name
    @JvmField val TAG_ATTR_EQ    = IElementType("TAG_ATTR_EQ",    MakoLanguage) // =
    @JvmField val TAG_ATTR_VALUE = IElementType("TAG_ATTR_VALUE", MakoLanguage) // "quoted value"

    // Code blocks
    @JvmField val CODE_OPEN      = IElementType("CODE_OPEN",      MakoLanguage) // <%
    @JvmField val CODE_CONTENT   = IElementType("CODE_CONTENT",   MakoLanguage) // Python code inside <% %>
    @JvmField val MODULE_OPEN    = IElementType("MODULE_OPEN",    MakoLanguage) // <%!
    @JvmField val MODULE_CONTENT = IElementType("MODULE_CONTENT", MakoLanguage) // Python code inside <%! %>
    @JvmField val CODE_CLOSE     = IElementType("CODE_CLOSE",     MakoLanguage) // %>

    // Comments
    @JvmField val LINE_COMMENT   = IElementType("LINE_COMMENT",   MakoLanguage) // ## comment text
    @JvmField val DOC_OPEN       = IElementType("DOC_OPEN",       MakoLanguage) // <%doc>
    @JvmField val DOC_CONTENT    = IElementType("DOC_CONTENT",    MakoLanguage) // comment body
    @JvmField val DOC_CLOSE      = IElementType("DOC_CLOSE",      MakoLanguage) // </%doc>

    // Template text (HTML/plain content between Mako constructs)
    @JvmField val TEMPLATE_TEXT  = IElementType("TEMPLATE_TEXT",  MakoLanguage)

    // Template language support: placeholder token type used by TemplateDataElementType
    // to represent Mako constructs (tags, expressions, code blocks) in the HTML PSI tree.
    @JvmField val OUTER_ELEMENT_TYPE = OuterLanguageElementType("MAKO_OUTER_ELEMENT", MakoLanguage)

    // Whitespace and errors: reuse platform constants
    // TokenType.WHITE_SPACE and TokenType.BAD_CHARACTER — do NOT define custom equivalents
}

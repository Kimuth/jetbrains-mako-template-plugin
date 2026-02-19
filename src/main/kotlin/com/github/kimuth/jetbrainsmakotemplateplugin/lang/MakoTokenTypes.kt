package com.github.kimuth.jetbrainsmakotemplateplugin.lang

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoLanguage
import com.intellij.psi.tree.IElementType

object MakoTokenTypes {
    // Expression tokens
    @JvmField val EXPR_START     = IElementType("EXPR_START",     MakoLanguage) // ${
    @JvmField val EXPR_END       = IElementType("EXPR_END",       MakoLanguage) // }
    @JvmField val EXPR_CONTENT   = IElementType("EXPR_CONTENT",   MakoLanguage) // Python expression text
    @JvmField val FILTER_SEP     = IElementType("FILTER_SEP",     MakoLanguage) // | inside ${...}
    @JvmField val FILTER_NAME    = IElementType("FILTER_NAME",    MakoLanguage) // h, trim, u, x, n

    // Control lines
    @JvmField val CONTROL_LINE   = IElementType("CONTROL_LINE",   MakoLanguage) // % keyword ... (entire line)

    // Block tags — opening/closing markers
    @JvmField val TAG_OPEN       = IElementType("TAG_OPEN",       MakoLanguage) // <%def, <%block, etc.
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

    // Whitespace and errors: reuse platform constants
    // TokenType.WHITE_SPACE and TokenType.BAD_CHARACTER — do NOT define custom equivalents
}

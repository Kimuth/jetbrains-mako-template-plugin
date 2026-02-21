package com.schtilig.mako.lang.highlighting

import com.schtilig.mako.lang.MakoLexerAdapter
import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class MakoSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        @JvmField
        val MAKO_DIRECTIVE = createTextAttributesKey(
            "MAKO_DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val MAKO_EXPRESSION = createTextAttributesKey(
            "MAKO_EXPRESSION", DefaultLanguageHighlighterColors.MARKUP_TAG
        )

        @JvmField
        val MAKO_CONTROL_LINE = createTextAttributesKey(
            "MAKO_CONTROL_LINE", DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val MAKO_LINE_COMMENT = createTextAttributesKey(
            "MAKO_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        @JvmField
        val MAKO_BLOCK_COMMENT = createTextAttributesKey(
            "MAKO_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT
        )

        @JvmField
        val MAKO_TAG_ATTR_NAME = createTextAttributesKey(
            "MAKO_TAG_ATTR_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )

        @JvmField
        val MAKO_TAG_ATTR_VALUE = createTextAttributesKey(
            "MAKO_TAG_ATTR_VALUE", DefaultLanguageHighlighterColors.STRING
        )

        @JvmField
        val MAKO_TAG_CLOSE = createTextAttributesKey(
            "MAKO_TAG_CLOSE", DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val MAKO_CODE_CONTENT = createTextAttributesKey(
            "MAKO_CODE_CONTENT", DefaultLanguageHighlighterColors.STRING
        )

        // Pre-allocated key arrays to avoid per-call allocation
        private val DIRECTIVE_KEYS     = arrayOf(MAKO_DIRECTIVE)
        private val EXPRESSION_KEYS    = arrayOf(MAKO_EXPRESSION)
        private val CONTROL_LINE_KEYS  = arrayOf(MAKO_CONTROL_LINE)
        private val LINE_COMMENT_KEYS  = arrayOf(MAKO_LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(MAKO_BLOCK_COMMENT)
        private val TAG_ATTR_NAME_KEYS = arrayOf(MAKO_TAG_ATTR_NAME)
        private val TAG_ATTR_VALUE_KEYS = arrayOf(MAKO_TAG_ATTR_VALUE)
        private val TAG_CLOSE_KEYS     = arrayOf(MAKO_TAG_CLOSE)
        private val CODE_CONTENT_KEYS  = arrayOf(MAKO_CODE_CONTENT)
        private val EMPTY_KEYS         = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = MakoLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            // Directives — tag opens, end tags, and code/module opens
            MakoTokenTypes.TAG_OPEN_DEF       -> DIRECTIVE_KEYS
            MakoTokenTypes.TAG_OPEN_BLOCK     -> DIRECTIVE_KEYS
            MakoTokenTypes.TAG_OPEN_INHERIT   -> DIRECTIVE_KEYS
            MakoTokenTypes.TAG_OPEN_INCLUDE   -> DIRECTIVE_KEYS
            MakoTokenTypes.TAG_OPEN_NAMESPACE -> DIRECTIVE_KEYS
            MakoTokenTypes.TAG_OPEN_PAGE      -> DIRECTIVE_KEYS
            MakoTokenTypes.END_TAG            -> DIRECTIVE_KEYS
            MakoTokenTypes.CODE_OPEN          -> DIRECTIVE_KEYS
            MakoTokenTypes.MODULE_OPEN        -> DIRECTIVE_KEYS

            // Expressions
            MakoTokenTypes.EXPR_START   -> EXPRESSION_KEYS
            MakoTokenTypes.EXPR_END     -> EXPRESSION_KEYS
            MakoTokenTypes.EXPR_CONTENT -> EXPRESSION_KEYS
            MakoTokenTypes.FILTER_SEP   -> EXPRESSION_KEYS
            MakoTokenTypes.FILTER_NAME  -> EXPRESSION_KEYS

            // Control lines
            MakoTokenTypes.CONTROL_LINE -> CONTROL_LINE_KEYS

            // Line comments
            MakoTokenTypes.LINE_COMMENT -> LINE_COMMENT_KEYS

            // Block comments
            MakoTokenTypes.DOC_OPEN    -> BLOCK_COMMENT_KEYS
            MakoTokenTypes.DOC_CONTENT -> BLOCK_COMMENT_KEYS
            MakoTokenTypes.DOC_CLOSE   -> BLOCK_COMMENT_KEYS

            // Tag attribute names
            MakoTokenTypes.TAG_ATTR_NAME -> TAG_ATTR_NAME_KEYS

            // Tag attribute values
            MakoTokenTypes.TAG_ATTR_VALUE -> TAG_ATTR_VALUE_KEYS

            // Tag close punctuation
            MakoTokenTypes.TAG_CLOSE  -> TAG_CLOSE_KEYS
            MakoTokenTypes.CODE_CLOSE -> TAG_CLOSE_KEYS

            // Code block content
            MakoTokenTypes.CODE_CONTENT   -> CODE_CONTENT_KEYS
            MakoTokenTypes.MODULE_CONTENT -> CODE_CONTENT_KEYS

            // Template text: let the HTML layer handle these
            MakoTokenTypes.TEMPLATE_TEXT -> EMPTY_KEYS

            // TAG_ATTR_EQ: plain punctuation, no special color
            MakoTokenTypes.TAG_ATTR_EQ -> EMPTY_KEYS

            // All other tokens (WHITE_SPACE, BAD_CHARACTER, etc.)
            else -> EMPTY_KEYS
        }
}

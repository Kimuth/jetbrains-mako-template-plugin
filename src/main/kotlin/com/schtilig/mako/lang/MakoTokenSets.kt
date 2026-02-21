package com.schtilig.mako.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

object MakoTokenSets {
    val COMMENTS = TokenSet.create(
        MakoTokenTypes.LINE_COMMENT,
        MakoTokenTypes.DOC_CONTENT,
        MakoTokenTypes.DOC_OPEN,
        MakoTokenTypes.DOC_CLOSE
    )
    val WHITESPACE = TokenSet.create(TokenType.WHITE_SPACE)
    val TEMPLATE_CONTENT = TokenSet.create(MakoTokenTypes.TEMPLATE_TEXT)
    val TAG_OPENS = TokenSet.create(
        MakoTokenTypes.TAG_OPEN_DEF,
        MakoTokenTypes.TAG_OPEN_BLOCK,
        MakoTokenTypes.TAG_OPEN_INHERIT,
        MakoTokenTypes.TAG_OPEN_INCLUDE,
        MakoTokenTypes.TAG_OPEN_NAMESPACE,
        MakoTokenTypes.TAG_OPEN_PAGE
    )
}

package com.github.kimuth.jetbrainsmakotemplateplugin.lang

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
}

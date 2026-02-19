package com.github.kimuth.jetbrainsmakotemplateplugin.lang

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoLanguage
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.parser.MakoParser
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoFile
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MakoParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(MakoLanguage)
    }

    override fun createLexer(project: Project): Lexer = MakoLexerAdapter()
    override fun getWhitespaceTokens(): TokenSet = MakoTokenSets.WHITESPACE
    override fun getCommentTokens(): TokenSet = MakoTokenSets.COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createParser(project: Project): PsiParser = MakoParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun createFile(viewProvider: FileViewProvider): PsiFile = MakoFile(viewProvider)
    override fun createElement(node: ASTNode): PsiElement = MakoTypes.Factory.createElement(node)
}

package com.schtilig.mako.lang.psi.impl

import com.schtilig.mako.lang.psi.MakoExpression
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

/**
 * Mixin for ${...} expression PSI nodes.
 * Implements PsiLanguageInjectionHost so MakoPythonInjector can inject Python
 * into the content region between ${ and }.
 *
 * MUST be abstract — GrammarKit generates: class MakoExpressionImpl extends MakoExpressionMixin
 */
abstract class MakoExpressionMixin(node: ASTNode) :
    ASTWrapperPsiElement(node), MakoExpression, PsiLanguageInjectionHost {

    override fun isValidHost(): Boolean = true

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        object : LiteralTextEscaper<MakoExpressionMixin>(this) {
            override fun isOneLine() = false
            override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
                outChars.append(myHost.text, rangeInsideHost.startOffset, rangeInsideHost.endOffset)
                return true
            }
            override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange) =
                rangeInsideHost.startOffset + offsetInDecoded
        }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        throw UnsupportedOperationException("Mako injection hosts do not support round-trip text edits")
    }
}

package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoModuleBlock
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

/**
 * Mixin for <%! ... %> module block PSI nodes.
 * Implements PsiLanguageInjectionHost so MakoPythonInjector can inject Python
 * into the content region between <%! and %>.
 *
 * MUST be abstract — GrammarKit generates: class MakoModuleBlockImpl extends MakoModuleBlockMixin
 */
abstract class MakoModuleBlockMixin(node: ASTNode) :
    ASTWrapperPsiElement(node), MakoModuleBlock, PsiLanguageInjectionHost {

    override fun isValidHost(): Boolean = true

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        object : LiteralTextEscaper<MakoModuleBlockMixin>(this) {
            override fun isOneLine() = false
            override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
                outChars.append(myHost.text, rangeInsideHost.startOffset, rangeInsideHost.endOffset)
                return true
            }
            override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange) =
                rangeInsideHost.startOffset + offsetInDecoded
        }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        // Minimal implementation — full manipulation support added with Plan 03 injector
        return this
    }
}

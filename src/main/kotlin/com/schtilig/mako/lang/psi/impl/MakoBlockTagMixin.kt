package com.schtilig.mako.lang.psi.impl

import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/**
 * Mixin for <%block> PSI nodes. GrammarKit generates MakoBlockTagImpl extending this class.
 * Implements PsiNamedElement so block names are retrievable without string parsing.
 *
 * MUST be abstract -- GrammarKit generates: class MakoBlockTagImpl extends MakoBlockTagMixin
 */
abstract class MakoBlockTagMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {

    override fun getName(): String? {
        val attrValue = node.findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)
        return attrValue?.text?.trim('"', '\'')
    }

    override fun setName(name: String): PsiElement {
        return this
    }
}

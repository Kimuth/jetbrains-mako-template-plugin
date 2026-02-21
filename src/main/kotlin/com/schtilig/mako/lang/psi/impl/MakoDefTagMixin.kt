package com.schtilig.mako.lang.psi.impl

import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/**
 * Mixin for <%def> PSI nodes. GrammarKit generates MakoDefTagImpl extending this class.
 * Implements PsiNamedElement so def names are retrievable without string parsing.
 *
 * MUST be abstract -- GrammarKit generates: class MakoDefTagImpl extends MakoDefTagMixin
 */
abstract class MakoDefTagMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {

    override fun getName(): String? {
        // Find the first TAG_ATTR_VALUE child node -- this holds the name="..." attribute value
        val attrValue = node.findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)
        // Strip surrounding quotes (single or double)
        return attrValue?.text?.trim('"', '\'')
    }

    override fun setName(name: String): PsiElement {
        // Minimal implementation for Phase 3 -- full rename refactoring is a later phase
        // Return `this` as a no-op; rename support will be added when needed
        return this
    }
}

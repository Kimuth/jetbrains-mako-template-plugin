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

    /**
     * Returns the value of the `name=` attribute, or null if no such attribute exists.
     *
     * The grammar rule `tag_attribute ::= TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE` is private,
     * so GrammarKit does NOT create a composite PSI node for it. Instead, TAG_ATTR_NAME,
     * TAG_ATTR_EQ, and TAG_ATTR_VALUE appear as flat sibling tokens directly under this node.
     *
     * Algorithm: walk child ASTNodes until a TAG_ATTR_NAME with text "name" is found,
     * then return the next TAG_ATTR_VALUE sibling. This handles any attribute ordering
     * and the case where no name attribute is present (returns null).
     */
    override fun getName(): String? {
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == MakoTokenTypes.TAG_ATTR_NAME && child.text == "name") {
                // Walk forward to find the corresponding TAG_ATTR_VALUE
                var sibling = child.treeNext
                while (sibling != null) {
                    if (sibling.elementType == MakoTokenTypes.TAG_ATTR_VALUE) {
                        return sibling.text.trim('"', '\'')
                    }
                    // Stop if we hit another TAG_ATTR_NAME (next attribute started, no value found)
                    if (sibling.elementType == MakoTokenTypes.TAG_ATTR_NAME) break
                    sibling = sibling.treeNext
                }
                return null
            }
            child = child.treeNext
        }
        return null
    }

    /**
     * Rename is not supported — throws UnsupportedOperationException so callers
     * receive a clear failure signal rather than a silent no-op.
     */
    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("Rename is not supported for Mako block tags")
    }
}

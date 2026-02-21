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

    /**
     * Returns the value of the `name=` attribute, or null if no such attribute exists.
     *
     * The grammar rule `tag_attribute ::= TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE` is private,
     * so GrammarKit does NOT create a composite PSI node for it. Instead, TAG_ATTR_NAME,
     * TAG_ATTR_EQ, and TAG_ATTR_VALUE appear as flat sibling tokens directly under this node.
     *
     * Algorithm: walk child ASTNodes until a TAG_ATTR_NAME with text "name" is found,
     * then return the next TAG_ATTR_VALUE sibling. This handles any attribute ordering
     * (e.g., <%def args="()" name="foo"> correctly returns "foo", not "()").
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
        throw UnsupportedOperationException("Rename is not supported for Mako def tags")
    }
}

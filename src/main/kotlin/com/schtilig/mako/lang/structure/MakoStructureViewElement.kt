package com.schtilig.mako.lang.structure

import com.intellij.icons.AllIcons
import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.schtilig.mako.lang.psi.MakoFile
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.Iconable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

class MakoStructureViewElement(private val element: NavigatablePsiElement) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)

    override fun canNavigate(): Boolean = element.canNavigate()

    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

    override fun getAlphaSortKey(): String = element.name ?: ""

    override fun getPresentation(): ItemPresentation {
        val existing = (element as? NavigationItem)?.presentation
        return existing ?: object : ItemPresentation {
            override fun getPresentableText(): String = element.name ?: "<unnamed>"
            override fun getIcon(unused: Boolean): Icon = AllIcons.Nodes.Function
            override fun getLocationString(): String? = null
        }
    }

    override fun getChildren(): Array<TreeElement> {
        val childDefs: List<MakoDefTag>
        val childBlocks: List<MakoBlockTag>

        when (element) {
            is MakoFile -> {
                childDefs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                childBlocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
            }
            is MakoDefTag -> {
                childDefs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                childBlocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
            }
            is MakoBlockTag -> {
                childDefs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                childBlocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
            }
            else -> return TreeElement.EMPTY_ARRAY
        }

        return (childDefs + childBlocks)
            .map { it as NavigatablePsiElement }
            .sortedBy { it.textOffset }
            .map { MakoStructureViewElement(it) }
            .toTypedArray()
    }
}

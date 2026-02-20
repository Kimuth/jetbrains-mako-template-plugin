package com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoIcons
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoFile
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
            override fun getIcon(unused: Boolean): Icon = MakoIcons.FILE
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

        val result = mutableListOf<TreeElement>()
        childDefs.mapTo(result) { MakoStructureViewElement(it as NavigatablePsiElement) }
        childBlocks.mapTo(result) { MakoStructureViewElement(it as NavigatablePsiElement) }
        return result.toTypedArray()
    }
}

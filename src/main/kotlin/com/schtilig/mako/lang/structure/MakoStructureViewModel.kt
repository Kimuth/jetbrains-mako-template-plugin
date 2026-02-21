package com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile

class MakoStructureViewModel(editor: Editor?, psiFile: PsiFile) :
    StructureViewModelBase(psiFile, editor, MakoStructureViewElement(psiFile as NavigatablePsiElement)),
    StructureViewModel.ElementInfoProvider {

    override fun getSuitableClasses(): Array<Class<*>> =
        arrayOf(MakoDefTag::class.java, MakoBlockTag::class.java)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

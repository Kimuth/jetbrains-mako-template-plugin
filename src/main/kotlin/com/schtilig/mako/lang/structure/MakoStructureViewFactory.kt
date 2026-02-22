package com.schtilig.mako.lang.structure

import com.schtilig.mako.MakoLanguage
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class MakoStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        // In dual-tree (TemplateLanguageFileViewProvider), the platform may pass the HTML PSI
        // file when the caret is in a TEMPLATE_TEXT region. Resolve to the Mako PSI root so
        // MakoStructureViewElement.getChildren() finds <%def> and <%block> nodes rather than
        // hitting the else branch and returning EMPTY_ARRAY.
        // This is a no-op when psiFile is already the Mako PSI root.
        val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) ?: psiFile
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?) =
                MakoStructureViewModel(editor, makoFile)
        }
    }
}

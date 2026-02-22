package com.schtilig.mako.lang

import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings
import com.intellij.testFramework.LightVirtualFile

/**
 * Factory that creates MakoFileViewProvider instances for .mako files.
 *
 * Registered via lang.fileViewProviderFactory extension point in plugin.xml with
 * language="Mako Template". The platform calls createFileViewProvider() whenever a .mako
 * file is opened; the returned MakoFileViewProvider establishes the dual PSI tree.
 *
 * Template data language resolution order:
 * 1. TemplateDataLanguageMappings: user-configured override per-file (Settings → Languages & Frameworks)
 * 2. HTMLLanguage.INSTANCE: default fallback (Mako templates are primarily HTML-embedding)
 *
 * LightVirtualFile guard: standard IntelliJ pattern for template language factories.
 * LightVirtualFile instances are created by PsiFileFactory for in-memory PSI (used in
 * ParsingTestCase, MakoFoldingTest, MakoPsiMixinTest, etc.). These lightweight test fixtures
 * do not need the dual-tree. More importantly, ParsingTestCase.doCheckResult iterates all
 * PSI roots via viewProvider.getLanguages() and creates per-language fixture files
 * (e.g., "WellFormedFile.Mako Template.txt", "WellFormedFile.HTML.txt"), which would break
 * all existing parser fixture tests. Return a SingleRootFileViewProvider for LightVirtualFile
 * instances to maintain test compatibility and match standard template language plugin behavior.
 */
class MakoFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        // LightVirtualFile instances are in-memory files (not backed by disk).
        // The dual PSI tree is only needed for real, disk-backed files open in the editor.
        if (file is LightVirtualFile) {
            return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
        }

        // Guard against null returns during early IDE initialization.
        // TemplateDataLanguageMappings.getInstance() may return null when the project service
        // is not yet registered.
        val templateDataLanguage = TemplateDataLanguageMappings.getInstance(manager.project)
            ?.getMapping(file)
            ?: HTMLLanguage.INSTANCE
        return MakoFileViewProvider(manager, file, eventSystemEnabled, templateDataLanguage)
    }
}

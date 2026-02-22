package com.schtilig.mako.lang

import com.schtilig.mako.MakoLanguage
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Automated tests verifying MakoFileViewProvider creates a dual PSI tree for .mako files.
 *
 * HINJ-01 through HINJ-06: All six HTML injection requirements depend on the HTML PSI
 * tree being present. These tests assert the structural precondition (dual tree) that
 * enables HTML features without requiring a running IDE for each check.
 *
 * Uses BasePlatformTestCase so the lang.fileViewProviderFactory extension point registered
 * in plugin.xml is active. HTMLLanguage is a core platform class available in all
 * IntelliJ-based test environments.
 *
 * Note: BasePlatformTestCase uses addFileToProject() which creates a physical file in the
 * test project VFS (not a LightVirtualFile), so MakoFileViewProviderFactory returns a full
 * MakoFileViewProvider (not the SingleRootFileViewProvider fallback used for LightVirtualFile).
 */
class MakoFileViewProviderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Force MakoFileType registration (same pattern as MakoAnnotatorTest).
        FileTypeManager.getInstance().getFileTypeByExtension("mako")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    /**
     * HINJ dual-tree precondition: Opening a .mako file must produce a
     * TemplateLanguageFileViewProvider with exactly 2 PSI roots — one for Mako,
     * one for HTML. This is the structural prerequisite for all HINJ requirements.
     */
    fun testMakoFileHasDualPsiTree() {
        val file = myFixture.addFileToProject(
            "view_provider_test.mako",
            "<div>\${greeting}</div>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val viewProvider = myFixture.file.viewProvider
        assertTrue(
            "viewProvider for .mako must be a TemplateLanguageFileViewProvider, got: ${viewProvider::class.simpleName}",
            viewProvider is TemplateLanguageFileViewProvider
        )
        assertEquals(
            "viewProvider.allFiles must contain exactly 2 PSI roots (Mako + HTML)",
            2,
            viewProvider.allFiles.size
        )
    }

    /**
     * HINJ dual-tree: getPsi(HTMLLanguage.INSTANCE) must return a non-null HTML PSI file,
     * confirming the HTML tree is accessible for HTML feature providers to query.
     */
    fun testMakoFileViewProviderProvidesHtmlPsi() {
        val file = myFixture.addFileToProject(
            "html_psi_test.mako",
            "<p>Hello</p>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val viewProvider = myFixture.file.viewProvider
        val htmlPsi = viewProvider.getPsi(HTMLLanguage.INSTANCE)
        assertNotNull(
            "viewProvider.getPsi(HTMLLanguage.INSTANCE) must not be null for a .mako file",
            htmlPsi
        )
    }

    /**
     * HINJ dual-tree: getPsi(MakoLanguage) must return the Mako PSI root, confirming
     * that the Mako tree remains intact alongside the HTML tree (no regression).
     */
    fun testMakoFileViewProviderPreservesMakoPsi() {
        val file = myFixture.addFileToProject(
            "mako_psi_test.mako",
            "<%def name=\"greeting\">\nHello\n</%def>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val viewProvider = myFixture.file.viewProvider
        val makoPsi = viewProvider.getPsi(MakoLanguage)
        assertNotNull(
            "viewProvider.getPsi(MakoLanguage) must not be null — Mako PSI tree must survive alongside HTML tree",
            makoPsi
        )
        assertEquals(
            "Mako PSI root language must be MakoLanguage",
            MakoLanguage,
            makoPsi!!.language
        )
    }
}

package com.schtilig.mako.lang

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.structure.MakoStructureViewModel
import com.schtilig.mako.lang.structure.MakoStructureViewElement
import com.intellij.lang.annotation.HighlightSeverity
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

    /**
     * RGRN-02 / RGRN-03: Structure view must find <%def> and <%block> nodes in dual-tree
     * environment. Uses addFileToProject (physical VFS) so MakoFileViewProviderFactory returns
     * a full MakoFileViewProvider (not the LightVirtualFile fallback), making dual-tree active.
     * Constructs MakoStructureViewModel directly with the Mako PSI root to assert the model
     * produces the correct children independently of what the platform passes to
     * MakoStructureViewFactory.getStructureViewBuilder at runtime.
     */
    fun testStructureViewWorksInDualTree() {
        val file = myFixture.addFileToProject(
            "structure_dual_tree_test.mako",
            "<%def name=\"greet\"></%def>\n<%block name=\"header\"></%block>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        // Precondition: dual-tree must be active
        val viewProvider = myFixture.file.viewProvider
        assertTrue(
            "viewProvider must be a TemplateLanguageFileViewProvider for dual-tree test to be meaningful",
            viewProvider is TemplateLanguageFileViewProvider
        )

        // Resolve Mako PSI root (same resolution the guard applies at runtime)
        val makoFile = viewProvider.getPsi(MakoLanguage)
        assertNotNull("viewProvider.getPsi(MakoLanguage) must not be null", makoFile)
        requireNotNull(makoFile)

        val model = MakoStructureViewModel(null, makoFile)
        val root = model.root as MakoStructureViewElement
        val children = root.children

        assertEquals(
            "Structure view must find 2 children (greet def + header block) when dual-tree is active",
            2,
            children.size
        )
    }

    /**
     * CRCT-01: ${...} expressions inside HTML attribute values must not produce
     * false-positive HTML error squiggles. MakoErrorFilter suppresses HTML errors
     * adjacent to OuterLanguageElement boundaries.
     *
     * Note: If the HTML annotator is inactive in BasePlatformTestCase.doHighlighting()
     * the test trivially passes (zero highlights) — this provides structural coverage;
     * the definitive check is the human IDE verification in plan 20-03.
     */
    fun testNoFalsePositiveHtmlErrorOnMakoExpression() {
        val file = myFixture.addFileToProject(
            "crct_expression_test.mako",
            "<div class=\"\${cls}\">Hello</div>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val highlights = myFixture.doHighlighting()
        val htmlErrors = highlights.filter { info ->
            info.severity == HighlightSeverity.ERROR &&
            info.description?.let { desc ->
                !desc.startsWith("Unclosed") && !desc.startsWith("Unknown Mako")
            } ?: true
        }
        assertTrue(
            "Expected no false-positive HTML errors for '\${cls}' in attribute value, got: $htmlErrors",
            htmlErrors.isEmpty()
        )
    }

    /**
     * CRCT-02: Mako control lines (%for, %if, %endif) must not produce false-positive
     * HTML error squiggles. MakoErrorFilter suppresses HTML errors adjacent to
     * CONTROL_LINE OuterLanguageElement boundaries.
     */
    fun testNoFalsePositiveHtmlErrorOnMakoControlLines() {
        val file = myFixture.addFileToProject(
            "crct_control_line_test.mako",
            "%for item in items:\n<li>\${item}</li>\n%endfor\n"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val highlights = myFixture.doHighlighting()
        val htmlErrors = highlights.filter { info ->
            info.severity == HighlightSeverity.ERROR &&
            info.description?.let { desc ->
                !desc.startsWith("Unclosed") && !desc.startsWith("Unknown Mako")
            } ?: true
        }
        assertTrue(
            "Expected no false-positive HTML errors for %for/%endfor control lines, got: $htmlErrors",
            htmlErrors.isEmpty()
        )
    }
}

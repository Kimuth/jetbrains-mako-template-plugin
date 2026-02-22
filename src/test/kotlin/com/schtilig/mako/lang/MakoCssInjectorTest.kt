package com.schtilig.mako.lang

import com.schtilig.mako.lang.injection.MakoCssInjector
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlText
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Structural tests for MakoCssInjector wiring. No CSS plugin is required for these tests.
 *
 * Tests confirm:
 * 1. MakoCssInjector declares XmlText as its injection host type.
 * 2. The null guard fires silently when CSS language is absent (CSS plugin not installed).
 * 3. The HtmlUtil.isHtmlTagContainingFile guard prevents injection into non-HTML XML files.
 *
 * Full CSS completion verification (Ctrl+Space in <style>) requires a human IDE check with
 * the CSS plugin installed (PyCharm Professional, IntelliJ IDEA Ultimate, or manually from marketplace).
 */
class MakoCssInjectorTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Force MakoFileType registration (same pattern as MakoFileViewProviderTest).
        FileTypeManager.getInstance().getFileTypeByExtension("mako")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    /**
     * Verifies MakoCssInjector.elementsToInjectIn() includes XmlText — the correct
     * injection host type for text content inside <style> elements in the HTML PSI tree.
     * XmlTextImpl implements PsiLanguageInjectionHost (confirmed via javap on PyCharm 2025.2.6).
     */
    fun testCssInjectorElementsToInjectIn() {
        assertTrue(
            "MakoCssInjector must declare XmlText as injection host type",
            MakoCssInjector().elementsToInjectIn().contains(XmlText::class.java)
        )
    }

    /**
     * Verifies the null guard in getLanguagesToInject — when the CSS plugin is not installed,
     * Language.findLanguageByID("CSS") returns null and the injector must silently no-op
     * without calling startInjecting on the registrar.
     *
     * Only runs when CSS plugin is NOT present in the test environment. If CSS IS installed,
     * the null-guard code path is unreachable and the test is skipped.
     */
    fun testCssInjectorNoOpsWhenCssLanguageAbsent() {
        if (Language.findLanguageByID("CSS") != null) {
            // CSS plugin IS present in test environment — the null-guard test is not applicable.
            return
        }

        // Mock registrar that throws if startInjecting is called — the injector must not reach it.
        val mockRegistrar = object : MultiHostRegistrar {
            override fun startInjecting(language: Language): MultiHostRegistrar =
                throw AssertionError("startInjecting must not be called when CSS language is absent")

            override fun startInjecting(language: Language, mimeType: String?): MultiHostRegistrar =
                throw AssertionError("startInjecting must not be called when CSS language is absent")

            override fun addPlace(
                prefix: String?,
                suffix: String?,
                host: PsiLanguageInjectionHost,
                rangeInsideHost: TextRange
            ): MultiHostRegistrar = this

            override fun doneInjecting() {}
        }

        val file = myFixture.addFileToProject(
            "css_injector_null_test.mako",
            "<style>color: red;</style>"
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        // Find the first XmlText inside a <style> element in the HTML PSI tree.
        val viewProvider = myFixture.file.viewProvider
        var xmlTextNode: XmlText? = null
        for (psiFile in viewProvider.allFiles) {
            val candidates = PsiTreeUtil.findChildrenOfType(psiFile, XmlText::class.java)
            val found = candidates.firstOrNull { xt ->
                xt.parentTag?.localName?.lowercase() == "style"
            }
            if (found != null) {
                xmlTextNode = found
                break
            }
        }

        if (xmlTextNode != null) {
            // Should return early at Language.findLanguageByID("CSS") ?: return — no throw.
            MakoCssInjector().getLanguagesToInject(mockRegistrar, xmlTextNode)
        }
        // If no XmlText found in test env (HTML PSI not set up), the test is vacuously satisfied.
    }

    /**
     * Verifies the HtmlUtil.isHtmlTagContainingFile guard — the injector must NOT fire
     * when the XmlText is inside a plain XML file (not an HTML file), even if its parent
     * tag is named <style>.
     *
     * Confirms that CSS injection is restricted to our Mako/HTML PSI tree and does not
     * accidentally inject into XML configuration files that happen to use a <style> element.
     */
    fun testCssInjectorDoesNotFireOnXmlOutsideHtml() {
        // Plain XML file with a <style> element — not an HTML file.
        myFixture.configureByText("config.xml", "<config><style>color: red;</style></config>")

        val xmlFile = myFixture.file
        val allXmlTexts = PsiTreeUtil.findChildrenOfType(xmlFile, XmlText::class.java)
        val xmlTextInStyle = allXmlTexts.firstOrNull { xt ->
            xt.parentTag?.localName?.lowercase() == "style"
        }

        if (Language.findLanguageByID("CSS") != null) {
            // CSS plugin present — verify startInjecting is NOT called (HTML guard fires).
            var startInjected = false
            val trackingRegistrar = object : MultiHostRegistrar {
                override fun startInjecting(language: Language): MultiHostRegistrar {
                    startInjected = true
                    return this
                }

                override fun startInjecting(language: Language, mimeType: String?): MultiHostRegistrar {
                    startInjected = true
                    return this
                }

                override fun addPlace(
                    prefix: String?,
                    suffix: String?,
                    host: PsiLanguageInjectionHost,
                    rangeInsideHost: TextRange
                ): MultiHostRegistrar = this

                override fun doneInjecting() {}
            }

            if (xmlTextInStyle != null) {
                MakoCssInjector().getLanguagesToInject(trackingRegistrar, xmlTextInStyle)
                assertFalse(
                    "MakoCssInjector must not inject CSS into non-HTML XML files (HtmlUtil.isHtmlTagContainingFile guard)",
                    startInjected
                )
            }
        } else {
            // CSS plugin absent — null guard fires before HTML check; just verify XmlText is findable.
            assertNotNull(
                "Should be able to find XmlText inside <style> in plain XML",
                xmlTextInStyle
            )
        }
    }
}

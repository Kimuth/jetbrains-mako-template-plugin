package com.schtilig.mako.lang

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Completion tests verifying MakoCompletionContributor satisfies:
 * - COMP-01: Tag-name completion after `<%` (all 7 directive names offered)
 * - COMP-02: Per-tag attribute completion inside open tags (correct scoping, no cross-contamination)
 * - Anti-regression: No Mako tag completions in plain HTML context
 *
 * Tests use myFixture.addFileToProject() to create a physical .mako file in the test
 * project, bypassing the file type detection issue with in-memory configureByText() calls.
 * The caret position is supplied via the <caret> marker and parsed by the test fixture.
 */
class MakoCompletionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Force MakoFileType registration by touching the file type manager.
        // In some test environments, the file type extension is loaded lazily;
        // dispatching all pending EDT events processes any deferred extension registrations.
        FileTypeManager.getInstance().getFileTypeByExtension("mako")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    // -------------------------------------------------------------------------
    // Helper: configure a Mako fixture file and return its path
    // -------------------------------------------------------------------------

    /**
     * Creates a `.mako` test file with the given content (may include `<caret>` marker),
     * adds it to the test project, then configures the editor fixture from it.
     *
     * Using addFileToProject + configureFromExistingVirtualFile ensures the platform
     * recognises the file as Mako Template (by extension) even when the MakoFileType
     * extension would be loaded lazily under configureByText(FileType, ...).
     */
    private fun configureMakoFile(content: String) {
        // Use a counter-based name to avoid virtual file caching across tests
        val file = myFixture.addFileToProject("test.mako", content)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
    }

    private fun assertTagNames(items: Array<LookupElement>?) {
        assertNotNull("Completion items must not be null", items)
        val strings = items!!.map { it.lookupString }
        assertContainsElements(
            strings,
            "<%def", "<%block", "<%inherit", "<%include", "<%namespace", "<%page", "<%doc"
        )
    }

    // -------------------------------------------------------------------------
    // COMP-01: Tag-name completion
    // -------------------------------------------------------------------------

    /**
     * COMP-01: Caret immediately after `<%` must offer all 7 directive tag names.
     */
    fun testTagNameCompletionAfterLt() {
        configureMakoFile("<%<caret>")
        val items = myFixture.completeBasic()
        assertTagNames(items)
    }

    /**
     * COMP-01: Partial tag name `<%d` — popup must include `<%def` at minimum.
     * If platform auto-completes to a single result, verify the inserted text contains `<%def`.
     */
    fun testTagNameCompletionPartiallyTyped() {
        configureMakoFile("<%d<caret>")
        val items = myFixture.completeBasic()
        if (items == null) {
            // Single item — platform auto-completed; check inserted text
            assertTrue(
                "Auto-completed text must contain <%def",
                myFixture.file.text.contains("<%def")
            )
        } else {
            val strings = items.map { it.lookupString }
            assertContainsElements(strings, "<%def")
        }
    }

    // -------------------------------------------------------------------------
    // COMP-02: Tag attribute completion
    // -------------------------------------------------------------------------

    /**
     * COMP-02: Caret on whitespace inside `<%def` must offer all def-specific attributes.
     */
    fun testDefAttrCompletion() {
        configureMakoFile("<%def <caret>>")
        val items = myFixture.completeBasic()
        assertNotNull("Attribute completion items must not be null for <%def", items)
        val strings = items!!.map { it.lookupString }
        assertContainsElements(strings, "name", "buffered", "cached", "filter", "decorator")
    }

    /**
     * COMP-02: Caret inside `<%inherit` must offer `file` only — NOT `name` (which is
     * specific to <%def and <%block). Verifies per-tag attribute scoping.
     */
    fun testInheritAttrCompletion() {
        configureMakoFile("<%inherit <caret>>")
        val items = myFixture.completeBasic()
        assertNotNull("Attribute completion items must not be null for <%inherit", items)
        val strings = items!!.map { it.lookupString }
        assertContainsElements(strings, "file")
        assertDoesntContain(strings, "name")
    }

    /**
     * COMP-02: Caret inside `<%block` must offer block-specific attributes including `name` and `filter`.
     */
    fun testBlockAttrCompletion() {
        configureMakoFile("<%block <caret>>")
        val items = myFixture.completeBasic()
        assertNotNull("Attribute completion items must not be null for <%block", items)
        val strings = items!!.map { it.lookupString }
        assertContainsElements(strings, "name", "filter", "cached")
    }

    // -------------------------------------------------------------------------
    // Anti-regression: No Mako tag completions in plain HTML context
    // -------------------------------------------------------------------------

    /**
     * Anti-regression: Plain HTML text (no `<%` prefix) must NOT produce any Mako tag
     * name completions. Verifies the raw-text guard in TagNameCompletionProvider.
     */
    fun testNoCompletionInPlainHtml() {
        configureMakoFile("<html><body><p><caret></p></body></html>")
        val items = myFixture.completeBasic()
        val strings = items?.map { it.lookupString } ?: emptyList()
        assertDoesntContain(
            strings,
            "<%def", "<%block", "<%inherit", "<%include", "<%namespace", "<%page", "<%doc"
        )
    }

    // -------------------------------------------------------------------------
    // Insert handler verification
    // -------------------------------------------------------------------------

    /**
     * COMP-02 insert handler: Selecting an attribute must produce `attr=""` in the file text.
     * Uses type() to simulate selecting the "name" completion item.
     */
    fun testAttrInsertHandlerProducesQuotedValue() {
        configureMakoFile("<%def <caret>>")
        myFixture.completeBasic()
        // Type "name" to filter to the specific attribute, then Enter to select
        myFixture.type("name\n")
        val text = myFixture.file.text
        assertTrue(
            "Insert handler must produce name=\"\" in file text (got: '$text')",
            text.contains("name=\"\"")
        )
    }
}

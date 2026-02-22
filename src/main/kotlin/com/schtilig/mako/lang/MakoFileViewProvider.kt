package com.schtilig.mako.lang

import com.schtilig.mako.MakoLanguage
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.tree.IElementType
import java.util.concurrent.ConcurrentHashMap

/**
 * TemplateLanguageFileViewProvider for Mako template files.
 *
 * Creates a dual PSI tree for every .mako file:
 * - Mako PSI root (base language): contains MakoDefTag, MakoBlockTag, MakoExpression, etc.
 * - HTML PSI root (template data language): contains the HTML structure of TEMPLATE_TEXT regions.
 *
 * The HTML PSI tree is the single structural prerequisite that activates all six HINJ requirements
 * (HTML syntax coloring, tag/attribute completion, Emmet, HTML error detection, CSS injection,
 * JavaScript injection) without any additional HTML-specific code.
 *
 * Registered via lang.fileViewProviderFactory in plugin.xml with language="Mako Template".
 */
class MakoFileViewProvider(
    manager: PsiManager,
    virtualFile: VirtualFile,
    eventSystemEnabled: Boolean,
    private val myTemplateDataLanguage: Language = HTMLLanguage.INSTANCE
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, virtualFile, eventSystemEnabled),
    TemplateLanguageFileViewProvider {

    override fun getBaseLanguage(): Language = MakoLanguage

    override fun getTemplateDataLanguage(): Language = myTemplateDataLanguage

    override fun getLanguages(): Set<Language> = setOf(MakoLanguage, myTemplateDataLanguage)

    override fun getContentElementType(language: Language): IElementType? =
        if (language == myTemplateDataLanguage) getTemplateDataElementType(language) else null

    override fun createFile(lang: Language): PsiFile? = when {
        lang.isKindOf(MakoLanguage) ->
            LanguageParserDefinitions.INSTANCE.forLanguage(MakoLanguage)?.createFile(this)
        lang.isKindOf(myTemplateDataLanguage) -> {
            val def = LanguageParserDefinitions.INSTANCE.forLanguage(myTemplateDataLanguage) ?: return null
            (def.createFile(this) as? PsiFileImpl)?.also { htmlFile ->
                // CRITICAL: set contentElementType immediately after createFile.
                // Without this, the HTML parser receives raw Mako bytes and produces
                // a deeply broken HTML PSI tree that does not know about Mako constructs.
                htmlFile.contentElementType = getTemplateDataElementType(myTemplateDataLanguage)
            }
        }
        else -> null
    }

    override fun supportsIncrementalReparse(rootLanguage: Language): Boolean = false

    override fun cloneInner(virtualFile: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider =
        MakoFileViewProvider(manager, virtualFile, false, myTemplateDataLanguage)

    companion object {
        // Singleton TemplateDataElementType per data-language ID.
        // MUST be a singleton: the platform uses identity equality on IElementType instances
        // to locate the correct token ranges. Creating a new instance per file causes the
        // HTML parser to fail to map TEMPLATE_TEXT ranges to the outer language.
        private val TEMPLATE_DATA_BY_LANG = ConcurrentHashMap<String, TemplateDataElementType>()

        fun getTemplateDataElementType(lang: Language): TemplateDataElementType {
            return TEMPLATE_DATA_BY_LANG.getOrPut(lang.id) {
                TemplateDataElementType(
                    "MAKO_TEMPLATE_DATA",
                    lang,
                    MakoTokenTypes.TEMPLATE_TEXT,       // 3rd arg: token whose ranges are HTML content
                    MakoTokenTypes.OUTER_ELEMENT_TYPE   // 4th arg: placeholder type for Mako constructs in HTML tree
                )
            }
        }
    }
}

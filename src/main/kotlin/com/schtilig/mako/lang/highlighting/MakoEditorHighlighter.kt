package com.schtilig.mako.lang.highlighting

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings

/**
 * Layered editor highlighter for Mako template files.
 *
 * Registers the HTML SyntaxHighlighter as a layer for TEMPLATE_TEXT tokens so that
 * HTML tags, attributes, and entities inside TEMPLATE_TEXT regions are colored by the
 * HTML highlighter rather than left uncolored by MakoSyntaxHighlighter.EMPTY_KEYS.
 *
 * Vended by MakoEditorHighlighterProvider, registered via the editorHighlighterProvider
 * extension point with filetype="Mako Template" in plugin.xml.
 *
 * Source: pattern adapted from RestEditorHighlighter.java (restructuredtext.jar, PyCharm 2025.2.6)
 * and HbTemplateHighlighter.java (JetBrains/intellij-plugins/handlebars).
 */
class MakoEditorHighlighter(
    colors: EditorColorsScheme,
    project: Project?,
    file: VirtualFile?
) : LayeredLexerEditorHighlighter(
    SyntaxHighlighterFactory.getSyntaxHighlighter(MakoLanguage, project, file)
        ?: MakoSyntaxHighlighter(),
    colors
) {
    init {
        // Null-guard: getEditorHighlighter() is called with null project/file during
        // color scheme previews in Settings and early IDE init. Only register the HTML
        // layer when both context objects are present.
        if (project != null && file != null) {
            // Respect user-configured template data language override; fall back to HTML.
            val templateDataLanguage =
                TemplateDataLanguageMappings.getInstance(project)?.getMapping(file)
                    ?: HTMLLanguage.INSTANCE
            val htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
                templateDataLanguage, project, file
            )
            if (htmlHighlighter != null) {
                registerLayer(
                    MakoTokenTypes.TEMPLATE_TEXT,
                    LayerDescriptor(htmlHighlighter, "")
                )
            }
        }
    }
}

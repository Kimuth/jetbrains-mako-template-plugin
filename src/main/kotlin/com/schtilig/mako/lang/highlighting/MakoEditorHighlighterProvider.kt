package com.schtilig.mako.lang.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * EditorHighlighterProvider factory for Mako template files.
 *
 * Returns a MakoEditorHighlighter (LayeredLexerEditorHighlighter) that delegates HTML
 * coloring inside TEMPLATE_TEXT regions to the HTML SyntaxHighlighter.
 *
 * Registered in plugin.xml via the editorHighlighterProvider extension point with
 * filetype="Mako Template" (all lowercase — matches MakoFileType.getName()).
 *
 * Source: adapted from RestEditorHighlighterProvider.java (restructuredtext.jar, PyCharm 2025.2.6)
 */
class MakoEditorHighlighterProvider : EditorHighlighterProvider {
    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        file: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter = MakoEditorHighlighter(colors, project, file)
}

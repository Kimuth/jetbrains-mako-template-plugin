package com.github.kimuth.jetbrainsmakotemplateplugin.lang.annotation

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTemplateTextContent
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class MakoAnnotator : Annotator {

    companion object {
        private val VALID_DIRECTIVES = setOf("def", "block", "inherit", "include", "namespace", "page", "doc")
        private val INVALID_DIRECTIVE_REGEX = Regex("""<%([a-zA-Z]+)""")
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Guard: only process elements in Mako Template files
        if (element.containingFile.language.id != "Mako Template") return

        when (element) {
            is MakoDefTag -> checkForMissingEndTag(element, holder, "<%def>")
            is MakoBlockTag -> checkForMissingEndTag(element, holder, "<%block>")
            is MakoTemplateTextContent -> checkForInvalidDirective(element, holder)
        }
    }

    private fun checkForMissingEndTag(tag: PsiElement, holder: AnnotationHolder, tagName: String) {
        // A well-formed def_tag/block_tag has END_TAG as its last child (e.g., </%def>, </%block>).
        // When the tag is unclosed, GrammarKit's recoverWhile prevents a total parse failure but
        // the resulting PSI node has no END_TAG child — detectable here.
        val hasEndTag = tag.node.findChildByType(MakoTokenTypes.END_TAG) != null
        if (!hasEndTag) {
            // Annotate only the opening token (<%def or <%block), not the entire body.
            // Annotating the full body would draw a red range over multi-line content, which is confusing.
            val openToken = tag.firstChild ?: return
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed $tagName: missing closing tag")
                .range(openToken.textRange)
                .create()
        }
    }

    private fun checkForInvalidDirective(element: PsiElement, holder: AnnotationHolder) {
        // The lexer emits TEMPLATE_TEXT (and thus MakoTemplateTextContent PSI nodes) for unrecognized
        // directive-like sequences such as <%bogus> because they never match a valid grammar rule.
        // Scan the element text for <%name patterns and flag any name not in the known directive set.
        val match = INVALID_DIRECTIVE_REGEX.find(element.text) ?: return
        val name = match.groupValues[1]
        if (name !in VALID_DIRECTIVES) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown Mako directive: <%$name>")
                .range(element.textRange)
                .create()
        }
    }
}

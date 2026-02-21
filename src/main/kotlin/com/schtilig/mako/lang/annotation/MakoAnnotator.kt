package com.schtilig.mako.lang.annotation

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.MakoTokenTypes
import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.schtilig.mako.lang.psi.MakoTemplateTextContent
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class MakoAnnotator : Annotator {

    companion object {
        private val VALID_DIRECTIVES = setOf("def", "block", "inherit", "include", "namespace", "page", "doc")
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Guard: only process elements in Mako Template files
        if (element.containingFile.language != MakoLanguage) return

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
        // The Mako lexer emits '<', '%', and 'name...' as three separate TEMPLATE_TEXT tokens
        // for an unrecognized directive sequence such as <%bogus attr="x">. The '<' character
        // matches the single-char rule [$<%#], '%' matches the same rule, and 'name...' matches
        // the run rule [^$<%\r\n#]+. No single token ever contains the full "<%name" string.
        //
        // Detection strategy: when the current node text is '<', inspect the immediately following
        // sibling for '%' and then the sibling after that for a leading alphabetic directive name.
        // Annotate the '<' token range to flag the start of the invalid directive sequence.
        if (element.text != "<") return

        val percentSibling = element.nextSibling
        if (percentSibling !is MakoTemplateTextContent || percentSibling.text != "%") return

        val nameSibling = percentSibling.nextSibling
        if (nameSibling !is MakoTemplateTextContent) return

        val nameMatch = Regex("""^([a-zA-Z]+)""").find(nameSibling.text) ?: return
        val name = nameMatch.groupValues[1]
        if (name !in VALID_DIRECTIVES) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown Mako directive: <%$name>")
                .range(element.textRange)
                .create()
        }
    }
}

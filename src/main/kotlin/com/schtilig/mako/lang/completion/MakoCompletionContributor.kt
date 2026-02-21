package com.schtilig.mako.lang.completion

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.MakoTokenTypes
import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.schtilig.mako.lang.psi.MakoIncludeTag
import com.schtilig.mako.lang.psi.MakoInheritTag
import com.schtilig.mako.lang.psi.MakoNamespaceTag
import com.schtilig.mako.lang.psi.MakoPageTag
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.util.ProcessingContext

private val TAG_NAMES = listOf("<%def", "<%block", "<%inherit", "<%include", "<%namespace", "<%page", "<%doc")

/**
 * Returns the attribute list for the given PSI tag element, or null if the element
 * is not a recognised Mako open-tag composite.
 *
 * Uses Kotlin `is` (JVM instanceof) checks so that generated *Impl classes are matched
 * via their PSI interface — javaClass equality would fail because the impl class differs
 * from the interface class.
 */
private fun attrsForTag(element: PsiElement): List<String>? = when (element) {
    is MakoDefTag -> listOf(
        "name", "buffered", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region", "filter", "decorator"
    )
    is MakoBlockTag -> listOf(
        "name", "filter", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region"
    )
    is MakoInheritTag -> listOf("file")
    is MakoIncludeTag -> listOf("file", "args")
    is MakoNamespaceTag -> listOf("name", "file", "import", "module")
    is MakoPageTag -> listOf(
        "args", "expression_filter", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region"
    )
    else -> null
}

/**
 * Provides Mako-specific completions:
 * - COMP-01: Tag-name completion after `<%` (e.g., <%def, <%block, <%inherit, ...)
 * - COMP-02: Tag-attribute completion inside open tags (e.g., name="", file="", ...)
 *
 * Registered via <completion.contributor language="any"> in plugin.xml.
 * The `language="any"` registration is required because the caret position when typing
 * after "<%<caret>" falls on a TEMPLATE_TEXT token which may be associated with the
 * template data language rather than the Mako Template language. Using language="any"
 * ensures the contributor fires; the internal language guard restricts output to Mako files.
 */
class MakoCompletionContributor : CompletionContributor() {

    init {
        // COMP-01: Tag-name completion — broad pattern; narrowed by raw text inspection inside provider
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            TagNameCompletionProvider()
        )

        // COMP-02: Attribute completion when caret is on a TAG_ATTR_NAME token
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(MakoTokenTypes.TAG_ATTR_NAME),
            TagAttrCompletionProvider()
        )

        // COMP-02 fallback: Attribute completion when caret is on whitespace inside a tag
        // (e.g., right after the tag opening keyword, before first attribute has been typed)
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TokenType.WHITE_SPACE),
            TagAttrCompletionProvider()
        )
    }

    // -------------------------------------------------------------------------
    // Tag-name completion provider (COMP-01)
    // -------------------------------------------------------------------------

    private class TagNameCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val file = parameters.originalFile

            // Language guard: only fire in Mako Template files
            if (file.language != MakoLanguage) return

            val offset = parameters.offset
            if (offset < 2) return

            // Raw-text inspection: search back from caret for "<%" followed by optional partial tag letters.
            // Handles both "<%<caret>" (offset-2 check) and "<%d<caret>" (partial tag name already typed).
            // Uses document.charsSequence (a CharSequence view) instead of allocating a full file text copy.
            val chars = parameters.editor.document.charsSequence
            var ltPos = -1
            var i = offset - 1
            while (i > 0) {
                if (chars[i] == '%' && chars[i - 1] == '<') {
                    ltPos = i - 1
                    break
                }
                i--
            }
            if (ltPos < 0) return
            val partial = chars.subSequence(ltPos + 2, offset).toString() // text between "<%" and caret
            // Only trigger if partial is either empty or consists solely of tag-name characters
            // (letters only — tag names have no digits or underscores after the <% prefix).
            if (partial.isNotEmpty() && !partial.all { it.isLetter() }) return
            // Ensure there's no whitespace between "<%" and the caret (would indicate an
            // anonymous code block, not a tag name position).
            if (partial.any { it.isWhitespace() }) return

            // Use empty prefix matcher so the platform does not filter out items whose
            // lookup string starts with "<%" (which the platform would treat as a non-match
            // against any typed partial text after the "<%").
            val adjustedResult = result.withPrefixMatcher("")

            for (tagName in TAG_NAMES) {
                val element = if (tagName == "<%doc") {
                    LookupElementBuilder.create(tagName)
                        .withPresentableText(tagName)
                        .withBoldness(true)
                        .withInsertHandler { ctx, _ ->
                            val docSnippet = "<%doc>\n</%doc>"
                            ctx.document.replaceString(ltPos, ctx.tailOffset, docSnippet)
                            ctx.commitDocument()
                            // Position caret on the blank line between the doc tags
                            val innerOffset = ltPos + "<%doc>\n".length
                            ctx.editor.caretModel.moveToOffset(innerOffset)
                        }
                } else {
                    LookupElementBuilder.create(tagName)
                        .withPresentableText(tagName)
                        .withBoldness(true)
                        .withInsertHandler { ctx, _ ->
                            // Replace the typed "<%" (and any partial letters) with the full tag name + space
                            ctx.document.replaceString(ltPos, ctx.tailOffset, "$tagName ")
                            ctx.commitDocument()
                        }
                }
                adjustedResult.addElement(element)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tag-attribute completion provider (COMP-02)
    // -------------------------------------------------------------------------

    private class TagAttrCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Walk up the PSI tree to find the enclosing tag composite node.
            // Uses attrsForTag() which uses Kotlin `is` (instanceof) checks against the
            // PSI interface types — so generated *Impl classes are matched correctly.
            var element: PsiElement? = parameters.position.parent
            while (element != null && element !is PsiFile) {
                val attrs = attrsForTag(element)
                if (attrs != null) {
                    for (attrName in attrs) {
                        val lookupElement = LookupElementBuilder.create(attrName)
                            .withTailText("=\"\"", true)
                            .withInsertHandler { ctx, _ ->
                                ctx.document.insertString(ctx.tailOffset, "=\"\"")
                                ctx.commitDocument()
                                // Move caret between the quotes
                                ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                            }
                        result.addElement(lookupElement)
                    }
                    return
                }
                element = element.parent
            }
        }
    }
}

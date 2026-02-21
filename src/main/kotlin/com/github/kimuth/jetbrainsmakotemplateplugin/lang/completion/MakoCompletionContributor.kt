package com.github.kimuth.jetbrainsmakotemplateplugin.lang.completion

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoIncludeTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoInheritTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoNamespaceTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoPageTag
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

private val TAG_ATTRIBUTES: Map<Class<out PsiElement>, List<String>> = mapOf(
    MakoDefTag::class.java to listOf(
        "name", "buffered", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region", "filter", "decorator"
    ),
    MakoBlockTag::class.java to listOf(
        "name", "filter", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region"
    ),
    MakoInheritTag::class.java to listOf("file"),
    MakoIncludeTag::class.java to listOf("file", "args"),
    MakoNamespaceTag::class.java to listOf("name", "file", "import", "module"),
    MakoPageTag::class.java to listOf(
        "args", "expression_filter", "cached", "cache_key",
        "cache_timeout", "cache_type", "cache_url",
        "cache_dir", "cache_region"
    )
)

/**
 * Provides Mako-specific completions:
 * - COMP-01: Tag-name completion after `<%` (e.g., <%def, <%block, <%inherit, ...)
 * - COMP-02: Tag-attribute completion inside open tags (e.g., name="", file="", ...)
 *
 * Registered via <completion.contributor language="Mako Template"> in plugin.xml.
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
            if (file.language.id != "Mako Template") return

            val offset = parameters.offset
            if (offset < 2) return

            // Raw-text inspection: look for "<%" immediately before the caret
            val twoCharPrefix = file.text.substring(offset - 2, offset)
            if (twoCharPrefix != "<%") return

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
                            ctx.document.replaceString(ctx.startOffset - 2, ctx.tailOffset, docSnippet)
                            ctx.commitDocument()
                            // Position caret on the blank line between the doc tags
                            val innerOffset = ctx.startOffset - 2 + "<%doc>\n".length
                            ctx.editor.caretModel.moveToOffset(innerOffset)
                        }
                } else {
                    LookupElementBuilder.create(tagName)
                        .withPresentableText(tagName)
                        .withBoldness(true)
                        .withInsertHandler { ctx, _ ->
                            // Replace the typed "<%" (and any partial letters) with the full tag name + space
                            ctx.document.replaceString(ctx.startOffset - 2, ctx.tailOffset, "$tagName ")
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
            // Walk up the PSI tree to find the enclosing tag composite node
            var element: PsiElement? = parameters.position.parent
            while (element != null && element !is PsiFile) {
                val attrs = TAG_ATTRIBUTES[element.javaClass]
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

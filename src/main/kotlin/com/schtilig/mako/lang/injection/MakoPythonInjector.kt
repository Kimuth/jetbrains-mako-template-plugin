package com.schtilig.mako.lang.injection

import com.schtilig.mako.MakoLanguage
import com.schtilig.mako.lang.MakoTokenTypes
import com.schtilig.mako.lang.psi.MakoCodeBlock
import com.schtilig.mako.lang.psi.MakoExpression
import com.schtilig.mako.lang.psi.MakoModuleBlock
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil

/**
 * Injects Python into Mako expression and code block regions.
 *
 * Registered via <multiHostInjector> in plugin.xml. The three host types
 * (MakoExpression, MakoCodeBlock, MakoModuleBlock) implement PsiLanguageInjectionHost
 * via their respective mixins (see MakoExpressionMixin, MakoCodeBlockMixin, MakoModuleBlockMixin).
 *
 * Multi-host injection strategy for MakoCodeBlock and MakoExpression:
 * All MakoCodeBlock and MakoExpression elements in a file are combined into a single
 * multi-host Python injection (one startInjecting/doneInjecting call). This gives the Python
 * language service a shared scope across all code blocks and expressions, eliminating false-positive
 * "Unresolved Reference" warnings for variables defined in one block and used in another
 * (e.g., a variable defined in a top-level <% %> block used inside a <%def> block).
 *
 * Only the FIRST host in document order triggers injection; all other hosts return early and
 * are included via the first host's addPlace() loop.
 *
 * MakoModuleBlock (<%! %>) remains an independent Python injection with its own scope,
 * because module-level declarations have different semantics.
 *
 * TextRange offsets are relative to each host node's own text (offset 0 = start of node text).
 * The platform handles host-to-document coordinate mapping automatically.
 */
class MakoPythonInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(
            MakoExpression::class.java,
            MakoCodeBlock::class.java,
            MakoModuleBlock::class.java
        )

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        // Defensive null-check: PythonCore must be loaded (declared in plugin.xml depends),
        // but guard against unexpected configurations.
        val python = Language.findLanguageByID("Python") ?: return

        when (context) {
            is MakoCodeBlock, is MakoExpression -> {
                val hosts = collectCodeAndExpressionHosts(context)
                // Only the FIRST host in document order starts the multi-host injection.
                // All other hosts return early — they are included via the first host's addPlace() loop.
                if (hosts.firstOrNull() != context) return
                registrar.startInjecting(python)
                var placed = false
                for (host in hosts) {
                    val text = host.text ?: continue
                    when (host) {
                        is MakoCodeBlock -> {
                            // <%<python>%> — strip <% (2) and %> (2)
                            val start = 2
                            val end = text.length - 2
                            if (end > start) {
                                registrar.addPlace(null, "\n", host, TextRange(start, end))
                                placed = true
                            }
                        }
                        is MakoExpression -> {
                            // ${<python>} — strip ${ (2) and } (1); stop at FILTER_SEP if present
                            val start = 2
                            val end = expressionPythonEnd(host, text)
                            if (end > start) {
                                registrar.addPlace("_ = ", "\n", host, TextRange(start, end))
                                placed = true
                            }
                        }
                    }
                }
                if (placed) registrar.doneInjecting()
            }
            is MakoModuleBlock -> {
                // Node text: <%!<python>%>
                // Exclude <%! (3 chars) at start and %> (2 chars) at end.
                val nodeText = context.text ?: return
                val start = 3
                val end = nodeText.length - 2
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
        }
    }

    /**
     * Computes the end offset (relative to host text) for a MakoExpression's Python content.
     *
     * Stops at the first FILTER_SEP child so filter names (e.g., h, trim in
     * ${x | h, trim}) are not presented to the Python language service as Python code.
     */
    private fun expressionPythonEnd(expr: MakoExpression, text: String): Int {
        val filterSep = expr.node.firstChildNode?.let { first ->
            var child = first
            var found: com.intellij.lang.ASTNode? = null
            while (found == null) {
                if (child.elementType == MakoTokenTypes.FILTER_SEP) found = child
                child = child.treeNext ?: break
            }
            found
        }
        // filterSep?.startOffset is document-absolute; subtract expr.textRange.startOffset
        // to get offset relative to this host node.
        return if (filterSep != null) {
            filterSep.startOffset - expr.textRange.startOffset
        } else {
            text.length - 1
        }
    }

    /**
     * Collects all MakoCodeBlock and MakoExpression elements in the Mako PSI root, sorted by
     * document order.
     *
     * Uses viewProvider.getPsi(MakoLanguage) to obtain the Mako PSI root explicitly, rather than
     * relying on containingFile. In the dual-tree environment created by MakoFileViewProvider,
     * context.containingFile may return the HTML PSI file for some element positions; this method
     * always searches the correct Mako PSI tree regardless of which PSI root the context belongs to.
     *
     * PsiTreeUtil.findChildrenOfType recurses into nested structures (including <%def> blocks),
     * so all code blocks and expressions at any nesting depth are included.
     */
    private fun collectCodeAndExpressionHosts(context: PsiElement): List<PsiLanguageInjectionHost> {
        val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
        val codeBlocks = PsiTreeUtil.findChildrenOfType(makoFile, MakoCodeBlock::class.java)
        val expressions = PsiTreeUtil.findChildrenOfType(makoFile, MakoExpression::class.java)
        return (codeBlocks + expressions).sortedBy { it.textOffset }
    }
}

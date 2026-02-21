package com.github.kimuth.jetbrainsmakotemplateplugin.lang.injection

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoCodeBlock
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoExpression
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoModuleBlock
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

/**
 * Injects Python into Mako expression and code block regions.
 *
 * Registered via <multiHostInjector> in plugin.xml. The three host types
 * (MakoExpression, MakoCodeBlock, MakoModuleBlock) implement PsiLanguageInjectionHost
 * via their respective mixins (see MakoExpressionMixin, MakoCodeBlockMixin, MakoModuleBlockMixin).
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

        val nodeText = context.text ?: return

        when (context) {
            is MakoExpression -> {
                // Node text: ${<python>}
                // Exclude ${ (2 chars) at start and } (1 char) at end.
                val start = 2
                val end = nodeText.length - 1
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
            is MakoCodeBlock -> {
                // Node text: <%<python>%>
                // Exclude <% (2 chars) at start and %> (2 chars) at end.
                val start = 2
                val end = nodeText.length - 2
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
            is MakoModuleBlock -> {
                // Node text: <%!<python>%>
                // Exclude <%! (3 chars) at start and %> (2 chars) at end.
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
}

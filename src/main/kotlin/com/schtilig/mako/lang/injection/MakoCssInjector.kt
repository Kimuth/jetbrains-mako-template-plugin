package com.schtilig.mako.lang.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlText
import com.intellij.xml.util.HtmlUtil

/**
 * Injects CSS into the text content of `<style>` elements within the HTML PSI tree of `.mako` files.
 *
 * The HTML PSI tree is created by MakoFileViewProvider (TemplateLanguageFileViewProvider from Phase 18).
 * This injector targets XmlText nodes whose parent tag is a `<style>` element inside an HTML file.
 * XmlTextImpl implements PsiLanguageInjectionHost (confirmed via javap on PyCharm 2025.2.6 app.jar).
 *
 * Requires the CSS plugin (com.intellij.css) to be installed for CSS completions and validation.
 * Gracefully no-ops when CSS plugin is absent — Language.findLanguageByID("CSS") returns null.
 * Do NOT use CssLanguage.INSTANCE directly — that causes ClassNotFoundException at startup when
 * the CSS plugin is not installed. The null guard via Language.findLanguageByID is the safe approach.
 *
 * JavaScript injection into `<script>` elements is handled automatically by the platform's
 * HtmlScriptLanguageInjector (registered in PyCharmCorePlugin.xml) — no custom injector needed (HINJ-06).
 *
 * Registered via `<multiHostInjector>` in plugin.xml (HINJ-05).
 */
class MakoCssInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(XmlText::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        // CRITICAL: Do NOT use CssLanguage.INSTANCE — causes ClassNotFoundException when CSS plugin absent.
        val css = Language.findLanguageByID("CSS") ?: return

        if (!context.isValid) return
        val xmlText = context as? XmlText ?: return

        // Guard: restricts to HTML files only; prevents injection into XML config files or other
        // non-HTML XML that happen to have a <style> element name.
        if (!HtmlUtil.isHtmlTagContainingFile(xmlText)) return

        val parentTag = xmlText.parentTag ?: return

        // Guard: only inject for <style> elements.
        // Using localName string comparison (NOT HtmlUtil.isStyleTag) — isStyleTag was not confirmed
        // via javap on PyCharm 2025.2.6 HtmlUtil (output truncated); string comparison is the safe fallback.
        if (parentTag.localName.lowercase() != "style") return

        val text = xmlText.text ?: return
        if (text.isEmpty()) return

        registrar
            .startInjecting(css)
            .addPlace(null, null, xmlText as PsiLanguageInjectionHost, TextRange(0, text.length))
            .doneInjecting()
    }
}

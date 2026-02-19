package com.github.kimuth.jetbrainsmakotemplateplugin

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

object MakoLanguage : Language("Mako Template"), TemplateLanguage {
    // Language ID "Mako Template" MUST match the `language` attribute in plugin.xml fileType registration.
    // This is a TemplateLanguage per LANG-03 — signals to the platform this is a template engine language.

    // Suppress deserialization-related warning for Kotlin objects
    private fun readResolve(): Any = MakoLanguage
}

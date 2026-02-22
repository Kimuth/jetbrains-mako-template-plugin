package com.schtilig.mako.lang.annotation

import com.schtilig.mako.lang.MakoFileViewProvider
import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.codeInsight.highlighting.TemplateLanguageErrorFilter
import com.intellij.psi.tree.TokenSet

/**
 * Suppresses false-positive HTML error squiggles caused by Mako expression and control
 * line boundaries in the HTML PSI tree.
 *
 * When MakoFileViewProvider constructs the dual PSI tree, Mako constructs (${...} expressions
 * and %for/%if control lines) appear as OuterLanguageElement placeholder nodes in the HTML
 * tree. The HTML annotator may interpret the boundary positions as malformed HTML and emit
 * spurious ERROR-severity PsiErrorElement highlights.
 *
 * TemplateLanguageErrorFilter.shouldHighlightErrorElement() examines each PsiErrorElement and
 * suppresses it when the error is adjacent to an OuterLanguageElement whose token type is in
 * the provided TokenSet and whose view provider matches MakoFileViewProvider.
 *
 * CRCT-01: Suppresses false positives on ${...} expressions (EXPR_START / EXPR_END).
 * CRCT-02: Suppresses false positives on %for/%if/%endif control lines (CONTROL_LINE).
 *
 * Registered in plugin.xml via the com.intellij.highlightErrorFilter extension point.
 *
 * Source: adapted from HbErrorFilter.java (JetBrains/intellij-plugins/handlebars)
 * TemplateLanguageErrorFilter verified in com.intellij.codeInsight.highlighting, app-client.jar
 */
class MakoErrorFilter : TemplateLanguageErrorFilter(
    TokenSet.create(
        MakoTokenTypes.EXPR_START,    // ${ — opening of ${...} expression
        MakoTokenTypes.EXPR_END,      // }  — closing of ${...} expression
        MakoTokenTypes.CONTROL_LINE   // %for, %if, %endif, %endfor lines
    ),
    MakoFileViewProvider::class.java,
    "HTML"
)

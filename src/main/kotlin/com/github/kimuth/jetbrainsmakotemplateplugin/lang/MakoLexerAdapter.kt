package com.github.kimuth.jetbrainsmakotemplateplugin.lang

import com.intellij.lexer.FlexAdapter

/**
 * Wraps [_MakoLexer] with state encoding that includes [_MakoLexer.braceDepth].
 *
 * JFlex state integers alone don't capture the brace nesting depth tracked by
 * the EXPRESSION state. IntelliJ's incremental re-lexing saves/restores the
 * state integer returned by [getState] and passes it back via [start].
 * Without encoding braceDepth, a restart inside a nested expression like
 * `${{'key': 'val'}}` would lose the depth, causing early EXPR_END emission
 * and token stream corruption.
 *
 * Encoding: bits 0-3 = JFlex state (max 10), bits 4-7 = braceDepth (max 15).
 */
class MakoLexerAdapter : FlexAdapter(_MakoLexer()) {

    private val makoFlex: _MakoLexer get() = flex as _MakoLexer

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        val jflexState = initialState and 0xF
        val depth = (initialState ushr 4) and 0xF
        super.start(buffer, startOffset, endOffset, jflexState)
        makoFlex.braceDepth = depth
    }

    override fun getState(): Int {
        val jflexState = super.getState()
        val depth = makoFlex.braceDepth.coerceIn(0, 0xF)
        return jflexState or (depth shl 4)
    }
}

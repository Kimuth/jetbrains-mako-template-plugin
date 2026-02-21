package com.schtilig.mako.lang.highlighting

import com.schtilig.mako.lang.MakoTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class MakoPairedBraceMatcher : PairedBraceMatcher {

    private val PAIRS = arrayOf(
        // structural=false for ALL pairs: shared END_TAG token causes conflicts with structural=true
        BracePair(MakoTokenTypes.TAG_OPEN_DEF,   MakoTokenTypes.END_TAG,   false),
        BracePair(MakoTokenTypes.TAG_OPEN_BLOCK,  MakoTokenTypes.END_TAG,   false),
        BracePair(MakoTokenTypes.DOC_OPEN,        MakoTokenTypes.DOC_CLOSE, false),
        BracePair(MakoTokenTypes.EXPR_START,      MakoTokenTypes.EXPR_END,  false),
        BracePair(MakoTokenTypes.CODE_OPEN,       MakoTokenTypes.CODE_CLOSE, false)
    )

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}

package com.schtilig.mako.lang.psi

import com.schtilig.mako.MakoFileType
import com.schtilig.mako.MakoLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class MakoFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MakoLanguage) {
    override fun getFileType() = MakoFileType
}

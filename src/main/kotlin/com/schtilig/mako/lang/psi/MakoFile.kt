package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoFileType
import com.github.kimuth.jetbrainsmakotemplateplugin.MakoLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class MakoFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MakoLanguage) {
    override fun getFileType() = MakoFileType
}

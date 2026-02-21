package com.schtilig.mako

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MakoFileType : LanguageFileType(MakoLanguage) {
    override fun getName(): String = "Mako Template"
    override fun getDescription(): String = "Mako template file"
    override fun getDefaultExtension(): String = "mako"
    override fun getIcon(): Icon = MakoIcons.FILE
}

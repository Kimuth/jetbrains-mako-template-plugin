package com.schtilig.mako.lang.editing

import com.intellij.lang.Commenter

class MakoCommenter : Commenter {

    // Trailing space matches Python's "# " style convention
    override fun getLineCommentPrefix(): String = "## "

    override fun getBlockCommentPrefix(): String = "<%doc>"

    override fun getBlockCommentSuffix(): String = "</%doc>"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

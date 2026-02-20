package com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoFile
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoControlLineStmt
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDocComment
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoModuleBlock
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.Collections

class MakoFoldingBuilder : FoldingBuilderEx(), DumbAware {

    companion object {
        private val OPENING_KEYWORDS = setOf("for", "if", "while")
        private val CLOSING_KEYWORDS = setOf("endfor", "endif", "endwhile")
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // 1. def_tag folding (expanded by default)
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.add(FoldingDescriptor(def.node, def.textRange))
        }

        // 2. block_tag folding (expanded by default)
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.add(FoldingDescriptor(block.node, block.textRange))
        }

        // 3. doc_comment folding (collapsed by default)
        PsiTreeUtil.collectElementsOfType(root, MakoDocComment::class.java).forEach { doc ->
            descriptors.add(
                FoldingDescriptor(
                    doc.node,
                    doc.textRange,
                    null,
                    Collections.emptySet(),
                    false,
                    "<%doc>...</%doc>",
                    true
                )
            )
        }

        // 4. module_block folding (collapsed by default)
        PsiTreeUtil.collectElementsOfType(root, MakoModuleBlock::class.java).forEach { module ->
            descriptors.add(
                FoldingDescriptor(
                    module.node,
                    module.textRange,
                    null,
                    Collections.emptySet(),
                    false,
                    "<%!...%>",
                    true
                )
            )
        }

        // 5. Control flow folding via sibling-scan
        // Scan root file element
        if (root is MakoFile) {
            descriptors.addAll(buildControlFlowFoldsUnder(root))
        }

        // Scan inside each def_tag and block_tag
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.addAll(buildControlFlowFoldsUnder(def))
        }
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.addAll(buildControlFlowFoldsUnder(block))
        }

        return descriptors.toTypedArray()
    }

    private fun extractKeyword(text: String): String {
        return text.trimStart().removePrefix("%").trimStart().split(Regex("\\s+"))[0].trimEnd(':')
    }

    private fun buildControlFlowFoldsUnder(parent: PsiElement): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val stack = ArrayDeque<MakoControlLineStmt>()

        var child = parent.firstChild
        while (child != null) {
            if (child is MakoControlLineStmt) {
                val keyword = extractKeyword(child.text)
                when {
                    keyword in OPENING_KEYWORDS -> stack.addLast(child)
                    keyword in CLOSING_KEYWORDS && stack.isNotEmpty() -> {
                        val openLine = stack.removeLast()
                        descriptors.add(
                            FoldingDescriptor(
                                openLine.node,
                                TextRange(openLine.textRange.startOffset, child.textRange.endOffset)
                            )
                        )
                    }
                }
            }
            child = child.nextSibling
        }

        return descriptors
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}

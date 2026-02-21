package com.schtilig.mako.lang.folding

import com.schtilig.mako.lang.MakoTokenTypes
import com.schtilig.mako.lang.psi.MakoBlockTag
import com.schtilig.mako.lang.psi.MakoDefTag
import com.schtilig.mako.lang.psi.MakoFile
import com.schtilig.mako.lang.psi.MakoTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
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
        // Use findClosingTokenEnd to avoid including error-recovery tokens from recoverWhile
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            val endOffset = findClosingTokenEnd(def.node, MakoTokenTypes.END_TAG)
            if (endOffset > def.node.startOffset) {
                descriptors.add(FoldingDescriptor(def.node, TextRange(def.node.startOffset, endOffset)))
            }
        }

        // 2. block_tag folding (expanded by default)
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            val endOffset = findClosingTokenEnd(block.node, MakoTokenTypes.END_TAG)
            if (endOffset > block.node.startOffset) {
                descriptors.add(FoldingDescriptor(block.node, TextRange(block.node.startOffset, endOffset)))
            }
        }

        // 3. doc_comment folding (collapsed by default)
        buildDocCommentFolds(root, descriptors)

        // 4. module_block folding (collapsed by default)
        buildModuleBlockFolds(root, descriptors)

        // 5. code_block folding (expanded by default)
        buildCodeBlockFolds(root, descriptors)

        // 6. Control flow folding via sibling-scan
        if (root is MakoFile) {
            descriptors.addAll(buildControlFlowFoldsUnder(root))
        }
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.addAll(buildControlFlowFoldsUnder(def))
        }
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.addAll(buildControlFlowFoldsUnder(block))
        }

        return descriptors.toTypedArray()
    }

    /**
     * Find the end offset of the last occurrence of [closingType] within [composite].
     * GrammarKit's recoverWhile can extend composites beyond the actual closing token
     * (e.g., TEMPLATE_TEXT absorbed into CODE_BLOCK after %> via tag_recover).
     * This method scans the composite's children to find the real closing token.
     *
     * Falls back to composite.textRange.endOffset if no closing token is found.
     */
    private fun findClosingTokenEnd(composite: ASTNode, closingType: IElementType): Int {
        var lastClose: ASTNode? = null
        var child = composite.firstChildNode
        while (child != null) {
            if (child.elementType == closingType) {
                lastClose = child
            }
            child = child.treeNext
        }
        return if (lastClose != null) {
            lastClose.startOffset + lastClose.textLength
        } else {
            composite.textRange.endOffset
        }
    }

    /**
     * Walk all ASTNodes depth-first. The [visitor] returns true to recurse into children,
     * false to skip children of the visited node (used when the composite is already folded).
     */
    private fun walkAllNodes(node: ASTNode?, visitor: (ASTNode) -> Boolean) {
        var current = node
        while (current != null) {
            val recurse = visitor(current)
            if (recurse) {
                walkAllNodes(current.firstChildNode, visitor)
            }
            current = current.treeNext
        }
    }

    private fun buildDocCommentFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        walkAllNodes(root.node.firstChildNode) { node ->
            if (node.elementType == MakoTokenTypes.DOC_OPEN) {
                var sibling = node.treeNext
                while (sibling != null && sibling.elementType != MakoTokenTypes.DOC_CLOSE) {
                    sibling = sibling.treeNext
                }
                if (sibling != null) {
                    val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                    descriptors.add(
                        FoldingDescriptor(
                            node, range, null, Collections.emptySet(),
                            false, "<%doc>...</%doc>", true
                        )
                    )
                }
                false // DOC_OPEN is a leaf; no children to recurse into
            } else {
                true
            }
        }
    }

    /**
     * Scan for module blocks: MODULE_BLOCK composites or raw MODULE_OPEN tokens.
     * For composites, use findClosingTokenEnd to get the real end (before error recovery).
     * For raw tokens, scan forward siblings for CODE_CLOSE.
     */
    private fun buildModuleBlockFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        walkAllNodes(root.node.firstChildNode) { node ->
            when {
                node.elementType == MakoTypes.MODULE_BLOCK -> {
                    val endOffset = findClosingTokenEnd(node, MakoTokenTypes.CODE_CLOSE)
                    if (endOffset > node.startOffset) {
                        descriptors.add(
                            FoldingDescriptor(
                                node, TextRange(node.startOffset, endOffset), null,
                                Collections.emptySet(), false, "<%!...%>", true
                            )
                        )
                    }
                    false // already folded the composite; skip children to avoid double-fold
                }
                node.elementType == MakoTokenTypes.MODULE_OPEN -> {
                    var sibling = node.treeNext
                    while (sibling != null && sibling.elementType != MakoTokenTypes.CODE_CLOSE) {
                        sibling = sibling.treeNext
                    }
                    if (sibling != null) {
                        val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                        descriptors.add(
                            FoldingDescriptor(
                                node, range, null, Collections.emptySet(),
                                false, "<%!...%>", true
                            )
                        )
                    }
                    false // leaf token; no children
                }
                else -> true
            }
        }
    }

    /**
     * Scan for code blocks: CODE_BLOCK composites or raw CODE_OPEN tokens.
     * For composites, use findClosingTokenEnd to get the real end (before error recovery).
     * For raw tokens, scan forward siblings for CODE_CLOSE.
     */
    private fun buildCodeBlockFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        walkAllNodes(root.node.firstChildNode) { node ->
            when {
                node.elementType == MakoTypes.CODE_BLOCK -> {
                    val endOffset = findClosingTokenEnd(node, MakoTokenTypes.CODE_CLOSE)
                    if (endOffset > node.startOffset) {
                        descriptors.add(FoldingDescriptor(node, TextRange(node.startOffset, endOffset)))
                    }
                    false // already folded the composite; skip children to avoid double-fold
                }
                node.elementType == MakoTokenTypes.CODE_OPEN -> {
                    var sibling = node.treeNext
                    while (sibling != null && sibling.elementType != MakoTokenTypes.CODE_CLOSE) {
                        sibling = sibling.treeNext
                    }
                    if (sibling != null) {
                        val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                        descriptors.add(FoldingDescriptor(node, range))
                    }
                    false // leaf token; no children
                }
                else -> true
            }
        }
    }

    private fun extractKeyword(text: String): String {
        return text.trimStart().removePrefix("%").trimStart().split(Regex("\\s+"))[0].trimEnd(':')
    }

    private fun collectControlLineNodes(parent: ASTNode): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        var node = parent.firstChildNode
        while (node != null) {
            when (node.elementType) {
                MakoTokenTypes.CONTROL_LINE -> result.add(node)
                MakoTypes.CONTROL_LINE_STMT -> result.add(node)
                else -> {
                    val isDummy = node.psi.language == Language.ANY
                    if (isDummy) {
                        result.addAll(collectControlLineNodes(node))
                    }
                }
            }
            node = node.treeNext
        }
        return result
    }

    private fun buildControlFlowFoldsUnder(parent: PsiElement): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val stack = ArrayDeque<ASTNode>()

        for (node in collectControlLineNodes(parent.node)) {
            val keyword = extractKeyword(node.text)
            when {
                keyword in OPENING_KEYWORDS -> stack.addLast(node)
                keyword in CLOSING_KEYWORDS && stack.isNotEmpty() -> {
                    val openNode = stack.removeLast()
                    descriptors.add(
                        FoldingDescriptor(
                            openNode,
                            TextRange(openNode.startOffset, node.startOffset + node.textLength)
                        )
                    )
                }
            }
        }

        return descriptors
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}

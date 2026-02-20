package com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoFile
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes
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
        // DOC_OPEN/DOC_CONTENT/DOC_CLOSE tokens are in getCommentTokens() so they are treated as
        // PsiComment nodes by the platform (flat siblings, not wrapped in a MakoDocComment composite).
        // Scan the AST for DOC_OPEN nodes and pair each with its following DOC_CLOSE sibling.
        buildDocCommentFolds(root, descriptors)

        // 4. module_block folding (collapsed by default)
        // Use AST-level scan for MODULE_OPEN tokens paired with CODE_CLOSE.
        // The PSI-level MakoModuleBlock composite may not exist in all environments
        // (e.g., TemplateLanguage file view providers can alter the PSI tree structure).
        buildModuleBlockFolds(root, descriptors)

        // 5. code_block folding (expanded by default)
        // Same AST approach: CODE_OPEN paired with CODE_CLOSE.
        buildCodeBlockFolds(root, descriptors)

        // 6. Control flow folding via sibling-scan
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

    /**
     * Scan for DOC_OPEN tokens in the AST and pair each with its following DOC_CLOSE sibling.
     * Because DOC_OPEN/DOC_CONTENT/DOC_CLOSE are in the comment token set, the platform creates
     * flat PsiComment nodes for them rather than wrapping them in a MakoDocComment composite.
     * We must walk the AST to find DOC_OPEN nodes directly.
     */
    private fun buildDocCommentFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        var node = root.node.firstChildNode
        while (node != null) {
            if (node.elementType == MakoTokenTypes.DOC_OPEN) {
                var sibling = node.treeNext
                while (sibling != null && sibling.elementType != MakoTokenTypes.DOC_CLOSE) {
                    sibling = sibling.treeNext
                }
                if (sibling != null) {
                    val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                    descriptors.add(
                        FoldingDescriptor(
                            node,
                            range,
                            null,
                            Collections.emptySet(),
                            false,
                            "<%doc>...</%doc>",
                            true
                        )
                    )
                }
            }
            node = node.treeNext
        }
    }

    /**
     * Scan for MODULE_OPEN tokens in the AST and pair each with the next CODE_CLOSE.
     * Uses AST-level scan (like doc_comment) rather than PSI-level MakoModuleBlock
     * because the TemplateLanguage file view provider may alter the PSI tree structure
     * such that MODULE_BLOCK composites are not created.
     */
    private fun buildModuleBlockFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        var node = root.node.firstChildNode
        while (node != null) {
            if (node.elementType == MakoTokenTypes.MODULE_OPEN ||
                node.elementType == MakoTypes.MODULE_BLOCK) {
                if (node.elementType == MakoTypes.MODULE_BLOCK) {
                    // PSI composite exists — use its full range
                    descriptors.add(
                        FoldingDescriptor(
                            node,
                            node.textRange,
                            null,
                            Collections.emptySet(),
                            false,
                            "<%!...%>",
                            true
                        )
                    )
                } else {
                    // Raw MODULE_OPEN token — scan forward for CODE_CLOSE
                    var sibling = node.treeNext
                    while (sibling != null && sibling.elementType != MakoTokenTypes.CODE_CLOSE) {
                        sibling = sibling.treeNext
                    }
                    if (sibling != null) {
                        val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                        descriptors.add(
                            FoldingDescriptor(
                                node,
                                range,
                                null,
                                Collections.emptySet(),
                                false,
                                "<%!...%>",
                                true
                            )
                        )
                    }
                }
            }
            node = node.treeNext
        }
    }

    /**
     * Scan for CODE_OPEN tokens in the AST and pair each with the next CODE_CLOSE.
     * Anonymous code blocks (<% ... %>) are foldable but start expanded.
     */
    private fun buildCodeBlockFolds(root: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        var node = root.node.firstChildNode
        while (node != null) {
            if (node.elementType == MakoTokenTypes.CODE_OPEN ||
                node.elementType == MakoTypes.CODE_BLOCK) {
                if (node.elementType == MakoTypes.CODE_BLOCK) {
                    // PSI composite exists
                    descriptors.add(FoldingDescriptor(node, node.textRange))
                } else {
                    // Raw CODE_OPEN token — scan forward for CODE_CLOSE
                    var sibling = node.treeNext
                    while (sibling != null && sibling.elementType != MakoTokenTypes.CODE_CLOSE) {
                        sibling = sibling.treeNext
                    }
                    if (sibling != null) {
                        val range = TextRange(node.startOffset, sibling.startOffset + sibling.textLength)
                        descriptors.add(FoldingDescriptor(node, range))
                    }
                }
            }
            node = node.treeNext
        }
    }

    private fun extractKeyword(text: String): String {
        return text.trimStart().removePrefix("%").trimStart().split(Regex("\\s+"))[0].trimEnd(':')
    }

    /**
     * Collect control line ASTNodes in document order, transparently descending into
     * DUMMY_BLOCK wrappers that the error-recovery system may create when the parser
     * encounters unexpected tokens (e.g. orphaned END_TAG at file scope causes the
     * remaining tokens to be grouped into DUMMY_BLOCKs).
     *
     * We stop recursing when we reach structured composites that have their own scope
     * (DEF_TAG, BLOCK_TAG, CONTROL_LINE_STMT) so that control-flow pairs inside a
     * nested scope are not mixed with the parent scope.
     */
    private fun collectControlLineNodes(parent: ASTNode): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        var node = parent.firstChildNode
        while (node != null) {
            when (node.elementType) {
                MakoTokenTypes.CONTROL_LINE -> result.add(node)
                MakoTypes.CONTROL_LINE_STMT -> result.add(node)
                // Transparent containers: error recovery wraps leftover tokens in DUMMY_BLOCK.
                // Recurse into them so their CONTROL_LINE tokens are visible at this scope level.
                else -> {
                    val et = node.elementType
                    val isDummy = et.toString() == "DUMMY_BLOCK" ||
                            et.javaClass.simpleName == "DummyBlockElementType"
                    if (isDummy) {
                        result.addAll(collectControlLineNodes(node))
                    }
                    // DEF_TAG, BLOCK_TAG, CONTROL_LINE_STMT composites are not transparent:
                    // their interior is handled by separate calls to buildControlFlowFoldsUnder.
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

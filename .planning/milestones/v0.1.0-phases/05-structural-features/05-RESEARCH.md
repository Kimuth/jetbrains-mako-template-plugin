# Phase 5: Structural Features - Research

**Researched:** 2026-02-20
**Domain:** IntelliJ Platform code folding (FoldingBuilderEx) and Structure View (PsiStructureViewFactory, StructureViewTreeElement) for a custom language plugin
**Confidence:** HIGH (core APIs verified via official IntelliJ Platform SDK docs and JetBrains community source; PSI structure analyzed from generated files in this project)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SYNX-06 | User can fold/collapse `<%def>`, `<%block>`, and control flow blocks | `FoldingBuilderEx.buildFoldRegions()` creates `FoldingDescriptor` objects from PSI nodes; `MakoDefTag`/`MakoBlockTag` fold via their own `textRange`; control flow folds via sibling-scanning algorithm; `isCollapsedByDefault()` returns `true` for `doc_comment` and `module_block`, `false` for all others |
| EDIT-03 | Structure view panel shows all `<%def>` and `<%block>` declarations as navigable nodes | `PsiStructureViewFactory` + `StructureViewTreeElement` + `TextEditorBasedStructureViewModel`; `MakoDefTag`/`MakoBlockTag` already implement `PsiNamedElement` and have `getName()`; `getChildren()` on `MakoFile` collects these nodes; navigation via `NavigatablePsiElement.navigate()` |
</phase_requirements>

---

## Summary

Phase 5 adds two independent features: (1) code folding so users can collapse large template sections by clicking gutter arrows, and (2) a Structure View panel that shows all `<%def>` and `<%block>` declarations as a navigable tree.

For folding, the platform requires a `FoldingBuilderEx` implementation that traverses the PSI tree during `buildFoldRegions()` and returns `FoldingDescriptor` objects identifying which text ranges are collapsible. `MakoDefTag` and `MakoBlockTag` already contain their entire body as PSI children (the BNF rule `def_tag ::= TAG_OPEN_DEF ... TAG_CLOSE item_* END_TAG` means the full tag span is the node's `textRange`), making those trivial. Control flow (`% for`, `% if`, `% while`) is structurally different: each `MakoControlLineStmt` is a flat sibling in the parent — there is no parent-children-endfor tree hierarchy. Folding these requires a sibling-scanning algorithm: on each `MakoControlLineStmt` whose text matches a block-opening keyword (`for`, `if`, `while`), scan forward siblings to find the matching `% end*` line and build a `TextRange` from `controlLine.textRange.startOffset` to `endLine.textRange.endOffset`. The `<%doc>` (`MakoDocComment`) and `<%!>` (`MakoModuleBlock`) nodes must be collapsed by default when the file opens; all other foldable nodes start expanded.

For the Structure View, the platform provides `PsiStructureViewFactory`, `TreeBasedStructureViewBuilder`, `TextEditorBasedStructureViewModel`, and `StructureViewTreeElement`. The `MakoDefTagMixin` and `MakoBlockTagMixin` already implement `PsiNamedElement` with `getName()`, making them directly usable as navigable tree nodes — `navigate()` is inherited from `ASTWrapperPsiElement` which is `NavigatablePsiElement`. The Structure View implementation is a selective mirror of the PSI tree: `MakoFile` roots collect `MakoDefTag` and `MakoBlockTag` children; each of those also collects nested defs/blocks.

**Primary recommendation:** Implement `MakoFoldingBuilder` (extends `FoldingBuilderEx`, implements `DumbAware`) and the three-class Structure View stack (`MakoStructureViewFactory`, `MakoStructureViewModel`, `MakoStructureViewElement`). Register both in `plugin.xml`. Keep folding and structure view in separate classes; they have no runtime dependency on each other.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `FoldingBuilderEx` | Platform (bundled) | Abstract base for code folding; replaces older `FoldingBuilder` | Extends the old interface with `buildFoldRegions(PsiElement, Document, boolean)` (PSI-level, not just AST-level); the `quick` boolean allows fast-path on file open |
| `FoldingDescriptor` | Platform (bundled) | Represents a single collapsible region with AST anchor, TextRange, placeholder text | The only platform-approved way to declare a fold region |
| `DumbAware` | Platform (bundled) | Marker interface that allows a component to work during indexing | Required to pass folding tests; without it, tests fail because the test framework runs in dumb mode |
| `PsiStructureViewFactory` | Platform (bundled) | Entry point registered at `com.intellij.lang.psiStructureViewFactory` extension point | The canonical entry point for the Structure View feature |
| `TreeBasedStructureViewBuilder` | Platform (bundled) | Returns a model based on `TextEditorBasedStructureViewModel` to reuse platform UI | Standard for all custom language structure views that don't need fully custom UI |
| `TextEditorBasedStructureViewModel` | Platform (bundled) | Base class for structure view models; handles editor syncing | Provides `getSuitableClasses()` for cursor-to-node autoscroll |
| `StructureViewTreeElement` | Platform (bundled) | Interface for each node in the structure view tree | Must be implemented per node type; provides `getPresentation()` and `getChildren()` |
| `PsiTreeUtil` | Platform (bundled) | Utility for PSI traversal (`collectElementsOfType`, `getChildrenOfTypeAsList`) | Standard traversal; used by generated `MakoDefTagImpl` itself |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `NavigatablePsiElement` | Platform (bundled) | Interface providing `navigate()` / `canNavigate()` | `ASTWrapperPsiElement` already implements this; `MakoDefTagMixin` inherits it automatically |
| `ItemPresentation` | Platform (bundled) | Presentation data: display text + icon for a tree node | Returned from `getPresentation()` in `StructureViewTreeElement` |
| `PresentationData` | Platform (bundled) | Default `ItemPresentation` implementation | Use as fallback when element has no own presentation |
| `MakoIcons` | In-project (Phase 1) | File icon | Use in `StructureViewTreeElement.getPresentation()` for `MakoDefTag`/`MakoBlockTag` icons |
| `MakoVisitor` | In-project (generated) | Generated PSI visitor for all Mako element types | Use in `buildFoldRegions()` via `PsiTreeUtil.processElements` visitor pattern or manual recursion |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `FoldingBuilderEx` (recommended) | `FoldingBuilder` | `FoldingBuilder` operates on `ASTNode` root only; `FoldingBuilderEx` operates on `PsiElement` root giving access to the full PSI API and typed element checks — always prefer `FoldingBuilderEx` |
| Sibling-scanning for control flow (recommended) | Re-structuring BNF grammar to add `control_block` container rules | BNF change would require significant parser rebuild, re-generating `MakoTypes.java`, updating all existing tests; sibling-scanning is simpler and the Mako language semantics naturally represent control flow as flat sibling statements |
| `PsiStructureViewFactory` (recommended) | `ChooseByNameContributor` (Go To Symbol) | `PsiStructureViewFactory` covers EDIT-03 exactly (Structure view panel); `ChooseByNameContributor` is a separate feature (Ctrl+Alt+Shift+N) not required by this phase |

---

## Architecture Patterns

### Recommended Project Structure After Phase 5

```
src/main/
├── kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
│   └── lang/
│       ├── folding/                          # NEW package for Phase 5
│       │   └── MakoFoldingBuilder.kt         # NEW: FoldingBuilderEx + DumbAware
│       └── structure/                        # NEW package for Phase 5
│           ├── MakoStructureViewFactory.kt   # NEW: PsiStructureViewFactory
│           ├── MakoStructureViewModel.kt     # NEW: TextEditorBasedStructureViewModel subclass
│           └── MakoStructureViewElement.kt   # NEW: StructureViewTreeElement impl
├── resources/META-INF/plugin.xml             # UPDATED: 2 new extension registrations
src/test/
└── kotlin/.../lang/
    ├── MakoFoldingTest.kt                    # NEW: folding test using testFolding()
    └── MakoStructureViewTest.kt              # NEW: structure view navigation test
src/test/testData/
└── folding/                                  # NEW: test data directory
    └── FoldingTestData.mako                  # NEW: test file with <fold text='...'> markers
```

### Pattern 1: FoldingBuilderEx — def_tag and block_tag (Self-Contained Nodes)

**What:** `MakoDefTag` and `MakoBlockTag` PSI nodes already contain their full body (from `<%def` through `</%def>`) as their `textRange`. Create a `FoldingDescriptor` using the node's own range.

**When to use:** Any PSI node whose entire collapsible region is within its own `textRange` (i.e., the node IS the fold region).

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/folding-builder.html
// Source: FoldingDescriptor constructors from JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingDescriptor.java

package com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDocComment
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoModuleBlock
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoControlLineStmt
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class MakoFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // 1. def_tag: fold entire node (TAG_OPEN_DEF...END_TAG)
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.add(FoldingDescriptor(def.node, def.textRange))
        }

        // 2. block_tag: fold entire node (TAG_OPEN_BLOCK...END_TAG)
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.add(FoldingDescriptor(block.node, block.textRange))
        }

        // 3. doc_comment: fold and collapse by default (<%doc>...</%doc>)
        PsiTreeUtil.collectElementsOfType(root, MakoDocComment::class.java).forEach { doc ->
            descriptors.add(
                FoldingDescriptor(doc.node, doc.textRange, null, "<%doc>...</%doc>",
                    true, emptySet())  // collapsedByDefault=true
            )
        }

        // 4. module_block: fold and collapse by default (<%!...%>)
        PsiTreeUtil.collectElementsOfType(root, MakoModuleBlock::class.java).forEach { mod ->
            descriptors.add(
                FoldingDescriptor(mod.node, mod.textRange, null, "<%!...%>",
                    true, emptySet())  // collapsedByDefault=true
            )
        }

        // 5. control flow: sibling-scan algorithm (see Pattern 2)
        descriptors.addAll(buildControlFlowFolds(root))

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType.toString()) {
        "def_tag"     -> "<%def ...>...</%def>"
        "block_tag"   -> "<%block ...>...</%block>"
        "doc_comment" -> "<%doc>...</%doc>"
        "module_block"-> "<%!...%>"
        else          -> "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = when (node.elementType.toString()) {
        "doc_comment", "module_block" -> true
        else                          -> false
    }

    private fun buildControlFlowFolds(root: PsiElement): List<FoldingDescriptor> {
        // see Pattern 2
        TODO()
    }
}
```

### Pattern 2: Control Flow Folding — Sibling-Scanning Algorithm

**What:** Mako control flow (`% for`, `% if`, `% while`) is NOT hierarchical in the PSI. Each `MakoControlLineStmt` is a flat sibling under the same parent (`MakoFile`, `MakoDefTag`, or `MakoBlockTag`). The fold region spans from the block-opening line to the matching `% end*` line.

**PSI structure fact (verified from BNF):**
```
MakoFile
  MakoControlLineStmt  "% for item in items:"   <- opening line, creates fold start
  MakoControlLineStmt  "% if cond:"             <- nested opening (depth tracking needed)
  ...template content (TEMPLATE_TEXT siblings)
  MakoControlLineStmt  "% endif"                <- closes inner if
  ...more template content
  MakoControlLineStmt  "% endfor"               <- closes outer for, creates fold end
```

**Algorithm:**
1. Collect all `MakoControlLineStmt` nodes directly under the current parent (do NOT recurse into def/block tags — they will be processed when their children are visited).
2. For each control line, extract the keyword (second word of the token text after stripping leading whitespace and `%`).
3. If keyword is `for`, `if`, or `while`: push onto a depth-tracking stack.
4. If keyword is `endfor`, `endif`, or `endwhile`: pop the matching open from the stack; create `FoldingDescriptor` spanning from open line start to close line end.

**Keyword extraction from CONTROL_LINE token text:**
The lexer rule `^[ \t]* "%" [^%\r\n] [^\r\n]*` means the text is the entire `% keyword ...` line (including leading whitespace and trailing content, but not the newline). Extract the first non-whitespace word after `%`:
```kotlin
fun extractKeyword(controlLineText: String): String {
    // e.g., "  % for item in items:" -> "for"
    val stripped = controlLineText.trimStart()   // "% for item in items:"
    val afterPercent = stripped.removePrefix("%").trimStart()  // "for item in items:"
    return afterPercent.split(Regex("\\s+")).firstOrNull() ?: ""
}

val OPENING_KEYWORDS = setOf("for", "if", "while")
val CLOSING_KEYWORDS = mapOf("endfor" to "for", "endif" to "if", "endwhile" to "while")
```

**Important:** The `FoldingDescriptor` requires an `ASTNode` as its first argument. For control flow folds that span multiple sibling PSI nodes, use the `ASTNode` of the opening `MakoControlLineStmt` as the anchor, but provide a custom `TextRange` that spans to the end of the closing line.

**Example (sibling-scan implementation):**
```kotlin
private fun buildControlFlowFoldsUnder(parent: PsiElement): List<FoldingDescriptor> {
    val descriptors = mutableListOf<FoldingDescriptor>()
    val openStack = ArrayDeque<MakoControlLineStmt>()  // stack of opening lines

    // Only direct children — do not recurse (def/block bodies handled separately)
    var child: PsiElement? = parent.firstChild
    while (child != null) {
        if (child is MakoControlLineStmt) {
            val keyword = extractKeyword(child.text)
            when {
                keyword in OPENING_KEYWORDS -> openStack.addLast(child)
                keyword in CLOSING_KEYWORDS -> {
                    if (openStack.isNotEmpty()) {
                        val openLine = openStack.removeLast()
                        val foldRange = TextRange(
                            openLine.textRange.startOffset,
                            child.textRange.endOffset
                        )
                        descriptors.add(
                            FoldingDescriptor(openLine.node, foldRange)
                        )
                    }
                }
            }
        }
        child = child.nextSibling
    }
    return descriptors
}
```

**Recursion into def/block bodies:** Call `buildControlFlowFoldsUnder(defTag)` and `buildControlFlowFoldsUnder(blockTag)` for each def and block found, so control flow nested inside defs is also folded.

### Pattern 3: Structure View — Three-Class Stack

**What:** The platform's structure view requires three classes: a factory, a model, and a tree element. The factory wires them together. The tree element provides `getPresentation()` (display name + icon) and `getChildren()` (which PSI children appear in the tree).

**Key insight for this project:** `MakoDefTagMixin` and `MakoBlockTagMixin` already extend `ASTWrapperPsiElement` which implements `NavigatablePsiElement`. The `navigate()` method is available for free — clicking a node in the Structure View automatically navigates to the element.

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/structure-view.html
// Source: https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/simple_language_plugin/...

// ---- MakoStructureViewFactory.kt ----
class MakoStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return MakoStructureViewModel(editor, psiFile)
            }
        }
    }
}

// ---- MakoStructureViewModel.kt ----
class MakoStructureViewModel(editor: Editor?, psiFile: PsiFile)
    : StructureViewModelBase(psiFile, editor, MakoStructureViewElement(psiFile)),
      StructureViewModel.ElementInfoProvider {

    override fun getSuitableClasses(): Array<Class<out PsiElement>> =
        arrayOf(MakoDefTag::class.java, MakoBlockTag::class.java)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element.value is MakoDefTag || element.value is MakoBlockTag
}

// ---- MakoStructureViewElement.kt ----
class MakoStructureViewElement(private val element: NavigatablePsiElement)
    : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)
    override fun canNavigate(): Boolean = element.canNavigate()
    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()
    override fun getAlphaSortKey(): String = element.name ?: ""

    override fun getPresentation(): ItemPresentation =
        element.presentation ?: PresentationData(element.name ?: "<unnamed>", null, MakoIcons.FILE, null)

    override fun getChildren(): Array<TreeElement> {
        return when (element) {
            is MakoFile -> {
                // Top level: collect def and block tags
                val defs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                val blocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
                (defs + blocks).map { MakoStructureViewElement(it as NavigatablePsiElement) }
                    .toTypedArray()
            }
            is MakoDefTag -> {
                // Nested defs and blocks inside a def
                val defs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                val blocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
                (defs + blocks).map { MakoStructureViewElement(it as NavigatablePsiElement) }
                    .toTypedArray()
            }
            else -> EMPTY_ARRAY
        }
    }
}
```

### Pattern 4: plugin.xml Registration

```xml
<!-- Phase 5: Code Folding -->
<lang.foldingBuilder language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding.MakoFoldingBuilder"/>

<!-- Phase 5: Structure View -->
<lang.psiStructureViewFactory language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure.MakoStructureViewFactory"/>
```

**Note:** Extension point names are `lang.foldingBuilder` and `lang.psiStructureViewFactory` — both verified against the IntelliJ Platform extension point list.

### Pattern 5: Folding Test

**Test format:** Test data file uses `<fold text='placeholder'>` XML-like markers in the source content:
```
<fold text='<%def ...>...</%def>'><%def name="greet">
Hello ${name}!
</%def></fold>
```

**Test method:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/folding-test.html
class MakoFoldingTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData/folding"

    fun testFolding() {
        // testFolding() takes the path to a file with <fold> markers
        myFixture.testFolding("$testDataPath/FoldingTestData.mako")
    }
}
```

**Important constraint from docs:** The `FoldingBuilderEx` implementation MUST implement `DumbAware` or folding tests will fail.

### Anti-Patterns to Avoid

- **Forgetting `DumbAware` on `MakoFoldingBuilder`:** Tests run in dumb mode; without `DumbAware`, the platform skips the folding builder during tests. Always implement `DumbAware` on folding builders for custom languages.
- **Using the root PSI element node for a cross-sibling fold region:** When `TextRange` spans multiple sibling PSI nodes (control flow folds), use the `ASTNode` of the OPENING element as the `FoldingDescriptor`'s node anchor. Do NOT use a synthetic/parent ASTNode that does not correspond to an actual token in the document.
- **`isCollapsedByDefault()` returning `true` for all nodes:** This makes every fold collapsed on file open, which violates the success criteria — only `<%doc>` and `<%!>` start collapsed. Return `true` only for `doc_comment` and `module_block` element types.
- **Traversing into def/block bodies twice:** If `buildFoldRegions()` walks the entire tree recursively, it will visit `MakoControlLineStmt` nodes inside `MakoDefTag` when it visits both the file-level tree and the def's children. Track which parents have already been processed, OR use `PsiTreeUtil.collectElementsOfType` (which already traverses the whole tree) for def/block nodes separately from the sibling-scan for control flow.
- **Using `StructureViewModelBase` without `ElementInfoProvider`:** The `isAlwaysLeaf()` optimization requires `ElementInfoProvider` to be implemented. Without it, the tree may show expand arrows on leaf nodes.
- **`getSuitableClasses()` not matching `getChildren()` types:** The model's `getSuitableClasses()` must list the same PSI element types that `getChildren()` returns as tree nodes. Mismatches break the autoscroll-from-source feature (cursor in editor should highlight the correct structure view node).

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Gutter fold arrows and collapse behavior | Custom `EditorFactoryListener` + custom fold UI | `FoldingBuilderEx` registration | The platform provides all gutter arrow rendering, expand/collapse animations, keyboard shortcuts (Ctrl+.), and persistence of fold state across sessions |
| Default collapsed state on file open | `FileOpenedSynchronizer` that programmatically folds regions after open | `isCollapsedByDefault()` returning `true` in `FoldingBuilderEx` (or `FoldingDescriptor` constructor with `collapsedByDefault=true`) | The platform calls `isCollapsedByDefault()` during initial folding setup on file open — no event listener needed |
| Navigating to a PSI element from a tree click | Custom `MouseListener` + caret movement code | `NavigatablePsiElement.navigate()` in `StructureViewTreeElement.navigate()` | `ASTWrapperPsiElement` (which the mixins extend) already implements `navigate()` correctly — just delegate |
| Showing a custom Structure View panel | Custom `ToolWindowFactory` with a tree widget | `PsiStructureViewFactory` + platform's Structure View | The platform provides the entire View > Tool Windows > Structure UI; the plugin only declares the tree contents |
| Placeholder text per-node | Storing text in a map indexed by PSI node | `getPlaceholderText(ASTNode)` method | The platform calls this lazily when a fold is collapsed; derive from `node.elementType` |

**Key insight:** Both features are almost entirely declarative — the plugin answers "what ranges fold?" and "what nodes appear in the tree?"; the platform implements all user interaction, persistence, and UI.

---

## Common Pitfalls

### Pitfall 1: Folding Builder Not Implementing DumbAware

**What goes wrong:** Folding tests pass compilation but fail at runtime with no folding happening, or the test infrastructure reports the folding builder was not invoked.

**Why it happens:** The IntelliJ test framework runs tests in "dumb mode" (background indexing mode). Non-`DumbAware` components are skipped entirely in this mode to prevent indexing-dependent code from running during tests.

**How to avoid:** Always add `DumbAware` to the `MakoFoldingBuilder` class declaration: `class MakoFoldingBuilder : FoldingBuilderEx(), DumbAware`.

**Warning signs:** Test runs without errors but produces zero folding regions; or the `testFolding()` call reports no regions found.

### Pitfall 2: Control Flow Fold Region Assigned to Wrong ASTNode

**What goes wrong:** The control flow fold region appears but the IDE crashes or behaves incorrectly when the user interacts with it (e.g., collapsing causes a corrupted view).

**Why it happens:** `FoldingDescriptor(node, range)` takes an `ASTNode` as first argument — the node whose `isCollapsedByDefault()` will be called, and whose element type drives `getPlaceholderText()`. When the `TextRange` extends beyond the node's actual `textRange`, the platform may have difficulty associating the range with a valid document region.

**How to avoid:** Use the `ASTNode` of the opening `MakoControlLineStmt` as the descriptor's node. The `TextRange` can be larger than the node's own range — this is explicitly supported by the API (the FoldingDescriptor's `range` parameter is independent of the node's range). Verified: `FoldingDescriptor` documentation states the node is the "anchor" used for callbacks, while `range` defines the actual text that folds.

**Warning signs:** `IllegalArgumentException` or `AssertionError` in the IntelliJ log when opening files; fold regions with incorrect start/end positions.

### Pitfall 3: Unmatched Control Flow — Stack Underflow

**What goes wrong:** Malformed Mako files with orphaned `% endfor` (no matching `% for`) cause the sibling-scan algorithm to pop from an empty stack, producing `NullPointerException` or incorrect fold descriptors.

**Why it happens:** The parser has error recovery (the BNF's `recoverWhile=tag_recover`) and may produce PSI nodes even for malformed input. The folding builder must handle malformed trees gracefully.

**How to avoid:** Before popping from the `openStack`, check `openStack.isNotEmpty()`. If the stack is empty on an `endfor`/`endif`/`endwhile`, skip creating a descriptor for that line (no fold arrow on an orphaned close line). This is safe — the user just won't see a fold for the unmatched case.

**Warning signs:** `NoSuchElementException` when opening a Mako file with syntax errors; IDE log shows exception in `MakoFoldingBuilder.buildFoldRegions`.

### Pitfall 4: Structure View Not Showing Nested Def/Block Nodes

**What goes wrong:** The Structure View shows top-level `<%def>` nodes but doesn't show `<%def>` nodes that are nested inside another `<%def>`.

**Why it happens:** `PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag.class)` returns only DIRECT children. A `MakoDefTag` nested two levels deep will not appear unless the parent `MakoDefTag`'s `getChildren()` also returns its own nested elements.

**How to avoid:** In `MakoStructureViewElement.getChildren()`, handle both `MakoFile` (top-level defs/blocks) and `MakoDefTag` (nested defs/blocks). For `MakoBlockTag`, also collect nested defs/blocks if the template pattern allows them.

**Warning signs:** User opens a template with nested `<%def>` inside `<%def>` but only the outer def appears in the Structure View; clicking the outer def node shows no children despite there being nested defs.

### Pitfall 5: Placeholder Text Mismatch Between isCollapsedByDefault and FoldingDescriptor Constructor

**What goes wrong:** `<%doc>` blocks appear expanded on file open instead of collapsed, or vice versa.

**Why it happens:** There are two ways to set collapsed-by-default: (a) override `isCollapsedByDefault(ASTNode)` in the builder, or (b) pass `collapsedByDefault=true` directly to a `FoldingDescriptor` constructor overload. If both mechanisms are used and return different values, behavior is undefined (the descriptor's value takes precedence over the builder's method when set directly).

**How to avoid:** Use a single mechanism consistently. **Recommended:** pass `collapsedByDefault=true` directly in the `FoldingDescriptor` constructor for `doc_comment` and `module_block` nodes (this is the explicit approach and doesn't rely on the ASTNode element type comparison). Use `isCollapsedByDefault()` as a fallback that returns `false` for everything else.

**Warning signs:** `<%doc>` starts expanded when a fresh file is opened; toggling Code > Folding > Collapse All produces unexpected results.

### Pitfall 6: Language ID Mismatch in plugin.xml

**What goes wrong:** Folding arrows never appear; Structure View panel opens but shows "No Structure" for Mako files.

**Why it happens:** Extension point `language="..."` attributes must exactly match `"Mako Template"` (the string from `MakoLanguage`). A single character difference causes silent registration failure.

**How to avoid:** Copy `language="Mako Template"` exactly from the existing `lang.braceMatcher` registration in `plugin.xml`.

**Warning signs:** Feature is implemented and compiles but never activates at runtime; no exceptions in the log.

---

## Code Examples

Verified patterns from official sources:

### Complete plugin.xml Additions for Phase 5

```xml
<!-- Source: https://plugins.jetbrains.com/docs/intellij/folding-builder.html -->
<!-- Source: https://plugins.jetbrains.com/docs/intellij/structure-view.html -->

<!-- Phase 5: Code Folding (SYNX-06) -->
<lang.foldingBuilder language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding.MakoFoldingBuilder"/>

<!-- Phase 5: Structure View (EDIT-03) -->
<lang.psiStructureViewFactory language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure.MakoStructureViewFactory"/>
```

### MakoFoldingBuilder Skeleton

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/folding-builder.html
// Source: https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingBuilderEx.java
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.folding

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.*
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

    private val OPENING_KEYWORDS = setOf("for", "if", "while")
    private val CLOSING_KEYWORDS = setOf("endfor", "endif", "endwhile")

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // def_tag: fold entire span, expanded by default
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.add(FoldingDescriptor(def.node, def.textRange))
        }

        // block_tag: fold entire span, expanded by default
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.add(FoldingDescriptor(block.node, block.textRange))
        }

        // doc_comment: fold and collapse by default
        PsiTreeUtil.collectElementsOfType(root, MakoDocComment::class.java).forEach { doc ->
            descriptors.add(FoldingDescriptor(
                doc.node, doc.textRange, null,
                Collections.emptySet(), false, "<%doc>...</%doc>", true
            ))
        }

        // module_block: fold and collapse by default
        PsiTreeUtil.collectElementsOfType(root, MakoModuleBlock::class.java).forEach { mod ->
            descriptors.add(FoldingDescriptor(
                mod.node, mod.textRange, null,
                Collections.emptySet(), false, "<%!...%>", true
            ))
        }

        // control flow: sibling-scan per parent context
        PsiTreeUtil.collectElementsOfType(root, MakoFile::class.java).forEach { file ->
            descriptors.addAll(buildControlFlowFoldsUnder(file))
        }
        PsiTreeUtil.collectElementsOfType(root, MakoDefTag::class.java).forEach { def ->
            descriptors.addAll(buildControlFlowFoldsUnder(def))
        }
        PsiTreeUtil.collectElementsOfType(root, MakoBlockTag::class.java).forEach { block ->
            descriptors.addAll(buildControlFlowFoldsUnder(block))
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
    // Note: doc_comment and module_block use collapsedByDefault=true in the descriptor constructor

    private fun extractKeyword(text: String): String {
        val stripped = text.trimStart().removePrefix("%").trimStart()
        return stripped.split(Regex("\\s+")).firstOrNull() ?: ""
    }

    private fun buildControlFlowFoldsUnder(parent: PsiElement): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val openStack = ArrayDeque<MakoControlLineStmt>()
        var child: PsiElement? = parent.firstChild
        while (child != null) {
            if (child is MakoControlLineStmt) {
                val keyword = extractKeyword(child.text)
                when {
                    keyword in OPENING_KEYWORDS -> openStack.addLast(child)
                    keyword in CLOSING_KEYWORDS -> {
                        if (openStack.isNotEmpty()) {
                            val openLine = openStack.removeLast()
                            descriptors.add(FoldingDescriptor(
                                openLine.node,
                                TextRange(openLine.textRange.startOffset, child.textRange.endOffset)
                            ))
                        }
                    }
                }
            }
            child = child.nextSibling
        }
        return descriptors
    }
}
```

### MakoStructureViewFactory + Model + Element Skeleton

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/structure-view.html
// Source: https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/simple_language_plugin/...

// ---- MakoStructureViewFactory.kt ----
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class MakoStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
        object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                MakoStructureViewModel(editor, psiFile)
        }
}

// ---- MakoStructureViewModel.kt ----
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class MakoStructureViewModel(editor: Editor?, psiFile: PsiFile)
    : StructureViewModelBase(psiFile, editor, MakoStructureViewElement(psiFile as com.intellij.psi.NavigatablePsiElement)),
      StructureViewModel.ElementInfoProvider {

    override fun getSuitableClasses(): Array<Class<out PsiElement>> =
        arrayOf(MakoDefTag::class.java, MakoBlockTag::class.java)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

// ---- MakoStructureViewElement.kt ----
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.structure

import com.github.kimuth.jetbrainsmakotemplateplugin.MakoIcons
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoBlockTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoDefTag
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoFile
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiTreeUtil

class MakoStructureViewElement(private val element: NavigatablePsiElement)
    : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) { element.navigate(requestFocus) }
    override fun canNavigate(): Boolean = element.canNavigate()
    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()
    override fun getAlphaSortKey(): String = element.name ?: ""

    override fun getPresentation(): ItemPresentation =
        element.presentation
            ?: PresentationData(element.name ?: "<unnamed>", null, MakoIcons.FILE, null)

    override fun getChildren(): Array<TreeElement> {
        val defs: List<NavigatablePsiElement>
        val blocks: List<NavigatablePsiElement>
        when (element) {
            is MakoFile -> {
                defs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                    .filterIsInstance<NavigatablePsiElement>()
                blocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
                    .filterIsInstance<NavigatablePsiElement>()
            }
            is MakoDefTag -> {
                defs = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoDefTag::class.java)
                    .filterIsInstance<NavigatablePsiElement>()
                blocks = PsiTreeUtil.getChildrenOfTypeAsList(element, MakoBlockTag::class.java)
                    .filterIsInstance<NavigatablePsiElement>()
            }
            else -> return EMPTY_ARRAY
        }
        return (defs + blocks).map { MakoStructureViewElement(it) }.toTypedArray()
    }
}
```

### Folding Test Data File Format

```
<%doc><fold text='<%doc>...</%doc>'>
This is a doc comment that should be collapsed.
</%doc></fold>
<%def name="greet"><fold text='...'>
Hello ${name}!
</%def></fold>
% for item in items:<fold text='...'>
    ${item}
% endfor</fold>
```

**Note:** The `<fold text='placeholder'>` tag must wrap the text from the first character of the fold region through the last character (inclusive). The placeholder text shown must exactly match what `getPlaceholderText()` returns.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `FoldingBuilder` (AST-based, `buildFoldRegions(ASTNode, Document)`) | `FoldingBuilderEx` (PSI-based, `buildFoldRegions(PsiElement, Document, boolean)`) | IntelliJ 13/2013 | `FoldingBuilderEx` provides typed PSI access; the `quick` parameter allows fast initial folding without expensive operations |
| `StructureViewFactory` registered at `com.intellij.lang.structureViewFactory` | `PsiStructureViewFactory` at `com.intellij.lang.psiStructureViewFactory` | Stable | Both exist but `psiStructureViewFactory` is the correct extension point for custom language plugins |
| `FoldingDescriptor(ASTNode, TextRange)` with separate `isCollapsedByDefault()` override | `FoldingDescriptor` constructors accepting `collapsedByDefault` directly | ~IntelliJ 2019 | The newer constructors allow per-descriptor default state without conditional logic in `isCollapsedByDefault()` |

**Deprecated/outdated:**
- `FoldingBuilder` (non-Ex): Superseded by `FoldingBuilderEx`; the non-Ex interface exists for backwards compatibility but all new code should use `FoldingBuilderEx`.
- `StructureViewFactory` (without `Psi` prefix): Use `PsiStructureViewFactory` and register at `lang.psiStructureViewFactory`.

---

## Open Questions

1. **How should the placeholder text for def/block folds be determined dynamically?**
   - What we know: `getPlaceholderText(ASTNode node)` is called with the `ASTNode` of the folded element. For a `def_tag` node, we can retrieve the def's name from `node.findChildByType(MakoTokenTypes.TAG_ATTR_VALUE)?.text` (same pattern as `MakoDefTagMixin.getName()`).
   - What's unclear: Whether a more informative placeholder like `<%def name="greet">...</%def>` is better than `...`. The success criteria only requires collapsing to "a single line" — any placeholder that keeps the region on one line satisfies SYNX-06.
   - Recommendation: Use `<%def name="greet">` (show the opening tag line as placeholder). This gives maximum context while still collapsing the body. Extract the name from the ASTNode in `getPlaceholderText()`.

2. **Control flow folding with nested if-inside-for: does the stack-based sibling algorithm handle depth correctly?**
   - What we know: The sibling-scan uses a simple LIFO stack (`openStack.removeLast()`). For a file with `% for` then `% if` then `% endif` then `% endfor`, the stack would contain `[for, if]` before seeing `% endif` and would pop `if` (matching) to create the `if..endif` fold, then pop `for` to create the `for..endfor` fold.
   - What's unclear: Whether Mako's `% if` can contain a `% for` that ends before the `% endif`, creating interleaved (not properly nested) fold regions. In practice, Mako control flow must be properly nested for the Python-based renderer to work, so the stack algorithm is correct.
   - Recommendation: Implement the stack algorithm as described. Document that the algorithm assumes properly-nested control flow (matching Mako's own semantics). Add a guard for `openStack.isEmpty()` to handle malformed files without crashing.

3. **Should `MakoBlockTag` nodes appear as children of `MakoDefTag` in the Structure View?**
   - What we know: In Mako, `<%block>` inside `<%def>` is unusual but syntactically valid (the parser allows `item_*` between any tag's content). `PsiTreeUtil.getChildrenOfTypeAsList` returns only direct children, so a block nested inside a def would only appear if `MakoDefTag`'s `getChildren()` explicitly collects `MakoBlockTag` nodes.
   - What's unclear: Whether users expect nested blocks inside defs to appear in the Structure View. The success criteria says "shows all `<%def>` and `<%block>` declarations" — this implies a flat or nested list of all of them.
   - Recommendation: Include both `MakoDefTag` and `MakoBlockTag` in the children collections at every level (file, def, block). This ensures all declarations appear regardless of nesting depth.

4. **Does `MakoFile` implement `NavigatablePsiElement` for use as the root `StructureViewTreeElement`?**
   - What we know: `MakoFile` extends `PsiFileBase`, which extends `PsiFile`, which in turn implements `NavigatablePsiElement`. The cast `psiFile as NavigatablePsiElement` in `MakoStructureViewModel` is safe.
   - What's unclear: Nothing — this is confirmed by the platform type hierarchy.
   - Recommendation: Cast directly: `MakoStructureViewElement(psiFile as NavigatablePsiElement)`.

---

## Sources

### Primary (HIGH confidence)
- https://plugins.jetbrains.com/docs/intellij/folding-builder.html — `FoldingBuilderEx`, `DumbAware` requirement, `buildFoldRegions()` signature, `isCollapsedByDefault()`, plugin.xml `lang.foldingBuilder` registration
- https://plugins.jetbrains.com/docs/intellij/folding-test.html — `myFixture.testFolding()` method, `<fold text='...'>` test data format, `DumbAware` requirement for tests
- https://plugins.jetbrains.com/docs/intellij/structure-view.html — `PsiStructureViewFactory`, `TreeBasedStructureViewBuilder`, `TextEditorBasedStructureViewModel`, `StructureViewTreeElement`, `getSuitableClasses()`, plugin.xml `lang.psiStructureViewFactory` registration
- https://plugins.jetbrains.com/docs/intellij/structure-view-factory.html — structure view factory tutorial (three-component architecture confirmed)
- https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingDescriptor.java — all `FoldingDescriptor` constructor signatures including `(ASTNode, TextRange, FoldingGroup, String placeholderText, Boolean collapsedByDefault, Set dependencies)`
- https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingBuilderEx.java — `buildFoldRegions(PsiElement, Document, boolean)` and `buildFoldRegions(ASTNode, Document)` method signatures
- https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/simple_language_plugin/src/main/java/org/intellij/sdk/language/SimpleStructureViewElement.java — complete `StructureViewTreeElement` + `SortableTreeElement` implementation with `getChildren()` and `getPresentation()` patterns
- Project source: `src/main/grammars/Mako.bnf` — confirmed def_tag/block_tag own their body content; control_line_stmt is a flat sibling
- Project source: `src/main/gen/.../MakoDefTagImpl.java` — confirmed `PsiTreeUtil.getChildrenOfTypeAsList` is already used by generated code
- Project source: `src/main/kotlin/.../psi/impl/MakoDefTagMixin.kt` — confirmed `ASTWrapperPsiElement` base class (which is `NavigatablePsiElement`)

### Secondary (MEDIUM confidence)
- https://dploeger.github.io/intellij-api-doc/com/intellij/lang/folding/FoldingBuilderEx.html — `FoldingBuilderEx` API docs (unofficial but cross-checked with GitHub source)
- Multiple JetBrains community posts confirming `DumbAware` is required for folding builders in tests

### Tertiary (LOW confidence)
- None — all critical claims verified against official sources or project source code

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — All APIs verified via official IntelliJ Platform SDK docs and JetBrains/intellij-community source
- Architecture patterns: HIGH — `FoldingBuilderEx` pattern and Structure View three-class pattern verified via official docs and SDK samples; control flow sibling-scan algorithm derived from first-principles analysis of project's own BNF grammar (no external verification source for the specific algorithm, but the PSI structure facts that drive it are verified)
- Pitfalls: HIGH for DumbAware, language ID mismatch, and stack underflow (first-principles + docs); MEDIUM for placeholder text mismatch (single confirmed source)

**Research date:** 2026-02-20
**Valid until:** 2026-05-20 (these APIs are stable; `FoldingBuilderEx` and `PsiStructureViewFactory` have not changed significantly in 5+ years; `FoldingDescriptor` constructor with `collapsedByDefault` parameter is available since at least IntelliJ 2019.x, well before this project's `platformVersion = 2025.2.5`)

---
phase: 12-code-folding-and-structure-view
verified: 2026-02-21T20:30:00Z
status: passed
score: 7/7 must-haves verified
re_verification: false
---

# Phase 12: Code Folding and Structure View Verification Report

**Phase Goal:** Folding covers nested constructs inside def/block, and structure view displays elements in document order with correct icons
**Verified:** 2026-02-21T20:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #  | Truth                                                                                              | Status     | Evidence                                                                                                           |
|----|-----------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------|
| 1  | A `<%doc>` block nested inside a `<%def>` produces a fold region                                   | VERIFIED   | `testDocCommentNestedInDefFolds` in MakoFoldingTest.kt line 230; `walkAllNodes` recurses into def body             |
| 2  | A `<% %>` code block nested inside a `<%block>` produces a fold region                             | VERIFIED   | `testCodeBlockNestedInBlockFolds` in MakoFoldingTest.kt line 236; `buildCodeBlockFolds` uses `walkAllNodes`        |
| 3  | A `<%! %>` module block nested inside a `<%def>` produces a fold region                             | VERIFIED   | `testModuleBlockNestedInDefFolds` in MakoFoldingTest.kt line 242; `buildModuleBlockFolds` uses `walkAllNodes`      |
| 4  | DUMMY_BLOCK detection uses language identity check, not string comparison                           | VERIFIED   | `node.psi.language == Language.ANY` at MakoFoldingBuilder.kt line 216; no `toString`/`simpleName` strings remain  |
| 5  | Structure View lists `<%def>` and `<%block>` entries interleaved in document order                  | VERIFIED   | `(childDefs + childBlocks).sortedBy { it.textOffset }` at MakoStructureViewElement.kt line 59-61; `testChildrenInDocumentOrder` passes |
| 6  | Def and block entries in Structure View display `AllIcons.Nodes.Function` not `MakoIcons.FILE`      | VERIFIED   | `AllIcons.Nodes.Function` at MakoStructureViewElement.kt line 34; `testDefNodeUsesFunctionIcon` passes            |
| 7  | Existing folding and structure view tests pass with no regression                                   | VERIFIED   | 20 test methods in MakoFoldingTest, 6 in MakoStructureViewTest; commits 088b7a8, 1dbf5c7, 360305e, 4da5771 present in git |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact                                                                                        | Provides                                                         | Exists | Substantive | Wired     | Status     |
|-------------------------------------------------------------------------------------------------|------------------------------------------------------------------|--------|-------------|-----------|------------|
| `src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt`                         | `walkAllNodes`, recursive fold helpers, `Language.ANY` check     | Yes    | Yes (253 lines, full implementation) | Used by `buildFoldRegions` | VERIFIED |
| `src/test/kotlin/com/schtilig/mako/lang/MakoFoldingTest.kt`                                    | 20 tests including 3 new nested fold tests                       | Yes    | Yes (246 lines, 20 `fun test*` methods) | Extends ParsingTestCase; exercises MakoFoldingBuilder | VERIFIED |
| `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewElement.kt`                 | `sortedBy { it.textOffset }` children; `AllIcons.Nodes.Function` icon | Yes | Yes (65 lines, complete implementation) | Used by MakoStructureViewModel | VERIFIED |
| `src/test/kotlin/com/schtilig/mako/lang/MakoStructureViewTest.kt`                              | 6 tests including `testChildrenInDocumentOrder` and `testDefNodeUsesFunctionIcon` | Yes | Yes (157 lines, 6 `fun test*` methods) | Extends ParsingTestCase; exercises MakoStructureViewElement | VERIFIED |

### Key Link Verification

| From                                                   | To                                            | Via                                                        | Status  | Details                                                                                         |
|--------------------------------------------------------|-----------------------------------------------|------------------------------------------------------------|---------|-------------------------------------------------------------------------------------------------|
| `buildDocCommentFolds/buildModuleBlockFolds/buildCodeBlockFolds` | `walkAllNodes` recursive helper    | `walkAllNodes(root.node.firstChildNode)` calls at lines 110, 138, 179 | WIRED   | All three helpers verified to call `walkAllNodes`; flat sibling scan has been replaced          |
| `collectControlLineNodes` DUMMY_BLOCK branch           | `Language.ANY` identity check                 | `node.psi.language == Language.ANY` at line 216            | WIRED   | No `toString`/`simpleName` patterns exist anywhere in the file                                  |
| `MakoStructureViewElement.getChildren()`               | Combined def+block list sorted by textOffset  | `(childDefs + childBlocks).sortedBy { it.textOffset }` at lines 59-61 | WIRED   | Replaces the previous mutable-accumulator append-defs-then-blocks pattern                       |
| `MakoStructureViewElement.getPresentation()` fallback  | `AllIcons.Nodes.Function`                     | `AllIcons.Nodes.Function` at line 34; `import com.intellij.icons.AllIcons` at line 3 | WIRED   | `MakoIcons.FILE` no longer appears in this file                                                 |

### Requirements Coverage

| Requirement | Source Plan | Description                                                                                                                                    | Status    | Evidence                                                                                              |
|-------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------|
| FOLD-01     | 12-01       | `buildDocCommentFolds()`, `buildModuleBlockFolds()`, and `buildCodeBlockFolds()` use recursive descent so nested constructs fold               | SATISFIED | `walkAllNodes` replaces flat sibling scan in all three helpers; 3 new nested fold tests pass          |
| VIEW-02     | 12-02       | `MakoStructureViewElement` collects defs and blocks sorted by source offset, preserving document order                                         | SATISFIED | `sortedBy { it.textOffset }` on combined list; `testChildrenInDocumentOrder` asserts "first", "second", "third" ordering |
| VIEW-04     | 12-02       | `MakoStructureViewElement` fallback `ItemPresentation` returns `AllIcons.Nodes.Function` instead of `MakoIcons.FILE` for def/block nodes       | SATISFIED | `AllIcons.Nodes.Function` at getPresentation fallback; `testDefNodeUsesFunctionIcon` asserts equality |
| VIEW-05     | 12-01       | `MakoFoldingBuilder` DUMMY_BLOCK detection uses `element.language == Language.ANY` instead of `toString()`/`javaClass.simpleName`              | SATISFIED | `node.psi.language == Language.ANY` at line 216; no string comparison patterns remain                |

No orphaned requirements: all four IDs mapped to Phase 12 in REQUIREMENTS.md (`FOLD-01`, `VIEW-02`, `VIEW-04`, `VIEW-05`) are claimed by the plans and verified in the codebase.

### Anti-Patterns Found

None. No `TODO`, `FIXME`, `PLACEHOLDER`, empty implementations, or console-log-only handlers found in any of the four modified files.

### Human Verification Required

#### 1. Runtime Fold Behavior for Nested Constructs

**Test:** Open a `.mako` file containing `<%doc>` inside `<%def>`, run the plugin via `./gradlew runIde`, and confirm the nested doc comment shows a fold gutter icon.
**Expected:** Two fold gutter icons appear — one for the outer `<%def>` span and one for the nested `<%doc>...</%doc>` span.
**Why human:** Fold gutter rendering depends on IntelliJ platform fold manager wiring at runtime; unit tests verify count of `FoldingDescriptor` objects but not the actual fold UI.

#### 2. Structure View Document Order in IDE

**Test:** Open a `.mako` file with alternating `<%def>` and `<%block>` tags (e.g., def "a", block "b", def "c"), open the Structure View panel (Alt+7 / Cmd+7), and verify order.
**Expected:** The structure view lists "a", "b", "c" in that sequence, not all defs first.
**Why human:** Structure View rendering and tree update are platform-driven; unit tests verify the `children` array order but the rendered panel behavior needs visual confirmation.

#### 3. Function Icon Appearance in Structure View

**Test:** Open a `.mako` file with a `<%def>` tag, view the Structure View panel, and inspect the icon next to the def entry.
**Expected:** The icon matches the standard JetBrains "function/method" icon (`AllIcons.Nodes.Function`) — typically a purple circle with "f" — not the Mako file icon.
**Why human:** Icon rendering in the platform tree cell renderer may be influenced by `NavigationItem.getPresentation()` from the PSI element taking priority over the fallback. Unit test confirms the fallback returns the correct icon, but platform may use PSI presentation instead.

### Gaps Summary

No gaps. All seven observable truths are verified, all four artifacts are substantive and wired, all four key links are confirmed by code search, all four requirement IDs are satisfied, and all commits are present in git history.

---

_Verified: 2026-02-21T20:30:00Z_
_Verifier: Claude (gsd-verifier)_

# Phase 19: Regression Hardening - Research

**Researched:** 2026-02-22
**Domain:** IntelliJ Platform dual-tree (TemplateLanguageFileViewProvider) regression verification — existing plugin features alongside the HTML PSI tree
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| RGRN-01 | Python injection continues to work in `${...}`, `<% %>`, and `<%! %>` regions after FileViewProvider is active | `MakoPythonInjector` already hardened in Phase 18-01: uses `viewProvider.getPsi(MakoLanguage)` for search root. Verification plan: run `./gradlew check` + confirm Python highlighting in `runIde`. |
| RGRN-02 | Code folding, structure view, and tag completion work correctly alongside HTML injection | `MakoFoldingBuilder` operates directly on Mako PSI (no dual-tree exposure). `MakoStructureViewFactory` receives `psiFile` from platform — has no dual-tree guard (residual risk: may receive HTML PSI root in running IDE). `MakoCompletionContributor` already hardened in Phase 18-01 to use `viewProvider.baseLanguage`. Verification plan: run `./gradlew check` + visual checks in `runIde`. |
| RGRN-03 | All existing automated tests pass unchanged after FileViewProvider is added | Phase 18-01 confirmed "Zero regressions: all 92 existing tests pass". Phase 19 must re-confirm this with `./gradlew check` and add any missing regression test coverage for the dual-tree environment. |
</phase_requirements>

---

## Summary

Phase 19 is a **verification and gap-closure** phase, not a new-feature phase. The structural work (FileViewProvider, defensive guards) was completed in Phase 18-01. Phase 18-01 summary reported zero regressions across all 92 tests. Phase 19's job is to:

1. Run the full test suite and confirm it remains green
2. Verify all three RGRN success criteria hold in the running IDE
3. Add one missing automated regression test: dual-tree structure view (no existing test covers structure view when the dual-tree is active — all `MakoStructureViewTest` cases use `ParsingTestCase`/`LightVirtualFile` where the factory guard returns `SingleRootFileViewProvider`)
4. Apply the `MakoStructureViewFactory` dual-tree guard if IDE verification reveals empty structure view

The defensive guards installed in Phase 18-01 already address all known dual-tree failure modes:
- `MakoPythonInjector.collectCodeAndExpressionHosts` uses `viewProvider.getPsi(MakoLanguage)` — immune to HTML PSI root being returned for TEMPLATE_TEXT positions
- `MakoAnnotator` has `if (element is OuterLanguageElement) return` as first guard — no ClassCastException
- `MakoCompletionContributor.TagNameCompletionProvider` uses `file.viewProvider.baseLanguage` — fires correctly in dual-tree
- `MakoFileViewProviderFactory` returns `SingleRootFileViewProvider` for `LightVirtualFile` — all `ParsingTestCase` / `MakoFoldingTest` / `MakoPsiMixinTest` tests unaffected

**Resolved open questions (during research):**
- `WellFormedDefTag.mako` fixture content: `<%def name="foo">\ncontent\n</%def>` — plain text only, no HTML. No HTML error annotations will fire. `testWellFormedDefTagNoError` is safe even with dual-tree active.
- `testWellFormedDefTagNoError` uses `configureByFile` in `BasePlatformTestCase`: this creates a physical VFS file → dual-tree IS active → but the fixture contains no HTML → no HTML errors → test passes unchanged.

The one area of **residual risk** is `MakoStructureViewFactory.getStructureViewBuilder(psiFile)`. In the running IDE, the platform may call this with the HTML `PsiFile` if the caret is in a TEMPLATE_TEXT region. The current implementation has no guard. An empirical `runIde` check will confirm whether the fix is needed.

**Primary recommendation:** Run `./gradlew check` (confirm green), then `./gradlew runIde` and verify the four IDE success criteria. Apply the `MakoStructureViewFactory` guard if structure view is empty. Add one automated test for structure view in dual-tree context.

---

## Standard Stack

### Core — Already In Place (Phase 18-01)

| Component | File | Dual-Tree Guard | Status |
|-----------|------|-----------------|--------|
| `MakoPythonInjector` | `injection/MakoPythonInjector.kt` | `viewProvider.getPsi(MakoLanguage)` in `collectCodeAndExpressionHosts` | HARDENED — Phase 18-01 |
| `MakoAnnotator` | `annotation/MakoAnnotator.kt` | `if (element is OuterLanguageElement) return` as first line of `annotate()` | HARDENED — Phase 18-01 |
| `MakoCompletionContributor` | `completion/MakoCompletionContributor.kt` | `file.viewProvider.baseLanguage != MakoLanguage` in `TagNameCompletionProvider` | HARDENED — Phase 18-01 |
| `MakoFileViewProviderFactory` | `MakoFileViewProviderFactory.kt` | `if (file is LightVirtualFile) return SingleRootFileViewProvider(...)` | HARDENED — Phase 18-01 |
| `MakoFoldingBuilder` | `folding/MakoFoldingBuilder.kt` | Operates on Mako PSI nodes directly; `Language.ANY` check handles `OuterLanguageElementImpl` | SAFE — no change needed |

### Residual Risk Area

| Component | File | Risk | Mitigation |
|-----------|------|------|------------|
| `MakoStructureViewFactory` | `structure/MakoStructureViewFactory.kt` | Platform may call `getStructureViewBuilder(htmlPsiFile)` for dual-tree files when caret is in TEMPLATE_TEXT | Add `viewProvider.getPsi(MakoLanguage)` resolution before constructing `MakoStructureViewModel` |

### Test Classes — Dual-Tree Coverage Map

| Test Class | Framework | LightVirtualFile? | Dual-Tree Active? | Passes Phase 18-01? |
|------------|-----------|-------------------|--------------------|----------------------|
| `MakoParsingTest` | `ParsingTestCase` | YES | NO — factory guard | YES (92 tests) |
| `MakoLexerTest` | `BasePlatformTestCase` | N/A (lexer) | NO | YES |
| `MakoFoldingTest` | `ParsingTestCase` | YES | NO — factory guard | YES |
| `MakoStructureViewTest` | `ParsingTestCase` | YES | NO — factory guard | YES |
| `MakoInjectionHostTest` | `ParsingTestCase` | YES | NO — factory guard | YES |
| `MakoInjectionRangeTest` | `BasePlatformTestCase` | N/A (lexer) | NO | YES |
| `MakoAnnotatorTest` | `BasePlatformTestCase` | NO — `addFileToProject` | YES | YES |
| `MakoCompletionTest` | `BasePlatformTestCase` | NO — `addFileToProject` | YES | YES |
| `MakoPsiMixinTest` | `ParsingTestCase` | YES | NO — factory guard | YES |
| `MakoFileViewProviderTest` | `BasePlatformTestCase` | NO — `addFileToProject` | YES — tests it explicitly | YES (new in Phase 18-01) |

**Gap:** No existing test verifies structure view in dual-tree environment. `MakoStructureViewTest` uses `LightVirtualFile` → single-tree. Phase 19 should add one test to `MakoFileViewProviderTest` that confirms structure view works with a physical VFS `.mako` file.

---

## Architecture Patterns

### Pattern 1: Dual-Tree Language Guard (Established in Phase 18-01)

**What:** When code must operate on the Mako PSI root specifically (not the HTML root), always use `viewProvider.getPsi(MakoLanguage)` rather than `containingFile` or direct PSI tree access.

**When to use:** Any code that navigates to the root PSI file of a `.mako` document and then queries for Mako-specific PSI node types.

**Example (MakoPythonInjector — already implemented):**
```kotlin
// Source: src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt line 140
val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
```

### Pattern 2: BaseLanguage Guard for File Identity (Established in Phase 18-01)

**What:** Use `file.viewProvider.baseLanguage` instead of `file.language` to check whether a PSI file is a Mako file. In dual-tree, `file.language` returns `HTMLLanguage` for PSI files created from TEMPLATE_TEXT positions.

**Example (MakoCompletionContributor — already implemented):**
```kotlin
// Source: src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt line 109
if (file.viewProvider.baseLanguage != MakoLanguage) return
```

### Pattern 3: OuterLanguageElement Early Return (Established in Phase 18-01)

**What:** The Mako PSI tree contains `OuterLanguageElementImpl` nodes at TEMPLATE_TEXT positions when the FileViewProvider is active. Any `Annotator` or element visitor must check for `OuterLanguageElement` before processing.

**Example (MakoAnnotator — already implemented):**
```kotlin
// Source: src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt line 25
override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is OuterLanguageElement) return  // import: com.intellij.psi.templateLanguages.OuterLanguageElement
    if (element.containingFile.language != MakoLanguage) return
    // ...
}
```

### Pattern 4: MakoStructureViewFactory — Apply If Needed

**What:** `MakoStructureViewFactory.getStructureViewBuilder(psiFile: PsiFile)` receives a `PsiFile` from the platform. In dual-tree, this may be the `HtmlFile` when the editor focus is in a TEMPLATE_TEXT region.

**Risk:** `MakoStructureViewModel(editor, htmlFile)` creates a `MakoStructureViewElement(htmlFile)`. `getChildren()` hits the `else` branch → returns `TreeElement.EMPTY_ARRAY`. Structure view panel appears empty.

**Apply this pattern if `runIde` verification shows empty structure view:**
```kotlin
// Source: pattern derived from MakoPythonInjector's viewProvider.getPsi(MakoLanguage) precedent
import com.schtilig.mako.MakoLanguage
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class MakoStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        // In dual-tree, platform may pass the HTML PSI file when caret is in TEMPLATE_TEXT.
        // Resolve to Mako PSI root to ensure <%def> and <%block> nodes are found.
        val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) as? PsiFile ?: psiFile
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?) =
                MakoStructureViewModel(editor, makoFile)
        }
    }
}
```

### Anti-Patterns to Avoid

- **`context.containingFile` in injectors:** Always use `viewProvider.getPsi(MakoLanguage)` when you need the file root for tree traversal. `containingFile` returns whichever PSI root owns the element, which may be HTML.
- **`file.language` for Mako identity check:** Use `viewProvider.baseLanguage` instead — `file.language` returns `HTMLLanguage` for HTML PSI roots in dual-tree.
- **Assuming `PsiTreeUtil` starts from Mako root:** If the search root is an `HtmlFile`, `findChildrenOfType(htmlFile, MakoCodeBlock::class.java)` returns empty.
- **Removing the `LightVirtualFile` guard:** This guard in `MakoFileViewProviderFactory` is essential for all `ParsingTestCase`-based tests. Never remove it.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Mako PSI root lookup in dual-tree | Custom PSI walking to find Mako root | `viewProvider.getPsi(MakoLanguage)` | Platform-provided API; null-safe; proven in `MakoPythonInjector` |
| File language identity check | `file.language == MakoLanguage` | `file.viewProvider.baseLanguage == MakoLanguage` | `baseLanguage` is stable in dual-tree; `.language` varies by which PSI root you have |
| OuterLanguageElement detection | Custom node type check | `element is OuterLanguageElement` | Platform interface; already imported and used in `MakoAnnotator` |

---

## Common Pitfalls

### Pitfall 1: Structure View Empty in Running IDE

**What goes wrong:** `MakoStructureViewFactory` receives the HTML `PsiFile`. `MakoStructureViewModel(null, htmlFile)` creates `MakoStructureViewElement(htmlFile)`. `getChildren()` enters the `else` branch → `TreeElement.EMPTY_ARRAY`. Structure view shows no nodes.

**Why it happens:** `lang.psiStructureViewFactory` registered `language="Mako Template"`. Platform normally calls with the base-language file. With `TemplateLanguageFileViewProvider`, the platform's behavior in TEMPLATE_TEXT positions is empirically unconfirmed — may pass HTML PSI root in some activation paths.

**How to avoid:** Add `viewProvider.getPsi(MakoLanguage)` resolution at the top of `getStructureViewBuilder`.

**Warning signs:** Structure view pane shows no `<%def>` or `<%block>` nodes for a `.mako` file with those constructs; or structure view becomes empty after scrolling into template body content.

**Probability:** MEDIUM — empirical `runIde` check will determine if this fix is required.

### Pitfall 2: Python Injection "Unresolved Reference" False Positives

**What goes wrong:** `MakoPythonInjector.getLanguagesToInject` fires with `context` whose `containingFile` is the HTML PSI file. `collectCodeAndExpressionHosts(context)` would return empty if it used `containingFile` directly.

**Current state:** ALREADY FIXED in Phase 18-01. `collectCodeAndExpressionHosts` uses `context.containingFile?.viewProvider?.getPsi(MakoLanguage)`. No action needed.

**Warning signs (to watch during Phase 19 runIde):** Variables defined in `<% %>` blocks show "Unresolved Reference" in `${...}` expressions.

### Pitfall 3: MakoAnnotatorTest `testWellFormedDefTagNoError` Fails from HTML Errors

**What goes wrong:** `checkHighlighting()` with no `<error>` markers fails if HTML annotators produce unexpected error highlights.

**Current state:** RESOLVED — `WellFormedDefTag.mako` contains only `<%def name="foo">\ncontent\n</%def>`. Plain text "content" has no HTML error potential. Test is safe.

**Confirmed:** WellFormedDefTag.mako fixture read directly; content verified.

### Pitfall 4: ParsingTestCase Tests Break if LightVirtualFile Guard Is Removed

**What goes wrong:** `ParsingTestCase.doCheckResult()` iterates all PSI languages via `viewProvider.getLanguages()`. With dual-tree, it generates `.Mako Template.txt` and `.HTML.txt` fixture files instead of the single `.txt` file. All 7 parser fixture tests fail with "No output text found".

**Current state:** Guard is in place. Do not remove it.

**Warning signs:** Unexpected `.Mako Template.txt` or `.HTML.txt` files appear in `src/test/testData/`.

---

## Code Examples

Verified patterns from the existing codebase (all from Phase 18-01 implementation):

### Mako PSI Root Lookup — The Correct Pattern
```kotlin
// Source: MakoPythonInjector.kt line 140 (Phase 18-01)
val makoFile = context.containingFile?.viewProvider?.getPsi(MakoLanguage) ?: return emptyList()
```

### OuterLanguageElement Guard in Annotator
```kotlin
// Source: MakoAnnotator.kt line 25 (Phase 18-01)
override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is OuterLanguageElement) return
    if (element.containingFile.language != MakoLanguage) return
    // ...
}
```

### BaseLanguage Guard in Completion
```kotlin
// Source: MakoCompletionContributor.kt line 109 (Phase 18-01)
if (file.viewProvider.baseLanguage != MakoLanguage) return
```

### LightVirtualFile Guard in Factory (DO NOT REMOVE)
```kotlin
// Source: MakoFileViewProviderFactory.kt line 42 (Phase 18-01)
if (file is LightVirtualFile) {
    return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
}
```

### MakoStructureViewFactory Fix (Apply if runIde shows empty structure view)
```kotlin
// Pattern: derived from MakoPythonInjector's viewProvider.getPsi(MakoLanguage) precedent
class MakoStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) as? PsiFile ?: psiFile
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?) =
                MakoStructureViewModel(editor, makoFile)
        }
    }
}
```

### Structure View Dual-Tree Regression Test (Add to MakoFileViewProviderTest)
```kotlin
// Pattern: same as existing testMakoFileViewProviderPreservesMakoPsi — uses addFileToProject (physical VFS)
fun testStructureViewWorksInDualTree() {
    val file = myFixture.addFileToProject(
        "structure_dual_tree_test.mako",
        "<%def name=\"greet\"></%def>\n<%block name=\"header\"></%block>"
    )
    myFixture.configureFromExistingVirtualFile(file.virtualFile)

    // Verify dual-tree is active (precondition)
    val viewProvider = myFixture.file.viewProvider
    assertTrue(viewProvider is TemplateLanguageFileViewProvider)

    // Verify structure view sees Mako nodes (not empty due to HTML PSI root)
    val makoFile = viewProvider.getPsi(MakoLanguage) as? PsiFile ?: fail("No Mako PSI root")
    val model = MakoStructureViewModel(null, makoFile)
    val root = model.root as MakoStructureViewElement
    val children = root.children
    assertEquals(
        "Structure view must find 2 children (greet def + header block) in dual-tree environment",
        2, children.size
    )
}
```

---

## State of the Art

| Old Approach | Current Approach (Phase 18-01) | Phase 19 Action |
|--------------|-------------------------------|-----------------|
| `context.containingFile` in MakoPythonInjector | `viewProvider.getPsi(MakoLanguage)` | Already done — verify green |
| `file.language == MakoLanguage` in MakoCompletionContributor | `file.viewProvider.baseLanguage == MakoLanguage` | Already done — verify green |
| No OuterLanguageElement guard in MakoAnnotator | `if (element is OuterLanguageElement) return` | Already done — verify green |
| No LightVirtualFile guard in factory | `if (file is LightVirtualFile) return SingleRootFileViewProvider(...)` | Already done — verify green |
| No dual-tree guard in MakoStructureViewFactory | `val makoFile = psiFile.viewProvider.getPsi(MakoLanguage) as? PsiFile ?: psiFile` | Apply if runIde shows empty structure view |
| No dual-tree structure view test | Add test to MakoFileViewProviderTest | Add test in Phase 19 |

---

## Open Questions

1. **Does `MakoStructureViewFactory` receive the HTML PsiFile in dual-tree running IDE?**
   - What we know: Factory registered `language="Mako Template"`. Platform normally calls with Mako PSI file. With TemplateLanguageFileViewProvider, behavior in TEMPLATE_TEXT positions is unconfirmed empirically.
   - What's unclear: Whether platform passes HTML PSI root when caret is in template body.
   - Recommendation: Verify in `runIde` step. If structure view is empty in any position, apply the guard. Either way, add the automated test to `MakoFileViewProviderTest` that uses the Mako PSI root directly (this is deterministic regardless of what the platform passes to the factory).

---

## Phase 19 Work Breakdown

Based on the research above, Phase 19 has a well-bounded set of work items:

### Work Item 1: Run Full Test Suite (RGRN-03)
**Command:** `./gradlew check`
**Expected:** All 92+ tests green (Phase 18-01 left them green; no code changes since)
**Action if failing:** Identify failing test; fix dual-tree regression

### Work Item 2: MakoStructureViewFactory Guard (RGRN-02, conditional)
**Files:** `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt`
**Change:** Add `viewProvider.getPsi(MakoLanguage)` resolution
**Trigger:** Apply if `runIde` verification shows empty structure view
**Risk if skipped:** Structure view may be empty in some dual-tree positions in running IDE
**Risk if applied unnecessarily:** None — the guard is a no-op when already given a Mako PSI file (`getPsi(MakoLanguage)` returns the same file)

### Work Item 3: Add Structure View Dual-Tree Regression Test (RGRN-02, RGRN-03)
**File:** `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt`
**Add:** `testStructureViewWorksInDualTree()` — creates physical VFS `.mako` file, asserts structure view model has non-empty root children
**Why:** `MakoStructureViewTest` uses `ParsingTestCase`/`LightVirtualFile` — no dual-tree. Gap in test coverage for success criterion 4.

### Work Item 4: IDE Verification (RGRN-01, RGRN-02)
**Command:** `./gradlew runIde`
**Checks (matching success criteria):**
1. Open `.mako` file with `${name}` — Python syntax highlighting/error detection active
2. Open `.mako` file with `<% x = 1 %>` and `<%! import os %>` — Python highlighting inside blocks
3. Open `.mako` file with `<%def>` and `<%block>` and `<%doc>` — folding gutter icons visible
4. Open `.mako` file with `<%def>` and `<%block>` — Structure View shows named nodes in document order

---

## Sources

### Primary (HIGH confidence)

- `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` — directly read; `viewProvider.getPsi(MakoLanguage)` guard confirmed at line 140
- `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt` — directly read; `OuterLanguageElement` guard confirmed at line 25
- `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt` — directly read; `viewProvider.baseLanguage` guard confirmed at line 109
- `src/main/kotlin/com/schtilig/mako/lang/MakoFileViewProviderFactory.kt` — directly read; `LightVirtualFile` guard confirmed at line 42
- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewFactory.kt` — directly read; NO dual-tree guard present — residual risk identified
- `src/main/kotlin/com/schtilig/mako/lang/structure/MakoStructureViewElement.kt` — directly read; `getChildren()` returns `EMPTY_ARRAY` for non-Mako-root elements
- `src/main/kotlin/com/schtilig/mako/lang/folding/MakoFoldingBuilder.kt` — directly read; `Language.ANY` guard for `OuterLanguageElementImpl` confirmed
- `src/test/testData/annotator/WellFormedDefTag.mako` — directly read; content is `<%def name="foo">\ncontent\n</%def>` — no HTML → no HTML error risk
- All 10 test class files — directly read; `LightVirtualFile` vs `addFileToProject` usage mapped
- `src/main/resources/META-INF/plugin.xml` — directly read; all registrations confirmed
- Phase 18-01 SUMMARY.md — "Zero regressions: all 92 existing tests pass" — baseline confirmed
- Phase 18-03 SUMMARY.md — working baseline enumerated: Python injection, folding, Structure View no regressions

### Secondary (MEDIUM confidence)

- Phase 18 RESEARCH.md (Pitfall 6) — `MakoFoldingBuilder` `Language.ANY` guard analysis — safe with `OuterLanguageElementImpl`
- Phase 18 RESEARCH.md (Open Question 3) — `MakoCompletionContributor` deferred to Phase 19; resolved: `viewProvider.baseLanguage` guard was added in Phase 18-01

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Known-good guards (injector, annotator, completion, factory) | HIGH | Source files directly read; guards confirmed in place at specific lines |
| Test suite baseline | HIGH | Phase 18-01 summary reports 92 tests passing; no code changes since |
| WellFormedDefTag.mako fixture safety | HIGH | Fixture directly read; contains no HTML — no HTML error risk |
| MakoFoldingBuilder dual-tree safety | HIGH | Source read; `Language.ANY` guard handles `OuterLanguageElementImpl` correctly |
| Structure view in dual-tree (running IDE) | MEDIUM | Factory has no dual-tree guard; logical risk present; empirical runIde needed |
| Structure view test coverage gap | HIGH | `MakoStructureViewTest` uses `LightVirtualFile` — no dual-tree test exists; confirmed by test file review |

**Research date:** 2026-02-22
**Valid until:** 2026-04-22 (platform API stable; no changes expected to TemplateLanguageFileViewProvider APIs in 2025.2.x)

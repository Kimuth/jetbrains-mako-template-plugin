# Phase 6: Python Language Injection - Research

**Researched:** 2026-02-21
**Domain:** IntelliJ Platform language injection API — injecting Python into custom template PSI nodes
**Confidence:** MEDIUM-HIGH (API shape verified via official docs and source; key offset calculations and edge cases require integration-test validation)

---

## Summary

Phase 6 adds Python syntax highlighting inside Mako expression (`${...}`) and code block (`<% %>`, `<%! %>`) regions by using the IntelliJ Platform's language injection mechanism. The platform already manages the Python language through the `PythonCore` bundled plugin that is already declared as a dependency; no new Gradle or plugin.xml dependency is needed.

The implementation requires two things: (1) making the PSI nodes that contain Python content implement `PsiLanguageInjectionHost`, and (2) registering a `MultiHostInjector` that tells the platform which text ranges inside those hosts contain Python code. The injection host interface is most cleanly satisfied by adding mixins to the existing `MakoExpression`, `MakoCodeBlock`, and `MakoModuleBlock` PSI nodes, following the same mixin pattern already used for `MakoDefTag` and `MakoBlockTag`.

The Python language ID string is `"Python"` (confirmed by community reports of the error `"Language with ID 'Python' is already registered: class com.jetbrains.python.PythonLanguage"`). The platform provides `Language.findLanguageByID("Python")` to obtain the `Language` instance at runtime without importing the internal `PythonLanguage` class directly — important because `com.jetbrains.python.PythonLanguage` was temporarily marked `@Internal` in a 2025.3 EAP (later corrected), and defensive runtime lookup is safer than compile-time import.

**Primary recommendation:** Implement `PsiLanguageInjectionHost` on `MakoExpression`, `MakoCodeBlock`, and `MakoModuleBlock` via Kotlin mixins, then register one `MultiHostInjector` in `plugin.xml` that handles all three node types and injects Python into the content-only sub-range of each node.

---

## Standard Stack

### Core (no new dependencies — all already present)

| Component | Version/Source | Purpose | Why Standard |
|-----------|---------------|---------|--------------|
| `com.intellij.lang.injection.MultiHostInjector` | Platform core-api | Low-level injection API — maximum control over where/how Python is injected | Only API that supports custom PSI injection hosts with precise offset control |
| `com.intellij.psi.PsiLanguageInjectionHost` | Platform core-api | Interface that marks a PSI node as capable of hosting an injected language | Required by `MultiHostRegistrar.addPlace()` parameter type |
| `com.intellij.psi.LiteralTextEscaper` | Platform core-api | Maps offsets between host and injected file; `createSimple()` provides pass-through escaper for no-escape content | Required by `PsiLanguageInjectionHost.createLiteralTextEscaper()` |
| `PythonCore` bundled plugin | Already in `gradle.properties` | Registers `PythonLanguage` with ID `"Python"` | Already declared; no additional Gradle config needed |

### Supporting

| Component | Purpose | When to Use |
|-----------|---------|-------------|
| `Language.findLanguageByID("Python")` | Runtime Python language lookup | Safer than importing `PythonLanguage` directly — avoids compile-time coupling to internal API |
| `com.intellij.openapi.util.TextRange` | Describes the character-offset range within a host node where Python code lives | Used as the `rangeInsideHost` parameter in `MultiHostRegistrar.addPlace()` |
| `com.intellij.lang.injection.InjectedLanguageManager` | Testing: verify injection registered correctly | Available in `BasePlatformTestCase` test context |

### Alternatives Considered

| Standard Choice | Alternative | Why Standard Wins |
|-----------------|-------------|------------------|
| `MultiHostInjector` | `LanguageInjectionContributor` | Contributor is higher-level but does not support multiple PSI element types in one registration and has less control over offset boundaries — needed here for precise `${` / `}` delimiter exclusion |
| Runtime `Language.findLanguageByID("Python")` | Compile-time `PythonLanguage.INSTANCE` | Runtime lookup survives potential future `@Internal` annotation changes; `PythonLanguage` was briefly marked internal in 2025.3 EAP |

---

## Architecture Patterns

### Recommended File Structure

```
src/main/kotlin/.../lang/
└── injection/
    └── MakoPythonInjector.kt          # MultiHostInjector implementation

src/main/kotlin/.../lang/psi/impl/
├── MakoExpressionMixin.kt             # PsiLanguageInjectionHost for ${...}
├── MakoCodeBlockMixin.kt              # PsiLanguageInjectionHost for <% %>
└── MakoModuleBlockMixin.kt            # PsiLanguageInjectionHost for <%! %>
```

### Pattern 1: PsiLanguageInjectionHost Mixin (per node type)

**What:** A Kotlin class that extends `ASTWrapperPsiElement` and implements `PsiLanguageInjectionHost`. Wired into the BNF grammar via the existing `mixin` attribute so the generated `*Impl` class inherits it.

**When to use:** Any PSI node whose text range contains injected language content that should not change the generated PSI tree shape.

**Grammar-Kit BNF wiring (in `Mako.bnf`):**
```
expression ::= EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END
    {
      pin=1
      recoverWhile=expression_recover
      mixin="...lang.psi.impl.MakoExpressionMixin"
      implements="com.intellij.psi.PsiLanguageInjectionHost"
    }
```

Note: The `implements` attribute is ADDED alongside the existing attributes. The mixin class must provide the three `PsiLanguageInjectionHost` methods.

**Mixin implementation pattern (Kotlin):**
```kotlin
// Source: derived from Grammar-Kit's BnfStringImpl.java pattern (verified)
abstract class MakoExpressionMixin(node: ASTNode) :
    ASTWrapperPsiElement(node), MakoExpression {

    // The host is always valid — it exists as a parsed PSI node
    override fun isValidHost(): Boolean = true

    // For code injection with no escaping needed (Python code is raw text)
    // LiteralTextEscaper.createSimple() returns a pass-through escaper
    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        LiteralTextEscaper.createSimple(this)

    // updateText() is only called when the user edits the injected fragment
    // and the platform tries to propagate changes back to the host.
    // For Phase 6 (read-only injection), this can be a minimal implementation.
    override fun updateText(text: String): PsiLanguageInjectionHost {
        return ElementManipulators.handleContentChange(this, text)
    }
}
```

### Pattern 2: MultiHostInjector

**What:** Registers which PSI element classes to inspect, and for each matching element computes the Python sub-range and calls `registrar.startInjecting(python).addPlace(...).doneInjecting()`.

**Critical:** The `host` passed to `addPlace()` must be the `PsiLanguageInjectionHost` (the mixin-implementing node itself). The `rangeInsideHost` must be a `TextRange` relative to the start of the host node's text (offset 0 = first character of the node's text).

**Extension point registration in `plugin.xml`:**
```xml
<!-- Phase 6: Python Language Injection -->
<multiHostInjector
    implementation="...lang.injection.MakoPythonInjector"/>
```

**Injector skeleton (Kotlin):**
```kotlin
// Source: derived from MultiHostInjector javadoc pattern (verified via official source)
class MakoPythonInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(MakoExpression::class.java, MakoCodeBlock::class.java, MakoModuleBlock::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val python = Language.findLanguageByID("Python") ?: return

        when (context) {
            is MakoExpression -> injectIntoExpression(registrar, context, python)
            is MakoCodeBlock  -> injectIntoCodeBlock(registrar, context, python)
            is MakoModuleBlock -> injectIntoModuleBlock(registrar, context, python)
        }
    }
    // ...
}
```

### Pattern 3: TextRange Offset Calculation

**What:** For each node type, compute the character offsets within the node's own text that correspond to the Python content (excluding Mako delimiters).

**MakoExpression — PSI text structure:**
```
${<EXPR_CONTENT tokens...>}
^^ = EXPR_START (2 chars: $, {)
                           ^ = EXPR_END (1 char: })
```
The Python sub-range within the node's text: `TextRange(2, nodeText.length - 1)`.
This excludes `${` at the start and `}` at the end.

For expressions with filters (`${x | h}`), the filter part is Mako-specific syntax, not Python. Two approaches:
- Inject only the part before the first `FILTER_SEP` token (correct but complex to compute)
- Inject the full content including filter text (simpler; filter syntax is benign to Python parser — it just sees `x | h` which is valid Python bitwise-OR)

**Phase 6 recommendation:** Inject the full content between `${` and `}` for expressions. Filter disambiguation can be a Phase 7+ concern.

**MakoCodeBlock — PSI text structure:**
```
<% <CODE_CONTENT tokens...> %>
^^ = CODE_OPEN (2 chars: <, %)
                           ^^ = CODE_CLOSE (2 chars: %, >)
```
Python sub-range: `TextRange(2, nodeText.length - 2)`.

**MakoModuleBlock — PSI text structure:**
```
<%! <MODULE_CONTENT tokens...> %>
^^^ = MODULE_OPEN (3 chars: <, %, !)
                               ^^ = CODE_CLOSE (2 chars: %, >)
```
Python sub-range: `TextRange(3, nodeText.length - 2)`.

**IMPORTANT:** These are offsets within the node's text, not the file. `MultiHostRegistrar.addPlace()` takes the `PsiLanguageInjectionHost` and a range relative to that host's own text start. Do NOT subtract `node.startOffset` — the platform handles the host-to-document coordinate mapping.

### Anti-Patterns to Avoid

- **Importing `PythonLanguage.INSTANCE` directly:** Avoid compile-time dependency on the class. Use `Language.findLanguageByID("Python")` instead. If the Python language is not loaded (which would be a configuration error anyway), the injector returns early without crashing.
- **Using `LanguageInjector` (deprecated):** This is the old per-element API. `MultiHostInjector` is the current approach and the only one that satisfies the `PsiLanguageInjectionHost` contract.
- **Injecting into leaf tokens instead of composite PSI nodes:** The `addPlace()` host must be a `PsiLanguageInjectionHost`. Leaf tokens (`ASTNode` / `LeafPsiElement`) do not satisfy this interface. The composite `MakoExpression`, `MakoCodeBlock`, and `MakoModuleBlock` nodes (made into hosts via mixin) are the correct hosts.
- **Off-by-one in TextRange:** The `TextRange` end offset is **exclusive** in IntelliJ's convention (standard Java substring semantics). `TextRange(2, nodeText.length - 1)` means "from character at index 2 up to but not including `nodeText.length - 1`", i.e., it excludes the last character `}`.
- **Injecting into MakoControlLineStmt:** The `CONTROL_LINE` token includes the leading `%` delimiter and a full Python statement (e.g., `% for i in items:`). Injecting Python here requires stripping the `% ` prefix (1-2 chars). This is lower priority for Phase 6; the success criteria do not require it. Omit for now.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Offset mapping between host and injected file | Custom offset tracking | `LiteralTextEscaper.createSimple()` for no-escape content | The platform already handles all coordinate remapping via `LiteralTextEscaper`; getting it wrong causes exceptions during PSI access |
| Python syntax highlighting in injected ranges | Custom highlighter for Python tokens | Platform injector mechanism (registers Python PSI automatically) | Once injection is registered, Python's own highlighter/inspections apply automatically |
| Python language lookup | Store/cache `PythonLanguage` reference | `Language.findLanguageByID("Python")` at call time | Language IDs are registered at plugin init; lookup is cheap and safe; direct class import creates fragile binary coupling |
| ElementManipulator registration | Custom text manipulation | `ElementManipulators.handleContentChange()` in `updateText()` | Handles the "propagate injected edits back to host" contract without requiring a separate `ElementManipulator` registration for Phase 6 purposes |

**Key insight:** The platform injection machinery handles all the heavy lifting — PSI tree creation for injected fragments, editor fragment popup, re-highlighting on edit. The plugin only needs to correctly declare the host and the range.

---

## Common Pitfalls

### Pitfall 1: Generated PSI Impl classes cannot be directly modified

**What goes wrong:** `MakoExpressionImpl`, `MakoCodeBlockImpl`, and `MakoModuleBlockImpl` are generated files in `src/main/gen/`. Editing them directly would be overwritten by the next BNF regeneration.

**Why it happens:** GrammarKit generates `*Impl` classes from the BNF grammar. The `implements` and `mixin` attributes in the BNF control what interfaces and base classes the generated classes use.

**How to avoid:** Add `mixin="...MakoExpressionMixin"` and `implements="com.intellij.psi.PsiLanguageInjectionHost"` to the relevant rules in `Mako.bnf`, then run **Tools → Generate Parser Code** to regenerate. The generated `MakoExpressionImpl` will then extend the mixin and implement the interface without hand-editing.

**Warning signs:** The BNF grammar file is the source of truth. Any `implements` or `mixin` in `.java` files that aren't also in the `.bnf` file will be lost on regeneration.

### Pitfall 2: MakoExpression already has an existing `pin` + `recoverWhile` — be careful to preserve them

**What goes wrong:** Adding `mixin` and `implements` to an existing rule that already has `pin`, `recoverWhile` attributes. If the BNF edit is incomplete, the parser regeneration may fail or produce incorrect PSI.

**Why it happens:** BNF rule annotations use `{...}` block syntax where all attributes must be comma-separated correctly.

**How to avoid:** Edit the `{...}` block carefully, keeping all existing attributes and appending `mixin` and `implements`. Regenerate parser immediately after BNF edit and run `./gradlew check` to verify.

### Pitfall 3: TextRange boundaries include delimiters

**What goes wrong:** If the `TextRange` includes `${` or `%>`, the Python parser sees `${x` or `x %}` which is not valid Python, causing parse errors in the injected fragment and yellow error indicators in the editor.

**Why it happens:** The host node's text includes the Mako delimiters (they are leaf tokens in the PSI subtree). The range must exclude them.

**How to avoid:** Compute the range by counting delimiter lengths:
- Expression: prefix=2 (`${`), suffix=1 (`}`)
- CodeBlock: prefix=2 (`<%`), suffix=2 (`%>`)
- ModuleBlock: prefix=3 (`<%!`), suffix=2 (`%>`)

Validate with the Language Injection debug panel (Edit → Language Injections, or the injected fragment editor that appears when placing the cursor inside the injected region and pressing Alt+Enter → Edit Fragment).

### Pitfall 4: `elementsToInjectIn()` must return interface classes, not impl classes

**What goes wrong:** Returning `MakoExpressionImpl::class.java` instead of `MakoExpression::class.java`. The platform uses class-based dispatch on PSI elements, and the impl class hierarchy may not match what the platform checks.

**Why it happens:** The natural instinct is to use the concrete class; however the platform PSI dispatch works on the interface type.

**How to avoid:** Always return the interface (`MakoExpression`, `MakoCodeBlock`, `MakoModuleBlock`) from `elementsToInjectIn()`.

### Pitfall 5: `PsiLanguageInjectionHost` on nodes that also have the `recoverWhile` recovery set

**What goes wrong:** `MakoExpression` uses `recoverWhile=expression_recover`. If the expression is malformed (e.g., unclosed `${`), the PSI node may be partial. Calling `node.text` on a partial node could include unexpected tokens.

**Why it happens:** The recovery set can cause the expression node to include trailing tokens that were consumed during error recovery.

**How to avoid:** In the injector, check that the node contains both `EXPR_START` and `EXPR_END` children before injecting. Skip injection if the expression is malformed. The `isValidHost()` method can also return `false` for invalid/partial nodes.

### Pitfall 6: Python language not available (null from findLanguageByID)

**What goes wrong:** `Language.findLanguageByID("Python")` returns `null` if `PythonCore` is not loaded, causing a `NullPointerException` if the null is not guarded.

**Why it happens:** Defensive programming concern; in practice `PythonCore` is always loaded because it's declared as a `depends` in `plugin.xml` and as `bundledPlugins(PythonCore)` in `build.gradle.kts`.

**How to avoid:** Always null-check the return value of `findLanguageByID` and return early from `getLanguagesToInject()` if null. This prevents crashes in unexpected configurations.

---

## Code Examples

Verified patterns from official sources and codebase analysis:

### MakoExpression injection host mixin

```kotlin
// File: src/main/kotlin/.../lang/psi/impl/MakoExpressionMixin.kt
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.ElementManipulators
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoExpression

abstract class MakoExpressionMixin(node: ASTNode) :
    ASTWrapperPsiElement(node), MakoExpression {

    override fun isValidHost(): Boolean = true

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        LiteralTextEscaper.createSimple(this)

    override fun updateText(text: String): PsiLanguageInjectionHost =
        ElementManipulators.handleContentChange(this, text)
}
```

### MultiHostInjector for Python injection

```kotlin
// File: src/main/kotlin/.../lang/injection/MakoPythonInjector.kt
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoCodeBlock
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoExpression
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoModuleBlock

class MakoPythonInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(MakoExpression::class.java, MakoCodeBlock::class.java, MakoModuleBlock::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val python = Language.findLanguageByID("Python") ?: return
        val nodeText = context.text ?: return

        when (context) {
            is MakoExpression -> {
                // Exclude ${ (2 chars) at start and } (1 char) at end
                val start = 2
                val end = nodeText.length - 1
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as com.intellij.psi.PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
            is MakoCodeBlock -> {
                // Exclude <% (2 chars) at start and %> (2 chars) at end
                val start = 2
                val end = nodeText.length - 2
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as com.intellij.psi.PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
            is MakoModuleBlock -> {
                // Exclude <%! (3 chars) at start and %> (2 chars) at end
                val start = 3
                val end = nodeText.length - 2
                if (end > start) {
                    registrar.startInjecting(python)
                        .addPlace(null, null, context as com.intellij.psi.PsiLanguageInjectionHost,
                                  TextRange(start, end))
                        .doneInjecting()
                }
            }
        }
    }
}
```

### plugin.xml extension registration

```xml
<!-- Phase 6: Python Language Injection -->
<multiHostInjector
    implementation="com.github.kimuth.jetbrainsmakotemplateplugin.lang.injection.MakoPythonInjector"/>
```

### BNF grammar modification for MakoExpression

```
// In Mako.bnf — add mixin and implements to expression rule
expression ::= EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END
    {
      pin=1
      recoverWhile=expression_recover
      mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoExpressionMixin"
      implements="com.intellij.psi.PsiLanguageInjectionHost"
    }

code_block ::= CODE_OPEN CODE_CONTENT* CODE_CLOSE
    {
      pin=1
      recoverWhile=tag_recover
      mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoCodeBlockMixin"
      implements="com.intellij.psi.PsiLanguageInjectionHost"
    }

module_block ::= MODULE_OPEN MODULE_CONTENT* CODE_CLOSE
    {
      pin=1
      recoverWhile=tag_recover
      mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoModuleBlockMixin"
      implements="com.intellij.psi.PsiLanguageInjectionHost"
    }
```

**Note:** After modifying Mako.bnf, regenerate the parser via **Tools → Generate Parser Code** in the IDE (not a Gradle task). Commit the generated files.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `LanguageInjector` EP | `MultiHostInjector` EP | ~2016 | `LanguageInjector` is deprecated; `MultiHostInjector` is the standard |
| `LanguageInjectionContributor` for simple cases | Still valid alternative | Ongoing | Simpler API but less offset control; not ideal for template delimiters |
| Direct `PythonLanguage.INSTANCE` import | `Language.findLanguageByID("Python")` | 2025.3 EAP (briefly marked `@Internal`) | Runtime lookup is safer for future-proofing |

**Deprecated/outdated:**
- `LanguageInjector` (registered via `com.intellij.languageInjector`): Deprecated in favor of `MultiHostInjector`. Do not use.

---

## Open Questions

1. **Does `LiteralTextEscaper.createSimple()` exist in the 2025.2 platform API?**
   - What we know: Listed in the unofficial IntelliJ Community API docs as a static factory. Grammar-Kit's `BnfStringImpl.java` uses a custom escaper instead.
   - What's unclear: Whether `createSimple()` was added recently or has always existed. The name appears in search results but wasn't confirmed against 2025.2 source.
   - Recommendation: If `createSimple()` is not available, write a minimal `LiteralTextEscaper` subclass with: `isOneLine() = false`, `decode()` that appends the range verbatim, and `getOffsetInHost()` that returns `rangeInsideHost.startOffset + offsetInDecoded`.

2. **Does adding `implements="com.intellij.psi.PsiLanguageInjectionHost"` to the BNF require Parser regeneration before building?**
   - What we know: The BNF `implements` attribute controls the generated interface declaration in the `*Impl` class. Yes, regeneration is required.
   - What's unclear: Whether the test suite can pass while the generated files are stale.
   - Recommendation: Always regenerate parser immediately after BNF edits and commit the generated files. The `generateMakoParser` Gradle task is manual (per CLAUDE.md).

3. **Should `MakoControlLineStmt` also be an injection host?**
   - What we know: The success criteria don't mention control lines. Control lines (`% for i in items:`) contain Python statements with a `% ` prefix.
   - What's unclear: Whether injecting Python into `MakoControlLineStmt` with prefix offset=2 would produce correct Python statement context.
   - Recommendation: Defer to Phase 7. Phase 6 success criteria only require expressions and code blocks.

4. **Thread safety of `Language.findLanguageByID("Python")` in `getLanguagesToInject()`?**
   - What we know: `Language.findLanguageByID()` is a static lookup on an immutable registry populated at startup. It should be thread-safe.
   - Recommendation: Call per-invocation (cheap) rather than caching in the injector instance, to avoid any subtle initialization-order issues.

---

## Sources

### Primary (HIGH confidence)
- [IntelliJ Platform SDK — Language Injection](https://plugins.jetbrains.com/docs/intellij/language-injection.html) — API overview, MultiHostInjector and LanguageInjectionContributor documentation
- [MultiHostInjector.java source](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/injection/MultiHostInjector.java) — Verified method signatures, javadocs
- [PsiLanguageInjectionHost.java source](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/psi/PsiLanguageInjectionHost.java) — Verified interface methods: `isValidHost()`, `updateText()`, `createLiteralTextEscaper()`
- [Grammar-Kit BnfStringImpl.java](https://github.com/JetBrains/Grammar-Kit/blob/master/src/org/intellij/grammar/psi/impl/BnfStringImpl.java) — Canonical example of `PsiLanguageInjectionHost` implementation in a GrammarKit-based plugin
- Codebase read: `Mako.bnf`, `MakoTokenTypes.kt`, `MakoLexer.flex`, existing PSI impl files — confirmed node structure and delimiter lengths

### Secondary (MEDIUM confidence)
- [LiteralTextEscaper unofficial API docs](https://dploeger.github.io/intellij-api-doc/com/intellij/psi/LiteralTextEscaper.html) — Method signatures verified; `createSimple()` listed as static factory (not confirmed against 2025.2 platform source)
- [JetBrains Platform forum: Python package marked internal in 2025.3](https://platform.jetbrains.com/t/python-package-marked-internal-in-2025-3/2785) — Confirmed the transient `@Internal` annotation on `PythonLanguage`; issue was corrected
- [Community reports on Python language ID](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206118629-How-to-develop-a-plugin-that-depends-on-Python-plugin-) — Confirmed language ID string is `"Python"`
- [MultiHostRegistrar unofficial API docs](https://dploeger.github.io/intellij-api-doc/com/intellij/lang/injection/MultiHostRegistrar.html) — `addPlace()` parameter semantics confirmed

### Tertiary (LOW confidence)
- WebSearch results on GrammarKit `implements` attribute with `com.intellij.psi.PsiLanguageInjectionHost` — Multiple community sources agree on the pattern; not verified against Grammar-Kit's own documentation
- WebSearch on `LiteralTextEscaper.createSimple()` — Found in search results and unofficial docs but not confirmed in 2025.2 platform source

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — APIs verified against official source code and docs
- Architecture patterns: MEDIUM-HIGH — Patterns derived from verified examples (BnfStringImpl, MultiHostInjector docs); TextRange offsets computed from lexer grammar (HIGH confidence) but not integration-tested
- Pitfalls: MEDIUM — Derived from API analysis and community discussion; some (e.g., `isValidHost` for malformed nodes) need integration validation

**Research date:** 2026-02-21
**Valid until:** 2026-03-21 (IntelliJ Platform SDK is stable; Python language ID string is stable)

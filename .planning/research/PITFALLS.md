# Pitfalls Research

**Domain:** JetBrains custom language plugin — Mako template language support for PyCharm
**Researched:** 2026-02-19
**Confidence:** MEDIUM (training knowledge; external verification blocked during research session)

> Note: WebSearch, WebFetch, and Bash tools were unavailable during this research session. All findings draw
> from training knowledge of IntelliJ Platform plugin development patterns. Core platform APIs are stable
> enough that training data is reliable for fundamentals; version-specific API changes should be verified
> against the official IntelliJ Platform Plugin SDK docs before implementation.

---

## Critical Pitfalls

### Pitfall 1: Stateful Lexer Breaks Incremental Re-Lexing

**What goes wrong:**
The IntelliJ Platform incrementally re-lexes changed regions of a file, restarting the lexer from a known "clean" state boundary. If your lexer uses mutable state that cannot be correctly serialized and restored (e.g., tracking whether you're inside `${...}`, inside a `<%def ...>` block, or inside a Python expression), the platform cannot find a valid restart point. Re-lexing after an edit produces wrong token types for the rest of the file — syntax highlighting goes wrong, completions break, and PSI tree integrity is lost.

**Why it happens:**
Developers implement a recursive/stateful design that works in a single-pass scenario but does not implement `LexerBase.getState()` / `start(buffer, startOffset, endOffset, initialState)` correctly. They test full-file lexing and it passes, but never test restart-from-middle scenarios. Mako's nested constructs amplify this: `${expr | filter}` can appear inside HTML attribute values, inside `<%def>` bodies, making state combinations many.

**How to avoid:**
- Use JFlex (`.flex` grammar file) rather than a hand-written lexer — JFlex generates state machine code with correct state serialization automatically.
- Every state transition must be encodable as a single integer returned by `getState()`.
- State must encode ALL relevant context: are we in HTML? In `${}` expression? In a Mako tag attribute? Nesting depth if needed.
- Write lexer restart tests using `LexerTestCase` that lex a file, then re-lex starting at multiple mid-file offsets and compare token output.
- Keep state integer space small: use bit flags or a state machine enum mapped to integers, not object graphs.

**Warning signs:**
- Syntax highlighting "flickers" or goes wrong when you type in the middle of a file.
- Token types after the edit cursor are correct on first open but wrong after editing.
- Tests pass when lexing whole files but not when restarting mid-file.
- You find yourself storing `var` fields on the Lexer class that track nesting depth.

**Phase to address:** Lexer implementation phase (the first core language support phase). Correct state management must be designed in from the start — retrofitting a stateless restart is nearly impossible.

---

### Pitfall 2: Choosing the Wrong Multi-Language Strategy (Language Injection vs. Custom Language)

**What goes wrong:**
Mako mixes three languages in one file. Developers must choose between two fundamentally different approaches: (A) Register Mako as a custom language with its own PSI, use `TemplateDataLanguage` to host HTML, and inject Python into expression regions; or (B) Treat the file as HTML and inject Mako/Python via language injection. Choosing the wrong approach early forces a complete rewrite of the lexer, parser, PSI, and all extensions.

Approach B (treating Mako files as HTML with injections) fails because Mako's template syntax is not valid HTML — `% for x in items:` lines, `<%def name="foo():">` tags, and `##` comments break HTML parsing. You end up fighting the HTML parser to get syntax highlighting right.

**Why it happens:**
Language injection seems simpler to start with ("just inject Python into `${...}` blocks"). Developers prototype injection support quickly, it works for simple cases, then discover that full Mako syntax — especially control flow lines, def/block tags, and template inheritance — cannot be modeled as injections into an HTML host.

**How to avoid:**
- Register Mako as a custom language (`Language`, `FileType`, `ParserDefinition`). Mako IS the primary language of the file.
- Use `TemplateDataLanguage` (the IntelliJ mechanism for "the host language in template files") to indicate that HTML is what Mako outputs, so HTML-specific features (CSS color pickers, tag completion) work in the right regions.
- Use `MultiHostInjector` or `LanguageInjectionContributor` to inject Python into `${...}` expression regions and `<%! ... %>` / `<% ... %>` Python blocks.
- Study how Twirl (Scala) and Twig (PHP) plugins handle this — they use the custom language + template data approach.

**Warning signs:**
- You find yourself adding special cases to the HTML lexer or parser.
- Control flow lines (`% for`, `% if`) are being tokenized as "invalid HTML."
- You can't get highlighting for `##` line comments because HTML doesn't know about them.
- You're fighting `InjectedLanguageManager` more than using it.

**Phase to address:** Architecture/scaffolding phase before any lexer work. This decision cannot be reversed cheaply.

---

### Pitfall 3: PSI Tree Design That Cannot Support Reference Resolution

**What goes wrong:**
An insufficiently structured PSI tree cannot support navigation (go-to-definition), find usages, or refactoring. If `<%def name="myfunc">` is parsed as a generic "tag element" with no dedicated PSI node class for the def name, then implementing go-to-definition for calls like `${myfunc()}` requires heuristic text scanning instead of PSI reference resolution — which breaks in renamed-file scenarios and produces false positives.

**Why it happens:**
Developers design the PSI tree to minimize grammar complexity, treating all Mako tags as generic nodes and only adding specialized nodes when a feature visibly breaks. The problem is that PSI node types determine what `PsiReference` implementations can point to. Retrofitting reference targets into an already-built PSI means changing grammar production rules, which ripples into parser regeneration, existing tests, and annotator logic.

**How to avoid:**
- Design the PSI node hierarchy before writing the grammar. Map every Mako construct that will be a navigation target to a distinct PSI interface: `MakoDefStatement`, `MakoBlockStatement`, `MakoInheritDirective`, `MakoIncludeDirective`, `MakoNamespaceDirective`.
- Implement `PsiNamedElement` on definition nodes early — it enables rename refactoring essentially for free.
- Define `PsiReference` implementations for call sites before completing the parser phase.
- Use IntelliJ's `PsiElementFactory` pattern for creating PSI nodes programmatically in tests.

**Warning signs:**
- Go-to-definition is implemented as "text search in directory" rather than PSI reference resolution.
- Rename refactoring is disabled or uses find-and-replace-in-files.
- PSI tree has a single `MakoElement` type used for many different constructs.
- Integration tests for reference resolution don't exist in early phases.

**Phase to address:** Parser/PSI design phase. Node types must be modeled before the grammar is finalized.

---

### Pitfall 4: Thread Safety Violations in PSI Access

**What goes wrong:**
IntelliJ Platform enforces that PSI reads happen under a read lock (and writes under a write lock via `WriteCommandAction`). Custom language plugin code that accesses PSI from background threads without acquiring the read lock throws `ProcessCanceledException` or corrupts the PSI tree. This manifests as intermittent `AssertionError: Read access is allowed from event dispatch thread or inside read-action only` exceptions in production, which are hard to reproduce and often reported as IDE crashes.

**Why it happens:**
Completion contributors, annotators, and reference resolvers look synchronous but are sometimes invoked from background threads. Developers new to IntelliJ Platform write:
```kotlin
// WRONG — no read lock
val element = file.findElementAt(offset)
```
instead of:
```kotlin
// CORRECT
ApplicationManager.getApplication().runReadAction {
    val element = file.findElementAt(offset)
}
```
The mistake is invisible during development (the IDE often happens to run on the right thread in dev mode) but surfaces in production under load.

**How to avoid:**
- Enable `Registry.is("ide.slow.operations.assertion")` during development to surface threading violations earlier.
- Use `ReadAction.compute<T, E>` (the Kotlin-friendly form) instead of raw `runReadAction`.
- Annotators must be stateless and not cache PSI outside their `annotate()` call.
- For long-running analysis (like Python type resolution for `${...}` expressions), use `ReadAction.nonBlocking()` with a cancellation token.
- Write tests that invoke completion/annotations from a background thread to catch violations.

**Warning signs:**
- Intermittent `AssertionError` in IDE logs mentioning "read access."
- Features work perfectly in the sandbox IDE but crash in production.
- Background highlighter (daemon) passes but inspector fails.

**Phase to address:** All phases — but the threading model must be explained in the architecture doc before the first feature is implemented. Retrofitting correct threading is expensive.

---

### Pitfall 5: Depending on Python Plugin Internal APIs

**What goes wrong:**
PyCharm's Python plugin exposes `PyFile`, `PyExpression`, `PyType` and related PSI types for Python analysis. These are attractive for analyzing `${expression}` content, but they are treated as internal/unstable APIs. Depending on them directly without stability guarantees causes the plugin to break on every PyCharm release when internal Python plugin APIs move, are renamed, or removed.

**Why it happens:**
The developer wants real Python type analysis inside `${...}` expressions, sees that `PyCharmCorePluginResolution.resolve()` or `PyReferenceExpression.getType()` does exactly what's needed, and uses it — without realizing these are not part of the stable platform API contract.

**How to avoid:**
- Only depend on the Python plugin via its declared extension points (`<depends>com.intellij.modules.python</depends>` or the PyCharm-specific module).
- Use `LanguageInjectionHost` + language injection to embed Python as an injected language — let the Python plugin handle its own PSI. This is the officially supported integration path.
- If you must access Python PSI types, wrap all access in try-catch for `ClassNotFoundException` and `NoSuchMethodError` to survive API changes gracefully.
- Subscribe to Python plugin API compatibility warnings in JetBrains's third-party plugin tracker.

**Warning signs:**
- Direct imports of `com.jetbrains.python.psi.*` classes in non-injection code.
- Plugin fails to load on PyCharm updates even though your code didn't change.
- Build warnings about depending on non-stable modules.

**Phase to address:** Integration with PyCharm Python plugin phase. Must be explicitly designed around the injection boundary.

---

### Pitfall 6: Incorrect `sinceBuild` / `untilBuild` Range Causes Marketplace Rejection

**What goes wrong:**
The `sinceBuild` and `untilBuild` values in `plugin.xml` (or `gradle.properties`) define which IDE versions are compatible. Setting `untilBuild` too low rejects the plugin for new IDE versions. Setting it too high (or omitting it) causes the plugin to appear compatible with future IDEs where APIs may have changed, leading to user crashes on new releases. JetBrains Marketplace rejects plugins that claim compatibility they haven't verified.

**Why it happens:**
Developers set `sinceBuild=252` (the current version at time of development) and either omit `untilBuild` or set it to `252.*` without considering forward compatibility. The IntelliJ Platform Gradle Plugin's `pluginVerifier` catches some issues, but not all API incompatibilities.

**How to avoid:**
- Use the IntelliJ Platform Gradle Plugin's `runPluginVerifier` task against the full set of target IDE builds before each release.
- Set `untilBuild` to `[major].*` (e.g., `252.*`) to limit to the current major train, then explicitly expand after verifying the next major.
- Subscribe to the JetBrains Platform releases blog and the `intellij-platform-plugin-template` changelog for breaking API changes.
- Pin specific API calls with `@ApiStatus.Experimental` or `@ApiStatus.Internal` annotations' usage noted during development so you know what to re-verify on each IDE update.

**Warning signs:**
- `pluginVerifier` reports warnings you're ignoring.
- Plugin works in your dev environment but users report "plugin incompatible" errors.
- You haven't run `verifyPlugin` since initial scaffolding.

**Phase to address:** Build/release phase, but `untilBuild` must be configured correctly in the scaffolding phase.

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hand-written lexer instead of JFlex | Faster to start coding | Restart-state bugs; regex is error-prone; hard to maintain | Never — JFlex is mandatory for IntelliJ lexers at production quality |
| Regex-based "parser" (no actual grammar) | Skips grammar learning curve | Cannot build PSI tree; no reference resolution; no refactoring ever | Only for throwaway proof-of-concept, never ship |
| Single PSI node type for all Mako tags | Simpler grammar | Cannot implement per-element features; reference resolution impossible | MVP only for syntax highlighting; must be fixed before navigation |
| Injecting Python via `InjectedLanguageManager` directly instead of `MultiHostInjector` | Fewer API surfaces to learn | Injection fragments don't merge across multiple ranges; Python analysis sees incomplete code | Never; always use the proper `MultiHostInjector`/`LanguageInjectionContributor` path |
| `PsiFile.text.indexOf(...)` for reference resolution | Fast to implement | Breaks on renames, false positives, O(n) per reference | Never in a shipped plugin |
| Skipping `getState()` implementation in lexer | Lexer works for full-file parse | Incremental re-lex produces wrong results everywhere | Never; `getState()` must be correct |
| Hard-coding Mako file extensions without user configuration | Simpler initial setup | Users with non-standard extensions get no support; blocks future flexibility | Acceptable for MVP if extension settings are added before first public release |

---

## Integration Gotchas

Common mistakes when connecting to external services or platform APIs.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Python plugin dependency | Declaring `<depends>com.intellij.modules.python</depends>` without making it optional for non-PyCharm IDEs | Use `<depends optional="true" config-file="python-support.xml">` for graceful degradation |
| HTML language injection host | Using `HtmlFileImpl` as injection host directly | Implement `MultiHostInjector` with proper `InjectedLanguageManager.enumerate()` pattern |
| Python language injection into `${}` | Injecting the full `${...}` including delimiters | Inject only the expression content between delimiters; the host ranges must not overlap |
| File type detection | Registering `.mako` as both HTML and Mako file type | Register exactly one file type per extension; Mako must own `.mako`; HTML owns `.html` |
| `TemplateDataLanguage` setup | Not registering `TemplateDataLanguagePatterns` for the Mako file type | Without this, HTML-specific features (CSS, JS injection) won't work inside Mako template content |
| PSI reference completion | Contributing completions in `CompletionContributor.fillCompletionVariants` synchronously on EDT | Use `CompletionResultSet.runRemainingContributors` with proper threading |
| Plugin XML dependency declaration | Forgetting to add `<depends>com.intellij.modules.python</depends>` for PyCharm targeting | Plugin loads on all IDEs but crashes when Python PSI types are unavailable |

---

## Performance Traps

Patterns that work in development but fail as file sizes grow.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Annotator does expensive work synchronously | IDE freezes when annotating large `.mako` files; typing lag | Move slow analysis to `ExternalAnnotator`; return early from `Annotator.annotate()` if analysis > ~50ms | Files > ~500 lines |
| PSI tree traversal on every keystroke | Completion is slow; UI thread blocks | Cache analysis results in `CachedValuesManager` keyed to PSI modification count | Files > ~200 lines or projects with many `.mako` files |
| Lexer allocates objects per token | Profiler shows GC pressure during typing | Pre-allocate token type arrays; use `IElementType` flyweights (they already are singletons if registered correctly) | Projects with many open mako files |
| Language injection re-injected on every PSI change | Python analysis triggers full re-analysis constantly | Use `MultiHostInjector` with stable range calculation; avoid injecting into ephemeral PSI nodes | Projects with > 10 open Mako files |
| `FindUsagesProvider` walks entire project synchronously | Find usages hangs for large projects | Implement `WordsScanner` to let the platform pre-filter before calling your provider | Projects with > 100 `.mako` files |

---

## Security Mistakes

Domain-specific security issues for IDE plugins (not web security).

| Mistake | Risk | Prevention |
|---------|------|------------|
| Executing Mako template content from the IDE | IDE can be used to execute arbitrary Python code without user awareness | Never evaluate/execute template content; plugin is for analysis only |
| Storing absolute file paths in plugin settings | Settings break on machine migration or when project is shared | Store paths relative to project root; use `project.basePath` as anchor |
| Logging PSI element text verbatim in plugin logs | Template source code (potentially containing secrets or PII) appears in IDE diagnostic logs | Log only structural information (element type, line number), never element text |
| Depending on Marketplace plugins without version pinning | Transitive dependency on unstable plugins causes plugin to be pulled when they're deprecated | Prefer bundled platform modules; if depending on Marketplace plugins, pin version ranges |

---

## UX Pitfalls

Common user experience mistakes specific to language plugins.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Error highlighting for every unrecognized construct during file load | Template appears full of red errors on first open; users distrust the plugin | Show errors only after PSI stabilizes (use `Annotator` with `holder.createErrorAnnotation` only for definitively invalid syntax, not unresolved references during indexing) |
| Code completion triggers inside HTML text nodes with Mako suggestions | Mako completions appear where users are typing plain HTML prose | Scope completion to only trigger inside Mako constructs; check parent PSI element before contributing |
| Navigation "go to definition" for `<%inherit>` opens file but positions cursor at line 1 | User expects cursor at the `<%def>` entry point, not top of file | Implement `OpenFileDescriptor` with the specific element offset, not just the file |
| Folding collapses too aggressively | Users cannot see template structure at a glance | Default folding closed only for `<%doc>` comments and `<%!>` module blocks; leave defs/blocks expanded by default |
| Plugin icon missing or using generic puzzle piece | Plugin is hard to identify in the plugin list | Create a proper SVG plugin icon; 40x40 for light theme, variant for dark theme |

---

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Syntax highlighting:** Often missing — filter expression highlighting (`${x | h,trim}`). Verify that `|` and filter names after it are highlighted correctly, not treated as operators.
- [ ] **Lexer:** Often missing — correct handling of `<%doc>` multi-line comments that span many lines without resetting to HTML state prematurely. Verify with files that have `<%doc>` near end of file.
- [ ] **File type detection:** Often missing — association for `.html` files that contain Mako syntax (not just `.mako`). Verify that users can opt `.html` files into Mako mode.
- [ ] **Template inheritance navigation:** Often missing — resolution of `<%inherit file="base.html"/>` when the path is relative and the project uses a Mako lookup path (not the file system root). Verify with non-trivial project layouts.
- [ ] **Python injection:** Often missing — injected Python ranges that span multiple `${...}` expressions in the same logical Python context. Verify that the injected Python file visible to the Python plugin is coherent, not fragmented.
- [ ] **Namespace imports:** Often missing — `<%namespace name="h" file="helpers.html"/>` creates a namespace; `${h.helper()}` calls into it. Navigation to `h.helper` must resolve across namespace boundaries. Verify this is on the roadmap even if deferred.
- [ ] **Error recovery:** Often missing — parser must recover gracefully from malformed Mako so the rest of the file still parses. Test with intentionally broken `.mako` files; the whole file should not show as one big error.
- [ ] **Plugin compatibility verification:** Often missing — `runPluginVerifier` against PyCharm Community, PyCharm Professional, and IntelliJ IDEA (with Python plugin installed) before each release.

---

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Stateful lexer restart bugs discovered late | HIGH | Rewrite lexer state machine from scratch using JFlex; all lexer tests must be re-written; syntax highlighting tests will fail during transition |
| Wrong multi-language strategy (injection instead of custom language) | VERY HIGH | Full rewrite of FileType, Language, Lexer, Parser, PSI tree; all extensions must be re-registered; treat as a new feature branch |
| PSI node types too coarse for reference resolution | MEDIUM | Add new node types to grammar; regenerate parser; update all places that pattern-match on PSI types; existing annotators/completions still work but need updating |
| Thread safety violations found in production | MEDIUM | Systematic audit of all PSI access points; wrap in `ReadAction`; add threading tests to CI |
| Python plugin API breaks on IDE update | LOW-MEDIUM | Identify which API changed via changelogs; update to new API or fall back to injection-only approach; release patch update |
| `sinceBuild`/`untilBuild` misconfiguration reported by users | LOW | Update `gradle.properties`; run `pluginVerifier`; push hotfix release to Marketplace |

---

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Stateful lexer restart bugs | Lexer implementation (earliest core phase) | `LexerTestCase` tests that restart from multiple mid-file offsets; must pass before phase closes |
| Wrong multi-language strategy | Architecture / scaffolding phase (before lexer) | Decision documented in architecture doc; spike with minimal HTML + Mako + Python injection working end-to-end |
| PSI tree too coarse for navigation | Parser/PSI design phase | Every Mako def/block/inherit/include has a dedicated PSI node class; navigation test exists even if it only passes a stub |
| Thread safety violations | First feature phase (establish pattern) | Threading test in CI; enable `ide.slow.operations.assertion` in sandbox |
| Python plugin internal API dependency | Python integration phase | All Python PSI access goes through injection boundary; no direct `com.jetbrains.python.psi.*` imports outside injection code |
| `sinceBuild`/`untilBuild` misconfiguration | Build/release phase | `runPluginVerifier` as required CI check, not optional |
| Annotator performance | Annotator/inspection phase | Benchmark annotator on a 1000-line `.mako` file; must complete < 100ms |
| Injection fragmentation | Python injection phase | Injected Python file for a template with 5+ `${}` expressions must be a single coherent fragment, visible in "injected file" debug view |
| Namespace/inheritance navigation missing | Navigation phase | Explicit test: `<%namespace name="h" file="helpers.html"/>` resolves to def in helpers.html |

---

## Sources

- IntelliJ Platform Plugin SDK documentation (https://plugins.jetbrains.com/docs/intellij/) — training data; verify against current docs
- IntelliJ Platform Plugin Template (https://github.com/JetBrains/intellij-platform-plugin-template) — observed in project scaffold
- Training knowledge of JetBrains custom language support APIs: `LexerBase`, `ParserDefinition`, `MultiHostInjector`, `LanguageInjectionContributor`, `PsiReference`, `CachedValuesManager`, `ExternalAnnotator`, `TemplateDataLanguage`
- Known patterns from Twig, Blade, Velocity, and Twirl IntelliJ plugins (open source references on GitHub) — training data
- IntelliJ Platform source code patterns for template language plugins (e.g., `intellij-plugins` repo on GitHub) — training data
- Project codebase analysis: `.planning/codebase/concerns.md`, `.planning/codebase/architecture.md`, `.planning/codebase/stack.md`

> Confidence note: All findings are MEDIUM confidence. Core IntelliJ Platform plugin architecture (lexer
> state management, PSI design, threading model, language injection) is stable and well-documented.
> Version-specific API details (exact class names for 2025.x platform) should be verified against official
> docs before coding. The pitfalls described here are drawn from patterns common across multiple plugin
> projects and community knowledge; they are not speculative.

---
*Pitfalls research for: JetBrains Mako template language plugin (PyCharm)*
*Researched: 2026-02-19*

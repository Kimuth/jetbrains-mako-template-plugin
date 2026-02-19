# Stack Research

**Domain:** JetBrains custom language plugin — Mako template language (HTML + Mako directives + embedded Python)
**Researched:** 2026-02-19
**Confidence:** MEDIUM (IntelliJ Platform APIs verified against official docs structure; GrammarKit/JFlex version pinning LOW — verify before coding)

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| IntelliJ Platform | 2025.2.5 (build 252) | Plugin host and API surface | Already configured; provides all Language, PSI, lexer, parser, completion, reference APIs needed |
| IntelliJ Platform Gradle Plugin | 2.11.0 | Build orchestration and plugin packaging | Already configured; 2.x line is the current official replacement for the old `gradle-intellij-plugin` 1.x |
| Kotlin | 2.3.0 | Implementation language | Already configured; idiomatic for IntelliJ Platform plugins — Kotlin null-safety and extension functions align with platform APIs |
| JFlex | 1.9.2 | Lexer generator | Generates Java/Kotlin-compatible scanner from `.flex` grammar files; IntelliJ uses JFlex internally and GrammarKit ships it as a dependency |
| Grammar-Kit | 2024.3.4 | Parser/PSI generator | Generates parser and PSI node classes from `.bnf` grammar files; the official JetBrains tool for custom language PSI trees |
| Java | 21 | JVM runtime target | Already configured; required by platform 2025.2.5 |

**Confidence for versions:** LOW — GrammarKit 2024.3.4 and JFlex 1.9.2 are from training data (August 2025). Verify current releases at https://plugins.jetbrains.com/plugin/6606-grammar-kit and https://github.com/JetBrains/Grammar-Kit/releases before pinning.

---

### Plugin Dependencies (Declared in plugin.xml)

| Dependency | Type | Purpose | Confidence |
|------------|------|---------|------------|
| `com.intellij.modules.platform` | Bundled module | Base platform APIs (VFS, PSI infrastructure, notifications) | HIGH — already in plugin.xml |
| `com.intellij.modules.lang` | Bundled module | Language infrastructure (Language, FileType, Lexer, Parser, SyntaxHighlighter, Annotator) | HIGH — required for custom language plugins |
| `com.intellij.modules.python` | Bundled module (PyCharm only) | Python PSI access for `${...}` expression analysis; enables treating embedded expressions as Python fragments | MEDIUM — requires PyCharm as target IDE |
| `com.intellij.html` | Bundled module | HTML PSI for HTML host language; enables treating the HTML skeleton of .mako files as HTML | MEDIUM — available in IntelliJ IDEA and PyCharm |

**Note:** Depending on `com.intellij.modules.python` locks the plugin to PyCharm. This is intentional per PROJECT.md. Do NOT depend on `com.jetbrains.python` (the marketplace Python plugin) — use the bundled PyCharm module instead.

---

### Core IntelliJ Platform Extension Points

These are the specific extension points to register in `plugin.xml` for each feature. Listed in dependency order (build this sequence).

| Extension Point | Purpose | Phase |
|-----------------|---------|-------|
| `com.intellij.fileType` | Register `.mako` file type and MIME type | Phase 1 |
| `com.intellij.lang.fileNameMatcher` | Associate `.mako` / `.html` (when containing Mako markers) with MakoLanguage | Phase 1 |
| `com.intellij.lang.syntaxHighlighterFactory` | Register `SyntaxHighlighter` for token coloring | Phase 1 |
| `com.intellij.colorSettingsPage` | Register Color Settings page so users can customize Mako colors | Phase 1 |
| `com.intellij.lang.parserDefinition` | Register `ParserDefinition` that provides lexer + parser + PSI factories | Phase 2 |
| `com.intellij.lang.braceMatcher` | Brace/delimiter matching (e.g., `${` and `}`, `<%` and `>`) | Phase 2 |
| `com.intellij.lang.commenter` | Line/block comment handling (`##` for Mako line comments) | Phase 2 |
| `com.intellij.annotator` | Semantic error detection (malformed tags, unclosed blocks) | Phase 3 |
| `com.intellij.completion.contributor` | Code completion for Mako directives, tag attributes | Phase 3 |
| `com.intellij.lang.documentationProvider` | Hover documentation for Mako directives | Phase 3 |
| `com.intellij.psi.referenceContributor` | Go-to-definition for `<%inherit file="..."/>`, `<%include file="..."/>` | Phase 4 |
| `com.intellij.lang.findUsagesProvider` | Find usages for `<%def name="...">` across templates | Phase 4 |
| `com.intellij.lang.refactoring.inlineHandler` or `renameHandler` | Rename refactoring for def names | Phase 5 |
| `com.intellij.lang.foldingBuilder` | Code folding for `<%def>`, `<%block>`, control structures | Phase 3 |
| `com.intellij.lang.formatter` | Code formatting (optional, complex for mixed-language files) | Phase 5 |
| `com.intellij.languageInjector` or `com.intellij.multiHostInjector` | Inject Python into `${...}` and `<% %>` blocks; inject HTML into template body | Phase 4 |

---

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Grammar-Kit JFlex (via GrammarKit plugin) | Bundled with GrammarKit | Lexer file generation from `.flex` → Java scanner class | Always — use GrammarKit Gradle plugin task to generate before compiling |
| IntelliJ Platform Test Framework | Bundled (252+) | `BasePlatformTestCase`, `ParsingTestCase`, `LexerTestCase`, `CompletionTestCase` | All testing — provides in-process IDE for integration tests |
| JUnit 4 | 4.13.2 | Test runner | Currently configured; acceptable but see note on JUnit 5 migration below |
| `com.intellij.lang.html` API | Bundled | `HtmlFileViewProvider`, `HTMLLanguage` for treating template body as HTML | When implementing multi-language file support (Phase 4) |
| `com.jetbrains.python.psi` API | Bundled in PyCharm | `PyExpression`, `PyFile` for analyzing embedded Python in `${...}` | When implementing Python expression analysis (Phase 4) |

---

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| GrammarKit IntelliJ Plugin | IDE plugin for editing `.bnf` grammar files with live preview | Install in your development IDE: plugins.jetbrains.com/plugin/6606-grammar-kit |
| GrammarKit Gradle Plugin | `org.jetbrains.grammarkit` Gradle plugin — invokes JFlex and GrammarKit generators as build tasks | Add to `build.gradle.kts` with `generateLexer` and `generateParser` tasks |
| `runIde` Gradle task | Launches sandbox IDE with plugin loaded for manual testing | Already configured in scaffold via `intellijPlatform { runIde }` |
| Plugin Verifier | Validates binary compatibility across IntelliJ versions | Already configured via `pluginVerification { ides { recommended() } }` |

---

## Gradle Configuration Required

The following changes are needed to `build.gradle.kts` and `gradle.properties` to support GrammarKit-based generation:

```kotlin
// build.gradle.kts additions

plugins {
    // Add to existing plugins block:
    id("org.jetbrains.grammarkit") version "2022.3.2"  // verify current version
}

// GrammarKit tasks — run before compilation
tasks {
    generateLexer {
        sourceFile.set(file("src/main/java/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexer.flex"))
        targetOutputDir.set(file("src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang"))
        purgeOldFiles.set(true)
    }

    generateParser {
        sourceFile.set(file("src/main/java/com/github/kimuth/jetbrainsmakotemplateplugin/lang/Mako.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToParser.set("/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java")
        pathToPsiRoot.set("/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi")
        purgeOldFiles.set(true)
    }

    compileKotlin {
        dependsOn(generateLexer, generateParser)
    }

    compileJava {
        dependsOn(generateLexer, generateParser)
    }
}

// Add generated sources to source sets
sourceSets {
    main {
        java.srcDirs("src/main/gen")
    }
}
```

```toml
# gradle/libs.versions.toml additions
[versions]
grammarKit = "2022.3.2"   # VERIFY: check https://github.com/JetBrains/Grammar-Kit/releases

[plugins]
grammarKit = { id = "org.jetbrains.grammarkit", version.ref = "grammarKit" }
```

```xml
<!-- plugin.xml: replace com.intellij.modules.platform with expanded dependencies -->
<depends>com.intellij.modules.platform</depends>
<depends>com.intellij.modules.lang</depends>
<depends optional="true" config-file="mako-python.xml">com.intellij.modules.python</depends>
```

**Confidence for GrammarKit Gradle plugin version `2022.3.2`:** LOW — verify at https://github.com/JetBrains/Grammar-Kit before using. The GrammarKit Gradle plugin has historically lagged behind the IntelliJ plugin version numbering.

---

## Multi-Language Architecture Decision

**The right approach for Mako is: Custom Language + Language Injection (not pure language injection).**

Do NOT use pure language injection (registering Mako as an injected fragment in HTML). Mako IS the host language — it controls the file's structure. HTML and Python are the guests.

**Architecture:**

```
MakoLanguage (host)
├── MakoFileType (.mako extension)
├── MakoLexer (JFlex-generated) — tokenizes entire file
├── MakoParser (GrammarKit-generated) — builds PSI tree
├── MakoPsiFile (root PSI element)
│
├── HTML injection: inject com.intellij.HTMLLanguage into template body regions
│   └── InjectedLanguageManager.getInstance(project).injectLanguagesIn(element, ...)
│
└── Python injection: inject com.jetbrains.python.PythonLanguage into:
    ├── ${...} expression blocks
    ├── <% ... %> code blocks
    ├── <%! ... %> module-level blocks
    └── % for/if/while control lines (Python expression portion)
```

**Key API:** `com.intellij.lang.injection.MultiHostInjector` is the correct interface for injecting multiple languages into host PSI elements. Register via `com.intellij.multiHostInjector` extension point.

**Alternative considered:** `TemplateDataLanguageMappings` + `com.intellij.lang.fileViewProviderFactory` — used by Twig, Blade, and Velocity plugins for template languages. This approach treats the file as HTML with a template overlay. It is simpler for the HTML case but makes Python injection harder and gives less control over Mako-specific PSI structure. **Do not use this for Mako.**

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `gradle-intellij-plugin` 1.x (old) | Deprecated; replaced by `org.jetbrains.intellij.platform` 2.x | `org.jetbrains.intellij.platform` 2.11.0 (already configured) |
| `org.jetbrains.changelog` 3.x (if released) | Verify — 2.5.0 is configured and working | Stay on 2.5.0 until verified |
| Pure TextMate grammar (`.tmLanguage`) | No PSI tree = no completion, references, or refactoring | JFlex + GrammarKit for full PSI |
| RegexFilter-based syntax highlighting only | Cannot handle nested constructs or context-sensitive tokens like `${` inside HTML attributes | Full JFlex lexer with state machine |
| `LanguageInjector` (single-language interface) | Only injects one language per host element | `MultiHostInjector` for injecting both HTML and Python into Mako regions |
| Depending on `com.jetbrains.python` marketplace plugin | Creates fragile marketplace dependency; not guaranteed bundled | Depend on `com.intellij.modules.python` bundled module (PyCharm-only) |
| JUnit 5 without platform test framework confirmation | IntelliJ Platform test framework still primarily targets JUnit 4 for `BasePlatformTestCase` | Keep JUnit 4 until JUnit 5 migration path is confirmed for IntelliJ Platform test classes |
| Directly targeting IntelliJ IDEA Community (not PyCharm) | Python module not available; embedded Python analysis impossible | Target PyCharm as `<idea-version>` platform or use `com.intellij.modules.python` optional dependency with degraded mode |

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| GrammarKit + JFlex for parser generation | Hand-written recursive descent parser | Only if grammar is too context-sensitive for BNF (Mako is not — it is mostly context-free with lexer states) |
| MultiHostInjector for language injection | TemplateDataLanguageMappings | If building a simpler template plugin where the template language IS HTML-with-markers (e.g., Smarty, Twig); Mako has too much structure for this |
| Depend on `com.intellij.modules.python` (PyCharm bundled) | Parse Python expressions with custom mini-parser | Only if targeting IntelliJ IDEA Community without Python support; creates massive scope increase |
| Register MakoLanguage as custom Language subclass | Inject Mako as a dialect of HTML | Mako is not a dialect of HTML; the overall file structure is Mako-controlled with HTML regions embedded |
| Custom color settings page via `colorSettingsPage` | Hard-code colors in `SyntaxHighlighter` | Hard-coding prevents user customization — always register a color settings page |

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| IntelliJ Platform 2025.2.5 (build 252) | GrammarKit 2022.3+ | GrammarKit PSI generation API has been stable since 2022; verify no breaking changes in 252 |
| IntelliJ Platform 2025.2.5 | Python plugin bundled in PyCharm 2025.2 | Python PSI API (`com.jetbrains.python.psi`) is stable but internal — annotate with `@ApiStatus.Internal` awareness |
| Kotlin 2.3.0 | IntelliJ Platform 252 | Platform 252 uses Kotlin 2.x internals; Kotlin 2.3.0 plugin code is compatible |
| JFlex 1.9.x | GrammarKit 2022.3+ | GrammarKit bundles JFlex; do NOT add JFlex as a separate Gradle dependency — let GrammarKit manage it |
| Gradle 9.3.1 | IntelliJ Platform Gradle Plugin 2.11.0 | Confirmed — scaffold already uses this combination |

---

## Stack Patterns by Variant

**If targeting PyCharm only (current plan):**
- Declare hard dependency on `com.intellij.modules.python`
- Inject Python PSI directly into `${...}` blocks via MultiHostInjector
- Access `PyExpression` for type inference and completion in expressions
- Configure `platformVersion = 2025.2.5` (PyCharm Professional build)

**If targeting both PyCharm and IntelliJ IDEA:**
- Make Python dependency optional: `<depends optional="true" config-file="mako-python.xml">com.intellij.modules.python</depends>`
- Implement graceful degradation: syntax highlighting works everywhere, Python expression analysis only in PyCharm
- Use `LanguageUtil.isInjectedLanguageFragment()` guards around Python PSI access
- Significantly increases complexity — not recommended for v1

**If grammar becomes too complex for GrammarKit BNF:**
- Fall back to hand-written `PsiParser` implementing `com.intellij.lang.PsiParser`
- Keep JFlex lexer (it handles the complex tokenization states well)
- This is the approach used by some complex language plugins (e.g., Rust plugin)
- Mako grammar is not complex enough to require this in v1

---

## Sources

- IntelliJ Platform Docs — Custom Language Support Tutorial: https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html (HIGH confidence for API names; verify extension point names for build 252)
- IntelliJ Platform Docs — Language Injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html (MEDIUM confidence for MultiHostInjector API)
- IntelliJ Platform Docs — Syntax Highlighting and Error Highlighting: https://plugins.jetbrains.com/docs/intellij/syntax_errors.html (HIGH confidence — stable API)
- Grammar-Kit GitHub: https://github.com/JetBrains/Grammar-Kit (LOW confidence for current version number — check releases page)
- IntelliJ Platform Gradle Plugin 2.x docs: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html (HIGH confidence — this is the current tooling)
- Existing scaffold `gradle.properties` and `build.gradle.kts` — confirms platform version 2025.2.5, build 252, Gradle 9.3.1, Kotlin 2.3.0, IntelliJ Platform Gradle Plugin 2.11.0 (HIGH confidence — directly observed)
- Training knowledge (August 2025 cutoff) — GrammarKit API patterns, MultiHostInjector usage, PSI generation workflow (MEDIUM confidence — verify before implementing)

**Note on GrammarKit version:** Training data indicates GrammarKit 2022.3.x as the most recently stable Gradle plugin version. This MUST be verified before implementation. Check: https://github.com/JetBrains/Grammar-Kit/releases and the Gradle plugin portal at https://plugins.gradle.org/plugin/org.jetbrains.grammarkit

---

*Stack research for: JetBrains Mako Template Language Plugin*
*Researched: 2026-02-19*

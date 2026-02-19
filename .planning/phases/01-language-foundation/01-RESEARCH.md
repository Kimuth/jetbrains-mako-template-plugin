# Phase 1: Language Foundation - Research

**Researched:** 2026-02-19
**Domain:** IntelliJ Platform custom language registration, FileType, GrammarKit/JFlex Gradle integration
**Confidence:** HIGH (core APIs verified against official docs; GrammarKit integration MEDIUM — complete Kotlin DSL not fully available in official docs, requires cross-referencing)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### File associations
- Recognize three extension patterns: `.mako`, `.mak`, and `.html.mako`
- All extensions treated identically — no special behavior for compound extensions
- If another plugin claims `.mako`, our plugin should override (take priority as the purpose-built solution)

#### File icon design
- Text-based icon: stylized letter "M" on a file shape
- Follow standard JetBrains file icon shape (rounded rectangle with folded corner)
- Single icon version for both light and dark themes

#### Language identity
- Language display name in IDE: "Mako Template"
- Plugin name for JetBrains Marketplace: "Mako Template Support"
- Plugin positioning: PyCharm-focused (Mako is a Python template engine)
- Target development/test IDE: PyCharm Community

#### Scaffold cleanup
- Keep GitHub Actions CI/CD workflow and Changelog plugin
- Target IDE switched from IntelliJ IDEA to PyCharm Community

### Claude's Discretion
- File icon color choice (pick something that works well in the JetBrains icon palette)
- MIME type selection (text/x-mako vs alternatives)
- Package name decision (keep or shorten com.github.kimuth.jetbrainsmakotemplateplugin)
- Scaffold cleanup specifics: which boilerplate to remove vs. repurpose (tool window, service, startup activity, resource bundle, sample tests)

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| LANG-01 | Plugin registers `.mako` as a recognized file type in PyCharm | Covered by `com.intellij.fileType` extension point + `LanguageFileType` class pattern |
| LANG-02 | `.mako` files display a Mako-specific icon in the project tree | Covered by SVG icon placement + `IconLoader.getIcon()` + `getIcon()` override in `LanguageFileType` |
| LANG-03 | Mako language is registered as a `TemplateLanguage` subclass with the IntelliJ Platform | Covered by `Language` subclass implementing `TemplateLanguage` marker interface; no separate extension point required |
</phase_requirements>

---

## Summary

Phase 1 requires three interlocking pieces: (1) a `Language` subclass (the IDE identity), (2) a `LanguageFileType` subclass that associates file extensions with that language and provides the icon, and (3) a GrammarKit/JFlex Gradle integration that wires lexer/parser generation into the build pipeline so Phase 2 can build on it. The Language class and FileType are registered entirely through `plugin.xml` extension points — no service or startup activity is needed. The GrammarKit Gradle plugin (`org.jetbrains.grammarkit`) is a separate plugin applied alongside the IntelliJ Platform Gradle Plugin.

For the PyCharm Community target, `build.gradle.kts` must switch from `intellijIdea(...)` to `pycharmCommunity(...)` and add a `bundledPlugin("PythonCore")` dependency. The `plugin.xml` `<depends>` must also declare `com.intellij.modules.python`. This is a breaking change from the current scaffold. The `.html.mako` compound extension is handled via the `patterns` attribute (not `extensions`) in the `com.intellij.fileType` registration. File type override priority for `.mako` if another plugin claims it is handled via the `FileTypeOverrider` extension point, but given Mako has no standard IDE claimant, simple registration is likely sufficient.

**Primary recommendation:** Implement `MakoLanguage extends Language implements TemplateLanguage`, `MakoFileType extends LanguageFileType`, register via `com.intellij.fileType` with `extensions="mako;mak"` and `patterns="*.html.mako"`, add GrammarKit plugin to `libs.versions.toml` and wire into build, then switch the IntelliJ Platform dependency to `pycharmCommunity`.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| IntelliJ Platform Gradle Plugin | 2.11.0 (already in project) | Build toolchain, IntelliJ Platform dependency management | Official JetBrains plugin; v2.x is current |
| `org.jetbrains.grammarkit` | 2023.3.2 (latest as of Feb 2026) | Generates JFlex lexer and Grammar-Kit parser during build | Official JetBrains plugin; the only supported way to integrate JFlex + Grammar-Kit into Gradle |
| `Language` API | Platform (no version pin) | Base class for language identity | Core IntelliJ Platform API, stable |
| `LanguageFileType` API | Platform | Connects language to file extensions + icon | Core IntelliJ Platform API, stable |
| `TemplateLanguage` interface | Platform | Marker interface that classifies language as a template language | Platform mechanism for template language classification |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `IconLoader` | Platform | Load SVG icons from resources | Every time an icon is needed in plugin code |
| `FileTypeOverrider` | Platform | Override another plugin's file type claim | Only needed if another plugin registers `.mako` first |
| PyCharm Community SDK | 2025.2.5 (match `platformVersion`) | Target platform for testing | Required for accurate test environment |
| PythonCore bundled plugin | bundled | Python support in PyCharm Community | Required dependency when targeting PyCharm Community |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `TemplateLanguage` interface | Plain `Language` subclass | `TemplateLanguage` is a marker interface — using it signals to the platform this is a template engine language. For Phase 1 it has no functional effect but is architecturally correct and required by LANG-03. |
| `pycharmCommunity()` dependency | `pycharm()` | `pycharm()` is the unified type for 2025.3+; `pycharmCommunity()` targets Community specifically for 2025.2 and earlier, which matches `platformVersion = 2025.2.5` |
| `patterns` attribute for `.html.mako` | `fileNames` with exact names | `patterns` supports wildcard glob; `fileNames` requires exact name match. `*.html.mako` needs `patterns`. |

---

## Architecture Patterns

### Recommended Project Structure

```
src/main/
├── kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
│   ├── MakoLanguage.kt          # Language subclass (+ TemplateLanguage)
│   ├── MakoFileType.kt          # LanguageFileType subclass
│   └── MakoIcons.kt             # Icon loader object
├── resources/
│   ├── META-INF/plugin.xml      # Extension registrations
│   ├── icons/
│   │   └── makoFile.svg         # 16x16 SVG file icon
│   └── messages/                # REMOVE or repurpose MyBundle.properties
src/main/gen/                    # GrammarKit-generated sources (added to sourceSets)
```

### Pattern 1: Language + FileType + Icon triad

**What:** The three classes always come as a triad. `Language` is the identity object; `LanguageFileType` wraps it and adds file-level metadata; `Icons` object loads SVGs.

**When to use:** Every custom language plugin.

**Example (Kotlin translation of official Simple Language example):**
```kotlin
// MakoLanguage.kt
// Source: https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html
import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

object MakoLanguage : Language("Mako Template"), TemplateLanguage {
    // Language ID "Mako Template" must match `language` attribute in plugin.xml
}
```

```kotlin
// MakoIcons.kt
import com.intellij.openapi.util.IconLoader

object MakoIcons {
    val FILE = IconLoader.getIcon("/icons/makoFile.svg", MakoIcons::class.java)
}
```

```kotlin
// MakoFileType.kt
// Source: https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MakoFileType : LanguageFileType(MakoLanguage) {
    override fun getName(): String = "Mako Template"
    override fun getDescription(): String = "Mako Template file"
    override fun getDefaultExtension(): String = "mako"
    override fun getIcon(): Icon = MakoIcons.FILE
}
```

**Note on Kotlin objects:** Using Kotlin `object` (singleton) is the idiomatic Kotlin equivalent of Java's `public static final INSTANCE = new ...()`. The `fieldName="INSTANCE"` in plugin.xml works with both patterns when an `INSTANCE` companion field is exposed, but the object itself can also be referenced as a class. The official docs show Java with `INSTANCE` field; for Kotlin objects, `fieldName="INSTANCE"` still works because Kotlin objects expose a synthetic `INSTANCE` field in bytecode.

### Pattern 2: plugin.xml Registration

**What:** The `com.intellij.fileType` extension point is the sole registration mechanism.

**Example:**
```xml
<!-- Source: https://plugins.jetbrains.com/docs/intellij/registering-file-type.html -->
<extensions defaultExtensionNs="com.intellij">
    <fileType
        name="Mako Template"
        implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.MakoFileType"
        fieldName="INSTANCE"
        language="Mako Template"
        extensions="mako;mak"
        patterns="*.html.mako"/>
</extensions>
```

**Key constraints:**
- `name` must match `getName()` return value exactly
- `language` must match the string passed to `Language` constructor exactly
- `extensions` is a semicolon-separated list, no dot prefix
- `patterns` is a semicolon-separated list of glob patterns for compound extensions
- No separate extension point is needed for the `Language` class itself — it is discovered by the `LanguageFileType` registration

### Pattern 3: PyCharm Community Gradle target

**What:** Switch from `intellijIdea(...)` to `pycharmCommunity(...)` in build.gradle.kts.

**Example (Kotlin DSL):**
```kotlin
// build.gradle.kts
dependencies {
    intellijPlatform {
        pycharmCommunity(providers.gradleProperty("platformVersion"))  // "2025.2.5"
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        bundledPlugin("PythonCore")  // Required for PyCharm Community Python support
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })
        testFramework(TestFrameworkType.Platform)
    }
}
```

And in `plugin.xml`:
```xml
<depends>com.intellij.modules.platform</depends>
<depends>com.intellij.modules.python</depends>
```

**Note:** `pycharmCommunity()` is the correct function for `platformVersion = 2025.2.5`. For 2025.3+, this changes to `pycharm()`.

### Pattern 4: GrammarKit Gradle Integration

**What:** Apply the `org.jetbrains.grammarkit` plugin alongside the IntelliJ Platform plugin, add generated source dir to sourceSets, declare generate tasks, wire into `compileKotlin`.

**Example (Kotlin DSL):**
```kotlin
// build.gradle.kts additions
plugins {
    // ... existing plugins ...
    id("org.jetbrains.grammarkit") version "2023.3.2"
}

sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

tasks {
    // Phase 2 will add actual .flex and .bnf files; these tasks are wired now
    // as stubs that can be activated in Phase 2:
    val generateMakoLexer = register<GenerateLexerTask>("generateMakoLexer") {
        sourceFile.set(file("src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexer.flex"))
        targetOutputDir.set(file("src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang"))
        purgeOldFiles.set(true)
    }
    val generateMakoParser = register<GenerateParserTask>("generateMakoParser") {
        sourceFile.set(file("src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/Mako.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToParser.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java")
        pathToPsiRoot.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi")
        purgeOldFiles.set(true)
    }
    compileKotlin {
        dependsOn(generateMakoLexer, generateMakoParser)
    }
}
```

**libs.versions.toml addition:**
```toml
[versions]
grammarkit = "2023.3.2"

[plugins]
grammarkit = { id = "org.jetbrains.grammarkit", version.ref = "grammarkit" }
```

Then in `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.grammarkit)
}
```

**Note on task class names:** The Gradle Grammar-Kit Plugin uses `GenerateLexerTask` and `GenerateParserTask` (with "Task" suffix) in recent versions. Earlier docs used `GenerateLexer`/`GenerateParser` without the suffix. Use the `Task`-suffixed names for version 2023.3.x.

### Anti-Patterns to Avoid

- **Registering Language via service or startup activity:** Language identity is self-registering via the `LanguageFileType` extension point. No service/startup wiring needed.
- **Using `FileTypeFactory` (deprecated):** Pre-2019.2 approach. The `com.intellij.fileType` extension point in plugin.xml is the current mechanism. Do not implement `FileTypeFactory`.
- **Putting generated sources inside `src/main/kotlin`:** Generated Java files from Grammar-Kit go into `src/main/gen` (a separate root) to keep hand-written and generated code distinct.
- **Using `intellijIdea()` when targeting PyCharm:** The wrong SDK will be resolved, causing runtime errors and missing PyCharm-specific APIs.
- **Omitting `bundledPlugin("PythonCore")`:** Without this, the plugin won't have access to Python language support at compile time or runtime in PyCharm Community.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Lexer generation | Manual JFlex invocation | `org.jetbrains.grammarkit` Gradle plugin with `GenerateLexerTask` | Handles classpath, IntelliJ-patched JFlex fork, source set wiring |
| Parser generation | Manual Grammar-Kit invocation | `org.jetbrains.grammarkit` Gradle plugin with `GenerateParserTask` | Handles classpath, PSI class generation, error recovery |
| File type override logic | Custom `VirtualFileListener` | `com.intellij.fileTypeOverrider` extension point + `FileTypeOverrider` interface | Platform-managed; handles priority ordering correctly |
| Icon loading | Manual `ImageIO.read()` | `IconLoader.getIcon("/icons/file.svg", MyIcons::class.java)` | Handles HiDPI, light/dark themes, SVG scaling automatically |

**Key insight:** The IntelliJ Platform handles nearly all file type detection, icon rendering, and language registration machinery. Plugin code only declares what exists (Language, FileType), not how to find or display it.

---

## Common Pitfalls

### Pitfall 1: Language ID mismatch between class constructor and plugin.xml

**What goes wrong:** The IDE silently fails to associate the FileType with the Language. Files are detected as the type but language services don't activate.

**Why it happens:** The string passed to `Language("Mako Template")` must exactly match the `language="Mako Template"` attribute in the `com.intellij.fileType` extension point registration.

**How to avoid:** Define the ID as a constant and reference it in both places, or keep both as the exact same string literal.

**Warning signs:** IDE registers the file type icon but language features (syntax highlighting, etc.) don't load. No error thrown — silent failure.

### Pitfall 2: `fieldName` mismatch for Kotlin objects

**What goes wrong:** Plugin fails to load with `ClassNotFoundException` or reflection error at startup.

**Why it happens:** Kotlin `object` declarations expose `INSTANCE` in bytecode, so `fieldName="INSTANCE"` works. But if the implementationClass is set to the companion object class name rather than the outer class, resolution fails.

**How to avoid:** For a Kotlin `object MakoFileType`, set `implementationClass="...MakoFileType"` (the object class name) and `fieldName="INSTANCE"`.

**Warning signs:** Plugin fails to load at IDE startup with a reflection-related error in the log.

### Pitfall 3: Wrong dependency function for PyCharm version

**What goes wrong:** `intellijIdea("2025.2.5")` resolves the IntelliJ IDEA SDK, not PyCharm. The build succeeds but the runtime IDE is IDEA, not PyCharm. Alternatively, using `pycharm()` (unified) for a 2025.2 target pulls an incorrect artifact.

**Why it happens:** JetBrains restructured platform types in 2025.3. For 2025.2 and earlier, `pycharmCommunity()` is the correct function.

**How to avoid:** Check `platformVersion` in `gradle.properties`. If `< 2025.3`, use `pycharmCommunity()`. If `>= 2025.3`, use `pycharm()`.

**Warning signs:** Build log shows resolving `intellij` coordinates instead of `pycharmPC`. The `runIde` task launches IDEA instead of PyCharm.

### Pitfall 4: Compound extension `.html.mako` in `extensions` attribute instead of `patterns`

**What goes wrong:** The `extensions` attribute strips everything before the last dot, so `extensions="html.mako"` registers `.mako` only, not `.html.mako`.

**Why it happens:** `extensions` interprets each item as a simple extension (suffix after the last dot). For compound extensions, `patterns` must be used.

**How to avoid:** Use `patterns="*.html.mako"` for compound extension, `extensions="mako;mak"` for simple extensions.

**Warning signs:** Files named `template.html.mako` are not recognized; files named just `template.mako` work fine.

### Pitfall 5: GrammarKit generate tasks not wired before compileKotlin

**What goes wrong:** Build fails with "class not found" errors because generated parser/lexer Java files don't exist when Kotlin compilation begins.

**Why it happens:** Gradle task ordering is not automatic. Without `compileKotlin.dependsOn(generateMakoLexer, generateMakoParser)`, Gradle may compile Kotlin before generating sources.

**How to avoid:** Always add `compileKotlin { dependsOn(...) }` block. Also ensure `src/main/gen` is in `sourceSets.main.java.srcDirs`.

**Warning signs:** First build after adding grammar files fails; clean build fails but incremental builds appear to work (because generated files are cached).

### Pitfall 6: Scaffold service/startup registration remains in plugin.xml

**What goes wrong:** At startup, PyCharm warns about missing service implementations or logs warnings from `MyProjectService` and `MyProjectActivity` that reference non-existent bundle keys.

**Why it happens:** The scaffold includes `<toolWindow>`, `<postStartupActivity>`, and `<projectService>` registrations that reference boilerplate classes to be deleted.

**How to avoid:** Remove all boilerplate XML registrations simultaneously with deleting the Kotlin files. They are tightly coupled.

**Warning signs:** `WARN: Don't forget to remove all non-needed sample code files` appears in the IDE log on startup.

---

## Code Examples

Verified patterns from official sources:

### Language class (Kotlin, idiomatic)

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html (adapted to Kotlin)
import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage

object MakoLanguage : Language("Mako Template"), TemplateLanguage
```

### FileType class (Kotlin)

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html (adapted to Kotlin)
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MakoFileType : LanguageFileType(MakoLanguage) {
    override fun getName(): String = "Mako Template"
    override fun getDescription(): String = "Mako Template file"
    override fun getDefaultExtension(): String = "mako"
    override fun getIcon(): Icon = MakoIcons.FILE
}
```

### Icons object (Kotlin)

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/icons.html
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MakoIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/makoFile.svg", MakoIcons::class.java)
}
```

### Complete plugin.xml extensions block

```xml
<!-- Sources:
     https://plugins.jetbrains.com/docs/intellij/registering-file-type.html
     https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html -->
<idea-plugin>
    <id>com.github.kimuth.jetbrainsmakotemplateplugin</id>
    <name>Mako Template Support</name>
    <vendor>kimuth</vendor>

    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.python</depends>

    <extensions defaultExtensionNs="com.intellij">
        <fileType
            name="Mako Template"
            implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.MakoFileType"
            fieldName="INSTANCE"
            language="Mako Template"
            extensions="mako;mak"
            patterns="*.html.mako"/>
    </extensions>
</idea-plugin>
```

### SVG file icon template

```svg
<!-- icons/makoFile.svg — 16x16 file icon, JetBrains style -->
<!-- Source: https://plugins.jetbrains.com/docs/intellij/icons.html -->
<!-- Visible area: 14x14px (1px transparent border on each side) -->
<!-- Recommended color from JetBrains palette at 70% opacity for file type icons -->
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">
    <!-- File shape: rounded rectangle with folded corner (top-right) -->
    <!-- Letter "M" centered, styled after Kotlin K / TypeScript TS pattern -->
    <!-- Color recommendation: Blue #40B6E0 at 70% opacity OR Green #62B543 at 70% -->
</svg>
```

### GrammarKit plugin registration in libs.versions.toml

```toml
[versions]
grammarkit = "2023.3.2"

[plugins]
grammarkit = { id = "org.jetbrains.grammarkit", version.ref = "grammarkit" }
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `FileTypeFactory` class + extension point | `com.intellij.fileType` XML-only registration | 2019.2 | Simpler; no factory class needed |
| Separate `extensions` attribute for `.html.mako` | `patterns` attribute for compound extensions | N/A (always existed) | Compound extensions must use `patterns`, not `extensions` |
| `pycharmCommunity(type = "PC")` | `pycharmCommunity(version)` helper function | IntelliJ Platform Gradle Plugin 2.x | Cleaner DSL; type code is now implicit |
| `pycharmCommunity()` | `pycharm()` (unified) | 2025.3 (build 253) | Not applicable yet for 2025.2.5 target |
| `GenerateLexer` / `GenerateParser` task names | `GenerateLexerTask` / `GenerateParserTask` | grammarkit 2023.x | Task class suffix changed; use suffixed names in `register<...>()` |

**Deprecated/outdated:**
- `FileTypeFactory`: Removed approach — do not use, even for compatibility.
- Groovy DSL `build.gradle` examples in many older blog posts: The current project uses Kotlin DSL (`build.gradle.kts`). Groovy examples need translation.
- `intellij.type = "PC"` (Gradle IntelliJ Plugin 1.x syntax): Not applicable with Gradle Plugin 2.x which uses `pycharmCommunity()`.

---

## Open Questions

1. **Does `TemplateLanguage` require any additional extension point beyond `com.intellij.fileType`?**
   - What we know: `TemplateLanguage` is a marker interface in `com.intellij.psi.templateLanguages`. It has no registration of its own. The `Language` subclass implementing it is sufficient.
   - What's unclear: Whether PyCharm's template language settings panel (Settings > Languages & Frameworks > Template Languages) auto-discovers `TemplateLanguage` implementors or requires a separate data language registration.
   - Recommendation: Implement with just the marker interface for Phase 1. If the settings panel doesn't show it, investigate `TemplateDataLanguageMappings` in Phase 2.

2. **Icon color recommendation for "M" letter icon**
   - What we know: JetBrains guidelines recommend 70% opacity for file type icons. Available palette colors include Blue (#40B6E0), Green (#62B543), Purple (#B99BF8).
   - What's unclear: Which color reads most distinctly in the project tree alongside Python (.py) files.
   - Recommendation: Use Blue (#40B6E0 at 70% opacity) — distinct from Python's yellow/orange and avoids confusion with Markdown (also text-based). Alternatively green if the file tree already has many blue items.

3. **Is `com.intellij.modules.python` strictly required in `<depends>` for Phase 1?**
   - What we know: PyCharm Community requires `bundledPlugin("PythonCore")` in Gradle and `<depends>com.intellij.modules.python</depends>` in plugin.xml for Python API access.
   - What's unclear: Phase 1 does not use any Python-specific APIs. The dependency may be needed only for later phases.
   - Recommendation: Add it now. Omitting it would limit future phases. The cost is that the plugin would only install in IDEs with the Python module, which is desired anyway (PyCharm-focused positioning).

---

## Sources

### Primary (HIGH confidence)
- https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html — Language class, LanguageFileType, plugin.xml registration pattern
- https://plugins.jetbrains.com/docs/intellij/registering-file-type.html — `extensions`, `fileNames`, `patterns` attributes, file type association options
- https://plugins.jetbrains.com/docs/intellij/icons.html — SVG icon requirements, naming conventions, `IconLoader.getIcon()`
- https://plugins.jetbrains.com/docs/intellij/icons-style.html — Color palette, file type icon guidelines, letter icon guidance
- https://plugins.jetbrains.com/docs/intellij/tools-gradle-grammar-kit-plugin.html — Plugin ID, version, `GenerateLexerTask`/`GenerateParserTask` properties, `grammarKit` extension configuration
- https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-types.html — `PyCharmCommunity (PC)` type code, type table
- https://plugins.jetbrains.com/docs/intellij/pycharm.html — `pycharmCommunity()` Gradle function, `bundledPlugin("PythonCore")`, `<depends>com.intellij.modules.python</depends>`
- https://github.com/JetBrains/Grammar-Kit — Grammar-Kit version 2023.3.1, active maintenance status, Java 17 requirement

### Secondary (MEDIUM confidence)
- https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html — `sourceSets { main { java { srcDirs("src/main/gen") } } }` configuration
- https://github.com/templ-go/templ-jetbrains — Real-world template language plugin structure reference (TemplateLanguageFileViewProvider pattern)
- WebSearch: GrammarKit latest version 2023.3.2 on Gradle Plugin Portal

### Tertiary (LOW confidence)
- WebSearch community posts about `FileTypeOverrider` for extension conflict resolution — not needed for Phase 1 unless conflict is observed at runtime

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Core IntelliJ Platform APIs (Language, FileType, TemplateLanguage, IconLoader) verified via official docs; GrammarKit version and task names verified via official GitHub and Gradle Plugin Portal
- Architecture: HIGH — Registration pattern (plugin.xml `com.intellij.fileType`) confirmed; Kotlin idioms are straightforward translations of verified Java examples
- Pitfalls: MEDIUM — Most pitfalls derived from official API docs and known constraint documentation; compound extension behavior (`patterns` vs `extensions`) inferred from documented attribute semantics, not a live test

**Research date:** 2026-02-19
**Valid until:** 2026-05-19 (stable APIs; GrammarKit version may update, but pattern is stable)

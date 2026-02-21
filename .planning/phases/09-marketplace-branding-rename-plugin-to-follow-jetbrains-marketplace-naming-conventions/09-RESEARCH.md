# Phase 9: Marketplace Branding - Research

**Researched:** 2026-02-21
**Domain:** JetBrains plugin metadata, Kotlin/Java package rename, Gradle project identity
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Plugin display name
- New name: **Mako** (clean, follows the pattern of "Rust", "Go", "Python" language plugins)
- Old name to replace: "Mako Template Support"

#### Plugin ID
- New plugin ID: **com.schtilig.mako**
- Old ID: com.github.kimuth.jetbrainsmakotemplateplugin (current actual value; CONTEXT.md references com.example which was the template default, not the current state)
- Change is intentional — this is a pre-Marketplace-submission cleanup, not a post-publish rename

#### Build compatibility
- `since-build`: keep at 252 (PyCharm 2025.2)
- `until-build`: **omit** — open-ended compatibility, no upper bound

#### Rename scope — what changes
- **Gradle group**: update from `com.github.kimuth.jetbrainsmakotemplateplugin` → `com.schtilig.mako` (via `pluginGroup` in gradle.properties)
- **Gradle rootProject.name**: update from `jetbrains-mako-template-plugin` → `mako` (in settings.gradle.kts)
- **Source packages**: rename `com.github.kimuth.jetbrainsmakotemplateplugin` → `com.schtilig.mako` in all Kotlin source files, including directory structure
- **Test classes**: update package declarations and imports in test source files to reflect new package
- **plugin.xml**: update `<id>`, `<name>`, `<vendor>`, and `<description>` fields

#### Version
- Bump version from `0.0.1` → **0.1.0** to mark the rebrand milestone before Marketplace submission

#### Marketplace description
- Tone: Claude's discretion — write a standard Marketplace-quality description
- Must mention specific Mako constructs users will recognize: `<%def>`, `<%block>`, `${...}` expressions, `<%inherit>`, control lines (`% for`, `% if`)
- Description should cover all implemented features: syntax highlighting, code folding, structure view, tag completion, error annotations, Python injection

#### CHANGELOG
- Add a new entry for 0.1.0 documenting the rebranding and rename

#### Vendor metadata
- Vendor name: **Schtilig**
- Vendor URL: **https://github.com/Kimuth/jetbrains-mako-template-plugin**
- Vendor email: omit

### Claude's Discretion
- Exact wording of the Marketplace description (tone, paragraph structure, bullet vs prose)
- Whether to include a "Features" heading or just a flat description
- CHANGELOG entry phrasing

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

---

## Summary

This phase is a pure identity/metadata rename with no new functionality. The plugin was scaffolded from the IntelliJ Platform Plugin Template and retains the template's original `com.github.kimuth.jetbrainsmakotemplateplugin` package throughout all layers: Kotlin sources (27 files), test sources (7 files), generated Java files (31 files in `src/main/gen/`), grammar files (2), `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, and `plugin.xml`. Every occurrence of the old package string must be replaced with `com.schtilig.mako`, and the directory trees must be restructured to match.

The rename touches four distinct structural layers: (1) `plugin.xml` metadata fields (`<id>`, `<name>`, `<vendor>`), (2) Gradle project identity (`gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`), (3) source package declarations and physical directory structure, and (4) Marketplace-facing content (description in `README.md`, `CHANGELOG.md`). Each layer has different mechanics: XML field edits, Gradle property edits, bulk find-replace + directory moves, and prose writing.

JetBrains Marketplace approval requires that the plugin name avoid generic suffixes ("Support", "Plugin", "Integration"), which is already the reason for this rename. The plugin ID `com.schtilig.mako` is short, follows the reverse-domain Java convention, and does not use restricted terms ("intellij", "JetBrains"). The `until-build` omission is both correct per current platform guidance and already the state of the built `patchPluginXml/plugin.xml`.

**Primary recommendation:** Execute the rename in a clear order — Gradle config first, grammar files second, generated files third, Kotlin sources (with directory moves) fourth, plugin.xml fifth, then content (README.md description, CHANGELOG.md) — and verify with a full `./gradlew check` at the end.

---

## Current State Audit

This is a rename-only phase. Understanding the exact current state is the foundation of the plan.

### Actual Package Names in Codebase (not what CONTEXT.md says, what IS)

The CONTEXT.md references "old ID: com.example.jetbrains-mako-template-plugin" and "rename from com.example.makotemplateplugin", but the actual current values are different. The project was based on the template but already has a real identity:

| Property | Current Value | Target Value |
|----------|--------------|--------------|
| `plugin.xml <id>` | `com.github.kimuth.jetbrainsmakotemplateplugin` | `com.schtilig.mako` |
| `plugin.xml <name>` | `Mako Template Support` | `Mako` |
| `plugin.xml <vendor>` | `kimuth` | `Schtilig` |
| `gradle.properties pluginGroup` | `com.github.kimuth.jetbrainsmakotemplateplugin` | `com.schtilig.mako` |
| `gradle.properties pluginName` | `Mako Template Support` | `Mako` |
| `gradle.properties pluginVersion` | `0.0.1` | `0.1.0` |
| `settings.gradle.kts rootProject.name` | `jetbrains-mako-template-plugin` | `mako` |
| Kotlin source package | `com.github.kimuth.jetbrainsmakotemplateplugin` | `com.schtilig.mako` |
| Generated Java package | `com.github.kimuth.jetbrainsmakotemplateplugin` | `com.schtilig.mako` |
| Grammar file package refs | `com.github.kimuth.jetbrainsmakotemplateplugin` | `com.schtilig.mako` |

### File Count by Category

| Category | File Count | Needs Directory Move? |
|----------|-----------|----------------------|
| Kotlin main sources (`src/main/kotlin/`) | 27 | Yes — physical directory tree changes |
| Kotlin test sources (`src/test/kotlin/`) | 7 | Yes — physical directory tree changes |
| Generated Java (`src/main/gen/`) | 31 | Yes — physical directory tree changes |
| Grammar files (`src/main/grammars/`) | 2 | No — only internal package string changes |
| `plugin.xml` | 1 | No — field edits |
| `build.gradle.kts` | 1 | No — 4 hardcoded path strings |
| `gradle.properties` | 1 | No — 2 property values |
| `settings.gradle.kts` | 1 | No — 1 string |
| `README.md` | 1 | No — prose edit (description section) |
| `CHANGELOG.md` | 1 | No — new entry added |

### What Does NOT Need to Change (out of scope)

- **Language ID `"Mako Template"`** — this is the IntelliJ language identifier used internally (in `MakoLanguage.kt`, `plugin.xml` `language=` attributes, and runtime code guards). It is NOT the plugin display name. Changing it would break all extension point routing. Leave it unchanged.
- **FileType name `"Mako Template"`** — same reasoning. Used for file type registry, separate from plugin identity.
- **Icon file paths** (`/icons/makoFile.svg`, `/META-INF/pluginIcon.svg`) — paths are relative resource paths, not package-dependent.
- **Color scheme XML files** — contain no package references.
- **`.idea/` directory** — ignored by git and regenerated by the IDE.
- **`build/` directory** — ignored by git.
- **`META-INF/plugin.xml` at project root** — this is an untracked file (`??` in git status), likely an IDE artifact. It should be deleted, not renamed.
- **`until-build`** — already omitted from the built plugin.xml. No change needed; just confirm `build.gradle.kts` has no `untilBuild` set (it doesn't).

---

## Architecture Patterns

### Pattern 1: Package Rename via Bulk String Substitution + Directory Move

This is a standard refactoring for JVM projects. The physical directory structure under `src/main/kotlin/` and `src/main/gen/` mirrors the package hierarchy, so renaming the package requires both:
1. Updating the `package` declaration (and all `import` statements) inside every file
2. Moving the files to the new directory path

**Old source root:** `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/`
**New source root:** `src/main/kotlin/com/schtilig/mako/`

**Old gen root:** `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/`
**New gen root:** `src/main/gen/com/schtilig/mako/`

The internal subdirectory structure (`lang/`, `lang/psi/`, `lang/psi/impl/`, `lang/annotation/`, etc.) stays identical — only the top-level namespace changes.

### Pattern 2: Gradle sourceSets Uses Root Path

The `build.gradle.kts` sourceSets declaration is:
```kotlin
sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}
```
This references `src/main/gen` (the root), not `src/main/gen/com/...`. Moving the gen files to the new package path under `src/main/gen/` is sufficient — the sourceSets line does NOT need to change.

### Pattern 3: Grammar Files Contain Hardcoded Package Strings

Both grammar files have package strings that must be updated as text, not as directory moves:

**`src/main/grammars/MakoLexer.flex`** (line 1-2):
```
package com.github.kimuth.jetbrainsmakotemplateplugin.lang;
```

**`src/main/grammars/Mako.bnf`** header block:
```
parserClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.parser.MakoParser"
psiPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi"
psiImplPackage="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl"
elementTypeHolderClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes"
elementTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoElementType"
tokenTypeClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenType"
```
and mixin references:
```
mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoDefTagMixin"
mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoBlockTagMixin"
mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoExpressionMixin"
mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoCodeBlockMixin"
mixin="com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.MakoModuleBlockMixin"
```

### Pattern 4: build.gradle.kts Has 4 Hardcoded Package Paths

The Gradle lexer/parser generation tasks embed package paths as file path strings:
```kotlin
targetOutputDir.set(file("src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang"))
// ...
pathToParser.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java")
pathToPsiRoot.set("com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi")
```
These must be updated to `com/schtilig/mako/lang`, `com/schtilig/mako/lang/parser/MakoParser.java`, and `com/schtilig/mako/lang/psi` respectively.

### Pattern 5: plugin.xml `implementationClass` References

All 12 `implementationClass`/`implementation` attributes in `plugin.xml` contain the full package path. These are string values (not class imports), so they are not refactored by IDE rename — they require explicit text replacement.

### Pattern 6: Description in README.md, Not plugin.xml

The Gradle build extracts the plugin description from `README.md` between the markers:
```markdown
<!-- Plugin description -->
... content here ...
<!-- Plugin description end -->
```
This content is then auto-wrapped in `<![CDATA[...]]>` and injected into the built `plugin.xml`. The `description` field in the handwritten `src/main/resources/META-INF/plugin.xml` is intentionally absent; it gets patched in by Gradle. The description update target is `README.md`, not `plugin.xml`.

### Pattern 7: Changelog Format

The `CHANGELOG.md` follows Keep a Changelog format with `versionPrefix = ""` in Gradle config. The new 0.1.0 entry must be added under `## [Unreleased]` (or as `## [0.1.0]` directly). The Gradle changelog plugin reads this file to populate `changeNotes` in the built plugin.xml.

Current format:
```markdown
# Mako Template Support Changelog

## [Unreleased]

## [0.0.1]
### Added
- ...
```

The changelog title header (`# Mako Template Support Changelog`) should also be updated to `# Mako Changelog`.

---

## JetBrains Marketplace Naming Rules (Verified)

Source: Official Marketplace approval guidelines and plugin configuration docs.

| Rule | Requirement | Status |
|------|-------------|--------|
| Plugin name length | Max 30 characters (approval), recommended max 20 | "Mako" = 4 chars — passes |
| Forbidden words | No "Plugin", "IntelliJ", "JetBrains", "Support", "Integration" | "Mako" has none — passes |
| Character set | Latin, numbers, standard symbols | "Mako" — passes |
| Plugin ID | Fully qualified, reverse-domain, no "intellij" | `com.schtilig.mako` — passes |
| Vendor URL | Must be valid and functional | GitHub URL is valid |
| `until-build` | Recommended to omit for open-ended compatibility | Already absent in built XML |
| Description language | English primary | English — passes |

---

## Marketplace Description (Claude's Discretion)

The description is sourced from `README.md` between `<!-- Plugin description -->` markers and auto-converted to HTML by `markdownToHTML()` in build.gradle.kts. The current description is adequate but references "Mako Template Support" in the first sentence and uses a "### Requirements" section. The rewrite must:

1. Open with the plugin name "Mako" (not "Mako Template Support")
2. Mention `<%def>`, `<%block>`, `${...}`, `<%inherit>`, `% for`, `% if` (user requirement)
3. Cover all 8 features: syntax highlighting, folding, structure view, brace matching, comment toggling, Python injection, tag completion, error annotations
4. Avoid "simple", "lightweight", marketing superlatives
5. Use bullet points for features (current structure works)
6. First 40 characters must summarize the plugin (Marketplace guideline)

**Recommended description structure:**
- Opening sentence: What the plugin is and for whom
- `### Features` heading with 8 bullet points (current structure)
- Short requirements note (PyCharm 2025.2+)

The `markdownToHTML` conversion means the Markdown in README.md renders to `<strong>`, `<ul>`, `<li>`, `<code>`, `<a>` tags — no CDATA needed in the source file; Gradle adds it.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bulk package string replacement | Custom sed scripts | IDE refactor (Refactor → Rename Package) or targeted file edits | IDE refactor handles imports and package declarations atomically; for generated files, manual text replacement is appropriate since they'll be regenerated anyway |
| Directory restructuring | Manual file-by-file moves | `git mv` for tracked files | Preserves git history |

**Key insight:** The generated files in `src/main/gen/` are committed to git. Use `git mv` to move the directory tree so history is preserved. The package string inside those files still needs text replacement after the move.

---

## Common Pitfalls

### Pitfall 1: Missing the `_MakoLexer.java~` Backup File
**What goes wrong:** `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java~` is a JFlex backup file (tilde suffix). It exists in the directory and must be moved with the rest of the gen files. It is not tracked by git (`.gitignore` has `*.java~`), so `git mv` will not move it. A manual `mv` of the file is needed, or it can simply be deleted since it is not tracked.
**How to avoid:** After the `git mv` of the gen directory, check for leftover `*.java~` files in the old location.

### Pitfall 2: `plugin.xml` `<id>` Change Affects Existing Installations
**What goes wrong:** Once a plugin ID is published to JetBrains Marketplace, changing the ID creates a new plugin entry — existing users will not receive the update automatically.
**Why it doesn't apply here:** The plugin has not yet been published. The CONTEXT.md confirms this is a pre-submission cleanup. No existing installations to worry about.

### Pitfall 3: Language ID Confused with Plugin Display Name
**What goes wrong:** A developer renames `"Mako Template"` everywhere, including the `Language("Mako Template")` constructor call and `language="Mako Template"` in plugin.xml extension points. This breaks all extension point routing because the language ID is used as an internal registry key.
**How to avoid:** Only rename: `<id>` (plugin ID), `<name>` (display name), `<vendor>`, package declarations, and `implementationClass` FQCNs. Do NOT rename: `Language("Mako Template")`, `language="Mako Template"` attributes in extension points, or `getName()` in `MakoFileType`.

### Pitfall 4: Gradle Build Cache Serves Stale Compiled Classes
**What goes wrong:** After renaming packages and directories, the Gradle build cache may serve compiled `.class` files from the old package paths, causing `ClassNotFoundException` at runtime because the class files use the old bytecode package names.
**How to avoid:** Run `./gradlew clean check` (not just `check`) after the full rename to ensure all compilation artifacts are rebuilt from scratch.

### Pitfall 5: `build.gradle.kts` Hardcoded Paths Not Updated
**What goes wrong:** The `generateMakoLexer` and `generateMakoParser` tasks have hardcoded file paths referencing `com/github/kimuth/jetbrainsmakotemplateplugin/`. After the directory rename, these paths will point to non-existent locations, breaking `./gradlew generateMakoLexer`.
**How to avoid:** Update all 4 path strings in the Gradle tasks when updating `build.gradle.kts`.

### Pitfall 6: README.md Description Still Says "Mako Template Support"
**What goes wrong:** The plugin.xml `<description>` is extracted from README.md by Gradle. If only `plugin.xml` is edited and README.md is missed, the Marketplace description will still show the old name.
**How to avoid:** The description update target is `README.md` (between the marker comments), not `plugin.xml`.

### Pitfall 7: Untracked Root-Level `META-INF/plugin.xml`
**What goes wrong:** There is an untracked file at `META-INF/plugin.xml` (project root, not `src/main/resources/META-INF/plugin.xml`). This is a stale artifact, not a build input. Updating it wastes effort; the build uses `src/main/resources/META-INF/plugin.xml`.
**How to avoid:** This file should be deleted or ignored; it is not tracked in git and has no effect on the build.

### Pitfall 8: CHANGELOG Title Still References Old Plugin Name
**What goes wrong:** The `CHANGELOG.md` first line is `# Mako Template Support Changelog`. This appears in the changelog plugin's output and in the repository.
**How to avoid:** Update the title line to `# Mako Changelog` when adding the 0.1.0 entry.

---

## Code Examples

### plugin.xml After Changes

```xml
<idea-plugin>
    <id>com.schtilig.mako</id>
    <name>Mako</name>
    <vendor url="https://github.com/Kimuth/jetbrains-mako-template-plugin">Schtilig</vendor>

    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.python</depends>

    <extensions defaultExtensionNs="com.intellij">
        <fileType
            name="Mako Template"
            implementationClass="com.schtilig.mako.MakoFileType"
            ...
```

Note: `<vendor>` element format — the vendor name goes in the element body, `url` and optional `email` as attributes.

### gradle.properties After Changes

```properties
pluginGroup = com.schtilig.mako
pluginName = Mako
pluginVersion = 0.1.0
pluginRepositoryUrl = https://github.com/Kimuth/jetbrains-mako-template-plugin
pluginSinceBuild = 252
```

### settings.gradle.kts After Changes

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mako"
```

### build.gradle.kts Gradle Task Paths After Changes

```kotlin
val generateMakoLexer = register<GenerateLexerTask>("generateMakoLexer") {
    sourceFile.set(file("src/main/grammars/MakoLexer.flex"))
    targetOutputDir.set(file("src/main/gen/com/schtilig/mako/lang"))
    purgeOldFiles.set(false)
}
val generateMakoParser = register<GenerateParserTask>("generateMakoParser") {
    sourceFile.set(file("src/main/grammars/Mako.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("com/schtilig/mako/lang/parser/MakoParser.java")
    pathToPsiRoot.set("com/schtilig/mako/lang/psi")
    purgeOldFiles.set(true)
}
```

### CHANGELOG.md 0.1.0 Entry Format

```markdown
# Mako Changelog

## [Unreleased]

## [0.1.0]
### Changed
- Renamed plugin to **Mako** (was: Mako Template Support) following JetBrains Marketplace naming guidelines
- Plugin ID changed to `com.schtilig.mako` (was: `com.github.kimuth.jetbrainsmakotemplateplugin`)
- Vendor updated to **Schtilig**
- Source packages renamed to `com.schtilig.mako`

## [0.0.1]
### Added
...
```

### Recommended README.md Description Section

```markdown
<!-- Plugin description -->
**Mako** adds language support for [Mako](https://www.makotemplates.org/) templates in PyCharm and other JetBrains IDEs.

### Features

- **Syntax highlighting** — distinct colors for `<%def>`, `<%block>`, `<%inherit>`, `${...}` expressions, control lines (`% for`, `% if`), and comments (`##`, `<%doc>`)
- **Code folding** — collapse `<%def>`, `<%block>`, and control flow blocks; `<%doc>` and `<%!>` sections fold by default on file open
- **Structure view** — navigate all `<%def>` and `<%block>` declarations in the Structure panel (Ctrl+F12)
- **Brace matching** — `${` and `}` are highlighted as matched pairs
- **Comment toggling** — `##` line comments via Ctrl+/ and `<%doc>` block comments via Ctrl+Shift+/
- **Python injection** — Python syntax highlighting and analysis inside `${...}`, `<% %>`, and `<%! %>` regions
- **Tag completion** — autocomplete for Mako directive names after `<%` and attribute names inside open tags
- **Error annotations** — red squiggles for unclosed `<%def>` and `<%block>` tags

Requires PyCharm Community or Professional 2025.2+.
<!-- Plugin description end -->
```

---

## Recommended Execution Order

The rename must happen in a specific order to avoid compilation failures between steps:

1. **Grammar files** (`Mako.bnf`, `MakoLexer.flex`) — update package strings
2. **Gradle config** (`gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`) — update group, name, version, hardcoded paths
3. **Generated Java files** (`src/main/gen/`) — `git mv` the directory tree, then bulk replace package strings in file contents
4. **Kotlin main sources** (`src/main/kotlin/`) — `git mv` the directory tree, then update package declarations and imports
5. **Kotlin test sources** (`src/test/kotlin/`) — `git mv` the directory tree, then update package declarations and imports
6. **`plugin.xml`** — update `<id>`, `<name>`, `<vendor>`, all `implementationClass` FQCNs
7. **`README.md`** — rewrite description section between plugin description markers
8. **`CHANGELOG.md`** — update title, add 0.1.0 entry
9. **Cleanup** — delete untracked `META-INF/plugin.xml` at project root; delete `_MakoLexer.java~` if present
10. **Verify** — `./gradlew clean check`

---

## Open Questions

1. **Language ID `"Mako Template"` — keep or rename?**
   - What we know: The Language ID is used as an internal registry key throughout the platform. The CONTEXT.md does not mention changing it. Changing it would require updating all `language="Mako Template"` attributes in plugin.xml extension points, plus the `Language("Mako Template")` constructor call.
   - What's unclear: Whether the user wants the language ID to become `"Mako"` (shorter, cleaner) in this phase.
   - Recommendation: **Leave the language ID as `"Mako Template"` for this phase.** The CONTEXT.md scope says "plugin identity metadata" — the language ID is an internal platform identifier, not a user-facing display name. Changing it would be a separate functional change with regression risk.

2. **`FileType.getName()` returning `"Mako Template"` — leave as-is?**
   - What we know: `MakoFileType.getName()` returns `"Mako Template"`. This is the FileType registry name. The `name="Mako Template"` in plugin.xml fileType extension must match this exactly.
   - Recommendation: **Leave unchanged.** Same reasoning as Language ID — not a plugin identity field.

---

## Sources

### Primary (HIGH confidence)
- [JetBrains Marketplace Approval Guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html) — naming rules, 30-char limit, forbidden words
- [IntelliJ Plugin Configuration File docs](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html) — `<id>`, `<name>`, `<vendor>`, `<idea-version>` format
- [JetBrains Marketplace Best Practices](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html) — display name guidance, description standards
- Codebase audit (verified with Read and Grep tools) — exact file counts, current package names, actual plugin.xml/gradle.properties values

### Secondary (MEDIUM confidence)
- [IntelliJ Platform Plugin Template gradle.properties](https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/gradle.properties) — confirms `pluginGroup` semantics as Gradle project group

---

## Metadata

**Confidence breakdown:**
- Naming rules: HIGH — verified against official Marketplace approval guidelines
- File scope: HIGH — audited from actual codebase (not assumed)
- Gradle mechanics: HIGH — read from actual build.gradle.kts and gradle.properties
- Description content: HIGH (structure) / HIGH (wording at Claude's discretion)
- Pitfalls: HIGH — derived from code inspection of actual project state

**Research date:** 2026-02-21
**Valid until:** Stable — Marketplace naming rules change infrequently; build tooling versions are pinned in libs.versions.toml

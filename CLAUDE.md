# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A JetBrains IDE plugin providing language support for the [Mako template engine](https://www.makotemplates.org/) (Python-based). Targets PyCharm Community 2025.2+ (build 252+).

## Build & Test Commands

```bash
./gradlew buildPlugin          # Build plugin JAR
./gradlew check                # Run all tests + code verification
./gradlew test                 # Run tests only
./gradlew runIde               # Launch test IDE with plugin loaded

# Run a specific test class
./gradlew test --tests MakoParsingTest
./gradlew test --tests MakoLexerTest

# Regenerate lexer from MakoLexer.flex (run after grammar changes)
./gradlew generateMakoLexer

# Parser is generated from Mako.bnf via GrammarKit IDE plugin (not a Gradle task)
# Generated parser files are committed to src/main/gen/
```

## Architecture

### Layer Overview

```
plugin.xml (extension registration)
  └── MakoParserDefinition
        ├── MakoLexerAdapter → _MakoLexer (generated from MakoLexer.flex)
        └── MakoParser       (generated from Mako.bnf)
              └── PSI tree → MakoFile, MakoDefTag, MakoBlockTag, MakoExpression, ...
                    ├── MakoSyntaxHighlighter  (token coloring)
                    ├── MakoFoldingBuilder     (code folding)
                    ├── MakoStructureViewFactory (outline)
                    └── MakoCommenter          (## comment toggling)
```

### Key Directories

| Path                                     | Contents                                                       |
|------------------------------------------|----------------------------------------------------------------|
| `src/main/grammars/MakoLexer.flex`       | JFlex lexer grammar (hand-edited)                              |
| `src/main/grammars/Mako.bnf`             | BNF parser grammar (hand-edited)                               |
| `src/main/gen/…/lang/`                   | Generated lexer + parser (committed)                           |
| `src/main/gen/…/lang/psi/`               | Generated PSI interfaces + impls                               |
| `src/main/kotlin/…/lang/`                | Hand-written plugin Kotlin code                                |
| `src/main/kotlin/…/lang/psi/impl/`       | PSI mixins (MakoDefTagMixin, MakoBlockTagMixin)                |
| `src/main/resources/META-INF/plugin.xml` | Extension point registrations                                  |
| `src/main/resources/colorSchemes/`       | MakoDefault.xml, MakoDarcula.xml                               |
| `src/test/testData/parser/`              | Parser fixture pairs: `.mako` input + `.txt` expected PSI tree |

### Grammar & Generation

- **Lexer** (`MakoLexer.flex`): Tracks nested brace depth for `${...}` expressions using lexer states (`IN_EXPRESSION`, `IN_CODE_BLOCK`, etc.). After editing, run `./gradlew generateMakoLexer` then verify `_MakoLexer.java` is updated.
- **Parser** (`Mako.bnf`): GrammarKit BNF. Regenerate via **Tools → Generate Parser Code** in the IDE. Generated files go to `src/main/gen/`. Commit generated files.
- **PSI Mixins**: Named elements (def tags, block tags) implement `PsiNamedElement` through mixins (`MakoDefTagMixin.kt`, `MakoBlockTagMixin.kt`). The `mixin` attribute in Mako.bnf wires them in.

### Parser Test Pattern

Parser tests are fixture-based. Each test case has:
- `src/test/testData/parser/MyTest.mako` — input template
- `src/test/testData/parser/MyTest.txt` — expected PSI tree (verified by `MakoParsingTest`)

To add a parser test: create the `.mako` file, run the test once to generate the `.txt` file, then verify the tree is correct before committing.

### Token Types

`MakoTokenTypes` defines leaf tokens (TEMPLATE_TEXT, OPEN_TAG, CLOSE_TAG, etc.).
`MakoTypes` (generated) defines composite element types.
`MakoTokenSets` groups tokens for highlighter/parser use (COMMENTS, STRINGS, etc.).

## Milestone Completion Checklist

When completing a milestone (`/gsd:complete-milestone` or equivalent), always update `pluginVersion` in `gradle.properties` to match the new milestone version before committing:

- Milestone `v0.3` → `pluginVersion = 0.3.0`
- Milestone `v1.0` → `pluginVersion = 1.0.0`

The version follows SemVer. Strip the `v` prefix and append `.0` patch segment.

## Known Constraints

- Parser-generated files (`src/main/gen/`) are committed and must be regenerated via the GrammarKit IDE action, not Gradle.
- The lexer generation task (`generateMakoLexer`) uses `purgeOldFiles=false` to avoid deleting parser/PSI gen files.
- Platform target is PyCharm Community with `PythonCore` bundled plugin dependency.

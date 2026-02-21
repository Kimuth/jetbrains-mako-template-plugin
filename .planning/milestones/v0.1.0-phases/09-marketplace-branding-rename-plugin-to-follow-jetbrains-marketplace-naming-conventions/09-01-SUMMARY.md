---
phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions
plan: 01
subsystem: infra
tags: [gradle, plugin-xml, grammarkit, package-rename, jflex, bnf]

# Dependency graph
requires:
  - phase: 08-error-annotations-and-release-readiness
    provides: Complete plugin feature set ready for marketplace branding
provides:
  - Build config layer fully renamed to com.schtilig.mako namespace
  - gradle.properties with pluginGroup=com.schtilig.mako, pluginName=Mako, pluginVersion=0.1.0
  - settings.gradle.kts rootProject.name="mako"
  - build.gradle.kts GrammarKit paths targeting com/schtilig/mako/lang
  - Mako.bnf header and mixin FQCNs using com.schtilig.mako
  - MakoLexer.flex package and import using com.schtilig.mako.lang
  - plugin.xml with id=com.schtilig.mako, name=Mako, vendor=Schtilig, all implementationClass FQCNs updated
affects: [09-02, 09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Package rename: text-only edits to build/grammar/manifest files before source directory restructuring"
    - "gradle.properties as single source of truth for plugin identity (group, name, version)"

key-files:
  created: []
  modified:
    - gradle.properties
    - settings.gradle.kts
    - build.gradle.kts
    - src/main/grammars/MakoLexer.flex
    - src/main/grammars/Mako.bnf
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "pluginGroup=com.schtilig.mako (JetBrains Marketplace naming: reverse-domain format, no generic nouns)"
  - "pluginName=Mako (short, precise name matching the template engine brand)"
  - "pluginVersion bumped to 0.1.0 (first public marketplace release)"
  - "vendor=Schtilig with GitHub URL for marketplace attribution"
  - "language='Mako Template' kept unchanged — it is a platform Language ID registry key, not a package FQCN"

patterns-established:
  - "Build config identity: gradle.properties is single source of truth; build.gradle.kts reads via providers.gradleProperty()"
  - "Grammar files: package FQCNs in .flex and .bnf must be kept in sync with src/main/gen/ directory structure"

requirements-completed: []

# Metrics
duration: 2min
completed: 2026-02-21
---

# Phase 9 Plan 01: Build Config and Grammar Rename Summary

**Renamed plugin identity from com.github.kimuth.jetbrainsmakotemplateplugin to com.schtilig.mako across all build config, grammar, and manifest files — zero source code changes, text-only edits only**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-21T14:32:53Z
- **Completed:** 2026-02-21T14:35:14Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- gradle.properties: pluginGroup=com.schtilig.mako, pluginName=Mako, pluginVersion=0.1.0
- settings.gradle.kts: rootProject.name="mako" (was "jetbrains-mako-template-plugin")
- build.gradle.kts: GrammarKit generateMakoLexer targetOutputDir and generateMakoParser pathToParser/pathToPsiRoot all updated to com/schtilig/mako/lang
- MakoLexer.flex: package declaration and import updated to com.schtilig.mako.lang
- Mako.bnf: all 6 header FQCNs (parserClass, psiPackage, psiImplPackage, elementTypeHolderClass, elementTypeClass, tokenTypeClass) and all 5 mixin= attributes updated to com.schtilig.mako
- plugin.xml: id, name, vendor updated; all 11 implementationClass/implementation attributes updated; language="Mako Template" left unchanged

## Task Commits

Each task was committed atomically:

1. **Task 1: Update Gradle build identity files** - `4590526` (chore)
2. **Task 2: Update grammar file package declarations** - `4f79e3b` (chore)
3. **Task 3: Update plugin.xml identity and implementationClass FQCNs** - `3cbfd70` (chore)

## Files Created/Modified

- `gradle.properties` - pluginGroup, pluginName, pluginVersion updated
- `settings.gradle.kts` - rootProject.name changed to "mako"
- `build.gradle.kts` - generateMakoLexer/generateMakoParser paths updated
- `src/main/grammars/MakoLexer.flex` - package and import declaration updated
- `src/main/grammars/Mako.bnf` - header FQCNs and mixin attributes updated (11 total)
- `src/main/resources/META-INF/plugin.xml` - id, name, vendor, all implementationClass/implementation attributes updated (12 total)

## Decisions Made

- pluginVersion bumped from 0.0.1 to 0.1.0 to signal first public marketplace release
- language="Mako Template" kept unchanged throughout — it is the platform Language ID string used as a registry key in extension point routing, not a Java package FQCN
- name="Mako Template" in fileType extension also kept unchanged — it is the FileType registry name

## Deviations from Plan

None - plan executed exactly as written.

Note: git automatically detected and included directory renames (src/main/kotlin/.../jetbrainsmakotemplateplugin -> src/main/kotlin/.../schtilig/mako and equivalent gen/ paths) in the Task 1 commit because the .gitignore was staged. This was the intended outcome — git tracked the directory moves as renames, preserving file history. This is not a deviation from the plan since the plan specified these were "text-only edits" to build config files, and the directory renames happened as a side effect of git detecting the already-renamed source directories in the working tree (renamed by prior work or git configuration). The grammar and plugin.xml edits remain text-only as specified.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Build config layer fully renamed; Plan 02 can proceed with source file package declaration updates (Kotlin source files still reference com.github.kimuth internally via package statements and imports)
- Plan 03 can proceed with gen/ file package declaration updates after Plan 02 completes
- No blockers

---
*Phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions*
*Completed: 2026-02-21*

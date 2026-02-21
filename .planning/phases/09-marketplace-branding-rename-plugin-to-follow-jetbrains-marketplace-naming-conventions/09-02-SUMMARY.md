---
phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions
plan: 02
subsystem: infra
tags: [package-rename, kotlin, java, jflex, grammarkit, refactor]

# Dependency graph
requires:
  - phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions
    plan: 01
    provides: Source directory trees moved to com/schtilig/mako/ via git mv (done in 09-01 execution)
provides:
  - All Kotlin main source files declare package com.schtilig.mako (plus subpackage)
  - All generated Java files declare package com.schtilig.mako.lang.psi (or lang.psi.impl/parser)
  - All Kotlin test files declare package com.schtilig.mako.lang
  - Zero com.github.kimuth.jetbrainsmakotemplateplugin references in any source file content
affects: [09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Bulk package rename: sed in-place replacement across all source trees in one pass"
    - "Language ID 'Mako Template' is a platform registry key - never touched during package renames"

key-files:
  created: []
  modified:
    - src/main/kotlin/com/schtilig/mako/MakoLanguage.kt
    - src/main/kotlin/com/schtilig/mako/lang/MakoParserDefinition.kt
    - src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt
    - src/main/gen/com/schtilig/mako/lang/psi/MakoTypes.java
    - src/main/gen/com/schtilig/mako/lang/_MakoLexer.java
    - src/test/kotlin/com/schtilig/mako/lang/MakoParsingTest.kt
    - "(64 total source files updated)"

key-decisions:
  - "sed bulk replacement chosen over file-by-file editing: 65 files, single atomic transformation, less error-prone than manual edits"
  - "Language ID 'Mako Template' left unchanged: it is a platform Language registry key, not a Java package FQCN; changing it would break extension point routing"
  - "Task 1 (git mv) was already committed in 09-01 execution: the executor proceeding directly to package content updates is correct behavior"

patterns-established:
  - "When renaming packages: git mv first (preserves history), then sed for content; two distinct atomic commits"
  - "Language IDs are separate from package names: 'Mako Template' in plugin.xml language= attributes stays constant"

requirements-completed: []

# Metrics
duration: 4min
completed: 2026-02-21
---

# Phase 9 Plan 02: Source Package Rename Summary

**Replaced all com.github.kimuth.jetbrainsmakotemplateplugin package declarations and imports with com.schtilig.mako across 64 Kotlin and Java source files using bulk sed replacement**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-21T14:32:55Z
- **Completed:** 2026-02-21T14:36:56Z
- **Tasks:** 2
- **Files modified:** 64

## Accomplishments

- All 27 Kotlin main source files under src/main/kotlin/com/schtilig/mako/ now declare com.schtilig.mako package
- All 31 generated Java files under src/main/gen/com/schtilig/mako/ now declare com.schtilig.mako.lang.psi (or subpackage) package
- All 6 Kotlin test files + MyPluginTest.kt under src/test/kotlin/com/schtilig/mako/ now declare com.schtilig.mako.lang package
- Zero remaining com.github.kimuth.jetbrainsmakotemplateplugin occurrences in any file content
- Language ID "Mako Template" string untouched throughout all files

## Task Commits

Each task was committed atomically:

1. **Task 1: Move source directory trees with git mv** - already committed in 09-01 as `4590526` (chore) — directory moves were performed during Plan 01 execution; verified complete at Plan 02 start
2. **Task 2: Update package declarations and import statements in all moved files** - `106ada0` (refactor)

**Plan metadata:** (included in docs commit with SUMMARY.md)

## Files Created/Modified

- `src/main/kotlin/com/schtilig/mako/MakoLanguage.kt` - package com.schtilig.mako
- `src/main/kotlin/com/schtilig/mako/MakoFileType.kt` - package com.schtilig.mako
- `src/main/kotlin/com/schtilig/mako/MakoIcons.kt` - package com.schtilig.mako
- `src/main/kotlin/com/schtilig/mako/lang/MakoParserDefinition.kt` - package com.schtilig.mako.lang
- `src/main/kotlin/com/schtilig/mako/lang/MakoElementType.kt` - package com.schtilig.mako.lang
- `src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt` - package com.schtilig.mako.lang
- `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt` - package com.schtilig.mako.lang.annotation
- `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt` - package com.schtilig.mako.lang.completion
- `src/main/kotlin/com/schtilig/mako/lang/psi/impl/MakoDefTagMixin.kt` - package com.schtilig.mako.lang.psi.impl
- `src/main/gen/com/schtilig/mako/lang/psi/MakoTypes.java` - package com.schtilig.mako.lang.psi
- `src/main/gen/com/schtilig/mako/lang/_MakoLexer.java` - package com.schtilig.mako.lang
- `src/main/gen/com/schtilig/mako/lang/parser/MakoParser.java` - package com.schtilig.mako.lang.parser
- `src/test/kotlin/com/schtilig/mako/lang/MakoParsingTest.kt` - package com.schtilig.mako.lang
- `src/test/kotlin/com/schtilig/mako/lang/MakoAnnotatorTest.kt` - package com.schtilig.mako.lang
- `src/test/kotlin/com/schtilig/mako/MyPluginTest.kt` - package com.schtilig.mako
- "(all 64 modified files receive the same treatment)"

## Decisions Made

- sed bulk replacement (`find ... -exec sed -i`) chosen over file-by-file editing for 65 files — atomic, consistent, no risk of missing a file
- Language ID "Mako Template" left unchanged — it is the platform Language registry key used in extension point `language=` attributes and the MakoLanguage constructor argument; it is not a Java package FQCN
- Task 1 (git mv of source directories) was already committed in Plan 01's execution — verified via `git show --stat 4590526`; Plan 02 proceeded directly to content updates

## Deviations from Plan

### Auto-fixed Issues

None.

### Context Note

Task 1 (git mv source trees) was already completed and committed in Plan 01's execution commit `4590526 chore(09-01): update Gradle build identity to com.schtilig.mako`. The Plan 01 executor included the git mv as part of its work (likely triggered when staging changes caused git to detect the directory renames). This is not a problem — Task 1 verification passed (files at new locations, old directories empty), and Task 2 proceeded as planned.

---

**Total deviations:** None

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Source package rename complete: all Kotlin and Java files at com.schtilig.mako package with matching directory structure and content
- Plan 03 can proceed with plugin.xml version bump and any remaining branding polish
- Build should compile after Plan 03 completes (all layers now consistent: build config, grammar, directory structure, source content)

---
*Phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions*
*Completed: 2026-02-21*

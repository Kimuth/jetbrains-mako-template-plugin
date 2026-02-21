---
phase: 01-language-foundation
plan: 02
subsystem: language-registration
tags: [intellij-platform, kotlin, language, filetype, template-language, svg-icon, pycharm]

# Dependency graph
requires:
  - phase: 01-language-foundation/01-01
    provides: "PyCharm Community build config, clean plugin.xml with empty <extensions> block"
provides:
  - "MakoLanguage singleton implementing Language('Mako Template') + TemplateLanguage"
  - "MakoFileType LanguageFileType wrapping MakoLanguage with mako/mak extensions and html.mako pattern"
  - "MakoIcons loading /icons/makoFile.svg via IconLoader"
  - "16x16 SVG file icon: rounded file shape with folded corner and blue #40B6E0 stylized M letter"
  - "plugin.xml <fileType> registration for .mako, .mak, and *.html.mako files"
  - "BUILD SUCCESSFUL with all Mako classes + SVG in plugin JAR"
affects: [02-lexer-parser, 03-parser-psi, 04-template-data-language, 05-highlighting]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TemplateLanguage pattern: MakoLanguage extends Language + TemplateLanguage for template engine signal to platform"
    - "Kotlin object + fieldName=INSTANCE: plugin.xml fileType fieldName attribute works for Kotlin singleton objects"
    - "Compound extension via patterns attribute: *.html.mako uses patterns= not extensions= for glob matching"
    - "String identity contract: Language ID in Language() constructor, getName() in FileType, and name= in plugin.xml must all be identical"

key-files:
  created:
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MakoLanguage.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MakoFileType.kt
    - src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MakoIcons.kt
    - src/main/resources/icons/makoFile.svg
  modified:
    - src/main/resources/META-INF/plugin.xml

key-decisions:
  - "MakoLanguage implements TemplateLanguage (not plain Language) — signals template engine to platform; required for Phase 4 TemplateDataLanguage integration"
  - "fieldName=INSTANCE in plugin.xml fileType — correct for Kotlin object singletons which expose a synthetic INSTANCE field to Java"
  - "compound extension .html.mako uses patterns= attribute not extensions= — extensions= only handles simple single-dot extensions, patterns= handles glob matching"
  - "Language ID 'Mako Template' matches getName() and plugin.xml name/language attributes — string contract enforced to prevent silent failures"

patterns-established:
  - "Language registration: Language singleton -> LanguageFileType -> plugin.xml fileType extension point"
  - "Icon loading: IconLoader.getIcon with class-relative path /icons/makoFile.svg"

requirements-completed: [LANG-01, LANG-02, LANG-03]

# Metrics
duration: 2min
completed: 2026-02-19
---

# Phase 1 Plan 02: Language Registration Summary

**MakoLanguage + MakoFileType registered in plugin.xml with 16x16 SVG icon — PyCharm now recognizes .mako, .mak, and .html.mako files as Mako Template language with a custom file icon**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-02-19T18:38:26Z
- **Completed:** 2026-02-19T18:40:10Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Created MakoLanguage singleton implementing `Language("Mako Template"), TemplateLanguage` — the core language identity that every subsequent phase builds on
- Created MakoFileType extending `LanguageFileType(MakoLanguage)` with name/description/extension/icon — registers .mako and .mak extensions plus *.html.mako glob pattern
- Created MakoIcons loading the SVG via `IconLoader.getIcon("/icons/makoFile.svg", MakoIcons::class.java)`
- Created 16x16 SVG icon with JetBrains-style file shape (rounded rect with folded top-right corner) and stylized blue "M" letter
- Registered all of the above in plugin.xml via `<fileType>` extension point with matching string identifiers
- Full `./gradlew build` passes; plugin JAR contains MakoLanguage.class, MakoFileType.class, MakoIcons.class, makoFile.svg, plugin.xml

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MakoLanguage, MakoFileType, MakoIcons, and SVG icon** - `12ea803` (feat)
2. **Task 2: Register fileType in plugin.xml and verify build** - `d4ac4d6` (feat)

**Plan metadata:** (docs commit recorded below)

## Files Created/Modified
- `src/main/kotlin/.../MakoLanguage.kt` - Language singleton implementing Language("Mako Template") + TemplateLanguage
- `src/main/kotlin/.../MakoFileType.kt` - LanguageFileType wrapping MakoLanguage with mako default extension and icon
- `src/main/kotlin/.../MakoIcons.kt` - Icon loader object exposing FILE constant from /icons/makoFile.svg
- `src/main/resources/icons/makoFile.svg` - 16x16 SVG: gray file shape with folded corner, blue #40B6E0 "M" letter at 70% opacity
- `src/main/resources/META-INF/plugin.xml` - Added <fileType> extension point with 6 required attributes

## Decisions Made
- Used TemplateLanguage interface (not plain Language subclass) — this is the correct pattern for template engine languages in IntelliJ Platform; it enables Phase 4's TemplateDataLanguage features
- fieldName="INSTANCE" in plugin.xml — Kotlin objects expose a synthetic Java INSTANCE field; this is the correct fieldName value for object singletons
- Used patterns="*.html.mako" (not extensions=) for the compound extension — the extensions attribute only handles single-dot extensions; compound extensions like .html.mako require glob matching via the patterns attribute
- String identifiers kept strictly consistent: Language ID "Mako Template" in MakoLanguage constructor, getName() in MakoFileType, and name=/language= in plugin.xml must all match exactly to avoid silent failures

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Language registration is complete and the build passes — Phase 2 (lexer/parser) can proceed immediately
- MakoLanguage as TemplateLanguage is in place; GrammarKit stub tasks in build.gradle.kts are ready to uncomment when .flex/.bnf files are created in Phase 2
- All three Phase 1 requirements (LANG-01, LANG-02, LANG-03) are now complete

---
*Phase: 01-language-foundation*
*Completed: 2026-02-19*

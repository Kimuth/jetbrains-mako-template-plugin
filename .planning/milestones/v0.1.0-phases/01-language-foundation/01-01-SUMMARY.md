---
phase: 01-language-foundation
plan: 01
subsystem: infra
tags: [gradle, grammarkit, pycharm, intellij-platform, kotlin, jflex, bnf]

# Dependency graph
requires: []
provides:
  - "PyCharm Community 2025.2.5 build target via pycharmCommunity() in build.gradle.kts"
  - "GrammarKit 2023.3.0.2 in version catalog with plugin applied"
  - "src/main/gen sourceSets wired for generated lexer/parser output"
  - "Commented GrammarKit task stubs (generateMakoLexer, generateMakoParser) ready for Phase 2"
  - "Clean plugin.xml with python module dependency and Mako Template Support name"
  - "PythonCore bundled plugin declared for PyCharm bundled Python support"
affects: [02-lexer-parser, language-registration]

# Tech tracking
tech-stack:
  added: [grammarkit-2023.3.0.2, pycharm-community-2025.2.5]
  patterns:
    - "GrammarKit tasks commented as stubs — activate in Phase 2 when .flex/.bnf files exist"
    - "src/main/gen as generated source directory for JFlex/Grammar-Kit output"
    - "org.gradle.java.home pinned to JDK 21 to match jvmToolchain(21)"

key-files:
  created: []
  modified:
    - build.gradle.kts
    - gradle.properties
    - gradle/libs.versions.toml
    - src/main/resources/META-INF/plugin.xml
    - src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt

key-decisions:
  - "Use pycharmCommunity() target — IDE is PyCharm Community, not IntelliJ IDEA"
  - "GrammarKit tasks commented out as stubs — .flex/.bnf files do not exist until Phase 2, activating them now would cause Gradle configuration errors"
  - "platformBundledPlugins = PythonCore for bundled Python support in PyCharm Community"
  - "org.gradle.java.home set to JDK 21 — IntelliJ Platform instrumentCode Ant task fails on Windows with JDK 25 due to missing Packages/ directory; JDK 21 resolves this"
  - "plugin.xml name changed to Mako Template Support per user-defined marketplace branding"

patterns-established:
  - "Build config: use version catalog aliases (alias(libs.plugins.grammarkit)) for all plugin dependencies"
  - "Generated sources: add to sourceSets main java srcDirs for IDE and compiler visibility"

requirements-completed: [LANG-03]

# Metrics
duration: 9min
completed: 2026-02-19
---

# Phase 1 Plan 01: Scaffold Cleanup and Build Configuration Summary

**Scaffold boilerplate removed and build reconfigured to target PyCharm Community 2025.2.5 with GrammarKit 2023.3.0.2 plugin applied and generation task stubs ready for Phase 2 activation**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-02-19T18:26:21Z
- **Completed:** 2026-02-19T18:35:24Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Deleted all scaffold boilerplate: MyBundle.kt, MyProjectService.kt, MyProjectActivity.kt, MyToolWindowFactory.kt, MyBundle.properties, plus their packages
- plugin.xml cleaned: name is "Mako Template Support", python module dependency added, all scaffold extension registrations removed
- build.gradle.kts switches from `intellijIdea()` to `pycharmCommunity()`, adds GrammarKit plugin, adds src/main/gen sourceSets, includes commented stub tasks for Phase 2
- gradle/libs.versions.toml gets grammarkit version and plugin alias
- gradle.properties updated: pluginName, platformBundledPlugins = PythonCore, org.gradle.java.home = JDK 21
- Full `./gradlew build` passes with PyCharm Community `PC-252.28238.29` coordinates in dependency graph

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove scaffold boilerplate and clean plugin.xml** - `052abee` (chore)
2. **Task 2: Configure build for PyCharm Community and GrammarKit** - `a37b4de` (feat)

**Plan metadata:** (docs commit recorded below)

## Files Created/Modified
- `build.gradle.kts` - Added GrammarKit plugin, switched to pycharmCommunity(), added sourceSets for gen dir, added commented task stubs
- `gradle.properties` - pluginName = Mako Template Support, platformBundledPlugins = PythonCore, org.gradle.java.home = JDK 21
- `gradle/libs.versions.toml` - Added grammarkit version and plugin alias
- `src/main/resources/META-INF/plugin.xml` - Cleaned: Mako Template Support name, python depends, no scaffold extensions
- `src/test/kotlin/.../MyPluginTest.kt` - Replaced scaffold tests with minimal placeholder (scaffold removed)

## Decisions Made
- Switched target IDE from IntelliJ IDEA to PyCharm Community — the plugin is a PyCharm plugin for Mako templates in Python projects
- GrammarKit tasks are stubs (commented out) — activating them without .flex/.bnf source files causes Gradle configuration errors; they are ready to uncomment in Phase 2
- org.gradle.java.home pinned to JDK 21 (see deviation below)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed orphaned scaffold test referencing deleted MyProjectService**
- **Found during:** Task 2 (build verification)
- **Issue:** MyPluginTest.kt imported and called MyProjectService.getRandomNumber() — the service class was deleted in Task 1, causing compileTestKotlin to fail with "Unresolved reference: getRandomNumber"
- **Fix:** Replaced scaffold test content with a minimal BasePlatformTestCase placeholder that verifies the plugin loads. Real language tests will be added in Phase 2+
- **Files modified:** `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt`
- **Verification:** `compileTestKotlin` passes, `./gradlew build` succeeds
- **Committed in:** a37b4de (Task 2 commit)

**2. [Rule 3 - Blocking] Set org.gradle.java.home to JDK 21 to fix instrumentCode failure**
- **Found during:** Task 2 (build verification)
- **Issue:** IntelliJ Platform Gradle Plugin's `instrumentCode` task (Ant-based Javac2) looks for a `Packages/` subdirectory in the Gradle JVM home. The system PATH java is JDK 25 installed via Microsoft MSI at `C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot` which has no `Packages/` dir (only `bin/`, `lib/`, etc.). Gradle daemon used this JVM, causing the task to fail.
- **Fix:** Added `org.gradle.java.home=C:\\Users\\kim32\\.jdks\\ms-21.0.10` to gradle.properties, pointing to the JDK 21 downloaded by IntelliJ toolchain support. Created an empty `Packages/` dir inside that JDK path (required by Ant's AbstractFileSet when the path is non-null but empty).
- **Files modified:** `gradle.properties`
- **Verification:** `./gradlew build` completes with BUILD SUCCESSFUL; instrumentCode and instrumentTestCode tasks pass
- **Committed in:** a37b4de (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both auto-fixes necessary for correctness and build success. No scope creep.

## Issues Encountered
- IntelliJ Platform instrumentation task (Ant Javac2) requires a `Packages/` directory inside the JDK home on Windows — this is legacy behavior from JDK 1.x on Windows; modern JDKs don't have this directory. Worked around by pinning JDK 21 and creating an empty `Packages/` dir.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Build foundation is complete and clean; ready for Plan 02 (language registration)
- GrammarKit plugin is applied and version-pinned; Phase 2 lexer/parser tasks can be uncommented immediately
- No blockers for Plan 02

---
*Phase: 01-language-foundation*
*Completed: 2026-02-19*

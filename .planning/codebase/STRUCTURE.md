# Codebase Structure

**Analysis Date:** 2026-02-19

## Directory Layout

```
jetbrains-mako-template-plugin/
├── src/                                    # Source code root
│   ├── main/
│   │   ├── kotlin/                        # Main plugin source code
│   │   │   └── com/github/kimuth/jetbrainsmakotemplateplugin/
│   │   │       ├── MyBundle.kt                    # Message bundle localization
│   │   │       ├── services/                      # Business logic layer
│   │   │       │   └── MyProjectService.kt        # Project-scoped service
│   │   │       ├── startup/                       # Lifecycle components
│   │   │       │   └── MyProjectActivity.kt       # Project startup hook
│   │   │       └── toolWindow/                    # UI presentation layer
│   │   │           └── MyToolWindowFactory.kt     # Tool window UI factory
│   │   └── resources/
│   │       ├── messages/                  # i18n message bundles
│   │       │   └── MyBundle.properties     # Localized message strings
│   │       └── META-INF/
│   │           └── plugin.xml             # Plugin manifest and extensions
│   └── test/
│       ├── kotlin/                        # Test source code
│       │   └── com/github/kimuth/jetbrainsmakotemplateplugin/
│       │       └── MyPluginTest.kt        # Plugin integration tests
│       └── testData/                      # Test fixture data
│           └── rename/                    # Rename operation test data
│               ├── foo.xml
│               └── foo_after.xml
├── gradle/                                # Gradle wrapper
│   └── wrapper/
├── build/                                 # Build output (generated)
├── build.gradle.kts                       # Main build configuration
├── settings.gradle.kts                    # Gradle project settings
├── gradle.properties                      # Plugin and platform properties
├── README.md                              # Plugin documentation
├── CHANGELOG.md                           # Version history
├── codecov.yml                            # Code coverage config
└── qodana.yml                             # Code quality config
```

## Directory Purposes

**src/main/kotlin/**
- Purpose: Primary plugin implementation source code
- Contains: All Kotlin classes implementing plugin functionality
- Key files: Service implementations, UI factories, bundles

**src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/**
- Purpose: Root plugin package
- Contains: Bundle localization and top-level plugin classes
- Key files: `MyBundle.kt`

**src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/**
- Purpose: Service and business logic layer
- Contains: Project-scoped services with `@Service` annotation
- Key files: `MyProjectService.kt`

**src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/**
- Purpose: Plugin lifecycle and initialization components
- Contains: Project activity implementations for startup hooks
- Key files: `MyProjectActivity.kt`

**src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/**
- Purpose: UI presentation and tool window components
- Contains: ToolWindowFactory implementations and UI panels
- Key files: `MyToolWindowFactory.kt`

**src/main/resources/messages/**
- Purpose: Internationalization message bundles
- Contains: Properties files with localized strings
- Key files: `MyBundle.properties` (English messages)

**src/main/resources/META-INF/**
- Purpose: Plugin metadata and manifest
- Contains: Plugin configuration and extension declarations
- Key files: `plugin.xml` (plugin manifest)

**src/test/kotlin/**
- Purpose: Test implementations
- Contains: Integration tests using IntelliJ Platform testing framework
- Key files: `MyPluginTest.kt`

**src/test/testData/**
- Purpose: Test fixture and sample data
- Contains: XML and other data files used by tests
- Key files: `rename/foo.xml`, `rename/foo_after.xml`

## Key File Locations

**Entry Points:**
- `src/main/resources/META-INF/plugin.xml`: Plugin manifest declaring extensions and lifecycle hooks
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/MyProjectActivity.kt`: Project startup entry point

**Configuration:**
- `gradle.properties`: Plugin version, platform version, and IntelliJ configuration
- `build.gradle.kts`: Build system configuration using IntelliJ Platform Gradle Plugin
- `settings.gradle.kts`: Gradle project settings

**Core Logic:**
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyProjectService.kt`: Main service with business logic
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyBundle.kt`: Message bundle for localization

**UI/Presentation:**
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt`: Tool window UI implementation

**Testing:**
- `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt`: Integration test suite
- `src/test/testData/rename/`: XML test fixtures

## Naming Conventions

**Files:**
- PascalCase: `MyBundle.kt`, `MyProjectService.kt`, `MyToolWindowFactory.kt`, `MyProjectActivity.kt`
- Package-scoped grouping by functionality: `services/`, `startup/`, `toolWindow/`

**Directories:**
- All lowercase: `services`, `startup`, `toolWindow`, `messages`, `META-INF`, `testData`
- Functional grouping (layer-based): services, startup, UI components in separate dirs

**Classes:**
- PascalCase, prefixed with feature: `MyProjectService`, `MyToolWindowFactory`, `MyProjectActivity`, `MyBundle`
- Nested inner classes: `MyToolWindow` (inner class of `MyToolWindowFactory`)

**Packages:**
- Reverse domain notation: `com.github.kimuth.jetbrainsmakotemplateplugin` root
- Functional subpackages: `.services`, `.startup`, `.toolWindow`
- Resource messages: `messages.MyBundle` (corresponds to `MyBundle.properties`)

**Properties/Messages:**
- camelCase keys: `projectService`, `randomLabel`, `shuffle`
- Descriptive, user-facing strings

## Where to Add New Code

**New Feature:**
- Primary code: Add Kotlin files to appropriate functional directory (`src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/{feature}/`)
- Tests: Add corresponding test file to `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/` with `Test` suffix
- Register: Add extension or service registration to `src/main/resources/META-INF/plugin.xml`

**New Service/Business Logic:**
- Implementation: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyNewService.kt`
- Annotation: Use `@Service(Service.Level.PROJECT)` for project-scoped, or `@Service(Service.Level.APP)` for application-scoped
- Registration: Automatically discovered by IntelliJ Platform (no XML needed for services)
- Usage: Retrieve via `project.service<MyNewService>()` or `ApplicationManager.getApplication().service<MyNewService>()`

**New UI Component:**
- Factory: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyNewToolWindowFactory.kt` implementing `ToolWindowFactory`
- Registration: Add `<toolWindow>` extension to `plugin.xml` with `factoryClass` attribute
- Content: Create UI in `createToolWindowContent()` method using IntelliJ UI components (`JBPanel`, `JBLabel`, etc.)

**New Message/Localization:**
- Add key=value pair to `src/main/resources/messages/MyBundle.properties`
- Reference in code via `MyBundle.message("keyName", ...params)`
- For other languages: Create `MyBundle_{locale}.properties` files

**Utilities/Helpers:**
- Shared helpers: Create new Kotlin file in root plugin package `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyUtility.kt`
- Layer-specific helpers: Place in corresponding layer directory (e.g., `services/` for service utilities)

## Special Directories

**build/:**
- Purpose: Build output and generated files
- Generated: Yes (gradle buildDir)
- Committed: No

**gradle/:**
- Purpose: Gradle wrapper and build tooling
- Generated: No
- Committed: Yes

**.gradle/:**
- Purpose: Gradle cache and metadata
- Generated: Yes
- Committed: No

**src/test/testData/:**
- Purpose: Fixture data and sample files for testing
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-02-19*

# Architecture

**Analysis Date:** 2026-02-19

## Pattern Overview

**Overall:** Plugin Extension Architecture (JetBrains IntelliJ Platform)

**Key Characteristics:**
- Service-based pattern using IntelliJ Platform dependency injection
- Event-driven startup and UI extension
- Layered separation between platform integration, business logic, and UI
- Declarative registration via `plugin.xml` manifest
- Message bundle localization pattern

## Layers

**Platform Integration Layer:**
- Purpose: Bridges IntelliJ Platform lifecycle and extension points
- Location: `src/main/resources/META-INF/plugin.xml`
- Contains: Plugin manifest declarations, extension registration
- Depends on: None
- Used by: IntelliJ Platform runtime

**Startup/Lifecycle Layer:**
- Purpose: Handles project initialization and plugin activation
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/`
- Contains: `MyProjectActivity` (implements `ProjectActivity`)
- Depends on: IntelliJ Platform logging utilities
- Used by: Platform (via registered `postStartupActivity`)

**Services Layer:**
- Purpose: Core business logic and project-scoped state management
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/`
- Contains: `MyProjectService` (decorated with `@Service(Service.Level.PROJECT)`)
- Depends on: IntelliJ Platform project reference, logging
- Used by: UI and startup components

**UI/Presentation Layer:**
- Purpose: Tool window UI and user interaction
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/`
- Contains: `MyToolWindowFactory` and nested `MyToolWindow` UI component
- Depends on: Services layer, message bundles, Swing UI components
- Used by: Platform (via registered `toolWindow` extension)

**Localization Layer:**
- Purpose: Centralized message and string management
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyBundle.kt` and `src/main/resources/messages/MyBundle.properties`
- Contains: `MyBundle` object extending `DynamicBundle`, property keys
- Depends on: IntelliJ Platform i18n framework
- Used by: All UI and presentation components

## Data Flow

**Plugin Startup Flow:**

1. IntelliJ Platform loads plugin manifest (`plugin.xml`)
2. Platform instantiates registered extensions: `MyToolWindowFactory` and `MyProjectActivity`
3. `MyProjectActivity.execute()` runs asynchronously on project load (logs warning)
4. Tool window is lazily registered but available in IDE UI
5. User opens tool window → `MyToolWindowFactory.createToolWindowContent()` invoked
6. Factory creates `MyToolWindow` instance, which retrieves `MyProjectService` via `project.service<MyProjectService>()`
7. Service is instantiated at PROJECT level (singleton per project), initializes logging
8. UI panel renders with label and shuffle button
9. Button click action calls `service.getRandomNumber()` and updates label via localized message

**State Management:**
- `MyProjectService` maintains state per project via IntelliJ `@Service` annotation
- No shared global state; services are scoped to project lifecycle
- Tool window recreated on demand but service instance persists across tool window opens

## Key Abstractions

**IntelliJ Platform Service Pattern:**
- Purpose: Automatic dependency injection and lifecycle management
- Examples: `MyProjectService` decorated with `@Service(Service.Level.PROJECT)`
- Pattern: Annotated class registered via IntelliJ container; retrieved via `project.service<T>()`

**Extension Point Registration:**
- Purpose: Declarative plugin functionality integration
- Examples: `postStartupActivity` and `toolWindow` in `plugin.xml`
- Pattern: XML declaration maps interface implementation to factory/class

**Message Bundle Pattern:**
- Purpose: Externalized, localizable strings
- Examples: `MyBundle.message("randomLabel", "?")` retrieves from `MyBundle.properties`
- Pattern: `DynamicBundle` with property key annotations; properties file provides i18n values

**Tool Window Factory Pattern:**
- Purpose: Lazy instantiation of tool window UI
- Examples: `MyToolWindowFactory.createToolWindowContent()`
- Pattern: Implements `ToolWindowFactory`, creates UI content on demand

## Entry Points

**Plugin Manifest (`plugin.xml`):**
- Location: `src/main/resources/META-INF/plugin.xml`
- Triggers: IntelliJ Platform plugin system initialization
- Responsibilities: Declares plugin identity, dependencies, extensions

**Project Startup Activity:**
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/MyProjectActivity.kt`
- Triggers: Each time a project opens in IDE
- Responsibilities: Execute initialization logic after project opens

**Tool Window UI:**
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt`
- Triggers: User selects tool window tab or IDE initializes it
- Responsibilities: Create and manage tool window panel content

**Project Service:**
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyProjectService.kt`
- Triggers: First access via `project.service<MyProjectService>()`
- Responsibilities: Provide business logic and state management

## Error Handling

**Strategy:** Logging-based error reporting via IntelliJ Platform

**Patterns:**
- `thisLogger()` used to emit warnings and info messages at startup
- No explicit error catching; relies on platform exception handling
- Warnings logged for template sample code cleanup reminders

## Cross-Cutting Concerns

**Logging:**
- Uses IntelliJ's `com.intellij.openapi.diagnostic.thisLogger()` for structured logging
- Messages emitted at initialization phases to guide developer cleanup

**Localization:**
- Centralized via `MyBundle` and `messages/MyBundle.properties`
- All user-facing strings keyed and externalized

**Service Injection:**
- IntelliJ Platform provides automatic service discovery and instantiation
- Retrieved via `project.service<T>()` or `toolWindow.project.service<T>()`

---

*Architecture analysis: 2026-02-19*

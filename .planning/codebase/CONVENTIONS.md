# Coding Conventions

**Analysis Date:** 2026-02-19

## Naming Patterns

**Files:**
- PascalCase for Kotlin class files: `MyBundle.kt`, `MyProjectService.kt`, `MyProjectActivity.kt`, `MyToolWindowFactory.kt`
- File names match primary public class name
- Package structure follows Java/Kotlin conventions: `com.github.kimuth.jetbrainsmakotemplateplugin.*`

**Classes:**
- PascalCase: `MyBundle`, `MyProjectService`, `MyProjectActivity`, `MyToolWindowFactory`
- Nested classes use PascalCase: `MyToolWindowFactory.MyToolWindow`
- Implementation of interfaces uses descriptive names: `class MyProjectService(project: Project)` implementing service patterns

**Functions:**
- camelCase: `message()`, `messagePointer()`, `getRandomNumber()`, `getContent()`, `execute()`, `createToolWindowContent()`
- Getter methods use `get` prefix: `getTestDataPath()`, `getRandomNumber()`, `getContent()`
- Action handlers: `addActionListener { ... }` - lambdas for event listeners

**Variables:**
- camelCase for local variables: `psiFile`, `xmlFile`, `label`, `projectService`, `myFixture`
- Private properties use underscore prefix convention not observed in this codebase (standard Kotlin `private val`)
- Latin/descriptive names for loop/temporary variables: no abbreviated variables observed

**Constants:**
- UPPER_SNAKE_CASE: `BUNDLE = "messages.MyBundle"`
- Marked with `const val` and `@NonNls` annotation where applicable

**Types:**
- Generic types explicitly named: `JBPanel<JBPanel<*>>`
- Type parameters not abbreviated in codebase

## Code Style

**Formatting:**
- IntelliJ IDEA default formatting applied (standard 4-space indentation inferred from codebase)
- One statement per line
- Trailing commas not used in this codebase
- Newlines between method definitions and properties
- Double blank line separation not observed in these short files

**Linting & Code Inspection:**
- Qodana inspections enabled via `qodana.yml`
- Profile: `qodana.recommended` (JVM community profile)
- JDK version: 21 (enforced)
- Code inspection runs automatically in CI pipeline

**Kotlin Compiler:**
- Kotlin 2.3.0
- JVM toolchain: 21
- Null safety: Explicit null checks observed (`?.let` operators used in `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt`)

## Import Organization

**Order:**
1. IntelliJ Platform API imports: `com.intellij.*`
2. JetBrains annotations: `org.jetbrains.annotations.*`
3. Project-specific imports: `com.github.kimuth.jetbrainsmakotemplateplugin.*`
4. Java/stdlib imports: `javax.swing.*`

**Path Aliases:**
- No path aliases or packages imports used in current codebase
- Explicit qualified imports only (no star imports observed)

**Examples:**
- `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyProjectService.kt`:
  ```kotlin
  import com.intellij.openapi.components.Service
  import com.intellij.openapi.diagnostic.thisLogger
  import com.intellij.openapi.project.Project
  import com.github.kimuth.jetbrainsmakotemplateplugin.MyBundle
  ```

## Error Handling

**Patterns:**
- IntelliJ Platform logging via `thisLogger()`: Used for warnings and info messages
- No try-catch blocks observed in current codebase (plugin framework handles most exceptions)
- Safe navigation operators: `?.let` pattern used when accessing potentially null values
  ```kotlin
  // From MyToolWindowFactory.kt
  xmlFile.rootTag?.let {
      assertEquals("foo", it.name)
      assertEquals("bar", it.value.text)
  }
  ```
- IntelliJ utilities for validation: `PsiErrorElementUtil.hasErrors()`, `assertInstanceOf()`, `assertNotNull()`

**Warnings:**
- Logger warnings for cleanup reminders: `thisLogger().warn("Don't forget to remove...")`
- Annotations used: `@Suppress("unused")` for intentionally unused members

## Logging

**Framework:** IntelliJ Platform's `thisLogger()`

**Patterns:**
- Info level for state notifications: `thisLogger().info(MyBundle.message(...))`
- Warn level for developer reminders: `thisLogger().warn("Don't forget...")`
- Localization support via `MyBundle` wrapper accessing resource properties

**Examples from codebase:**
```kotlin
// From MyProjectService.kt
thisLogger().info(MyBundle.message("projectService", project.name))
thisLogger().warn("Don't forget to remove all non-needed sample code files...")
```

## Comments

**When to Comment:**
- Not extensively used in this template codebase
- Plugin configuration documented via XML comments in `plugin.xml`

**KDoc/JavaDoc:**
- Used minimally in template
- Annotations used instead: `@NonNls`, `@PropertyKey`, `@JvmStatic`, `@Suppress`

## Function Design

**Size:**
- Methods kept small and focused
- Example: `getRandomNumber()` is 1-liner, `getContent()` constructs and returns in 8 lines
- Nested classes used for encapsulation: `MyToolWindowFactory.MyToolWindow` for tool window UI construction

**Parameters:**
- Explicit types always specified
- Constructor injection pattern used: `class MyProjectService(project: Project)`, `class MyToolWindowFactory.MyToolWindow(toolWindow: ToolWindow)`
- No default parameters observed in this codebase

**Return Values:**
- Explicit return types specified in function signatures
- Extension functions supported: `toolWindow.project.service<MyProjectService>()`
- Functional returns: lambda expressions used for event handlers

## Module Design

**Exports:**
- Single public class per file as primary export
- Nested classes allowed for closely related functionality: `MyToolWindowFactory.MyToolWindow`
- Interfaces implemented explicitly: `class MyProjectService`, `class MyToolWindowFactory : ToolWindowFactory`, `class MyProjectActivity : ProjectActivity`

**Object Singleton Pattern:**
- `object MyBundle : DynamicBundle(BUNDLE)` - Kotlin object singleton used for bundle access

**Service Locator Pattern:**
- IntelliJ Platform Service API used: `@Service(Service.Level.PROJECT)` annotation
- Project-level services accessed via: `project.service<MyProjectService>()`
- Dependency injection handled by IntelliJ platform

**Annotation-Driven Configuration:**
- `@NonNls` - Non-NLS string constants marked
- `@PropertyKey` - Resource bundle keys annotated
- `@JvmStatic` - For Java interop in object companions
- `@TestDataPath` - Test fixtures annotated in test classes

---

*Convention analysis: 2026-02-19*

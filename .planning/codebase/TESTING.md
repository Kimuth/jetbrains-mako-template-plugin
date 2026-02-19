# Testing Patterns

**Analysis Date:** 2026-02-19

## Test Framework

**Runner:**
- IntelliJ Platform Test Framework (TestFrameworkType.Platform)
- Configuration: `build.gradle.kts` lines 50
- Base class: `BasePlatformTestCase` from IntelliJ Platform

**Assertion Library:**
- JUnit 4.13.2 (stdlib assertions)
- OpenTest4j 1.3.0 (advanced test assertions)
- IntelliJ Platform testing utilities: `assertInstanceOf()`, `assertNotNull()`, `assertFalse()`, `assertEquals()`

**Run Commands:**
```bash
./gradlew test                    # Run unit tests via check task
./gradlew check                   # Run tests + verification (includes test task)
./gradlew buildPlugin             # Build plugin (runs tests as dependency)
./gradlew runIdeForUiTests        # Run IDE with UI test server on port 8082
```

**CI/CD Test Execution:**
- Build workflow: `./gradlew check` (line 112 in `.github/workflows/build.yml`)
- Plugin verification: `./gradlew verifyPlugin` (line 200 in `.github/workflows/build.yml`)
- Coverage reporting: Kover coverage report uploaded to CodeCov (line 126)

## Test File Organization

**Location:**
- Tests co-located with source: `src/test/kotlin/` mirrors `src/main/kotlin/` package structure
- Test data files: `src/test/testData/` directory
- Example: `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt`

**Naming:**
- Test classes use `Test` suffix: `MyPluginTest`
- Test methods prefixed with `test`: `testXMLFile()`, `testRename()`, `testProjectService()`
- Test data directory annotated: `@TestDataPath("\$CONTENT_ROOT/src/test/testData")`

**Structure:**
```
src/test/
├── kotlin/
│   └── com/github/kimuth/jetbrainsmakotemplateplugin/
│       └── MyPluginTest.kt
└── testData/
    └── rename/
        ├── foo.xml
        └── foo_after.xml
```

## Test Structure

**Suite Organization:**
```kotlin
@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testProjectService() {
        val projectService = project.service<MyProjectService>()
        assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
```

**Patterns:**
- No explicit setUp/tearDown: `BasePlatformTestCase` provides `project` and `myFixture` fixtures automatically
- Single method assertion style: Multiple assertions per test allowed
- PSI (Program Structure Interface) testing via `myFixture` for file/XML parsing
- Service injection testing via `project.service<T>()`

## Mocking

**Framework:** IntelliJ Platform test utilities (no explicit mocking library like Mockito observed)

**Patterns:**
- `myFixture.configureByText()` - Create in-memory PSI files for testing
- `myFixture.testRename()` - Rename refactoring test helper
- Real component injection: `project.service<MyProjectService>()` - Uses actual service instance
- No mock objects created; real IntelliJ platform services used

**What to Mock:**
- External file systems: use `myFixture.configureByText()` for in-memory PSI
- Editor interactions: myFixture provides virtual editor context

**What NOT to Mock:**
- IntelliJ Platform core services (Service.Level.PROJECT): Instantiated by platform
- PSI (Program Structure Interface): Real parsed structure tested
- Project context: Use `project` from `BasePlatformTestCase`

## Fixtures and Factories

**Test Data:**
```kotlin
// From MyPluginTest.kt
val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
myFixture.testRename("foo.xml", "foo_after.xml", "a2")
```

**Location:**
- Fixture files: `src/test/testData/rename/` directory
- Inline fixtures: `myFixture.configureByText()` for simple cases
- XML test data: `foo.xml` (before), `foo_after.xml` (after rename)

**Patterns:**
- Before/after file pairs for refactoring tests
- `myFixture` is auto-provided by `BasePlatformTestCase`
- `project` fixture is auto-provided for service testing

## Coverage

**Requirements:**
- Informational only (codecov.yml line 6: `informational: true`)
- No threshold enforced (line 6: `threshold: 0%`)
- Coverage tracking via Kover (build.gradle.kts lines 120-127)

**View Coverage:**
```bash
./gradlew koverReport                    # Generate coverage report
cat build/reports/kover/report.xml       # View XML report
# HTML report generated at: build/reports/kover/html/
```

**Coverage Configuration:**
- Enabled in `build.gradle.kts` (lines 120-127)
- Kover plugin: 0.9.5
- XML output enabled for CodeCov integration
- Report uploaded in CI: `.github/workflows/build.yml` (lines 123-127)

## Test Types

**Unit Tests:**
- Scope: Kotlin functions, service logic, bundle messages
- Approach: Direct method calls with assertions
- Example: `testProjectService()` verifies `getRandomNumber()` generates different values
- Framework: JUnit 4 with platform assertions

**Integration Tests:**
- Scope: PSI (Program Structure Interface) parsing, XML file handling, refactoring operations
- Approach: Use IntelliJ virtual filesystem and PSI infrastructure
- Example: `testXMLFile()` parses XML and validates structure; `testRename()` performs actual refactoring
- Framework: `BasePlatformTestCase` with myFixture

**E2E Tests:**
- Framework: UI Tests via Robot Server plugin
- Configuration: `.github/workflows/run-ui-tests.yml` (separate workflow)
- Runtime setup: `intellijPlatformTesting.runIde.register("runIdeForUiTests")` (build.gradle.kts lines 141-159)
- JVM args: Robot server on port 8082, disabled privacy dialogs

**Plugin Verification:**
- IntelliJ Plugin Verifier runs during CI (line 200 in build.yml)
- Validates plugin against multiple IDE versions
- Results: `build/reports/pluginVerifier/`

## Common Patterns

**Async Testing:**
- Not explicitly tested in current codebase
- IntelliJ Platform handles async via `ProjectActivity.execute()` suspend function
- Suspend function test pattern for ProjectActivity available but not demonstrated

**Error Testing:**
```kotlin
// From testXMLFile()
assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))
```

**PSI File Testing:**
```kotlin
// From testXMLFile()
val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)
assertNotNull(xmlFile.rootTag)
xmlFile.rootTag?.let {
    assertEquals("foo", it.name)
    assertEquals("bar", it.value.text)
}
```

**Service Injection Testing:**
```kotlin
// From testProjectService()
val projectService = project.service<MyProjectService>()
assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
```

**Refactoring Operation Testing:**
```kotlin
// From testRename()
myFixture.testRename("foo.xml", "foo_after.xml", "a2")
```

## Test Execution in CI

**Build Workflow:**
- Triggers: push to main, pull requests
- Jobs: build → test → inspectCode → verify → releaseDraft
- Test results archived if failure: `build/reports/tests`

**Coverage Reporting:**
```bash
# Generated by: ./gradlew check (koverReport runs as dependency)
# Uploaded to: CodeCov via codecov/codecov-action@v5
# Token: secrets.CODECOV_TOKEN
# Report file: build/reports/kover/report.xml
```

**Code Inspection:**
- Qodana runs separately from tests (`.github/workflows/build.yml` lines 129-165)
- Generates report on pull requests
- Profile: `qodana.recommended` for JVM projects

---

*Testing analysis: 2026-02-19*

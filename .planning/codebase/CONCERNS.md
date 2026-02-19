# Codebase Concerns

**Analysis Date:** 2026-02-19

## Tech Debt

**Template Sample Code Left in Production:**
- Issue: Multiple placeholder/sample classes (`MyBundle`, `MyProjectService`, `MyProjectActivity`, `MyToolWindowFactory`) and sample UI remain in codebase and are registered in `plugin.xml`. These serve only as template examples.
- Files:
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyBundle.kt`
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyProjectService.kt`
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/MyProjectActivity.kt`
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt`
  - `src/main/resources/META-INF/plugin.xml` (lines 12-13)
- Impact: Plugin ships with non-functional sample code, bloats JAR, and confuses actual plugin functionality. Template warnings logged at startup pollute IDE logs.
- Fix approach: Remove all template sample files. Replace with actual plugin implementation. Update `plugin.xml` to remove `toolWindow` and `postStartupActivity` registrations until real functionality exists. Retain only `MyBundle.kt` pattern if it will be reused for actual strings.

**Incomplete Plugin Manifest Configuration:**
- Issue: `plugin.xml` contains minimal dependency declaration (`<depends>com.intellij.modules.platform</depends>`) and no actual plugin capabilities beyond UI scaffolding. No description beyond placeholder text.
- Files: `src/main/resources/META-INF/plugin.xml`
- Impact: Plugin cannot declare actual features or dependencies. May fail compatibility checks or load unnecessarily on all projects.
- Fix approach: Define actual plugin dependencies, extension points, and capabilities. Add `<description>` element. Configure `since-build` and `until-build` bounds based on platform version support strategy.

**Placeholder README.md Not Updated:**
- Issue: README contains unchecked template checklist items and placeholder text (`MARKETPLACE_ID` not set, plugin description is generic "Fancy IntelliJ Platform Plugin").
- Files: `README.md` (lines 21, 4, 5, 14, 37-40)
- Impact: Users cannot find plugin on marketplace. Documentation doesn't convey actual purpose or features.
- Fix approach: Remove template checklist entirely. Write actual plugin description and purpose. Update marketplace URL once plugin is published.

## Known Bugs

**Flaky Service Instantiation Test:**
- Bug: `testProjectService()` in `MyPluginTest.kt` (line 32-36) asserts that two consecutive calls to `service.getRandomNumber()` return different values. This test is probabilistically flaky.
- Symptoms: Test may randomly pass or fail depending on whether random values happen to be different.
- Files: `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt` (lines 32-36)
- Trigger: Run test multiple times; occasionally fails when random values collide.
- Workaround: None; test is fundamentally flawed. Should verify service is a singleton or use mocked random.
- Fix approach: Either mock `random()` to return predictable values or verify service instance identity (`assertSame(project.service<MyProjectService>(), project.service<MyProjectService>())`).

## Security Considerations

**Unvalidated Plugin Signing Configuration:**
- Risk: `build.gradle.kts` (lines 91-95) reads signing credentials from environment variables (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`) but provides no validation, logging, or error handling if these are missing or malformed. Plugin could be signed with wrong keys silently.
- Files: `build.gradle.kts` (lines 91-95)
- Current mitigation: GitHub secrets (assumed configured).
- Recommendations: Add validation task to verify signing configuration exists before build. Log certificate details (not sensitive values) to confirm correct key loaded. Fail build explicitly if signing credentials missing.

**No Input Validation in Tool Window:**
- Risk: UI label in `MyToolWindowFactory.kt` (line 40) directly renders service output without sanitization. While current `getRandomNumber()` is safe, future functionality might accept user input without validation.
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (line 40)
- Current mitigation: Sample code only uses safe numeric output.
- Recommendations: Establish input validation pattern before accepting user input. Use message bundle parameters for all UI strings (already done).

**Exposed Project References in Init Blocks:**
- Risk: Service `init` block (line 11-14 in `MyProjectService.kt`) and factory `init` block (line 18-20 in `MyToolWindowFactory.kt`) log at startup. In multi-project IDEs, this could leak project information or create noisy logging. Init blocks run multiple times on component creation.
- Files:
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/services/MyProjectService.kt` (lines 11-14)
  - `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (lines 18-20)
- Current mitigation: Only logs warnings.
- Recommendations: Move init block logging to dedicated lifecycle methods or remove entirely if warning is removed (i.e., once template code is deleted).

## Performance Bottlenecks

**Synchronous Service Instantiation on UI Thread:**
- Problem: `MyToolWindowFactory.createToolWindowContent()` (line 22-25) synchronously retrieves `MyProjectService` via `project.service<MyProjectService>()` on the UI event dispatch thread. If future service implementation performs I/O or heavy computation in init, UI will block.
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (line 24)
- Cause: Direct blocking service retrieval in tool window content creation.
- Improvement path: Move service initialization to background thread or deferrable coroutine. Use `CoroutineScope` (available in IntelliJ 2020.2+) for async service setup.

**Random Number Generation in Hot Path:**
- Problem: Button click handler (line 39-41 in `MyToolWindowFactory.kt`) calls `service.getRandomNumber()` which internally creates a new range `(1..100)` on each invocation and calls `.random()`.
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (line 40)
- Cause: Not critical for sample code, but wasteful pattern.
- Improvement path: Cache random instance or use more efficient generation; this is negligible for sample functionality.

## Fragile Areas

**Hard-Coded Package Name Coupling:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/` entire package structure
- Why fragile: Package name `com.github.kimuth.jetbrainsmakotemplateplugin` is deeply nested and brittle. Refactoring or renaming breaks multiple references: `plugin.xml` extension class references, test imports, and bundle resource paths.
- Safe modification: Any rename must update: package declarations, import statements, `plugin.xml` factoryClass/implementation attributes, message bundle resource path `@PropertyKey(resourceBundle = ...)`, and test imports.
- Test coverage: Minimal; only basic XML parsing and service retrieval tested. No tests for tool window factory or UI rendering.

**Swing UI Without Layout Management:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (lines 34-43)
- Why fragile: `JBPanel.apply { add(label); add(button) }` uses default layout with no explicit layout manager. Adding more components may cause unexpected rendering. No padding, alignment, or responsiveness logic.
- Safe modification: Wrap in explicit `BorderLayout` or `GridBagLayout`. Define minimum/preferred sizes for components.
- Test coverage: UI layout not tested at all; manual testing only.

**Loose Service Dependency:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (line 32)
- Why fragile: Service is retrieved at constructor time, not injected. If service fails to instantiate, factory silently fails or crashes. No null check or error recovery.
- Safe modification: Add explicit null check after `.service<>()` call. Consider lazy initialization. Add error logging if service unavailable.
- Test coverage: Service instantiation failures not tested.

**Message Bundle Key Typos Undetected:**
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyBundle.kt`, `src/main/resources/messages/MyBundle.properties`
- Why fragile: String keys like `"randomLabel"` and `"shuffle"` are untyped strings. Typo in code (e.g., `message("randoomLabel")`) causes silent fallback to key name at runtime, not compile-time error.
- Safe modification: Use typed constants for bundle keys (e.g., `const val KEY_RANDOM_LABEL = "randomLabel"`). Enforce via linting or code generation.
- Test coverage: Bundle key correctness not validated in tests.

## Scaling Limits

**Single Tool Window Only:**
- Current capacity: One hardcoded tool window (`MyToolWindow`) per project
- Limit: Cannot easily add multiple tool windows or make them configurable
- Scaling path: Parameterize tool window factory, support registry/factory pattern for multiple tool windows, use IntelliJ Platform service composition

**No Settings/Preferences Panel:**
- Current capacity: Plugin has no UI for user configuration
- Limit: Cannot expose plugin options to users
- Scaling path: Implement `SearchableConfigurable` and register in `plugin.xml` `<extensions><projectConfigurable>` to allow Settings > [Plugin] configuration panel

**No Persistent State Management:**
- Current capacity: Service state lost on tool window close
- Limit: Cannot remember user preferences or session state across IDE restarts
- Scaling path: Implement `PersistentStateComponent` in service, or use IntelliJ's `PropertiesComponent` for lightweight key-value storage

## Dependencies at Risk

**Old JUnit 4 (4.13.2) with End-of-Life Status:**
- Risk: JUnit 4.13.2 is EOL; should migrate to JUnit 5 (Jupiter) for better maintainability and feature support
- Files: `gradle/libs.versions.toml` (line 3), `build.gradle.kts` (line 34)
- Impact: Security patches may not be available; newer IDE test frameworks assume JUnit 5
- Migration plan:
  1. Update `libs.versions.toml` to use `junit-api` + `junit-engine` (5.x)
  2. Migrate test annotations: `@Test` stays same, add `@ExtendWith` for custom extensions
  3. Replace `assertEquals` calls with Jupiter equivalents
  4. Update `build.gradle.kts` dependencies and `TestFrameworkType.Platform` to ensure platform test framework is compatible

**IntelliJ Platform 2025.2.5 May Have Security Updates:**
- Risk: Platform version may be behind latest patch; vulnerabilities in dependencies could affect plugin
- Files: `gradle.properties` (line 13)
- Impact: Plugin inherits platform vulnerabilities
- Mitigation: Monitor JetBrains release notes; update to latest 2025.x patch regularly. Consider monitoring GitHub dependabot or IntelliJ release notifications.

## Missing Critical Features

**No Error Handling for Service Failures:**
- Problem: If `MyProjectService` initialization fails (e.g., throws exception in init), plugin silently fails or crashes without user-visible error message
- Blocks: Reliable plugin operation; users don't know why plugin isn't working
- Recommendation: Wrap service instantiation in try-catch; log errors; optionally display IDE notification to user

**No Plugin Initialization/Activation Hooks:**
- Problem: Plugin lacks startup validation or capability detection. Should verify prerequisites (e.g., required plugins installed) or IDE version at load time
- Blocks: Cannot gracefully disable on incompatible IDEs; forces full failure instead
- Recommendation: Implement `ApplicationActivity` for app-level initialization; add version checks in startup activity

**No Internationalization Support Beyond English:**
- Problem: While message bundle pattern exists, only `MyBundle.properties` (English) included. No translations for other locales
- Blocks: Plugin cannot be marketed internationally
- Recommendation: Add property files for target locales (e.g., `MyBundle_de.properties`, `MyBundle_ja.properties`)

## Test Coverage Gaps

**Tool Window UI Not Tested:**
- What's not tested: `MyToolWindow.getContent()` UI rendering, button click behavior, label updates
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/toolWindow/MyToolWindowFactory.kt` (lines 30-44)
- Risk: UI layout changes or component initialization errors go undetected; users encounter crashes or incorrect rendering
- Priority: Medium (UI critical path, but sample code)

**Startup Activity Not Tested:**
- What's not tested: `MyProjectActivity.execute()` runs at project load
- Files: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/startup/MyProjectActivity.kt`
- Risk: If future logic added, no verification it runs on project open or doesn't block IDE
- Priority: Medium

**Service Lifecycle Not Verified:**
- What's not tested: Service singleton scope, reuse across multiple accesses, cleanup on project close
- Files: `src/test/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MyPluginTest.kt` (lines 32-36)
- Risk: Service could leak memory or create multiple instances undetected
- Priority: High (affects reliability)

**Bundle Message Keys Not Validated:**
- What's not tested: All property keys in `MyBundle.properties` are actually used and vice versa (unused keys)
- Files: `src/main/resources/messages/MyBundle.properties`
- Risk: Dead message keys waste space; typos in code silently use key as fallback text
- Priority: Low (minor impact)

**Platform Compatibility Not Verified:**
- What's not tested: Plugin verification against actual IntelliJ builds beyond CI pipeline
- Files: CI/CD coverage is adequate (`build.yml` runs `pluginVerifier`), but local testing sparse
- Risk: Plugin might not load on target IDE versions; compatibility regressions missed
- Priority: Medium (CI handles most cases)

---

*Concerns audit: 2026-02-19*

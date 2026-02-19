# Technology Stack

**Analysis Date:** 2026-02-19

## Languages

**Primary:**
- Kotlin 2.3.0 - Plugin logic and main implementation
- Java 21 (JVM) - Runtime compilation target

**Secondary:**
- XML - Plugin configuration files (`src/main/resources/META-INF/plugin.xml`)
- Properties - Message bundle configuration (`src/main/resources/messages/MyBundle.properties`)

## Runtime

**Environment:**
- Java 21 (JVM Toolchain)

**Package Manager:**
- Gradle 9.3.1
- Lockfile: Present (Gradle wrapper managed via `gradle/wrapper/gradle-wrapper.jar`)

## Frameworks

**Core:**
- IntelliJ Platform 2025.2.5 - JetBrains plugin framework for IDE integration
- IntelliJ Platform Gradle Plugin 2.11.0 - Build and plugin configuration for IntelliJ ecosystem

**Testing:**
- JUnit 4.13.2 - Unit test framework
- OpenTest4J 1.3.0 - Test assertion and reporting library
- IntelliJ Platform Test Framework - Provided by IntelliJ Platform (extends `BasePlatformTestCase`)

**Build/Dev:**
- Gradle Changelog Plugin 2.5.0 - CHANGELOG management and versioning
- Gradle Kover Plugin 0.9.5 - Code coverage reporting for Kotlin
- Gradle Qodana Plugin 2025.3.1 - Static analysis and code quality inspection

## Key Dependencies

**Critical:**
- IntelliJ Platform (via `intellijIdea()`) - Provides core IDE APIs and plugin runtime
  - Versioning: Build 252 minimum (set in `gradle.properties`)
  - Includes bundled plugins and modules based on `gradle.properties` configuration

**Infrastructure:**
- IntelliJ Annotations (org.jetbrains.annotations) - Type safety and null-safety annotations used in `MyBundle.kt`
- Kotlin Standard Library - Bundled via `kotlin.stdlib.default.dependency = false` (opt-out)

## Configuration

**Environment:**
- Configuration via `gradle.properties`:
  - `pluginGroup`: Package namespace (`com.github.kimuth.jetbrainsmakotemplateplugin`)
  - `pluginName`: Display name (`jetbrains-mako-template-plugin`)
  - `pluginVersion`: Semantic version (`0.0.1`)
  - `platformVersion`: IntelliJ Platform version (`2025.2.5`)
  - `platformBundledPlugins`: Built-in plugins to include (empty)
  - `platformPlugins`: Marketplace plugins to include (empty)
  - `platformBundledModules`: IntelliJ modules to include (empty)

**Build:**
- `build.gradle.kts`: Main build configuration
- `settings.gradle.kts`: Gradle settings and toolchain resolver
- Version catalog: `gradle/libs.versions.toml` - Centralized dependency versioning

**Signing & Publishing (Environment Variables):**
- `CERTIFICATE_CHAIN` - Plugin signing certificate chain
- `PRIVATE_KEY` - Plugin signing private key
- `PRIVATE_KEY_PASSWORD` - Private key password
- `PUBLISH_TOKEN` - JetBrains Marketplace publishing token

## Platform Requirements

**Development:**
- Gradle 9.3.1 or compatible wrapper
- Java 21 JDK (configured via `kotlin.jvmToolchain(21)`)
- IntelliJ IDEA or compatible IDE for testing

**Production:**
- IntelliJ IDEA 2025.2.5 or compatible (build 252+)
- Plugin deployment: JetBrains Marketplace
- Alternative manual installation via downloaded `.jar` file

## Gradle Configuration Features

**Caching & Performance:**
- Configuration cache enabled: `org.gradle.configuration-cache = true`
- Build cache enabled: `org.gradle.caching = true`

**Plugin Verification:**
- Automated compatibility testing with recommended IntelliJ build versions via `pluginVerifier`

**UI Testing Support:**
- Robot server plugin enabled for GUI testing (`robotServerPlugin()`)
- Custom run task: `runIdeForUiTests` with custom JVM arguments and robot server configuration

---

*Stack analysis: 2026-02-19*

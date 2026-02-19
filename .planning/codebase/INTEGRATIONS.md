# External Integrations

**Analysis Date:** 2026-02-19

## APIs & External Services

**JetBrains Marketplace:**
- JetBrains Plugin Marketplace - Distribution and hosting for IntelliJ plugins
  - SDK/Client: IntelliJ Platform Gradle Plugin (via `intellijPlatform` configuration)
  - Auth: `PUBLISH_TOKEN` environment variable (configured in `build.gradle.kts` lines 98)
  - Publishing endpoint: Configured via `publishing { token }` block

**GitHub:**
- GitHub Actions CI/CD pipelines (`.github/workflows/`)
- GitHub Releases for manual plugin distribution
- Code repository hosting

## Data Storage

**Databases:**
- None detected - This is a plugin, not a service with persistent data requirements

**File Storage:**
- Local IDE filesystem only
- Plugin stores configuration in IntelliJ's standard plugin directories

**Caching:**
- None detected in integrations

## Authentication & Identity

**Auth Provider:**
- Custom - JetBrains account-based (implicit)
  - Implementation: Marketplace authentication handled by IntelliJ IDE itself
  - Plugin publishing uses marketplace tokens (`PUBLISH_TOKEN`)
  - End-user authentication handled by IDE

## Monitoring & Observability

**Error Tracking:**
- None detected

**Logs:**
- IntelliJ Logger API (`com.intellij.openapi.diagnostic.thisLogger()`)
  - Used in `MyProjectService.kt` (line 13)
  - Used in `MyProjectActivity.kt`
  - Used in `MyToolWindowFactory.kt`
  - Logs written to IntelliJ IDE log file, not external service

**Code Quality Inspection:**
- Qodana (JetBrains static analysis) - Optional quality checks via `qodana.yml`

## CI/CD & Deployment

**Hosting:**
- GitHub Actions (Ubuntu latest runners) - Main CI/CD platform
  - Build workflow: `.github/workflows/build.yml`
  - Release workflow: `.github/workflows/release.yml`
  - UI test workflow: `.github/workflows/run-ui-tests.yml`

**CI Pipeline:**
- Gradle tasks executed via GitHub Actions:
  - `test` - Run unit tests
  - `verifyPlugin` - Validate plugin structure
  - `buildPlugin` - Create distribution JAR
  - `runPluginVerifier` - Verify compatibility across IntelliJ versions
  - Qodana analysis via Gradle plugin

**Deployment Target:**
- Primary: JetBrains Marketplace (automatic via `publishPlugin` task)
- Secondary: GitHub Releases (manual download)
- Tertiary: Manual installation from disk

## Environment Configuration

**Required env vars:**
- `PUBLISH_TOKEN` - JetBrains Marketplace authentication (required for publishing)
- `CERTIFICATE_CHAIN` - Plugin signing certificate (required for marketplace publishing)
- `PRIVATE_KEY` - Plugin signing private key (required for marketplace publishing)
- `PRIVATE_KEY_PASSWORD` - Private key password (required for marketplace publishing)

**Optional env vars:**
- `CODECOV_TOKEN` - Code coverage reporting to Codecov (referenced in README.md)

**Secrets location:**
- GitHub Secrets (configured in repository settings)
- Referenced in `build.gradle.kts` via `providers.environmentVariable()`
- Published in CI/CD workflows (`.github/workflows/`)

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- GitHub Actions workflow triggers: Push to `main` branch, pull requests
- Marketplace publish notifications (handled by JetBrains, not this plugin)

## External Dependencies Resolution

**Repositories:**
- Maven Central Repository (via `mavenCentral()`)
- IntelliJ Platform Gradle Plugin Repositories Extension (`intellijPlatform { defaultRepositories() }`)

**Dependency Sources:**
- JetBrains plugin marketplace (for IntelliJ platform)
- Maven Central (for JUnit, OpenTest4J, Kotlin stdlib)

---

*Integration audit: 2026-02-19*

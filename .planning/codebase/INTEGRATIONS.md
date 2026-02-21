# External Integrations

**Analysis Date:** 2025-02-21

## APIs & External Services

**None** - This plugin does not integrate with external APIs or third-party services.

The plugin is a language support tool that provides:
- Lexical analysis and syntax highlighting for Mako templates
- Parser and PSI tree construction for IDE features
- IDE feature implementations (code folding, structure view, comment toggling)

All functionality is self-contained within the IntelliJ Platform plugin infrastructure.

## Data Storage

**Databases:** None

**File Storage:** Local filesystem only
- No remote cloud storage
- Plugin reads/writes project files through IntelliJ's VirtualFile API
- Generated code placed in `src/main/gen/` directory

**Caching:** None
- No persistent caching layer
- Lexer/parser state is ephemeral (per-session)

## Authentication & Identity

**Auth Provider:** None required

The plugin operates within the user's IntelliJ IDE environment. No external authentication or identity services are used.

## Monitoring & Observability

**Error Tracking:** None
- No error reporting service (Sentry, Rollbar, etc.)
- No usage/telemetry collection

**Logs:**
- Standard Java logging (java.util.logging)
- Writes to IntelliJ IDE logs directory
- No remote log aggregation

## CI/CD & Deployment

**Hosting:**
- GitHub (repository: https://github.com/Kimuth/jetbrains-mako-template-plugin)
- JetBrains Marketplace (plugin distribution) - Not yet published (MARKETPLACE_ID placeholder in README)

**CI Pipeline:**
- GitHub Actions (workflow: Build)
- Tasks: ./gradlew buildPlugin, ./gradlew check, ./gradlew test
- Artifact: Plugin JAR published to JetBrains Marketplace via ./gradlew publishPlugin

## Environment Configuration

**Required env vars for publishing:**
- `CERTIFICATE_CHAIN` - Plugin signing certificate (PEM or PKCS12 format)
- `PRIVATE_KEY` - Plugin signing private key
- `PRIVATE_KEY_PASSWORD` - Password for private key
- `PUBLISH_TOKEN` - JetBrains Marketplace API authentication token

**Optional env vars:**
- `CODECOV_TOKEN` - Code coverage reporting (configured in GitHub secrets)

**Secrets location:**
- GitHub Actions secrets (not in version control)
- `.env` file pattern not used

## Webhooks & Callbacks

**Incoming:** None

**Outgoing:** None

The plugin does not send outbound webhooks or callbacks to external systems.

## IDE Feature APIs

**IntelliJ Platform Dependencies:**
- `com.intellij.modules.platform` - Core IDE services
- `com.intellij.modules.python` - Python language integration

**Bundled Dependencies:**
- PythonCore - Bundled plugin providing Python language features

**Extension Points Used (registered in plugin.xml):**
- `com.intellij.fileType` - Register Mako template file type
- `com.intellij.lang.parserDefinition` - Register parser/lexer
- `com.intellij.lang.syntaxHighlighterFactory` - Register syntax highlighter
- `com.intellij.colorSettingsPage` - Register color settings UI
- `com.intellij.additionalTextAttributes` - Register color scheme XML files
- `com.intellij.lang.braceMatcher` - Register brace pair matching
- `com.intellij.lang.commenter` - Register comment toggle behavior
- `com.intellij.lang.foldingBuilder` - Register code folding regions
- `com.intellij.lang.psiStructureViewFactory` - Register structure view outline

## Build Infrastructure

**Gradle Plugins (via Gradle Plugin Portal):**
- org.gradle.toolchains.foojay-resolver-convention 1.0.0 - Toolchain resolution

**No third-party Maven repositories besides:**
- mavenCentral() - Standard Maven Central
- IntelliJ Platform Gradle Plugin repositories (via defaultRepositories())

---

*Integration audit: 2025-02-21*

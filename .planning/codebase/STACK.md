# Technology Stack

**Analysis Date:** 2025-02-21

## Languages

**Primary:**
- Kotlin 2.3.0 - Plugin implementation, PSI mixins, syntax highlighting, code structure
- Java 21 - JVM target and build configuration
- JFlex - Lexer grammar (hand-written in `src/main/grammars/MakoLexer.flex`)
- BNF - Parser grammar (hand-written in `src/main/grammars/Mako.bnf`)

**Secondary:**
- XML - Plugin configuration (`src/main/resources/META-INF/plugin.xml`)

## Runtime

**Environment:**
- JVM (Java 21) - Build and execution target
- IntelliJ Platform 2025.2.5 - Host IDE runtime for plugin
- PyCharm Community 2025.2.5 (build 252+) - Supported IDE

**Package Manager:**
- Gradle 9.3.1
- Lockfile: Present (gradle wrapper binaries)

## Frameworks

**Core IntelliJ Platform:**
- IntelliJ Platform Gradle Plugin 2.11.0 - Plugin development and publication
- GrammarKit 2023.3.0.2 - Parser/lexer code generation from BNF and JFlex

**Testing:**
- JUnit 4.13.2 - Test framework
- OpenTest4J 1.3.0 - Assertion/exception library
- IntelliJ Platform Test Framework (bundled) - Platform testing utilities
- BasePlatformTestCase - Base test harness for lexer/parser unit tests
- ParsingTestCase - Fixture-based parser testing (for `.mako` → `.txt` PSI tree tests)

**Build & Code Quality:**
- Gradle Changelog Plugin 2.5.0 - Changelog management for releases
- Gradle Qodana Plugin 2025.3.1 - Code quality/static analysis
- Gradle Kover 0.9.5 - Test code coverage (XML reports enabled)

## Key Dependencies

**Critical:**
- IntelliJ Platform SDK (2025.2.5) - Core IDE APIs (lexing, parsing, PSI, syntax highlighting)
  - com.intellij.modules.platform - Platform module dependency
  - com.intellij.modules.python - Python module dependency (for PyCharm)
  - PythonCore (bundled plugin) - Python language support

**Build/Generation:**
- JetBrains GrammarKit 2.11.0 - Generates MakoParser.java and MakoTypes.java from Mako.bnf
- JFlex-based lexer generation - Generates _MakoLexer.java from MakoLexer.flex

## Configuration

**Build Configuration:**
- `build.gradle.kts` - Main build script with Gradle tasks and IntelliJ platform configuration
- `gradle.properties` - Plugin metadata and platform versions
- `settings.gradle.kts` - Gradle settings and toolchain resolution
- `gradle/libs.versions.toml` - Dependency version catalog

**Environment Variables (Build):**
- `CERTIFICATE_CHAIN` - Plugin signing certificate (for publishing)
- `PRIVATE_KEY` - Plugin signing private key (for publishing)
- `PRIVATE_KEY_PASSWORD` - Private key password (for publishing)
- `PUBLISH_TOKEN` - JetBrains Marketplace API token (for publishing)
- `org.gradle.java.home` - JDK 21 home directory (configured in gradle.properties)

**Color Schemes:**
- `src/main/resources/colorSchemes/MakoDefault.xml` - Light theme syntax highlighting
- `src/main/resources/colorSchemes/MakoDarcula.xml` - Dark theme syntax highlighting

## Platform Requirements

**Development:**
- JDK 21+ (Microsoft JDK 21.0.10 configured in gradle.properties)
- Gradle 9.3.1
- IntelliJ IDEA or PyCharm Community (for GrammarKit parser generation via IDE action)

**Production/Deployment:**
- PyCharm Community 2025.2.5+ (build 252+)
- IntelliJ Platform minimum build: 252

## Generated Code Management

**Lexer Generation:**
- Task: `./gradlew generateMakoLexer`
- Input: `src/main/grammars/MakoLexer.flex`
- Output: `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java`
- Committed: No (regenerated on every build, consumed by MakoLexerAdapter)
- Encoding: Lexer state encodes both JFlex state (bits 0-3) and brace nesting depth (bits 4+) for incremental re-lexing

**Parser Generation:**
- Task: Manual via `Tools → Generate Parser Code` in IntelliJ IDE (not automated in Gradle)
- Input: `src/main/grammars/Mako.bnf`
- Outputs:
  - `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java`
  - `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoTypes.java`
  - `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/*.java` (50+ PSI element implementations)
- Committed: Yes (committed to git for CI/CD without requiring IDE)
- Warning: MakoTypes.java contains hand-added token delegates that would be purged if regenerated with default settings

---

*Stack analysis: 2025-02-21*

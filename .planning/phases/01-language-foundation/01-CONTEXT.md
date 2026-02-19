# Phase 1: Language Foundation - Context

**Gathered:** 2026-02-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Register Mako as a distinct language in PyCharm. Create the Language class, FileType, and file icon so `.mako` files are recognized as first-class citizens. Wire GrammarKit and JFlex into the Gradle build. Remove scaffold boilerplate. The result: opening a `.mako` file shows a Mako icon, PyCharm lists "Mako Template" as a language, and the build is ready for lexer/parser work in Phase 2.

</domain>

<decisions>
## Implementation Decisions

### File associations
- Recognize three extension patterns: `.mako`, `.mak`, and `.html.mako`
- All extensions treated identically — no special behavior for compound extensions
- If another plugin claims `.mako`, our plugin should override (take priority as the purpose-built solution)

### File icon design
- Text-based icon: stylized letter "M" on a file shape
- Follow standard JetBrains file icon shape (rounded rectangle with folded corner)
- Single icon version for both light and dark themes

### Language identity
- Language display name in IDE: "Mako Template"
- Plugin name for JetBrains Marketplace: "Mako Template Support"
- Plugin positioning: PyCharm-focused (Mako is a Python template engine)
- Target development/test IDE: PyCharm Community

### Scaffold cleanup
- Keep GitHub Actions CI/CD workflow and Changelog plugin
- Target IDE switched from IntelliJ IDEA to PyCharm Community

### Claude's Discretion
- File icon color choice (pick something that works well in the JetBrains icon palette)
- MIME type selection (text/x-mako vs alternatives)
- Package name decision (keep or shorten com.github.kimuth.jetbrainsmakotemplateplugin)
- Scaffold cleanup specifics: which boilerplate to remove vs. repurpose (tool window, service, startup activity, resource bundle, sample tests)

</decisions>

<specifics>
## Specific Ideas

- Icon should be a stylized "M" — similar to how Kotlin uses "K" and TypeScript uses "TS"
- Follow JetBrains standard icon conventions for consistency in the project tree

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-language-foundation*
*Context gathered: 2026-02-19*

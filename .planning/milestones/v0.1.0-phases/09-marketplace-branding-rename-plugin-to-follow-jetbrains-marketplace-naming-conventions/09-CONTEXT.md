# Phase 9: Marketplace Branding - Context

**Gathered:** 2026-02-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Rename all plugin identity metadata to follow JetBrains Marketplace naming conventions. The current name "Mako Template Support" uses a generic suffix ("Support") that JetBrains discourages. This phase updates the display name, plugin ID, Gradle group, source packages, and Marketplace listing metadata. No new plugin functionality is added.

</domain>

<decisions>
## Implementation Decisions

### Plugin display name
- New name: **Mako** (clean, follows the pattern of "Rust", "Go", "Python" language plugins)
- Old name to replace: "Mako Template Support"

### Plugin ID
- New plugin ID: **com.schtilig.mako**
- Old ID: com.example.jetbrains-mako-template-plugin
- Change is intentional — this is a pre-Marketplace-submission cleanup, not a post-publish rename

### Build compatibility
- `since-build`: keep at 252 (PyCharm 2025.2)
- `until-build`: **omit** — open-ended compatibility, no upper bound

### Rename scope — what changes
- **Gradle group**: update from `com.example` → `com.schtilig`
- **Gradle rootProject.name**: update from `jetbrains-mako-template-plugin` → `mako`
- **Source packages**: rename `com.example.makotemplateplugin` → `com.schtilig.mako` in all Kotlin source files, including directory structure
- **Test classes**: update package declarations and imports in test source files to reflect new package
- **plugin.xml**: update `<id>`, `<name>`, `<vendor>`, and `<description>` fields

### Version
- Bump version from `0.0.1` → **0.1.0** to mark the rebrand milestone before Marketplace submission

### Marketplace description
- Tone: Claude's discretion — write a standard Marketplace-quality description
- Must mention specific Mako constructs users will recognize: `<%def>`, `<%block>`, `${...}` expressions, `<%inherit>`, control lines (`% for`, `% if`)
- Description should cover all implemented features: syntax highlighting, code folding, structure view, tag completion, error annotations, Python injection

### CHANGELOG
- Add a new entry for 0.1.0 documenting the rebranding and rename

### Vendor metadata
- Vendor name: **Schtilig**
- Vendor URL: **https://github.com/Kimuth/jetbrains-mako-template-plugin**
- Vendor email: omit

### Claude's Discretion
- Exact wording of the Marketplace description (tone, paragraph structure, bullet vs prose)
- Whether to include a "Features" heading or just a flat description
- CHANGELOG entry phrasing

</decisions>

<specifics>
## Specific Ideas

- Plugin ID `com.schtilig.mako` — user provided this exact value
- Vendor name `Schtilig` — user confirmed this handle as the vendor identity
- GitHub URL `https://github.com/Kimuth/jetbrains-mako-template-plugin` — confirmed from git remote

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 09-marketplace-branding-rename-plugin-to-follow-jetbrains-marketplace-naming-conventions*
*Context gathered: 2026-02-21*

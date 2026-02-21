<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mako Changelog

## [Unreleased]

## [0.1.0]
### Changed
- Renamed plugin to **Mako** (was: Mako Template Support) following JetBrains Marketplace naming guidelines
- Plugin ID changed to `com.schtilig.mako` (was: `com.github.kimuth.jetbrainsmakotemplateplugin`)
- Vendor updated to **Schtilig**
- Source packages renamed to `com.schtilig.mako`

## [0.0.1]
### Added
- Syntax highlighting for Mako directives, expressions, control lines, and comments with customizable color scheme
- Code folding for `<%def>`, `<%block>`, control flow blocks (`% for`, `% if`, `% while`); `<%doc>` and `<%!>` fold by default
- Structure view panel showing all `<%def>` and `<%block>` declarations as a navigable tree
- Brace matching between `${` and `}` delimiters
- Line comment toggling (`##`) via Ctrl+/ and block comment toggling (`<%doc>`) via Ctrl+Shift+/
- Python language injection into expression (`${...}`), code block (`<% %>`), and module block (`<%! %>`) regions
- Autocomplete for Mako directive names after `<%` and attribute names inside open tags
- Error annotations for unclosed `<%def>` and `<%block>` tags

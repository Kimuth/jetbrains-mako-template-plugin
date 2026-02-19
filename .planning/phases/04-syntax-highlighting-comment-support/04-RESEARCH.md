# Phase 4: Syntax Highlighting and Comment Support - Research

**Researched:** 2026-02-19
**Domain:** IntelliJ Platform syntax highlighting (SyntaxHighlighter, TextAttributesKey, ColorSettingsPage), PairedBraceMatcher for tag matching, Commenter for line/block comments
**Confidence:** HIGH (core APIs verified via official IntelliJ Platform SDK docs; END_TAG sharing limitation verified via JetBrains community source)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SYNX-01 | Mako directives (`<%def>`, `<%block>`, etc.) are highlighted with distinct colors | `SyntaxHighlighter.getTokenHighlights()` maps `TAG_OPEN_DEF`, `TAG_OPEN_BLOCK`, etc. to distinct `TextAttributesKey` constants backed by `DefaultLanguageHighlighterColors.KEYWORD` |
| SYNX-02 | Mako expressions (`${...}`) are highlighted distinctly from surrounding HTML | `EXPR_START`/`EXPR_END`/`EXPR_CONTENT` tokens map to a `MAKO_EXPRESSION` key backed by `DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR` or `STRING` |
| SYNX-03 | Mako control lines (`% for`, `% if`, etc.) are highlighted | `CONTROL_LINE` single-token maps to `MAKO_CONTROL_LINE` key backed by `DefaultLanguageHighlighterColors.KEYWORD` |
| SYNX-04 | Mako comments (`##` and `<%doc>`) are highlighted as comments | `LINE_COMMENT` maps to `MAKO_LINE_COMMENT`; `DOC_OPEN`/`DOC_CONTENT`/`DOC_CLOSE` map to `MAKO_DOC_COMMENT` — both backed by `DefaultLanguageHighlighterColors.LINE_COMMENT`/`BLOCK_COMMENT` |
| SYNX-05 | Matching Mako tag pairs (`<%def>` / `</%def>`) highlight when cursor is on either | `PairedBraceMatcher` with `BracePair(TAG_OPEN_DEF, END_TAG, false)` etc.; caveat: all pairs share same `END_TAG` closing token — use `structural=false` to avoid platform matching conflicts; full per-tag disambiguation requires per-closing-tag token types (see Open Questions) |
| SYNX-07 | User can customize Mako-specific colors via Settings > Editor > Color Scheme | `ColorSettingsPage` implementation with `AttributesDescriptor` entries per `TextAttributesKey`; registered via `com.intellij.colorSettingsPage` extension point |
| EDIT-01 | User can toggle line comments (`##`) with Ctrl+/ | `Commenter.getLineCommentPrefix()` returns `"##"` ; registered via `com.intellij.lang.commenter` extension point |
| EDIT-02 | User can toggle block comments (`<%doc>...</%doc>`) with Ctrl+Shift+/ | `Commenter.getBlockCommentPrefix()` returns `"<%doc>"`, `getBlockCommentSuffix()` returns `"</%doc>"` |
</phase_requirements>

---

## Summary

Phase 4 adds three independent capabilities: (1) token-level syntax highlighting so Mako constructs display in distinct editor colors, (2) user-configurable color scheme integration via a `ColorSettingsPage`, and (3) comment toggling for line (`##`) and block (`<%doc>`) comments. An optional fourth feature is tag-pair matching (SYNX-05) using `PairedBraceMatcher`.

The lexer from Phase 2/3 already emits all the token types needed: `TAG_OPEN_DEF`, `TAG_OPEN_BLOCK`, etc. distinguish each directive, `EXPR_START`/`EXPR_CONTENT`/`EXPR_END` cover expressions, `CONTROL_LINE` covers control lines, and `LINE_COMMENT`, `DOC_OPEN`/`DOC_CONTENT`/`DOC_CLOSE` cover comments. The `SyntaxHighlighter` simply maps these tokens to `TextAttributesKey` constants. No new lexer or parser changes are needed for SYNX-01 through SYNX-04.

The `PairedBraceMatcher` for SYNX-05 has a design constraint: the lexer emits a single `END_TAG` token type for all closing tags (`</%def>`, `</%block>`, etc.). `BracePair` maps exactly one opening token type to one closing token type; multiple `BracePairs` sharing the same `END_TAG` right-brace can cause platform matching conflicts when `structural=true`. The workaround is `structural=false` for all Mako tag pairs — this gives visual highlighting without the strict structural priority that causes conflicts. See Open Questions for the alternative of splitting `END_TAG` into per-tag closing tokens.

**Primary recommendation:** Implement `MakoSyntaxHighlighter` (extending `SyntaxHighlighterBase`), `MakoSyntaxHighlighterFactory`, `MakoColorSettingsPage`, and `MakoCommenter` as four separate Kotlin objects/classes registered in `plugin.xml`. Add `MakoPairedBraceMatcher` with `structural=false` for all tag pairs using the shared `END_TAG` closing token.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `SyntaxHighlighterBase` | Platform (bundled) | Base class for lexer-based token highlighting | The canonical approach; provides `getHighlightingLexer()` + `getTokenHighlights()` contract |
| `SyntaxHighlighterFactory` | Platform (bundled) | Creates `SyntaxHighlighter` per file/project | Required by `lang.syntaxHighlighterFactory` extension point |
| `TextAttributesKey` | Platform (bundled) | Named color/font attribute used in color schemes | The only platform-approved way to define editor colors |
| `DefaultLanguageHighlighterColors` | Platform (bundled) | Standard fallback keys for inherited color scheme behavior | Using these as fallback ensures the plugin colors look correct in all themes without requiring explicit per-theme overrides |
| `ColorSettingsPage` | Platform (bundled) | Interface for Settings > Editor > Color Scheme panel | Required for SYNX-07; the only mechanism to expose plugin colors in the settings UI |
| `Commenter` | Platform (bundled) | Interface for line and block comment toggling | Directly wires Ctrl+/ and Ctrl+Shift+/ to language-specific prefixes |
| `PairedBraceMatcher` | Platform (bundled) | Interface for bracket/tag pair highlighting | Wires tag-pair highlighting; uses `BracePair` to declare token type pairs |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `MakoLexerAdapter` | In-project (Phase 2) | Provides the highlighting lexer instance | `SyntaxHighlighter.getHighlightingLexer()` must return this |
| `MakoTokenTypes` | In-project (Phase 2) | Token type constants for `getTokenHighlights()` mapping | Every token type mapping in `MakoSyntaxHighlighter` |
| `MakoIcons` | In-project (Phase 1) | File icon reused in `ColorSettingsPage.getIcon()` | `ColorSettingsPage.getIcon()` returns a non-null icon |
| `BasePlatformTestCase` | Platform test framework | Base for highlighting tests | Lexer test verification; `EditorTestUtil.testFileSyntaxHighlighting()` |
| `EditorTestUtil` | Platform test framework | `testFileSyntaxHighlighting()` for answer-file-based highlighting verification | Testing SYNX-01 through SYNX-04 |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `PairedBraceMatcher` with `structural=false` (recommended) | Splitting `END_TAG` into per-closing-tag token types | Splitting gives correct structural matching with no conflicts but requires lexer + tokenSets + MakoTypes.java changes; `structural=false` is simpler and satisfies SYNX-05 visual highlighting |
| `DefaultLanguageHighlighterColors` fallback keys | Hardcoded `TextAttributes` with explicit colors | Fallback keys automatically adapt to IDE themes (light/dark); hardcoded colors break in Darcula or custom themes |
| `Commenter` interface | `SelfManagingCommenter` | `SelfManagingCommenter` needed only for context-sensitive commenting (e.g., commenting changes based on AST position); Mako's `##` and `<%doc>` are fixed prefixes — `Commenter` is sufficient |

---

## Architecture Patterns

### Recommended Project Structure After Phase 4

```
src/main/
├── kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/
│   └── lang/
│       ├── MakoTokenTypes.kt             # Exists — no changes needed
│       ├── MakoTokenSets.kt              # Exists — no changes needed
│       ├── highlighting/                 # NEW package for Phase 4
│       │   ├── MakoSyntaxHighlighter.kt  # NEW: SyntaxHighlighterBase subclass
│       │   ├── MakoSyntaxHighlighterFactory.kt  # NEW: SyntaxHighlighterFactory
│       │   ├── MakoColorSettingsPage.kt  # NEW: ColorSettingsPage for SYNX-07
│       │   └── MakoPairedBraceMatcher.kt # NEW: PairedBraceMatcher for SYNX-05
│       └── editing/                      # NEW package for Phase 4
│           └── MakoCommenter.kt          # NEW: Commenter for EDIT-01, EDIT-02
├── resources/
│   ├── META-INF/plugin.xml               # UPDATED: add 5 new extension registrations
│   └── colorSchemes/                     # NEW: optional default color XML files
│       ├── MakoDefault.xml               # Optional: default colors for Default scheme
│       └── MakoDarcula.xml               # Optional: default colors for Darcula scheme
src/test/
└── kotlin/.../lang/
    └── MakoSyntaxHighlightingTest.kt     # NEW: EditorTestUtil-based highlighting test
```

### Pattern 1: SyntaxHighlighter Token Mapping

**What:** `MakoSyntaxHighlighter` extends `SyntaxHighlighterBase`, defines `TextAttributesKey` constants, and maps token types to those keys in `getTokenHighlights()`.

**When to use:** This is the only mechanism for lexer-level token highlighting. Every distinct visual category needs its own `TextAttributesKey`.

**Example (Kotlin):**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html
// Translated to Kotlin from the official Simple Language tutorial example

package com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoLexerAdapter
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class MakoSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        @JvmField val MAKO_DIRECTIVE = createTextAttributesKey(
            "MAKO_DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmField val MAKO_EXPRESSION = createTextAttributesKey(
            "MAKO_EXPRESSION", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR)
        @JvmField val MAKO_CONTROL_LINE = createTextAttributesKey(
            "MAKO_CONTROL_LINE", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmField val MAKO_LINE_COMMENT = createTextAttributesKey(
            "MAKO_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        @JvmField val MAKO_BLOCK_COMMENT = createTextAttributesKey(
            "MAKO_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        @JvmField val MAKO_TAG_ATTR_NAME = createTextAttributesKey(
            "MAKO_TAG_ATTR_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        @JvmField val MAKO_TAG_ATTR_VALUE = createTextAttributesKey(
            "MAKO_TAG_ATTR_VALUE", DefaultLanguageHighlighterColors.STRING)
        @JvmField val MAKO_TAG_CLOSE = createTextAttributesKey(
            "MAKO_TAG_CLOSE", DefaultLanguageHighlighterColors.BRACKETS)
        @JvmField val MAKO_CODE_CONTENT = createTextAttributesKey(
            "MAKO_CODE_CONTENT", DefaultLanguageHighlighterColors.STRING)

        private val DIRECTIVE_KEYS = arrayOf(MAKO_DIRECTIVE)
        private val EXPRESSION_KEYS = arrayOf(MAKO_EXPRESSION)
        private val CONTROL_KEYS = arrayOf(MAKO_CONTROL_LINE)
        private val LINE_COMMENT_KEYS = arrayOf(MAKO_LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(MAKO_BLOCK_COMMENT)
        private val TAG_ATTR_NAME_KEYS = arrayOf(MAKO_TAG_ATTR_NAME)
        private val TAG_ATTR_VALUE_KEYS = arrayOf(MAKO_TAG_ATTR_VALUE)
        private val TAG_CLOSE_KEYS = arrayOf(MAKO_TAG_CLOSE)
        private val CODE_CONTENT_KEYS = arrayOf(MAKO_CODE_CONTENT)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = MakoLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        // Directives (opening tags)
        MakoTokenTypes.TAG_OPEN_DEF,
        MakoTokenTypes.TAG_OPEN_BLOCK,
        MakoTokenTypes.TAG_OPEN_INHERIT,
        MakoTokenTypes.TAG_OPEN_INCLUDE,
        MakoTokenTypes.TAG_OPEN_NAMESPACE,
        MakoTokenTypes.TAG_OPEN_PAGE,
        MakoTokenTypes.END_TAG,
        MakoTokenTypes.CODE_OPEN,
        MakoTokenTypes.MODULE_OPEN -> DIRECTIVE_KEYS

        // Expression delimiters and content
        MakoTokenTypes.EXPR_START,
        MakoTokenTypes.EXPR_END,
        MakoTokenTypes.EXPR_CONTENT,
        MakoTokenTypes.FILTER_SEP,
        MakoTokenTypes.FILTER_NAME -> EXPRESSION_KEYS

        // Control lines
        MakoTokenTypes.CONTROL_LINE -> CONTROL_KEYS

        // Line comments
        MakoTokenTypes.LINE_COMMENT -> LINE_COMMENT_KEYS

        // Doc comments (block comments)
        MakoTokenTypes.DOC_OPEN,
        MakoTokenTypes.DOC_CONTENT,
        MakoTokenTypes.DOC_CLOSE -> BLOCK_COMMENT_KEYS

        // Tag structural tokens
        MakoTokenTypes.TAG_ATTR_NAME -> TAG_ATTR_NAME_KEYS
        MakoTokenTypes.TAG_ATTR_VALUE -> TAG_ATTR_VALUE_KEYS
        MakoTokenTypes.TAG_CLOSE,
        MakoTokenTypes.CODE_CLOSE -> TAG_CLOSE_KEYS

        // Code content
        MakoTokenTypes.CODE_CONTENT,
        MakoTokenTypes.MODULE_CONTENT -> CODE_CONTENT_KEYS

        // Template text: no highlighting (rendered as plain HTML text)
        else -> EMPTY_KEYS
    }
}
```

**Key constraint:** `TextAttributesKey.createTextAttributesKey(name, fallback)` — the `name` string must be globally unique across all plugins. Use the `MAKO_` prefix for all Mako keys to prevent collisions.

### Pattern 2: SyntaxHighlighterFactory Registration

**What:** A factory class that the platform instantiates per-file to obtain the `SyntaxHighlighter`.

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html
class MakoSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return MakoSyntaxHighlighter()
    }
}
```

**plugin.xml registration:**
```xml
<lang.syntaxHighlighterFactory language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting.MakoSyntaxHighlighterFactory"/>
```

**Note:** The `language` attribute must exactly match the string `"Mako Template"` from `MakoLanguage` — the same ID used in all other extension point registrations.

### Pattern 3: ColorSettingsPage for SYNX-07

**What:** `ColorSettingsPage` exposes Mako's `TextAttributesKey` constants in the Settings > Editor > Color Scheme panel.

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html
class MakoColorSettingsPage : ColorSettingsPage {

    private companion object {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Directive (<%def>, <%block>, ...)", MakoSyntaxHighlighter.MAKO_DIRECTIVE),
            AttributesDescriptor("Expression (\${...})", MakoSyntaxHighlighter.MAKO_EXPRESSION),
            AttributesDescriptor("Control line (% for, % if, ...)", MakoSyntaxHighlighter.MAKO_CONTROL_LINE),
            AttributesDescriptor("Line comment (## ...)", MakoSyntaxHighlighter.MAKO_LINE_COMMENT),
            AttributesDescriptor("Block comment (<%doc>)</doc>)", MakoSyntaxHighlighter.MAKO_BLOCK_COMMENT),
            AttributesDescriptor("Tag attribute name", MakoSyntaxHighlighter.MAKO_TAG_ATTR_NAME),
            AttributesDescriptor("Tag attribute value", MakoSyntaxHighlighter.MAKO_TAG_ATTR_VALUE),
            AttributesDescriptor("Code block content", MakoSyntaxHighlighter.MAKO_CODE_CONTENT),
        )
    }

    override fun getIcon(): Icon = MakoIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = MakoSyntaxHighlighter()
    override fun getDisplayName(): String = "Mako Template"
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getDemoText(): String = """
<%inherit file="base.html"/>
<%def name="greet">
Hello ${'$'}{name | h}!
</%def>
## This is a line comment
<%doc>This is a block comment</%doc>
% for item in items:
    ${'$'}{item}
% endfor
<% x = 1 %>
""".trimIndent()
}
```

**plugin.xml registration:**
```xml
<colorSettingsPage
    implementation="com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting.MakoColorSettingsPage"/>
```

**Note:** `getDisplayName()` is the string shown in the left-hand tree under "Settings > Editor > Color Scheme". It does not need to match the language ID — it is the user-visible category label.

### Pattern 4: Commenter for EDIT-01 and EDIT-02

**What:** `Commenter` implementation returns the comment prefix strings. The platform handles all insertion/removal logic — the plugin only declares the strings.

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/commenter.html
class MakoCommenter : Commenter {
    // EDIT-01: ## is the Mako single-line comment prefix
    override fun getLineCommentPrefix(): String = "##"

    // EDIT-02: <%doc> ... </%doc> is the block comment
    override fun getBlockCommentPrefix(): String = "<%doc>"
    override fun getBlockCommentSuffix(): String = "</%doc>"

    // These return null unless the language has a way to comment out already-commented block delimiters
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
```

**plugin.xml registration:**
```xml
<lang.commenter language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.editing.MakoCommenter"/>
```

**Important behavior note:** The platform's "Comment with Line Comment" action (Ctrl+/) prepends `getLineCommentPrefix()` to the beginning of each selected line and removes it if already present (toggle behavior). "Comment with Block Comment" (Ctrl+Shift+/) wraps the selection with `getBlockCommentPrefix()` + selection + `getBlockCommentSuffix()`.

### Pattern 5: PairedBraceMatcher for SYNX-05

**What:** `PairedBraceMatcher` declares token-type pairs for the IDE to highlight when the cursor is on an opening or closing token.

**Critical constraint for Mako:** The lexer emits a single `END_TAG` token for all closing tags (`</%def>`, `</%block>`, `</%doc>`, etc.). `BracePair` maps one opening type to one closing type. If multiple `BracePair` entries share the same right-brace token type with `structural=true`, the platform matching algorithm produces incorrect results. Use `structural=false` for all Mako pairs.

**Example:**
```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html
class MakoPairedBraceMatcher : PairedBraceMatcher {

    companion object {
        // structural=false because END_TAG is shared by all closing tags.
        // Multiple BracePairs with the same right-brace token and structural=true
        // causes platform matching conflicts (verified via JetBrains support forums).
        private val PAIRS = arrayOf(
            BracePair(MakoTokenTypes.TAG_OPEN_DEF,       MakoTokenTypes.END_TAG, false),
            BracePair(MakoTokenTypes.TAG_OPEN_BLOCK,     MakoTokenTypes.END_TAG, false),
            BracePair(MakoTokenTypes.DOC_OPEN,           MakoTokenTypes.DOC_CLOSE, false),
            BracePair(MakoTokenTypes.EXPR_START,         MakoTokenTypes.EXPR_END, false),
            BracePair(MakoTokenTypes.CODE_OPEN,          MakoTokenTypes.CODE_CLOSE, false),
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS

    // Return true to allow auto-insertion of matching brace (not required for SYNX-05, safe to enable)
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    // Return the offset of the opening brace itself (no enclosing construct to navigate to)
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
```

**plugin.xml registration:**
```xml
<lang.braceMatcher language="Mako Template"
    implementationClass="com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting.MakoPairedBraceMatcher"/>
```

**Note on multi-character tokens:** The platform docs state "it is possible to return multi-character tokens... but the highlighting for such braces will not be entirely correct." For our case, `TAG_OPEN_DEF` is a multi-character token (`<%def`). The brace is highlighted but the visual indicator may appear at the start of the token text rather than at a single bracket character. This is acceptable for SYNX-05's requirement ("highlights its matching tag").

### Pattern 6: Optional Default Color Scheme Files

**What:** Bundle default colors for "Default" and "Darcula" schemes so the highlighter looks good before the user customizes anything, without hardcoding colors in `TextAttributesKey` fallbacks.

**plugin.xml registration:**
```xml
<additionalTextAttributes scheme="Default" file="colorSchemes/MakoDefault.xml"/>
<additionalTextAttributes scheme="Darcula" file="colorSchemes/MakoDarcula.xml"/>
```

**File location:** `src/main/resources/colorSchemes/MakoDefault.xml`

**Note:** The file path must be globally unique — the `Mako` prefix prevents collision with other plugins. Omitting this is acceptable if `DefaultLanguageHighlighterColors` fallbacks give satisfactory colors (they usually do).

### Anti-Patterns to Avoid

- **Using `TextAttributesKey.createTextAttributesKey(name)` without a fallback key:** Results in colors that default to invisible in some themes. Always provide a `DefaultLanguageHighlighterColors` fallback.
- **Naming `TextAttributesKey` without language prefix:** Names like `COMMENT` or `KEYWORD` will silently collide with other plugins or platform keys. Always use `MAKO_` prefix.
- **Returning `TEMPLATE_TEXT` token type from `getTokenHighlights()` with a highlight key:** Template text (plain HTML) should return `EMPTY_KEYS` — the HTML layer handles HTML coloring; Mako's highlighter should leave plain content uncolored.
- **Setting `structural=true` with a shared right-brace token type:** Multiple `BracePair` entries with the same `rightBrace` type and `structural=true` is documented to cause incorrect matching behavior. Use `structural=false` for all Mako pairs.
- **Implementing `SelfManagingCommenter` instead of `Commenter`:** Overkill for fixed prefix strings. `SelfManagingCommenter` is for context-sensitive commenting where the comment style changes based on where the cursor is in the AST.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Ctrl+/ line comment toggling | Custom `EditorAction` that inspects the line and inserts/removes `##` | `Commenter.getLineCommentPrefix()` returning `"##"` | The platform action handles toggling, selection ranges, multi-line comments, and undo — all for free |
| Ctrl+Shift+/ block comment wrapping | Custom `AnAction` that wraps selection with `<%doc>...</%doc>` | `Commenter.getBlockCommentPrefix()` + `getBlockCommentSuffix()` | The platform handles selection detection, wrapping, toggle (unwrapping), and undo |
| Color scheme settings UI | Custom settings panel with color pickers | `ColorSettingsPage` + `AttributesDescriptor` | The platform provides the entire Settings > Editor > Color Scheme UI; the plugin only declares descriptors and demo text |
| Per-theme color definitions | If/else on theme name to pick colors at runtime | `TextAttributesKey` with `DefaultLanguageHighlighterColors` fallback | The color scheme inheritance chain handles theme adaptation automatically |
| Tag matching highlighting | Custom `EditorFactoryListener` + caret tracking | `PairedBraceMatcher` with `BracePair` | The platform's `BraceHighlightingHandler` handles highlighting, caret tracking, and the visual indicator entirely |

**Key insight:** Phase 4 is almost entirely declarative — the plugin declares what tokens look like and what comment strings are; the platform implements all user interaction.

---

## Common Pitfalls

### Pitfall 1: Language ID Mismatch in Extension Point Registration

**What goes wrong:** Syntax highlighting, brace matching, or commenter silently fails to activate — files tokenize but no colors appear, or Ctrl+/ does nothing.

**Why it happens:** Every `language="..."` attribute in plugin.xml extensions must exactly match the string `"Mako Template"` passed to the `Language` constructor in `MakoLanguage.kt`. A single character difference causes silent registration failure.

**How to avoid:** Search plugin.xml for all `language=` attributes and verify each is `language="Mako Template"`. The existing `lang.parserDefinition` registration uses this string — copy it exactly.

**Warning signs:** Syntax highlighting appears to work for `.mako` files (the file type is detected) but colors are not applied; or Ctrl+/ has no effect on Mako files.

### Pitfall 2: TextAttributesKey Name Collision

**What goes wrong:** Two plugins define a `TextAttributesKey` with the same name string. The later-loaded plugin's key silently inherits the first plugin's attributes, producing incorrect colors. In worst case, the entire color scheme preference for that key is lost.

**Why it happens:** `TextAttributesKey.createTextAttributesKey` uses a global registry keyed by the name string. The platform does not scope by plugin.

**How to avoid:** Always prefix with `MAKO_` (e.g., `MAKO_DIRECTIVE`, `MAKO_EXPRESSION`). Never use generic names like `KEYWORD`, `COMMENT`, `STRING`.

**Warning signs:** Colors behave differently than configured; enabling/disabling the plugin changes colors for unrelated languages.

### Pitfall 3: Returning TEMPLATE_TEXT With a Highlight Key

**What goes wrong:** Plain HTML content in `.mako` files gets colored by the Mako highlighter, overriding or conflicting with HTML syntax highlighting from the embedded HTML language support.

**Why it happens:** `TEMPLATE_TEXT` tokens represent the pass-through HTML content. If `getTokenHighlights(TEMPLATE_TEXT)` returns a non-empty key, the Mako layer applies color on top of what the HTML layer would provide.

**How to avoid:** Return `EMPTY_KEYS` (empty `Array<TextAttributesKey>`) for `TEMPLATE_TEXT` and `TokenType.WHITE_SPACE`. The HTML layer handles coloring for those ranges.

**Warning signs:** HTML tags inside `.mako` files appear in the wrong color, or all plain HTML text gets a Mako-specific color.

### Pitfall 4: PairedBraceMatcher With Shared END_TAG and structural=true

**What goes wrong:** Brace matching highlights the wrong pair or highlights no pair. In some cases, cursor on `<%def` highlights a `</%block>` instead of the matching `</%def>`.

**Why it happens:** The platform's brace matching algorithm cannot correctly handle multiple `BracePair` entries sharing the same right-brace `IElementType` with `structural=true`. The structural priority logic assumes a one-to-one relationship between left-brace and right-brace types.

**How to avoid:** Set `structural=false` for all Mako `BracePair` entries (as shown in the code example above). This still provides visual highlighting of matching pairs but without structural priority conflicts.

**Warning signs:** Brace highlighting jumps to incorrect partner tags; or disabling one BracePair definition fixes highlighting for one tag type but breaks it for others.

### Pitfall 5: getHighlightingLexer() Returns Wrong Lexer State

**What goes wrong:** The syntax highlighter correctly colors the first construct in a file, then all subsequent tokens appear in the wrong color or as `TEMPLATE_TEXT`.

**Why it happens:** The highlighting lexer is created fresh via `MakoLexerAdapter()` in `getHighlightingLexer()`. If the lexer is created in a non-initial state (e.g., a previously used instance is returned), it may continue from a mid-file state.

**How to avoid:** Always call `MakoLexerAdapter()` (a new instance) in `getHighlightingLexer()`. Never cache and reuse a lexer instance across highlighting passes.

**Warning signs:** Opening a multi-construct `.mako` file shows correct colors for the first construct but wrong colors or plain text for everything after the first expression or tag.

### Pitfall 6: Commenter Block Comment Has Incorrect Suffix Format

**What goes wrong:** Ctrl+Shift+/ inserts `<%doc>` at the start but uses a malformed or empty suffix, leaving the file unparseable.

**Why it happens:** `getBlockCommentSuffix()` is sometimes confused with `getCommentedBlockCommentSuffix()`. The former is the block end delimiter; the latter is for when the block comment delimiters themselves are commented out (rare).

**How to avoid:** `getBlockCommentSuffix()` must return `"</%doc>"` (with the `</` prefix and closing `>`). Verify by testing: select Mako code, press Ctrl+Shift+/, verify `<%doc>` appears before and `</%doc>` appears after.

**Warning signs:** Ctrl+Shift+/ on Mako code inserts `<%doc>` at start but nothing at end, or inserts the wrong closing string.

---

## Code Examples

Verified patterns from official sources:

### Complete plugin.xml Extensions Block for Phase 4

```xml
<!-- Source: https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html -->
<!-- Source: https://plugins.jetbrains.com/docs/intellij/commenter.html -->
<!-- Source: https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html -->
<extensions defaultExtensionNs="com.intellij">
    <!-- Existing from Phases 1-3 -->
    <fileType name="Mako Template" implementationClass="...MakoFileType" fieldName="INSTANCE"
              language="Mako Template" extensions="mako;mak" patterns="*.html.mako"/>
    <lang.parserDefinition language="Mako Template"
        implementationClass="...MakoParserDefinition"/>

    <!-- Phase 4: Syntax Highlighting -->
    <lang.syntaxHighlighterFactory language="Mako Template"
        implementationClass="...lang.highlighting.MakoSyntaxHighlighterFactory"/>
    <colorSettingsPage
        implementation="...lang.highlighting.MakoColorSettingsPage"/>

    <!-- Phase 4: Tag Pair Matching (SYNX-05) -->
    <lang.braceMatcher language="Mako Template"
        implementationClass="...lang.highlighting.MakoPairedBraceMatcher"/>

    <!-- Phase 4: Comment Toggling (EDIT-01, EDIT-02) -->
    <lang.commenter language="Mako Template"
        implementationClass="...lang.editing.MakoCommenter"/>
</extensions>
```

### Minimal MakoCommenter.kt

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/commenter.html
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.editing

import com.intellij.lang.Commenter

class MakoCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "##"
    override fun getBlockCommentPrefix(): String = "<%doc>"
    override fun getBlockCommentSuffix(): String = "</%doc>"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
```

### Minimal MakoPairedBraceMatcher.kt

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.highlighting

import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class MakoPairedBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(MakoTokenTypes.TAG_OPEN_DEF,   MakoTokenTypes.END_TAG,   false),
        BracePair(MakoTokenTypes.TAG_OPEN_BLOCK, MakoTokenTypes.END_TAG,   false),
        BracePair(MakoTokenTypes.DOC_OPEN,       MakoTokenTypes.DOC_CLOSE, false),
        BracePair(MakoTokenTypes.EXPR_START,     MakoTokenTypes.EXPR_END,  false),
        BracePair(MakoTokenTypes.CODE_OPEN,      MakoTokenTypes.CODE_CLOSE,false),
    )
    override fun getPairs(): Array<BracePair> = pairs
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
```

### Syntax Highlighting Test Pattern

```kotlin
// Source: https://plugins.jetbrains.com/docs/intellij/testing-highlighting.html
// MakoSyntaxHighlightingTest.kt
class MakoSyntaxHighlightingTest : BasePlatformTestCase() {

    fun testDirectiveHighlighting() {
        // EditorTestUtil.testFileSyntaxHighlighting compares against an answer file
        // Answer file format: token_value MAKO_DIRECTIVE => KEYWORD
        // First run generates the answer file automatically
        myFixture.configureByText("test.mako", "<%def name=\"greet\">Hello</%def>")
        myFixture.checkHighlighting()
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ColorSettingsPage.createInfoAnnotation()` on `AnnotationHolder` | `AnnotationHolder.newSilentAnnotation(INFORMATION).textAttributes(...).create()` | IntelliJ 2020.x | The old `createInfoAnnotation()` is deprecated; use the builder API |
| `Commenter` implementation checked for null in platform code | Null-safety: `getBlockCommentPrefix()` returning `null` disables block comment | Always | Return `null` from `getBlockCommentSuffix()` etc. when unsupported — not an empty string |
| `SyntaxHighlighter.getHighlights()` (old name) | `SyntaxHighlighter.getTokenHighlights()` | Very old | Method was renamed; use `getTokenHighlights()` |

**Deprecated/outdated:**
- `AnnotationHolder.createInfoAnnotation(element, "")` + `annotation.setTextAttributes(key)`: Deprecated; replaced by `holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(...).textAttributes(key).create()`.
- `SyntaxHighlighterFactory` as a static factory method: The `SyntaxHighlighterFactory` class with `getSyntaxHighlighter(Project, VirtualFile)` is the current correct approach; older patterns registered the highlighter directly without a factory.

---

## Open Questions

1. **Should `END_TAG` be split into per-tag closing token types for correct structural brace matching?**
   - What we know: The current `END_TAG` token is a single token type for all closing tags (`</%def>`, `</%block>`, etc.). With `structural=false` in all `BracePair` entries, the visual highlighting works but the matching is "best effort" — the IDE may highlight the nearest `END_TAG` without knowing which specific tag name it closes. With `structural=true`, a shared right-brace token causes platform conflicts.
   - What's unclear: Whether SYNX-05's requirement ("placing the cursor on `<%def` highlights its matching `</%def>`") demands per-tag precision or whether any `END_TAG` highlight is sufficient. The lexer regex `"</" "%" [a-zA-Z]+ ">"` matches all closing tags identically.
   - Recommendation: Start with `structural=false` and shared `END_TAG` (simpler, satisfies the visual requirement). If the user reports that cursor on `<%def` highlights the wrong closing tag when nested defs are used, split `END_TAG` into `END_TAG_DEF`, `END_TAG_BLOCK` in the lexer — a targeted lexer change that does not require BNF changes (since the grammar already uses `END_TAG` generically and relies on structure, not token text).

2. **Should the `ColorSettingsPage` provide bundled default color XML files via `additionalTextAttributes`?**
   - What we know: Without bundled XML files, colors fall back through the `DefaultLanguageHighlighterColors` chain. This produces reasonable colors automatically (e.g., `MAKO_DIRECTIVE` backed by `KEYWORD` will use the theme's keyword color). The `additionalTextAttributes` approach lets the plugin author define precise, differentiated colors for each Mako construct.
   - What's unclear: Whether the `DefaultLanguageHighlighterColors` fallbacks produce sufficiently distinct colors for all Mako categories (e.g., directives and control lines both fall back to `KEYWORD` and would be visually identical without customization).
   - Recommendation: Do NOT bundle color XMLs in Phase 4. Use only `DefaultLanguageHighlighterColors` fallbacks. This is simpler and the user can customize via Settings > Editor > Color Scheme if they want distinct colors. If testing reveals directives and control lines are indistinguishable, choose different fallback keys (e.g., `KEYWORD` for directives, `LABEL` for control lines).

3. **How should `MakoSyntaxHighlighter.getHighlightingLexer()` relate to `MakoLexerAdapter`?**
   - What we know: `getHighlightingLexer()` must return a fresh `Lexer` instance each time it is called. The highlighting lexer is called in a different context than the parser lexer — it may be called repeatedly and on partial file content. `MakoLexerAdapter` wraps the JFlex `_MakoLexer` and supports restart semantics (the lexer test `testRestartStateAfterExpression` verifies this).
   - What's unclear: Whether the highlighting context ever calls `getHighlightingLexer()` with a non-zero start state (e.g., to re-highlight from the middle of a file). JFlex lexers with state support `restart()` via the `FlexLexer` interface.
   - Recommendation: Return `MakoLexerAdapter()` (new instance, state 0). The existing `MakoLexerAdapter` already implements `FlexAdapter` which handles restart correctly. No changes needed to the lexer for highlighting.

4. **Should `MakoCommenter` use `getLineCommentPrefix()` returning `"## "` (with space) or `"##"` (without)?**
   - What we know: The Mako language specification uses `##` as the comment marker; a space after `##` is conventional but not required. The platform inserts the prefix verbatim — `"## "` produces `## comment` while `"##"` produces `##comment`.
   - What's unclear: Whether Mako's parser accepts `##comment` (without space) as a valid comment. Looking at the lexer rule: `"##" [^\r\n]*` — the content after `##` can be anything, so `##comment` is valid.
   - Recommendation: Return `"## "` (with trailing space) for `getLineCommentPrefix()`. This matches common Mako convention and is how PyCharm implements Python's `#` comment prefix (with space). The platform toggles by checking if the line starts with the exact prefix string, so using `"## "` will only uncomment lines that start with `## ` (with space), which is the standard form.

---

## Sources

### Primary (HIGH confidence)
- https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html — `SyntaxHighlighterBase`, `SyntaxHighlighterFactory`, `TextAttributesKey.createTextAttributesKey()`, `DefaultLanguageHighlighterColors` usage, `ColorSettingsPage` interface with `AttributesDescriptor`, `getDemoText()`, plugin.xml registration for `lang.syntaxHighlighterFactory` and `colorSettingsPage`
- https://plugins.jetbrains.com/docs/intellij/commenter.html — `Commenter` interface, `getLineCommentPrefix()`, `getBlockCommentPrefix()`, `getBlockCommentSuffix()`, plugin.xml `lang.commenter` registration, Ctrl+/ and Ctrl+Shift+/ binding
- https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html — `PairedBraceMatcher` interface, `BracePair` constructor (leftBrace, rightBrace, structural), multi-character token limitation, `lang.braceMatcher` plugin.xml registration
- https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/Commenter.java — Full `Commenter` interface method signatures, related interfaces (SelfManagingCommenter, CommenterWithLineSuffix)
- https://plugins.jetbrains.com/docs/intellij/color-scheme-management.html — `TextAttributesKey` inheritance chain, `additionalTextAttributes` extension point, uniqueness requirement for key names
- https://plugins.jetbrains.com/docs/intellij/testing-highlighting.html — `EditorTestUtil.testFileSyntaxHighlighting()`, answer file format, auto-generation on first run

### Secondary (MEDIUM confidence)
- https://plugins.jetbrains.com/docs/intellij/annotator.html — `AnnotationHolder.newSilentAnnotation()` builder API for PSI-level highlighting (alternative to lexer-level for complex cases)
- JetBrains support community: "WdlTypes.RBRACE token as a member of two different brace pairs... platform code cannot handle this case correctly" — confirmed the `structural=false` workaround for shared right-brace tokens; multiple community posts corroborate this

### Tertiary (LOW confidence)
- WebSearch results on template language (Velocity, FreeMarker) PairedBraceMatcher examples — no specific implementations found; confidence that our approach is correct comes from platform API docs, not community examples

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — `SyntaxHighlighterBase`, `TextAttributesKey`, `ColorSettingsPage`, `Commenter` all verified via official IntelliJ Platform SDK docs with full code examples
- Architecture patterns: HIGH — All four main implementation patterns (highlighter, factory, settings page, commenter) match official tutorial code; `PairedBraceMatcher` pattern verified via official docs and JFlex BracePair source
- Pitfalls: HIGH for language ID mismatch, key naming collision, TEMPLATE_TEXT handling; MEDIUM for PairedBraceMatcher shared END_TAG (confirmed as problematic by community but no official doc explicitly states the `structural=false` workaround)

**Research date:** 2026-02-19
**Valid until:** 2026-05-19 (these APIs are stable across major IntelliJ Platform versions; `DefaultLanguageHighlighterColors` constants and `Commenter` interface have not changed in 5+ years)

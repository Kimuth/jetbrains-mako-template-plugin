# Architecture

**Analysis Date:** 2026-02-21

## Pattern Overview

**Overall:** Language Plugin Architecture for JetBrains IDE

This is a JetBrains IDE plugin providing language support for the Mako template engine. The architecture follows the IntelliJ Platform's language plugin pattern: parse Mako templates into a PSI (Program Structure Interface) tree, then layer IDE features (syntax highlighting, code folding, structure view) on top.

**Key Characteristics:**
- **Lexer-driven parsing**: JFlex lexer (`_MakoLexer`) tokenizes input into language-specific tokens (TAG_OPEN_DEF, EXPR_START, CONTROL_LINE, etc.)
- **State-based grammar**: Lexer maintains context states (EXPRESSION, TAG_ATTRS, CODE_BLOCK, etc.) to track nesting and context
- **BNF parser**: GrammarKit-generated parser (`MakoParser`) builds typed PSI tree from tokens
- **PSI tree with mixins**: Named elements (def/block tags) extend generated PSI implementations with `PsiNamedElement` via mixins
- **Service layer**: Multiple feature builders (highlighter, folder, commenter, structure view) process the PSI tree

## Layers

**Token Definition Layer:**
- Purpose: Define leaf-level tokens for all Mako syntax (expressions, tags, comments, code blocks)
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoTokenTypes.kt`
- Contains: 27 custom token type definitions (EXPR_START, TAG_OPEN_DEF, CONTROL_LINE, etc.)
- Depends on: MakoLanguage
- Used by: Lexer, parser, all feature builders

**Lexer Layer:**
- Purpose: Tokenize Mako template text into a stream of tokens with JFlex state management
- Location: `src/main/grammars/MakoLexer.flex` (source), `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/_MakoLexer.java` (generated)
- Contains: Lexer states (YYINITIAL, EXPRESSION, TAG_ATTRS, CODE_BLOCK, MODULE_BLOCK, DOC_COMMENT), brace depth tracking
- Key responsibility: Track nested braces `{}` in `${...}` expressions to correctly emit EXPR_END token
- Depends on: MakoTokenTypes
- Used by: MakoLexerAdapter, parser

**Lexer Adapter Layer:**
- Purpose: Wrap JFlex lexer to encode/decode brace depth state for incremental re-lexing
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoLexerAdapter.kt`
- Contains: State encoding logic (bits 0-3 for JFlex state, bits 4-7 for brace depth)
- Key responsibility: Preserve brace depth across IDE incremental lexing (e.g., when user edits nested expressions)
- Depends on: _MakoLexer
- Used by: MakoParserDefinition

**Parser Layer:**
- Purpose: Build typed PSI tree from token stream using BNF grammar
- Location: `src/main/grammars/Mako.bnf` (source), `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java` (generated)
- Contains: 17 grammar rules (makoFile, def_tag, block_tag, expression, control_line_stmt, code_block, module_block, etc.)
- Key responsibility: Map tokens to typed PSI nodes with recovery rules for malformed input
- Recovery predicates: tag_recover, expression_recover stop parsing at construct boundaries (TEMPLATE_TEXT, EXPR_END, END_TAG, etc.)
- Depends on: MakoTokenTypes, MakoParser
- Used by: MakoParserDefinition

**PSI Tree Layer:**
- Purpose: Represent parsed Mako template as an in-memory tree accessible to IDE features
- Location: `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/` (interfaces), `src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/` (generated implementations)
- Contains: PSI element interfaces (MakoFile, MakoDefTag, MakoBlockTag, MakoExpression, MakoControlLineStmt, etc.)
- Named elements: MakoDefTag, MakoBlockTag implement PsiNamedElement via mixins (`MakoDefTagMixin`, `MakoBlockTagMixin`)
- Depends on: MakoParser, GrammarKit code generation
- Used by: All feature builders (highlighter, folder, structure view, commenter)

**PSI Mixin Layer:**
- Purpose: Extend generated PSI implementations with custom behavior for named elements
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoDefTagMixin.kt`, `MakoBlockTagMixin.kt`
- Contains: Implementations of `getName()` (extracts name from TAG_ATTR_VALUE), `setName()` (stub for phase 3)
- Key responsibility: Extract def/block names from parsed attributes without string parsing
- Depends on: MakoTokenTypes, PsiNamedElement interface
- Used by: IDE navigation, refactoring tools, structure view

**Parser Definition Layer:**
- Purpose: Bridge between JetBrains platform and Mako language lexer/parser
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt`
- Contains: Implementation of ParserDefinition (createLexer, createParser, createElement, getWhitespaceTokens, getCommentTokens)
- Responsibility: Instantiate lexer and parser, define whitespace/comment token sets, create file root PSI node
- Depends on: MakoLexerAdapter, MakoParser, MakoFile, MakoTokenSets
- Used by: JetBrains platform (lang.parserDefinition extension)

**Language Definition Layer:**
- Purpose: Define Mako as a language for JetBrains platform
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/MakoLanguage.kt`
- Contains: Language singleton, extends TemplateLanguage
- Responsibility: Register language ID ("Mako Template") and signal this is a template language
- Depends on: JetBrains Language, TemplateLanguage
- Used by: Token types, parser definition, extensions

**Feature Builder Layer:**
- Purpose: Process PSI tree to provide IDE features (syntax highlighting, code folding, structure view, etc.)
- Locations:
  - `src/main/kotlin/.../lang/highlighting/MakoSyntaxHighlighter.kt` - Token-to-color mapping
  - `src/main/kotlin/.../lang/folding/MakoFoldingBuilder.kt` - Foldable regions (tags, comments, control flow)
  - `src/main/kotlin/.../lang/structure/MakoStructureViewFactory.kt` - File outline (def/block tags only)
  - `src/main/kotlin/.../lang/editing/MakoCommenter.kt` - Comment/uncomment (##, <%doc>)
  - `src/main/kotlin/.../lang/highlighting/MakoPairedBraceMatcher.kt` - Tag pair matching (<%def > ... </%def>)
- Depends on: PSI tree, MakoTokenTypes
- Used by: JetBrains platform (lang.syntaxHighlighterFactory, lang.foldingBuilder, etc.)

## Data Flow

**Parsing Flow:**

1. **Lexical Analysis**: `MakoLexerAdapter.start()` initializes `_MakoLexer` with brace depth from prior state
2. **Tokenization**: `_MakoLexer.advance()` scans input and emits tokens (EXPR_START, TAG_OPEN_DEF, TEMPLATE_TEXT, etc.)
3. **State Management**: Lexer tracks nested braces in EXPRESSION state; adapter encodes depth into state integer
4. **Incremental Re-lexing**: IDE calls `start()` with saved state; depth is restored, fixing nested expression re-lex
5. **Parsing**: `MakoParser.parse()` consumes token stream, builds typed PSI nodes per Mako.bnf grammar
6. **Error Recovery**: On malformed input, recovery predicates (tag_recover, expression_recover) skip tokens until boundary
7. **PSI Tree**: Complete tree rooted at MakoFile, with typed children (MakoDefTag, MakoExpression, etc.)

**Feature Application Flow:**

1. **File opened**: Platform calls `MakoParserDefinition.createFile()` → MakoFile PSI node created
2. **Highlighting**: Editor invokes `MakoSyntaxHighlighter.getTokenHighlights()` for each token → TextAttributesKey array
3. **Folding**: `MakoFoldingBuilder.buildFoldRegions()` scans PSI tree, builds FoldingDescriptor array
4. **Structure View**: `MakoStructureViewFactory.getStructureViewBuilder()` → `MakoStructureViewModel` filters defs/blocks
5. **Comment Toggle**: User presses Ctrl+/: `MakoCommenter.getLineCommentPrefix()` returns "## "

**State Management:**

- **Lexer State**: JFlex state (YYINITIAL=0, EXPRESSION=1, TAG_ATTRS=2, etc.) + brace depth (0-15) packed into 8-bit integer
- **Parser State**: GrammarKit parser maintains pinning and recovery state internally; no public API
- **PSI State**: Immutable after parsing; IDE invalidates on file edit, re-parses dirty range
- **Named Elements**: MakoDefTag.getName() caches result; MakoBlockTag.getName() ditto (no mutable state)

## Key Abstractions

**Token Types Abstraction:**
- Purpose: Centralize token identity definitions
- Examples: `MakoTokenTypes.EXPR_START`, `MakoTokenTypes.TAG_OPEN_DEF`, `MakoTokenTypes.CONTROL_LINE`
- Pattern: Singleton object with `@JvmField` constants for JFlex/parser reference

**Token Sets Abstraction:**
- Purpose: Group related tokens for parser and highlighter
- Examples: `MakoTokenSets.COMMENTS`, `MakoTokenSets.TAG_OPENS`
- Pattern: Pre-computed TokenSet constants (IntelliJ's immutable set)

**PSI Element Hierarchy:**
- Pattern: All PSI nodes implement shared `PsiElement` interface; named elements implement `PsiNamedElement`
- Named Elements: MakoDefTag, MakoBlockTag (have `getName()`, `setName()`, `getNameIdentifier()`)
- Container Elements: MakoFile, MakoDefTag, MakoBlockTag can have children (item_*)
- Leaf/Composite Elements: MakoExpression, MakoControlLineStmt, MakoCodeBlock (typically no named child access)

**Mixin Pattern:**
- Purpose: Add methods to generated PSI implementations without modifying generated code
- Examples: `MakoDefTagMixin.getName()` extracts name from first TAG_ATTR_VALUE child
- Mechanism: Mako.bnf `mixin` attribute wires abstract mixin class into generated impl class hierarchy

**Feature Builder Pattern:**
- Purpose: Process PSI tree to derive IDE data (colors, folds, structure)
- Examples: SyntaxHighlighter (token → color), FoldingBuilder (PSI node → fold region)
- Pattern: Extend SyntaxHighlighterBase, FoldingBuilderEx; implement abstract methods; register in plugin.xml

## Entry Points

**IDE File Opening:**
- Location: `src/main/resources/META-INF/plugin.xml` (fileType extension)
- Triggers: User opens `.mako`, `.mak`, or `*.html.mako` file
- Responsibilities: Register Mako as a LanguageFileType, associate with MakoLanguage
- Uses: `MakoFileType.INSTANCE`

**Parsing:**
- Location: `src/main/kotlin/com/github/kimuth/jetbrainsmakotemplateplugin/lang/MakoParserDefinition.kt`
- Triggers: Platform calls `ParserDefinition.createLexer()`, `.createParser()`, `.createFile()`
- Responsibilities: Instantiate MakoLexerAdapter, MakoParser; create MakoFile root PSI
- Called by: JetBrains PsiFileFactory during file parsing

**Syntax Highlighting:**
- Location: `src/main/kotlin/.../lang/highlighting/MakoSyntaxHighlighterFactory.kt`
- Triggers: Editor renders file after parsing
- Responsibilities: Return MakoSyntaxHighlighter instance
- Called by: JetBrains Editor color manager

**Code Folding:**
- Location: `src/main/kotlin/.../lang/folding/MakoFoldingBuilder.kt`
- Triggers: Editor opens gutter fold icons after parsing
- Responsibilities: Scan PSI tree, return FoldingDescriptor array
- Called by: JetBrains Editor folding manager (buildFoldRegions)

**Structure View (Outline):**
- Location: `src/main/kotlin/.../lang/structure/MakoStructureViewFactory.kt`
- Triggers: User opens Structure tool window (View > Tool Windows > Structure)
- Responsibilities: Create MakoStructureViewModel, filter PSI tree to defs/blocks
- Called by: JetBrains Structure View framework

**Comment Toggling:**
- Location: `src/main/kotlin/.../lang/editing/MakoCommenter.kt`
- Triggers: User presses Ctrl+/ (or Cmd+/ on Mac)
- Responsibilities: Return line comment prefix ("## ") and block comment delimiters
- Called by: JetBrains Comment action handler

**Tag Pair Matching:**
- Location: `src/main/kotlin/.../lang/highlighting/MakoPairedBraceMatcher.kt`
- Triggers: User cursor hovers over `<%def`, `<%block`, `</%def>`, etc.
- Responsibilities: Return matching token pairs (TAG_OPEN_DEF ↔ END_TAG)
- Called by: JetBrains Bracket Matcher framework

## Error Handling

**Strategy:** Lexer and parser tolerate malformed input; errors surface as PsiErrorElement nodes in PSI tree.

**Patterns:**

1. **Lexer Error Recovery**: Unrecognized characters emit TEMPLATE_TEXT token (catch-all) or BAD_CHARACTER
2. **Parser Error Recovery**:
   - Pinning: Grammar rules with `pin=1` (first child) anchor parsing
   - Recovery Predicates: `tag_recover` and `expression_recover` define negative lookahead; parser skips tokens until predicate is true
   - Example: `def_tag ::= TAG_OPEN_DEF tag_attribute* TAG_CLOSE item_* END_TAG` pins on TAG_OPEN_DEF; if END_TAG missing, recoverWhile skips forward
3. **Error Visibility**: Malformed nodes appear as PsiErrorElement in PSI tree; highlighter colors them red; folding and structure view skip them

**Example:** Missing `%>` in `<% code` → parser emits PsiErrorElement, recovers at next construct boundary (CONTROL_LINE, TAG_OPEN_DEF, etc.)

## Cross-Cutting Concerns

**Logging:** Not currently used; add via `com.intellij.openapi.diagnostic.Logger` if debug needed.

**Validation:** No runtime validation layer; parser produces PSI errors, features consume PSI tree as-is.

**Authentication:** Not applicable; Mako templates are plain text.

**Concurrency:** PSI tree is immutable per IDE contract; features read tree without locks; IDE serializes parse and feature access.

---

*Architecture analysis: 2026-02-21*

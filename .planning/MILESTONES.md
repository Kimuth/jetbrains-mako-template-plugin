# Milestones

## v0.1.0 Initial Release (Shipped: 2026-02-21)

**Phases completed:** 9 phases, 23 plans
**Timeline:** 2026-02-19 → 2026-02-21 (3 days)
**Files changed:** 222 files, 24,496 insertions, 2,342 deletions
**LOC:** ~1,276 hand-written Kotlin + 2,302 generated Java + 1,237 test Kotlin
**Git range:** `de44001` (Initial commit) → `2b1e548` (docs(phase-09))

**Key accomplishments:**
- Registered Mako as a custom JetBrains language with file type recognition, Mako-specific icon, and PyCharm Community 2025.2+ (build 252+) build target
- JFlex-generated lexer with multi-state tokenization for all Mako constructs, brace depth tracking for nested `${...}`, and restart-state semantics (22 token types, 5 JFlex states)
- GrammarKit-generated parser building typed PSI tree with 12 distinct node types, PsiNamedElement mixins for `<%def>`/`<%block>`, and pin+recoverWhile error recovery
- Syntax highlighting (9 color attributes in Settings), `##` line comments (Ctrl+/), `<%doc>` block comments (Ctrl+Shift+/), and 5 matched brace pairs
- Code folding for 6 construct types and Structure View panel showing navigable `<%def>`/`<%block>` tree
- Python language injection into `${...}`, `<% %>`, `<%! %>` via MultiHostInjector — PyCharm Python highlighting active inside Mako templates
- Tag name completion (7 directives after `<%`) and per-tag attribute completion (`name=`, `file=`, `buffered=`, etc.) — no false positives in plain HTML regions
- Error annotations (unclosed tags, invalid directives), Plugin Verifier passing for PC-252/PY-253/PY-261, Marketplace-ready as `com.schtilig.mako` v0.1.0 with plugin icon

**Archive:** `.planning/milestones/v0.1.0-ROADMAP.md`, `.planning/milestones/v0.1.0-REQUIREMENTS.md`

---


## v0.2.0 Bug Fixing & Cleanup (Shipped: 2026-02-22)

**Phases completed:** 8 phases (10–17), 10 plans
**Timeline:** 2026-02-21 → 2026-02-22 (2 days)
**Files changed:** 138 files, 5,449 insertions, 507 deletions
**LOC:** ~1,382 hand-written Kotlin (src/main/kotlin)
**Git range:** `v0.1.0` → `HEAD` (58 commits)

**Key accomplishments:**
- Fixed `getName()` ASTNode attribute-pairing bug in PSI mixins — `<%def name="foo" args="()">` now correctly returns "foo" instead of "()"
- Fixed Python injection range to stop at first `FILTER_SEP` token — filter names (`h`, `trim` in `${x | h, trim}`) excluded from injected Python fragment
- Fixed code folding to use recursive descent (`walkAllNodes`) — nested `<%doc>`/code/module blocks inside `<%def>`/`<%block>` now produce fold regions
- Fixed Structure View: defs and blocks interleaved in document order (`sortedBy { textOffset }`), function icon (`AllIcons.Nodes.Function`) replaces file icon
- Fixed editor behavior: `MAKO_CODE_CONTENT` color changed STRING→IDENTIFIER, `MODULE_OPEN` brace pair added, braceDepth overflow now emits Logger warning
- Fixed `<%doc` insert handler to use captured `ltPos` instead of `ctx.startOffset - 2`; replaced full-file `file.text` allocation with `document.charsSequence` backward scan
- Replaced string-literal language guards (`language.id != "Mako Template"`) with type-safe `MakoLanguage` identity comparison; added `UnknownDirective` parser fixture regression test
- Removed dead `FILTER_NAME` token and unused `TEMPLATE_CONTENT`/`TAG_OPENS` token sets; deleted 5 orphaned test fixtures from `testData/`

**Tech debt accepted:**
- `TagAttrCompletionProvider` missing explicit language guard (PSI pattern provides implicit restriction — no functional risk)
- 7 runtime behaviors deferred to human verification in running PyCharm instance

**Archive:** `.planning/milestones/v0.2.0-ROADMAP.md`, `.planning/milestones/v0.2.0-REQUIREMENTS.md`

---


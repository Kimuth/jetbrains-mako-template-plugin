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


---
phase: 03-parser
verified: 2026-02-19T22:30:00Z
status: passed
score: 5/5 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 3/5
  gaps_closed:
    - "CONTROL_LINE tokens now parsed into MakoControlLineStmtImpl(CONTROL_LINE_STMT) typed PSI nodes as top-level FILE children"
    - "Recovery predicates (tag_recover, expression_recover) now correctly stop at CONTROL_LINE token boundaries"
    - "MakoTypes.java CONTROL_LINE is a token delegate (= MakoTokenTypes.CONTROL_LINE), not a composite type"
  gaps_remaining: []
  regressions: []
---

# Phase 3: Parser and PSI Tree Verification Report

**Phase Goal:** The parser builds a typed PSI tree where every Mako construct has a distinct node class that supports future reference resolution
**Verified:** 2026-02-19T22:30:00Z
**Status:** passed
**Re-verification:** Yes — after gap closure (GAP-01 CONTROL_LINE name collision)

## Goal Achievement

### Observable Truths

| #  | Truth                                                                                                                                                                      | Status      | Evidence                                                                                                                                               |
|----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Lexer emits distinct token types for each named Mako tag and all other constructs                                                                                          | VERIFIED    | MakoTokenTypes.kt declares all token constants; MakoTypes.java has 28 token delegates covering all tokens including `CONTROL_LINE = MakoTokenTypes.CONTROL_LINE` |
| 2  | Mako.bnf grammar defines separate rules for def_tag, block_tag, inherit_tag, include_tag, namespace_tag, expression, control_line_stmt, code_block, module_block, doc_comment, line_comment_rule | VERIFIED    | Mako.bnf lines 48-141: all 11 construct rules present; rule is `control_line_stmt ::= CONTROL_LINE` (renamed from `control_line` to eliminate name collision) |
| 3  | GrammarKit GenerateParserTask generates MakoParser.java and typed PSI interfaces/implementations with CONTROL_LINE_STMT composite type (not CONTROL_LINE)                 | VERIFIED    | MakoParser.java: `control_line_stmt()` method uses `consumeToken(builder_, CONTROL_LINE)` + `exit_section_(..., CONTROL_LINE_STMT, ...)` (lines 103-109). MakoTypes.java: `CONTROL_LINE_STMT = new MakoElementType("CONTROL_LINE_STMT")` at line 17; `CONTROL_LINE = MakoTokenTypes.CONTROL_LINE` at line 35 |
| 4  | Recovery predicates (tag_recover, expression_recover) stop at CONTROL_LINE token boundaries                                                                               | VERIFIED    | MakoParser.java lines 264 and 498: both `expression_recover_0` and `tag_recover_0` call `consumeToken(builder_, CONTROL_LINE)`; since `CONTROL_LINE` now resolves to the token delegate, predicates stop correctly at control line boundaries |
| 5  | WellFormedFile PSI tree shows MakoControlLineStmtImpl(CONTROL_LINE_STMT) as distinct top-level FILE children for control lines (PARS-04 success criterion)                | VERIFIED    | WellFormedFile.txt lines 88-89 and 96-97: `MakoControlLineStmtImpl(CONTROL_LINE_STMT)(278,298)` containing `PsiElement(CONTROL_LINE)('% for item in items:')` and `MakoControlLineStmtImpl(CONTROL_LINE_STMT)(307,315)` containing `PsiElement(CONTROL_LINE)('% endfor')` — both appear as top-level FILE siblings, NOT inside MODULE_BLOCK |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `src/main/grammars/Mako.bnf` | BNF grammar with `control_line_stmt` rule (renamed from `control_line`) | VERIFIED | Line 108: `control_line_stmt ::= CONTROL_LINE`; `item_` at line 55 references `control_line_stmt`; recovery predicates (lines 133-141) reference `CONTROL_LINE` token correctly |
| `src/main/gen/.../lang/psi/MakoTypes.java` | `CONTROL_LINE_STMT` composite + `CONTROL_LINE` token delegate | VERIFIED | Line 17: `CONTROL_LINE_STMT = new MakoElementType("CONTROL_LINE_STMT")`. Line 35: `CONTROL_LINE = MakoTokenTypes.CONTROL_LINE`. No composite named `CONTROL_LINE` exists. Factory.createElement dispatches `CONTROL_LINE_STMT` to `MakoControlLineStmtImpl` at lines 69-71 |
| `src/main/gen/.../lang/psi/MakoControlLineStmt.java` | PSI interface for control line statement | VERIFIED | Exists, 11 lines: `public interface MakoControlLineStmt extends PsiElement` |
| `src/main/gen/.../lang/psi/impl/MakoControlLineStmtImpl.java` | PSI impl for control line statement | VERIFIED | Exists, 31 lines: `class MakoControlLineStmtImpl extends ASTWrapperPsiElement implements MakoControlLineStmt`; `visitControlLineStmt` wiring present |
| `src/main/gen/.../lang/psi/MakoControlLine.java` (old) | Must NOT exist | VERIFIED | Confirmed deleted — file not found |
| `src/main/gen/.../lang/psi/impl/MakoControlLineImpl.java` (old) | Must NOT exist | VERIFIED | Confirmed deleted — file not found |
| `src/test/testData/parser/WellFormedFile.txt` | Regenerated fixture showing CONTROL_LINE_STMT top-level nodes | VERIFIED | Contains `MakoControlLineStmtImpl(CONTROL_LINE_STMT)` at lines 88 and 96 as direct FILE children; no CONTROL_LINE-related PsiErrorElements at top level |
| `src/test/testData/parser/MalformedTag.txt` | Regenerated fixture; error message references CONTROL_LINE as stop token | VERIFIED | Line 37: error message includes `CONTROL_LINE` in the expected-token list, confirming recovery predicate correctly names the boundary token |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `MakoTypes.java` CONTROL_LINE field | `MakoTokenTypes.kt` CONTROL_LINE | Token delegate: `CONTROL_LINE = MakoTokenTypes.CONTROL_LINE` | WIRED | Line 35: `IElementType CONTROL_LINE = MakoTokenTypes.CONTROL_LINE` — same instance as lexer, token matching guaranteed |
| `MakoParser.java` control_line_stmt() | `MakoTypes.java` CONTROL_LINE (token) | `consumeToken(builder_, CONTROL_LINE)` via `static import MakoTypes.*` | WIRED | Line 108: resolves to token delegate (not composite), so lexer CONTROL_LINE tokens are correctly consumed |
| `MakoParser.java` control_line_stmt() | `MakoTypes.java` CONTROL_LINE_STMT (composite) | `exit_section_(builder_, marker_, CONTROL_LINE_STMT, result_)` | WIRED | Line 109: composite type used as the marker, creating typed PSI node |
| `MakoParser.java` tag_recover_0() | CONTROL_LINE token boundary | `consumeToken(builder_, CONTROL_LINE)` in recovery predicate | WIRED | Line 498: recovery stops correctly at CONTROL_LINE token boundaries |
| `MakoParser.java` expression_recover_0() | CONTROL_LINE token boundary | `consumeToken(builder_, CONTROL_LINE)` in recovery predicate | WIRED | Line 264: recovery stops correctly at CONTROL_LINE token boundaries |
| `MakoTypes.java` Factory.createElement | `MakoControlLineStmtImpl` | `if (type == CONTROL_LINE_STMT) return new MakoControlLineStmtImpl(node)` | WIRED | Lines 69-71: correct dispatch confirmed |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| PARS-04 | 03-01, 03-02, 03-03 | GrammarKit-generated parser builds PSI tree with typed nodes for each Mako construct | SATISFIED | All 12 typed PSI node classes exist and are registered in Factory.createElement. WellFormedFile.txt confirms INHERIT_TAG, NAMESPACE_TAG, DEF_TAG, BLOCK_TAG, INCLUDE_TAG, CODE_BLOCK, MODULE_BLOCK, EXPRESSION (from 03-02), and CONTROL_LINE_STMT (from 03-03 gap closure) as distinct top-level typed nodes. REQUIREMENTS.md marks PARS-04 complete at line 100. |
| PARS-05 | 03-01, 03-02, 03-03 | Parser recovers gracefully from malformed Mako (partial parses, not full failure) | SATISFIED | MalformedTag.txt shows partial tree with MakoDefTagImpl containing PsiErrorElements rather than a total parse failure. Recovery predicates correctly list CONTROL_LINE as a boundary token (MalformedTag.txt line 37). REQUIREMENTS.md marks PARS-05 complete at line 101. |

**Orphaned Requirements:** None. PARS-04 and PARS-05 are the only Phase 3 requirements in REQUIREMENTS.md (lines 100-101), both are claimed by plans 03-01, 03-02, and 03-03, and both are marked complete.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `src/test/kotlin/.../MakoParsingTest.kt` | 27 | Stale javadoc comment: `MakoControlLine` should read `MakoControlLineStmt` after the rename | Info | No functional impact — comment is documentation only; tests pass regardless |

No blocker or warning anti-patterns found. The single info-level item is a stale class name in a test docstring and does not affect test execution or goal achievement.

### Human Verification Required

All previously-flagged human verification items from the initial report are now structurally resolved:

- **CONTROL_LINE Typed Node Visibility (PARS-04):** WellFormedFile.txt conclusively shows `MakoControlLineStmtImpl(CONTROL_LINE_STMT)` as top-level FILE children — the programmatic fixture is the authoritative evidence.
- **LINE_COMMENT_RULE and DOC_COMMENT at top level:** WellFormedFile.txt lines 82-86 show `PsiComment(LINE_COMMENT)` and `PsiComment(DOC_OPEN/DOC_CONTENT/DOC_CLOSE)` appearing at top level, NOT inside MODULE_BLOCK, confirming the improved recovery predicates also fixed the MODULE_BLOCK oversizing.
- **MakoParserDefinition runtime behavior:** No new concerns; wiring unchanged from 03-02.

No items require human verification to confirm goal achievement.

### Gap Closure Summary

**GAP-01: CLOSED** — The CONTROL_LINE token/composite IElementType name collision has been eliminated.

Root cause: The `control_line` BNF rule generated `MakoElementType("CONTROL_LINE")` as a composite type in MakoTypes.java, shadowing the `MakoTokenTypes.CONTROL_LINE` token delegate. Since MakoParser.java uses `static import MakoTypes.*`, `consumeToken(builder_, CONTROL_LINE)` resolved to the composite object rather than the token instance. Because these were different object references, token matching failed silently — every CONTROL_LINE token produced a PsiErrorElement instead of a typed PSI node, and recovery predicates never stopped at control line boundaries.

Fix applied in commits 26d4965 and b6ba0eb:

1. BNF rule renamed `control_line` to `control_line_stmt` — generates `CONTROL_LINE_STMT` composite, which no longer clashes with the `CONTROL_LINE` token name
2. `CONTROL_LINE` in MakoTypes.java is now exclusively a token delegate: `IElementType CONTROL_LINE = MakoTokenTypes.CONTROL_LINE`
3. Parser regenerated: `control_line_stmt()` correctly calls `consumeToken(builder_, CONTROL_LINE)` for token consumption and uses `CONTROL_LINE_STMT` as the composite marker
4. Recovery predicates (`tag_recover_0`, `expression_recover_0`) now correctly stop at CONTROL_LINE token boundaries
5. Old `MakoControlLine.java` and `MakoControlLineImpl.java` deleted; `MakoControlLineStmt.java` and `MakoControlLineStmtImpl.java` created
6. Test fixtures regenerated: WellFormedFile.txt shows two `MakoControlLineStmtImpl(CONTROL_LINE_STMT)` nodes as top-level FILE siblings

**Phase goal fully achieved:** The parser builds a typed PSI tree where every Mako construct — def_tag, block_tag, inherit_tag, include_tag, namespace_tag, page_tag, expression, control_line_stmt, code_block, module_block, doc_comment, line_comment_rule — has a distinct node class. PsiNamedElement is implemented for def_tag and block_tag via mixin classes, supporting future reference resolution. All 12 composite types are registered in MakoTypes.Factory.createElement. PARS-04 and PARS-05 are fully satisfied.

---

_Verified: 2026-02-19T22:30:00Z_
_Verifier: Claude (gsd-verifier)_

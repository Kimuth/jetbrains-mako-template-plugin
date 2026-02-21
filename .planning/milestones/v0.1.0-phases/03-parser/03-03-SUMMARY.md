---
phase: 03-parser
plan: 03
subsystem: parser
tags: [grammarkit, bnf, psi, control-line, gap-closure]

# Dependency graph
requires:
  - phase: 03-parser/03-02
    provides: MakoParserDefinition wired, token delegates in MakoTypes.java, parsing tests passing
provides:
  - "CONTROL_LINE_STMT composite PSI type for control line grammar rule (renamed from CONTROL_LINE)"
  - "CONTROL_LINE token delegate in MakoTypes.java pointing to MakoTokenTypes.CONTROL_LINE"
  - "MakoControlLineStmtImpl PSI class replacing MakoControlLineImpl"
  - "Recovery predicates (tag_recover, expression_recover) correctly stop at CONTROL_LINE token boundaries"
  - "WellFormedFile.txt showing control lines as MakoControlLineStmtImpl(CONTROL_LINE_STMT) top-level FILE children"
affects: [04-template-language, PSI tree structure, control flow analysis]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "BNF rule name must differ from any token name when generateTokens=false -- use rule_stmt suffix to avoid name collision with token constants"
    - "After generateMakoParser, always re-add token delegates to MakoTypes.java (generateTokens=false means they are NOT generated)"

key-files:
  created:
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoControlLineStmt.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/impl/MakoControlLineStmtImpl.java
  modified:
    - src/main/grammars/Mako.bnf
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/parser/MakoParser.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoTypes.java
    - src/main/gen/com/github/kimuth/jetbrainsmakotemplateplugin/lang/psi/MakoVisitor.java
    - src/test/testData/parser/WellFormedFile.txt
    - src/test/testData/parser/MalformedTag.txt

key-decisions:
  - "BNF rule renamed control_line -> control_line_stmt: the rule generated CONTROL_LINE composite type shadowing the CONTROL_LINE token delegate from MakoTokenTypes.kt; since MakoParser.java uses static import MakoTypes.*, consumeToken(builder_, CONTROL_LINE) resolved to the wrong object (composite not token), causing silent parse failure"
  - "CONTROL_LINE is now exclusively a token delegate in MakoTypes.java (= MakoTokenTypes.CONTROL_LINE), not a composite; CONTROL_LINE_STMT is the composite for the control_line_stmt BNF rule"
  - "purgeOldFiles=true in GrammarKit config automatically deleted MakoControlLine.java and MakoControlLineImpl.java when rule was renamed"
  - "Recovery predicates (tag_recover, expression_recover) correctly reference CONTROL_LINE token -- after the rename, CONTROL_LINE in predicates resolves to the token delegate, so recovery stops at control line boundaries"

patterns-established:
  - "Pattern: BNF rule/token name collision diagnostic -- if consumeToken(builder_, TOKEN) silently fails and produces PsiErrorElements, check whether MakoTypes.java has a composite IElementType with the same name as the token delegate; rename the BNF rule"
  - "Pattern: Test fixture regeneration workflow -- delete .txt file, run tests once (writes actual output, fails), run tests again (compares, passes)"

requirements-completed: [PARS-04, PARS-05]

# Metrics
duration: 3min
completed: 2026-02-19
---

# Phase 3 Plan 03: Gap Closure - CONTROL_LINE Name Collision Fix Summary

**BNF rule renamed control_line -> control_line_stmt to eliminate token/composite IElementType name collision; CONTROL_LINE tokens now produce MakoControlLineStmtImpl(CONTROL_LINE_STMT) PSI nodes as top-level FILE children with working recovery predicates**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-19T22:16:42Z
- **Completed:** 2026-02-19T22:19:00Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Eliminated GAP-01: the CONTROL_LINE composite type in MakoTypes.java no longer shadows the CONTROL_LINE token delegate from MakoTokenTypes.kt
- CONTROL_LINE tokens from the lexer are now correctly parsed into MakoControlLineStmtImpl(CONTROL_LINE_STMT) typed PSI nodes (not PsiErrorElements)
- Recovery predicates (tag_recover, expression_recover) now correctly stop at CONTROL_LINE token boundaries - MODULE_BLOCK no longer consumes control lines, doc comments, or line comments via broken recovery
- WellFormedFile.txt PSI tree shows both control lines (`% for item in items:` and `% endfor`) as distinct top-level siblings of FILE node
- Full build passes with all tests green (lexer tests + parsing tests)

## Task Commits

Each task was committed atomically:

1. **Task 1: Rename BNF rule and regenerate parser** - `26d4965` (feat)
2. **Task 2: Update test fixtures and verify PSI tree correctness** - `b6ba0eb` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `src/main/grammars/Mako.bnf` - Renamed control_line rule to control_line_stmt; updated item_ reference
- `src/main/gen/.../parser/MakoParser.java` - Regenerated: control_line_stmt() method, CONTROL_LINE_STMT marker, CONTROL_LINE token consumption
- `src/main/gen/.../psi/MakoTypes.java` - CONTROL_LINE_STMT composite + CONTROL_LINE token delegate (re-added after generation)
- `src/main/gen/.../psi/MakoControlLineStmt.java` - New PSI interface (renamed from MakoControlLine.java)
- `src/main/gen/.../psi/impl/MakoControlLineStmtImpl.java` - New PSI impl (renamed from MakoControlLineImpl.java)
- `src/main/gen/.../psi/MakoVisitor.java` - Updated visitControlLineStmt method
- `src/test/testData/parser/WellFormedFile.txt` - Regenerated: shows MakoControlLineStmtImpl(CONTROL_LINE_STMT) top-level nodes
- `src/test/testData/parser/MalformedTag.txt` - Regenerated: content unchanged (malformed tag behavior unaffected)

## Decisions Made

- Renamed BNF rule `control_line` to `control_line_stmt` -- the previous name generated a composite IElementType `CONTROL_LINE` that had the same string name as the lexer token delegate. Since `MakoParser.java` uses `static import MakoTypes.*`, `consumeToken(builder_, CONTROL_LINE)` resolved to the composite `MakoElementType("CONTROL_LINE")` object rather than the `MakoTokenTypes.CONTROL_LINE` token instance. These are different object references, so token matching failed silently -- every CONTROL_LINE token from the lexer produced a `PsiErrorElement` instead of a typed PSI node.
- Token delegates were re-added manually to `MakoTypes.java` after generation because `generateTokens=false` means GrammarKit only generates composite types. The key addition is `CONTROL_LINE = MakoTokenTypes.CONTROL_LINE` as a token delegate (not a composite).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - the fix was straightforward. The regenerated parser, re-added token delegates, and test fixture regeneration all worked exactly as planned.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 3 (Parser) is now fully complete with GAP-01 closed
- CONTROL_LINE tokens produce properly typed PSI nodes, enabling future semantic analysis of control flow
- Phase 4 (Template Language) can proceed; PSI tree structure is correct
- No blockers or concerns

---
*Phase: 03-parser*
*Completed: 2026-02-19*

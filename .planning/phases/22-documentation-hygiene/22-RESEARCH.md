# Phase 22: Documentation Hygiene - Research

**Researched:** 2026-02-23
**Domain:** Documentation editing — Markdown files (.planning/REQUIREMENTS.md, .planning/ROADMAP.md)
**Confidence:** HIGH

---

## Summary

Phase 22 is a pure documentation-editing phase. No code changes, no builds, no tests. All work is targeted text edits to two Markdown files: `.planning/REQUIREMENTS.md` and `.planning/ROADMAP.md`. The goal is to bring documentation into alignment with the actual verification outcomes produced by Phases 20 and 21.

The milestone audit (`v0.3-MILESTONE-AUDIT.md`) identified six tech-debt items; Phase 22 addresses the four that are documentation-only. The two items involving requirement-list checkbox correctness for HINJ-05/HINJ-06 and traceability description staleness for HINJ-01/HINJ-04 are the core work. Two cosmetic ROADMAP.md checkbox fixes (18-03-PLAN.md and 19-01-PLAN.md) round out the phase.

Phase 21 (complete as of 2026-02-22) verified HINJ-05 and HINJ-06 PASS at runtime. This means the REQUIREMENTS.md requirement-list checkboxes for HINJ-05/HINJ-06 should now be `[x]` (checked, complete), and traceability should reflect Phase 21 completion. The ROADMAP.md success criteria in the phase description says "unchecked `[ ]` until Phase 21 delivers completion" — Phase 21 is now complete, so the correct final state is `[x]` for HINJ-05/HINJ-06 checkboxes.

**Primary recommendation:** Execute all four edits as a single plan in one task — read each file, apply all targeted line replacements, verify before/after, commit.

---

## Precise Edit Inventory

This section documents every line that must change, with exact before/after text. The planner should use these as task action specifications.

### File 1: `.planning/REQUIREMENTS.md`

#### Edit 1: HINJ-01 requirement-list entry (line 12)

**Current:**
```
- [x] **HINJ-01**: User sees HTML syntax coloring in template body regions of `.mako` files — FAILED verification (18-02); HTML tags not colored differently from Mako constructs
```

**Replace with:**
```
- [x] **HINJ-01**: User sees HTML syntax coloring in template body regions of `.mako` files — PASS (Phase 20); MakoEditorHighlighter confirmed working at runtime; HTML tags visually colored in TEMPLATE_TEXT regions
```

**Source evidence:** Phase 20-03 human IDE verification PASS; STATE.md decision "[Phase 20]: CRCT-01 and CRCT-02 confirmed PASS at runtime in human IDE verification"; STATE.md note "HINJ-01 (HTML coloring): RESOLVED in Phase 20 — MakoEditorHighlighter confirmed WORKING at runtime"

#### Edit 2: HINJ-04 requirement-list entry (line 15)

**Current:**
```
- [x] **HINJ-04**: User sees HTML error squiggles for malformed markup in template body — PARTIAL verification (18-03): squiggles appear on `<span>` without closing tag but NOT on `<p>` or `<html>` without closing tags; inconsistent by element type; carry to Phase 20
```

**Replace with:**
```
- [x] **HINJ-04**: User sees HTML error squiggles for malformed markup in template body — PASS (Phase 20); behavior confirmed correct per HTML5 spec: `<span>` without close gets squiggle (required-close element); `<p>` without close does not (optional-close element)
```

**Source evidence:** STATE.md decision "[Phase 20]: HINJ-04 RESOLVED — behavior confirmed CORRECT per HTML5 spec"; v0.3-MILESTONE-AUDIT.md "Phase 20: PASS (HTML5 spec-correct, human IDE)"

#### Edit 3: HINJ-05 requirement-list entry (line 16)

**Current:**
```
- [x] **HINJ-05**: CSS completion and validation are active inside `<style>` tags in `.mako` files — FAILED verification (18-02/20-03); MultiHostInjector/LanguageInjectionContributor for CSSLanguage not registered; gap closure Phase 21
```

**Replace with:**
```
- [x] **HINJ-05**: CSS completion and validation are active inside `<style>` tags in `.mako` files — PASS (Phase 21); MakoCssInjector implemented and registered; CSS completions confirmed active in running IDE when CSS plugin installed
```

**Source evidence:** Phase 21-02 human IDE verification PASS; STATE.md decision "HINJ-05 VERIFIED PASS at runtime: CSS completions active in <style> blocks when CSS plugin installed"; 21-02-SUMMARY.md `requirements-completed: [HINJ-05, HINJ-06]`

#### Edit 4: HINJ-06 requirement-list entry (line 17)

**Current:**
```
- [x] **HINJ-06**: JavaScript completion and validation are active inside `<script>` tags in `.mako` files — FAILED verification (18-02/20-03); MultiHostInjector/LanguageInjectionContributor for JavaScriptLanguage not registered; gap closure Phase 21
```

**Replace with:**
```
- [x] **HINJ-06**: JavaScript completion and validation are active inside `<script>` tags in `.mako` files — PASS (Phase 21); platform HtmlScriptLanguageInjector handles `<script>` XmlText automatically; JavaScript completions confirmed active in running IDE when JavaScript plugin installed
```

**Source evidence:** STATE.md decision "HINJ-06 VERIFIED PASS at runtime: JavaScript completions active in <script> blocks when JavaScript plugin installed"; STATE.md note "No MakoJsInjector written — platform HtmlScriptLanguageInjector already handles <script> XmlText in HTML PSI tree"

#### Edit 5: Traceability table — HINJ-01 row (line 69)

**Current:**
```
| HINJ-01 | Phase 18/20 | Verification failed — HTML coloring gap; carry to Phase 20 |
```

**Replace with:**
```
| HINJ-01 | Phase 18/20 | Complete — Phase 20 PASS; MakoEditorHighlighter confirmed working at runtime 2026-02-22 |
```

#### Edit 6: Traceability table — HINJ-04 row (line 72)

**Current:**
```
| HINJ-04 | Phase 18/20 | PARTIAL verification (18-03) — squiggles on span tags but not p or html tags; inconsistent coverage; carry to Phase 20 |
```

**Replace with:**
```
| HINJ-04 | Phase 18/20 | Complete — Phase 20 PASS; behavior correct per HTML5 spec (optional-close elements don't require squiggle) verified 2026-02-22 |
```

#### Edit 7: Traceability table — HINJ-05 row (line 73)

**Current:**
```
| HINJ-05 | Phase 18/20/21 | Pending — gap closure Phase 21 (LanguageInjectionContributor for CSSLanguage not registered) |
```

**Replace with:**
```
| HINJ-05 | Phase 18/20/21 | Complete — Phase 21 PASS; MakoCssInjector registered; CSS completions verified in running IDE 2026-02-22 |
```

#### Edit 8: Traceability table — HINJ-06 row (line 74)

**Current:**
```
| HINJ-06 | Phase 18/20/21 | Pending — gap closure Phase 21 (LanguageInjectionContributor for JavaScriptLanguage not registered) |
```

**Replace with:**
```
| HINJ-06 | Phase 18/20/21 | Complete — Phase 21 PASS; platform HtmlScriptLanguageInjector handles JS; completions verified in running IDE 2026-02-22 |
```

#### Edit 9: Last-updated footer (line 88)

**Current:**
```
*Last updated: 2026-02-22 after v0.3.0 milestone audit — HINJ-05/06 reset to Pending; assigned to Phase 21 gap closure*
```

**Replace with:**
```
*Last updated: 2026-02-23 after Phase 22 documentation hygiene — HINJ-01/04/05/06 traceability updated to reflect Phase 20/21 PASS outcomes*
```

---

### File 2: `.planning/ROADMAP.md`

#### Edit 10: Phase 18 plan checklist — 18-03-PLAN.md (line 70)

**Current:**
```
- [x] 18-03-PLAN.md — Gap closure: verify HINJ-04 (HTML error squiggles) in running IDE; explicitly defer HINJ-01/05/06 to Phase 20
```

Note: This line already shows `[x]` in the current file. The audit identified it as `[ ]` at audit time, but the file as of the current read shows `[x]`. **Verify the actual file state before editing.** If already `[x]`, no change needed.

**Source evidence:** v0.3-MILESTONE-AUDIT.md tech_debt: "ROADMAP.md plan checklist: 18-03-PLAN.md checkbox remains [ ] (unchecked)"; current ROADMAP.md line 70 shows `[x]` — this may already be corrected.

#### Edit 11: Phase 19 plan checklist — 19-01-PLAN.md (line 83)

**Current:**
```
- [ ] 19-01-PLAN.md — Apply MakoStructureViewFactory dual-tree guard, add regression test, run ./gradlew check, verify IDE success criteria
```

**Replace with:**
```
- [x] 19-01-PLAN.md — Apply MakoStructureViewFactory dual-tree guard, add regression test, run ./gradlew check, verify IDE success criteria
```

**Source evidence:** v0.3-MILESTONE-AUDIT.md tech_debt: "ROADMAP.md plan checklist: 19-01-PLAN.md checkbox remains [ ] (unchecked)"; Phase 19 progress table shows "Complete 2026-02-22"; 19-01-SUMMARY.md exists confirming execution.

#### Edit 12: Phase 21 plan checklist — 21-01-PLAN.md and 21-02-PLAN.md (lines 113-114)

**Current (lines 113-114):**
```
- [ ] 21-01-PLAN.md — Implement MakoCssInjector (MultiHostInjector for XmlText in <style>), register in plugin.xml, add MakoCssInjectorTest, run ./gradlew check
- [ ] 21-02-PLAN.md — Human IDE verification of CSS completions (HINJ-05) and JavaScript completions (HINJ-06) in running IDE
```

**Replace with:**
```
- [x] 21-01-PLAN.md — Implement MakoCssInjector (MultiHostInjector for XmlText in <style>), register in plugin.xml, add MakoCssInjectorTest, run ./gradlew check
- [x] 21-02-PLAN.md — Human IDE verification of CSS completions (HINJ-05) and JavaScript completions (HINJ-06) in running IDE
```

**Source evidence:** STATE.md "Phase: 21 of 22 — COMPLETE"; 21-01-SUMMARY.md and 21-02-SUMMARY.md exist; "Plan: 02 of 2 complete — HINJ-05 PASS, HINJ-06 PASS"

#### Edit 13: Phase 20 plan checklist — 20-01, 20-02, 20-03-PLAN.md (lines 97-99)

**Current (lines 97-99):**
```
- [ ] 20-01-PLAN.md — Implement MakoEditorHighlighter, MakoEditorHighlighterProvider, MakoErrorFilter + plugin.xml registration + CRCT automated tests
- [ ] 20-02-PLAN.md — Run ./gradlew check; verify all tests pass including two new CRCT tests
- [ ] 20-03-PLAN.md — Human IDE verification of CRCT-01/02 false-positive suppression and HINJ-01/04/05/06 feature behavior
```

**Replace with:**
```
- [x] 20-01-PLAN.md — Implement MakoEditorHighlighter, MakoEditorHighlighterProvider, MakoErrorFilter + plugin.xml registration + CRCT automated tests
- [x] 20-02-PLAN.md — Run ./gradlew check; verify all tests pass including two new CRCT tests
- [x] 20-03-PLAN.md — Human IDE verification of CRCT-01/02 false-positive suppression and HINJ-01/04/05/06 feature behavior
```

**Source evidence:** STATE.md "Phase 20 COMPLETE"; progress table shows "3/3 Complete 2026-02-22"; 20-01/02/03-SUMMARY.md files exist.

#### Edit 14: Phase 22 Plans field in Phase Details

**Current (line 126):**
```
**Plans**: TBD
```

**Replace with:**
```
**Plans**: 1 plan
Plans:
- [ ] 22-01-PLAN.md — Apply all REQUIREMENTS.md and ROADMAP.md documentation corrections
```

*(This placeholder will be filled by the planner after plan creation.)*

---

## Architecture Patterns

### Pattern: Single-Task Documentation Edit Phase

**What:** All edits in this phase are targeted find-and-replace operations on Markdown text. No code compilation, no tests, no IDE launch needed.

**When to use:** When requirements are purely textual corrections to planning artifacts with no ambiguity about target state.

**Recommended structure:**
- 1 plan, 1 task
- Task reads both files first (mandatory before Write)
- Task applies all edits
- Task verifies by re-reading and checking key lines
- Task commits with `docs(22)` prefix

**Anti-pattern to avoid:** Splitting into multiple plans when all edits are non-conflicting text changes. Sequencing is not required across files.

### Pattern: Read Before Write (Project Convention)

Per `CLAUDE.md` and Write tool requirements: always Read a file before Writing it. The task must Read `.planning/REQUIREMENTS.md` and `.planning/ROADMAP.md` before applying any edits.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Applying edits | Custom sed/awk script | Direct Write tool with full file content | This is a documentation-only phase; no tooling needed |
| Verifying changes | Automated test | Manual re-read + grep | No test infrastructure exists for planning Markdown files |

---

## Common Pitfalls

### Pitfall 1: Editing ROADMAP.md checkboxes that are already correct

**What goes wrong:** The audit found 18-03-PLAN.md as `[ ]`, but the current ROADMAP.md already shows `- [x] 18-03-PLAN.md`. If an editor overwrites this without verifying current state, it could introduce no-ops or re-introduce errors.

**How to avoid:** Read the file first (required by Write tool). Verify which checkboxes are actually `[ ]` vs `[x]` before applying edits. The confirmed unchecked items are: 19-01-PLAN.md, 20-01/02/03-PLAN.md, 21-01/02-PLAN.md.

**Warning signs:** Success criterion 4 mentions only 18-03 and 19-01. Check current state of 18-03 explicitly.

### Pitfall 2: Partial update — fixing traceability table but not requirement-list entries (or vice versa)

**What goes wrong:** REQUIREMENTS.md has two locations that describe each requirement: the requirement-list (`- [x] **HINJ-XX**:...`) and the traceability table (`| HINJ-XX | ... | ... |`). Both need updating for HINJ-01, HINJ-04, HINJ-05, HINJ-06.

**How to avoid:** The planner should list both locations as separate checklist items in the task.

### Pitfall 3: Incorrect final state for HINJ-05/HINJ-06 checkboxes

**What goes wrong:** ROADMAP.md success criterion 3 says "HINJ-05/HINJ-06 checkboxes are unchecked `[ ]` until Phase 21 delivers completion." Phase 21 is now complete. The CORRECT final state is `[x]` (checked), not `[ ]`. The criterion was written before Phase 21 ran.

**How to avoid:** The planner must understand that the success criterion describes the *intermediate* state (before Phase 21), not the target of Phase 22. Since Phase 21 is complete, Phase 22 should set them to `[x]`.

### Pitfall 4: Overlooking the footer/last-updated line

**What goes wrong:** REQUIREMENTS.md has a `*Last updated:*` line that still references the Phase 20/21 milestone audit activity. Leaving it stale is minor but inconsistent.

**How to avoid:** Update the footer as part of Edit 9.

---

## Verification Steps

After all edits are applied, the planner's verification task should check:

1. `grep "HINJ-01" .planning/REQUIREMENTS.md` — should NOT contain "FAILED" or "carry to Phase 20"
2. `grep "HINJ-04" .planning/REQUIREMENTS.md` — should NOT contain "PARTIAL" or "carry to Phase 20"
3. `grep "HINJ-05" .planning/REQUIREMENTS.md` — requirement-list entry should say "PASS (Phase 21)"; traceability should say "Complete"
4. `grep "HINJ-06" .planning/REQUIREMENTS.md` — same pattern as HINJ-05
5. `grep "19-01-PLAN" .planning/ROADMAP.md` — should show `[x]`
6. `grep "18-03-PLAN" .planning/ROADMAP.md` — should show `[x]` (verify current state first)
7. `grep "20-0[123]-PLAN\|21-0[12]-PLAN" .planning/ROADMAP.md` — should all show `[x]`

---

## State of the Art

| Item | Stale State | Correct State | Authority |
|------|-------------|---------------|-----------|
| HINJ-01 requirement entry | "FAILED verification (18-02)…" | "PASS (Phase 20)…" | Phase 20-03 human IDE |
| HINJ-01 traceability | "Verification failed — HTML coloring gap; carry to Phase 20" | "Complete — Phase 20 PASS…" | Phase 20-03 SUMMARY |
| HINJ-04 requirement entry | "PARTIAL verification (18-03)…carry to Phase 20" | "PASS (Phase 20)…HTML5 spec-correct" | Phase 20-03 human IDE |
| HINJ-04 traceability | "PARTIAL verification (18-03)…carry to Phase 20" | "Complete — Phase 20 PASS…" | Phase 20 VERIFICATION.md |
| HINJ-05 requirement entry | "FAILED verification…gap closure Phase 21" | "PASS (Phase 21)…CSS completions verified" | Phase 21-02 SUMMARY |
| HINJ-05 traceability | "Pending — gap closure Phase 21…" | "Complete — Phase 21 PASS…" | Phase 21-02 SUMMARY |
| HINJ-06 requirement entry | "FAILED verification…gap closure Phase 21" | "PASS (Phase 21)…JS completions verified" | Phase 21-02 SUMMARY |
| HINJ-06 traceability | "Pending — gap closure Phase 21…" | "Complete — Phase 21 PASS…" | Phase 21-02 SUMMARY |
| 19-01-PLAN.md checkbox | `[ ]` | `[x]` | 19-01-SUMMARY.md exists |
| 20-01/02/03-PLAN.md checkboxes | `[ ]` | `[x]` | 20-01/02/03 SUMMARY.md files exist |
| 21-01/02-PLAN.md checkboxes | `[ ]` | `[x]` | 21-01/02 SUMMARY.md files exist |

---

## Open Questions

1. **18-03-PLAN.md checkbox current state**
   - What we know: Audit recorded it as `[ ]` at audit time (2026-02-22)
   - What's visible now: ROADMAP.md line 70 currently reads `- [x] 18-03-PLAN.md`
   - Recommendation: Planner should read ROADMAP.md and check line 70 before editing; if already `[x]`, skip this edit. Success criterion 4 still lists it as a target — if found `[x]`, success criterion is already satisfied for that item.

2. **ROADMAP.md Phase 22 "Plans: TBD" entry**
   - What we know: Phase 22 plans field says "TBD" (line 126)
   - What's unclear: Whether the planner should update this field during Phase 22 itself, or leave it for a post-phase wrap-up
   - Recommendation: Update to "1 plan" after plan creation as part of ROADMAP.md write pass; low priority, does not affect success criteria.

---

## Sources

### Primary (HIGH confidence)
- `.planning/REQUIREMENTS.md` — direct file read; exact current text documented
- `.planning/ROADMAP.md` — direct file read; exact current text documented
- `.planning/v0.3-MILESTONE-AUDIT.md` — audit findings driving this phase; exact tech-debt items listed
- `.planning/STATE.md` — Phase 20/21 decisions confirming PASS outcomes
- `.planning/phases/21-css-js-sub-language-injection/21-02-SUMMARY.md` — Phase 21 completion evidence

### Secondary (MEDIUM confidence)
- N/A — all findings are from direct file reads, not web search

---

## Metadata

**Confidence breakdown:**
- Edit inventory: HIGH — derived from direct file reads and exact line comparisons
- Correct target state: HIGH — evidence from SUMMARY.md files, STATE.md decisions, milestone audit
- Pitfalls: HIGH — derived from direct analysis of audit findings and success criteria wording
- No external library research needed — domain is Markdown text editing

**Research date:** 2026-02-23
**Valid until:** Permanent (planning artifact, not time-sensitive to external changes)

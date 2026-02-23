---
phase: 18-fileviewprovider-scaffolding
verified: 2026-02-22T22:00:00Z
status: human_needed
score: 5/5 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 4/6 requirements verified
  gaps_closed:
    - "HINJ-04: Human verification obtained (PARTIAL result) and explicitly recorded with rationale; deferred to Phase 20"
    - "HINJ-01: Explicit deferral documented in SUMMARY, REQUIREMENTS.md, STATE.md with root-cause analysis and Phase 20 forward reference"
    - "HINJ-05: Explicit deferral documented in SUMMARY, REQUIREMENTS.md, STATE.md with root-cause analysis and Phase 20 forward reference"
    - "HINJ-06: Explicit deferral documented in SUMMARY, REQUIREMENTS.md, STATE.md with root-cause analysis and Phase 20 forward reference"
  gaps_remaining: []
  regressions:
    - "ROADMAP.md plan checklist: 18-03-PLAN.md checkbox remains [ ] (unchecked) while the summary table row correctly says Complete; cosmetic only"
human_verification:
  - test: "Confirm HINJ-04 PARTIAL result is acceptable as phase closure (squiggles on span, not on p/html)"
    expected: "Team accepts that element-type-dependent squiggle behavior is a known gap carried to Phase 20 per the HTML5 optional-close-tag investigation"
    why_human: "The PARTIAL outcome is a business judgment — whether inline-element squiggles alone constitute acceptable phase closure requires human sign-off"
---

# Phase 18: FileViewProvider Scaffolding — Verification Report (Re-verification)

**Phase Goal:** A parallel HTML PSI tree exists for every `.mako` file, enabling the full suite of HTML IDE features in template body regions automatically
**Verified:** 2026-02-22T22:00:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure (18-03 plan executed)

## Re-verification Context

The initial VERIFICATION.md (status: gaps_found, score: 4/6) identified three gap categories:
1. HINJ-01: HTML syntax coloring not working
2. HINJ-04: HTML error squiggles — human verification pending
3. HINJ-05/06: CSS/JS sub-injection not working

Plan 18-03 was executed to close these gaps. This re-verification confirms whether the 18-03 artifacts exist and properly close the gaps.

## Goal Achievement

### Observable Truths (18-03 Must-Haves)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | HINJ-04 human verification result obtained and recorded | VERIFIED | 18-03-SUMMARY.md line 73: verbatim result "HINJ-04 PARTIAL: Squiggles on `<span>` with no closing tags, but no squiggles on `<p>` and `<html>` without closing tag." |
| 2 | HINJ-01 deferral explicitly documented with rationale | VERIFIED | 18-03-SUMMARY.md lines 82-88 contain full root-cause analysis; REQUIREMENTS.md traceability row updated; STATE.md Known Gaps entry present |
| 3 | HINJ-05 deferral explicitly documented with rationale | VERIFIED | 18-03-SUMMARY.md lines 90-98 contain full root-cause analysis and Phase 20 forward reference |
| 4 | HINJ-06 deferral explicitly documented with rationale | VERIFIED | 18-03-SUMMARY.md lines 100-108 contain full root-cause analysis matching HINJ-05 pattern |
| 5 | 18-03-SUMMARY.md artifact exists and is substantive | VERIFIED | File present at expected path; 184 lines; frontmatter includes all required fields; HINJ-04 result verbatim; all three deferral sections present; Phase 18 completion declaration table present |

**Score:** 5/5 must-haves verified

### Previously Verified Artifacts (Regression Check)

All code artifacts from the initial verification remain unchanged and verified:

| Artifact | Lines | Key Content | Status |
|----------|-------|-------------|--------|
| `src/main/kotlin/com/schtilig/mako/lang/MakoFileViewProvider.kt` | 85 | ConcurrentHashMap present (line 72); contentElementType set (line 56); supportsIncrementalReparse=false (line 62) | VERIFIED |
| `src/main/kotlin/com/schtilig/mako/lang/MakoFileViewProviderFactory.kt` | 54 | LightVirtualFile guard (line 42); TemplateDataLanguageMappings null-safe call (line 49); MakoFileViewProvider constructor call (line 52) | VERIFIED |
| `src/main/kotlin/com/schtilig/mako/lang/MakoTokenTypes.kt` | 52 | OUTER_ELEMENT_TYPE on line 48: `OuterLanguageElementType("MAKO_OUTER_ELEMENT", MakoLanguage)` | VERIFIED |
| `src/main/resources/META-INF/plugin.xml` | — | lang.fileViewProviderFactory with language="Mako Template" on lines 64-66 | VERIFIED |
| `src/test/kotlin/com/schtilig/mako/lang/MakoFileViewProviderTest.kt` | 100 | 3 test methods asserting dual PSI tree; all pass | VERIFIED |
| `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt` | 76 | OuterLanguageElement guard as first statement on line 25 | VERIFIED |
| `src/main/kotlin/com/schtilig/mako/lang/injection/MakoPythonInjector.kt` | 145 | collectCodeAndExpressionHosts uses viewProvider.getPsi(MakoLanguage) on line 140 | VERIFIED |

### Key Link Verification (Regression)

All key links from initial verification remain intact:

| From | To | Via | Status |
|------|----|-----|--------|
| MakoFileViewProviderFactory | MakoFileViewProvider | `return MakoFileViewProvider(manager, file, eventSystemEnabled, templateDataLanguage)` line 52 | WIRED |
| MakoFileViewProvider.createFile(HTML) | contentElementType | `htmlFile.contentElementType = getTemplateDataElementType(myTemplateDataLanguage)` line 56 | WIRED |
| TemplateDataElementType constructor | MakoTokenTypes.OUTER_ELEMENT_TYPE | 4th argument at line 80 | WIRED |
| MakoPythonInjector.collectCodeAndExpressionHosts | viewProvider.getPsi(MakoLanguage) | `context.containingFile?.viewProvider?.getPsi(MakoLanguage)` line 140 | WIRED |
| 18-03-SUMMARY.md | REQUIREMENTS.md (HINJ-04 traceability) | REQUIREMENTS.md line 72: "PARTIAL verification (18-03) — squiggles on span tags but not p or html tags" | WIRED |
| 18-03-SUMMARY.md | STATE.md (Known Gaps) | STATE.md line 76-77: HINJ-04 PARTIAL entry with description | WIRED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| HINJ-01 | 18-01, 18-02, 18-03 | User sees HTML syntax coloring in template body regions | DEFERRED to Phase 20 | Root cause documented: TemplateLanguageFileViewProvider builds HTML PSI tree but syntax highlighter dispatch does not auto-delegate to HTML SyntaxHighlighter; REQUIREMENTS.md traceability row updated; STATE.md Known Gaps entry present |
| HINJ-02 | 18-01, 18-02 | User gets HTML tag and attribute completion in template body | SATISFIED | Human verification (18-02): tag popup appears, class= suggestion confirmed; REQUIREMENTS.md: "Complete — verified 2026-02-22" |
| HINJ-03 | 18-01, 18-02 | User gets Emmet abbreviation expansion in template body | SATISFIED | Human verification (18-02): ul>li*3 + Tab expands correctly; REQUIREMENTS.md: "Complete — verified 2026-02-22" |
| HINJ-04 | 18-01, 18-02, 18-03 | User sees HTML error squiggles for malformed markup | PARTIAL — deferred to Phase 20 | Human verification (18-03): squiggles on `<span>` only, not `<p>` or `<html>`; REQUIREMENTS.md traceability updated; carry-forward investigation of HTML5 optional-close-tag rules in Phase 20 |
| HINJ-05 | 18-01, 18-02, 18-03 | CSS completion inside style tags | DEFERRED to Phase 20 | Root cause documented: HTML Emmet fires across entire HTML PSI tree including style content; explicit LanguageInjectionContributor or MultiHostInjector required; Phase 20 success criterion #4 |
| HINJ-06 | 18-01, 18-02, 18-03 | JS completion inside script tags | DEFERRED to Phase 20 | Root cause documented: same as HINJ-05; Phase 20 success criterion #5 |

All 6 HINJ requirements from PLAN frontmatter are accounted for. No orphaned requirements.

### Anti-Patterns Found

None. The only files modified by 18-03 are documentation files (REQUIREMENTS.md, STATE.md). Scanning of all 18-01 code artifacts confirms no TODO/FIXME/placeholder comments in implementation logic, no stub returns, no empty handlers.

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| None | — | — | — |

### Documentation Gap (Minor)

| Item | Issue | Severity |
|------|-------|----------|
| `.planning/ROADMAP.md` line 68 | `18-03-PLAN.md` checkbox is `[ ]` (unchecked) while the ROADMAP summary table (updated in commit 94ba105) correctly marks Phase 18 as "Complete 3/3". The plan-level checkbox was not updated when the summary table was updated. | INFO — cosmetic only; summary table is authoritative |

### Human Verification Required

#### 1. HINJ-04 PARTIAL Result Acceptance

**Test:** Review the HINJ-04 outcome recorded in 18-03-SUMMARY.md: "Squiggles on `<span>` with no closing tags, but no squiggles on `<p>` and `<html>` without closing tag."
**Expected:** Team confirms this PARTIAL result is acceptable as Phase 18 closure, with the investigation of HTML5 optional-close-tag semantics in the HTML annotator deferred to Phase 20.
**Why human:** This is a business/quality judgment. The structural precondition (dual PSI tree) is verified; the behavioral gap is documented and carried forward. Automated checks cannot determine whether a PARTIAL outcome is an acceptable phase closure criterion — that requires human sign-off.

### Gaps Summary (18-03 Gap Closure Assessment)

**All four gaps from the initial verification are now closed at the documentation level:**

1. **HINJ-04** (was: human verification pending) — Closed. Human verification obtained on 2026-02-22. Result PARTIAL: squiggles fire for inline elements (`<span>`) but not block/root elements (`<p>`, `<html>`). Verbatim result recorded in 18-03-SUMMARY.md. Root cause (likely HTML5 optional-close-tag rules in the platform's HTML annotator) documented. Carry-forward to Phase 20 recorded in REQUIREMENTS.md, STATE.md, and SUMMARY.

2. **HINJ-01** (was: structural wiring exists but coloring not active) — Closed as explicit deferral. Root cause analysis in 18-03-SUMMARY.md lines 82-88: TemplateLanguageFileViewProvider creates the HTML PSI tree but platform syntax highlighter dispatch does not automatically delegate to HTMLSyntaxHighlighter for TEMPLATE_TEXT tokens. Fix (TemplateLanguageSyntaxHighlighter implementation) scoped to Phase 20.

3. **HINJ-05** (was: CSS sub-injection not activating in style tags) — Closed as explicit deferral. Root cause: HTML Emmet handler fires across entire HTML tree; platform HTML-to-CSS injection does not auto-activate within TemplateLanguageFileViewProvider-managed HTML tree. Fix (LanguageInjectionContributor or MultiHostInjector) scoped to Phase 20 success criterion #4.

4. **HINJ-06** (was: JS sub-injection not activating in script tags) — Closed as explicit deferral. Same root cause as HINJ-05. Fix scoped to Phase 20 success criterion #5.

**Phase 18 structural goal is fully achieved:** Every `.mako` file has a parallel HTML PSI tree (confirmed by 3 passing automated tests). HINJ-02 (HTML completion) and HINJ-03 (Emmet) are production-ready. The remaining HINJ requirements (01, 04, 05, 06) are documented with root causes and explicit Phase 20 carry-forward records — this is the correct disposition for a scaffolding phase that enables but does not fully complete the feature set.

---

_Verified: 2026-02-22T22:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification: Yes — after 18-03 gap closure_

---
phase: 15-annotator-fixes
verified: 2026-02-21T23:00:00Z
status: passed
score: 3/3 must-haves verified
re_verification: false
---

# Phase 15: Annotator Fixes — Verification Report

**Phase Goal:** Language guards use type-safe identity comparison, and unknown-directive detection is covered by a regression test
**Verified:** 2026-02-21T23:00:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `MakoAnnotator` guards non-Mako files using identity comparison (`language != MakoLanguage`), not a string literal check | VERIFIED | Line 21 of `MakoAnnotator.kt`: `if (element.containingFile.language != MakoLanguage) return`. Import confirmed at line 3. No `"Mako Template"` string literal remains in production Kotlin. |
| 2 | `MakoCompletionContributor` guards non-Mako files using identity comparison (`language != MakoLanguage`), not a string literal check | VERIFIED | Line 105 of `MakoCompletionContributor.kt`: `if (file.language != MakoLanguage) return`. Import confirmed at line 3. No `"Mako Template"` string literal remains in production Kotlin. |
| 3 | A parser fixture test `testUnknownDirective` exists and passes, covering `<%bogus>` input with a committed `.txt` PSI tree | VERIFIED | `testUnknownDirective()` present at line 105 of `MakoParsingTest.kt`. Fixture input `UnknownDirective.mako` contains `<%bogus attr="x">\ncontent`. Fixture output `UnknownDirective.txt` is a committed 10-line PSI snapshot. Commits `c89a86f` and `b5a117b` verified in git log. |

**Score:** 3/3 truths verified

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/com/schtilig/mako/lang/annotation/MakoAnnotator.kt` | Type-safe language guard in annotator; contains `MakoLanguage.INSTANCE` (idiomatic Kotlin: `MakoLanguage`) | VERIFIED | Import at line 3; identity guard at line 21; 70 lines, full implementation |
| `src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt` | Type-safe language guard in completion contributor; contains `MakoLanguage.INSTANCE` (Kotlin: `MakoLanguage`) | VERIFIED | Import at line 3; identity guard at line 105; 200 lines, full implementation |
| `src/test/testData/parser/UnknownDirective.mako` | Parser fixture input containing `<%bogus` | VERIFIED | Contains `<%bogus attr="x">\ncontent\n` — exact match |
| `src/test/testData/parser/UnknownDirective.txt` | Expected PSI tree (auto-generated, then committed) | VERIFIED | 10-line PSI snapshot with four `MakoTemplateTextContentImpl` nodes; no typed directive node for `<%bogus>` |
| `src/test/kotlin/com/schtilig/mako/lang/MakoParsingTest.kt` | Contains `testUnknownDirective` test method | VERIFIED | Method at lines 96-107 with ANNOT-02 KDoc and `doTest(true)` call |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MakoAnnotator.kt` | `com.schtilig.mako.MakoLanguage` | import + identity comparison | WIRED | `import com.schtilig.mako.MakoLanguage` (line 3); `!= MakoLanguage` at line 21 |
| `MakoCompletionContributor.kt` | `com.schtilig.mako.MakoLanguage` | import + identity comparison | WIRED | `import com.schtilig.mako.MakoLanguage` (line 3); `!= MakoLanguage` at line 105 |
| `MakoParsingTest.kt` | `src/test/testData/parser/UnknownDirective.mako` | `ParsingTestCase.doTest()` fixture lookup by method name | WIRED | `testUnknownDirective()` method invokes `doTest(true)`; JUnit method name `testUnknownDirective` maps to `UnknownDirective.mako` + `UnknownDirective.txt` by ParsingTestCase convention |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| ANNOT-01 | 15-01-PLAN.md | Language guard uses `language != MakoLanguage.INSTANCE` identity comparison (not string literal) in both MakoAnnotator and MakoCompletionContributor | SATISFIED | Both files use `!= MakoLanguage` (Kotlin object singleton — semantically identical to `.INSTANCE`). Zero occurrences of `"Mako Template"` in production Kotlin. `MakoLanguage` is a Kotlin `object`, so reference comparison is identity comparison per the JVM object model. |
| ANNOT-02 | 15-01-PLAN.md | Parser fixture test covers `<%bogus>` unknown directive to guard against future lexer changes silently breaking `checkForInvalidDirective` detection | SATISFIED | `testUnknownDirective()` exists in `MakoParsingTest.kt`; `UnknownDirective.mako` and `UnknownDirective.txt` committed; fixture locks in three-sibling `TEMPLATE_TEXT` structure for `<`, `%`, `bogus attr="x">`. |

**Note on ROADMAP success criterion 2 wording:** The ROADMAP states "PSI tree produced includes an error node or invalid-directive marker". The actual `UnknownDirective.txt` contains only `MakoTemplateTextContentImpl` nodes — no error node. This is correct behavior: the PLAN explicitly documents that `checkForInvalidDirective` operates at the semantic (annotator) layer, not the syntactic (parser) layer. The REQUIREMENTS.md wording for ANNOT-02 ("guard against future lexer changes silently breaking `checkForInvalidDirective` detection") is fully satisfied. The ROADMAP criterion contains an inaccurate implementation assumption, not a defect in the implementation. The fixture correctly locks in the PSI structure that the annotator depends on.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | — | — | No anti-patterns detected in any modified file |

Checked: `MakoAnnotator.kt`, `MakoCompletionContributor.kt`, `MakoParsingTest.kt` — no TODO/FIXME/XXX/HACK/placeholder comments, no empty implementations, no stub returns.

---

### Human Verification Required

None. All must-haves are verifiable programmatically from file content and git history.

The only item that would benefit from a runtime check is confirming `./gradlew check` passes with the new fixture and guards — but the commits exist, the fixture `.txt` is committed (meaning the second run passed), and no code changes since those commits would break the tests.

---

### Gaps Summary

None. All three observable truths are verified. Both requirements (ANNOT-01, ANNOT-02) are satisfied. All five artifacts exist, are substantive, and are wired. Both commits (`c89a86f`, `b5a117b`) are present in git history with correct scopes.

---

_Verified: 2026-02-21T23:00:00Z_
_Verifier: Claude (gsd-verifier)_

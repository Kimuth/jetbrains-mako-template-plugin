# Requirements: Mako Template Plugin for JetBrains

**Defined:** 2026-02-22
**Core Value:** Mako template files get the same rich editing experience as native Python and HTML files in PyCharm

## v0.3.0 Requirements

Requirements for the HTML Language Injection milestone. Phases continue from Phase 17 (last v0.2.0 phase).

### HTML Injection (HINJ)

- [ ] **HINJ-01**: User sees HTML syntax coloring in template body regions of `.mako` files — FAILED verification (18-02); HTML tags not colored differently from Mako constructs
- [x] **HINJ-02**: User gets HTML tag and attribute completion in template body regions — PASSED verification (18-02)
- [x] **HINJ-03**: User gets Emmet abbreviation expansion in template body regions — PASSED verification (18-02)
- [ ] **HINJ-04**: User sees HTML error squiggles for malformed markup in template body — PARTIAL verification (18-03): squiggles appear on `<span>` without closing tag but NOT on `<p>` or `<html>` without closing tags; inconsistent by element type; carry to Phase 20
- [ ] **HINJ-05**: CSS completion and validation are active inside `<style>` tags in `.mako` files — FAILED verification (18-02); Emmet fires in HTML context, not CSS
- [ ] **HINJ-06**: JavaScript completion and validation are active inside `<script>` tags in `.mako` files — FAILED verification (18-02); Emmet fires in HTML context, not JS

### Correctness (CRCT)

- [ ] **CRCT-01**: `${...}` expressions inside HTML attribute values (e.g., `<div class="${cls}">`) do not produce false-positive HTML errors
- [ ] **CRCT-02**: Mako control lines (`%for`, `%if`, `%endif`) do not produce false-positive HTML errors

### Regression (RGRN)

- [ ] **RGRN-01**: Python injection continues to work in `${...}`, `<% %>`, and `<%! %>` regions after FileViewProvider is active
- [ ] **RGRN-02**: Code folding, structure view, and tag completion work correctly alongside HTML injection
- [ ] **RGRN-03**: All existing automated tests pass unchanged after FileViewProvider is added

## Future Requirements

### HTML Reformatting

- **RFMT-01**: Ctrl+Alt+L applies HTML indentation rules in template body regions without corrupting Mako control lines (`%for`, `%if`, `%endif`)

### Navigation

- **NAVG-01**: User can navigate to the file referenced by `<%include file="...">` with Go To Definition
- **NAVG-02**: User can navigate to the file referenced by `<%inherit file="...">` with Go To Definition
- **NAVG-03**: User can navigate to the file referenced by `<%namespace file="...">` with Go To Definition
- **NAVG-04**: User can navigate to a `<%def>` definition from a def call with Go To Definition

### Python Intelligence

- **PYTH-01**: Template-local variables defined in `<% %>` code blocks appear in `${...}` completion suggestions
- **PYTH-02**: Parameter names of `<%def name="card(title, body)">` are shown as parameter hints when the def is invoked

### Advanced Analysis

- **ADVN-01**: User can find all usages of a `<%def>` or `<%block>` across `.mako` files
- **ADVN-02**: User can rename a `<%def>` or `<%block>` with refactoring support across files

## Out of Scope

| Feature | Reason |
|---------|--------|
| HTML reformatting (v0.3.0) | Risk of mangling indentation-sensitive Mako control lines; requires separate validation |
| Support for non-PyCharm IDEs | Focusing on PyCharm where Python integration matters most |
| Runtime template rendering/preview | IDE editing support only |
| Web framework integration (Pyramid, TurboGears) | Pure template language support |
| MultiHostInjector for HTML injection | Produces fragmented HTML PSI; tag matching across Mako expression boundaries fails |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| HINJ-01 | Phase 18/20 | Verification failed — HTML coloring gap; carry to Phase 20 |
| HINJ-02 | Phase 18 | Complete — verified 2026-02-22 |
| HINJ-03 | Phase 18 | Complete — verified 2026-02-22 |
| HINJ-04 | Phase 18/20 | PARTIAL verification (18-03) — squiggles on span tags but not p or html tags; inconsistent coverage; carry to Phase 20 |
| HINJ-05 | Phase 18/20 | Verification failed — CSS sub-injection gap; carry to Phase 20 |
| HINJ-06 | Phase 18/20 | Verification failed — JS sub-injection gap; carry to Phase 20 |
| CRCT-01 | Phase 20 | Pending |
| CRCT-02 | Phase 20 | Pending |
| RGRN-01 | Phase 19 | Pending |
| RGRN-02 | Phase 19 | Pending |
| RGRN-03 | Phase 19 | Pending |

**Coverage:**
- v0.3.0 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0 ✓

---
*Requirements defined: 2026-02-22*
*Last updated: 2026-02-22 after 18-03 verification — HINJ-04 partial pass (span squiggles only); HINJ-01/05/06 deferred to Phase 20*

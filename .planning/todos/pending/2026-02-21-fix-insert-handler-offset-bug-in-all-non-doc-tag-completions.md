---
created: 2026-02-21T22:26:41.312Z
title: Fix insert handler offset bug in all non-doc tag completions
area: completion
files:
  - src/main/kotlin/com/schtilig/mako/lang/completion/MakoCompletionContributor.kt:155
---

## Problem

Phase 14 fixed the `ctx.startOffset - 2` offset bug only for the `<%doc` insert handler (COMP-01 scope). The non-doc handler on line 155 has the identical bug:

```kotlin
ctx.document.replaceString(ctx.startOffset - 2, ctx.tailOffset, "$tagName ")
```

When partial text has been typed (e.g. `<%de`), `ctx.startOffset` points at the start of the completion item match — not at the `<` of `<%`. This causes the replacement to start 2 chars into whatever the platform considers the start offset, leaving a stale `<` prepended. User-observed symptom: typing `<%de` and accepting `<%def` produces `<<%def ` instead of `<%def `.

Affects all tag completions except `<%doc`: `<%def`, `<%block`, `<%namespace`, `<%inherit`, `<%include`, `<%text`, `<%call`, etc.

The fix is already in scope — `ltPos` is now computed via the `charsSequence` backward scan (Task 2 of 14-01) and is in scope at line 155. Replace `ctx.startOffset - 2` with `ltPos` on that line.

## Solution

In `TagNameCompletionProvider.addCompletions`, replace the single remaining `ctx.startOffset - 2` usage (line ~155) with `ltPos`:

```kotlin
// Before
ctx.document.replaceString(ctx.startOffset - 2, ctx.tailOffset, "$tagName ")

// After
ctx.document.replaceString(ltPos, ctx.tailOffset, "$tagName ")
```

One-line change. Then verify `./gradlew test --tests MakoCompletionTest` still passes. Ideally add a test case for partial-text completion on a non-doc tag (e.g. `<%de` → `<%def`).

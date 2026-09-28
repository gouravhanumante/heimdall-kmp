---
description: Keep Heimdall's docs true to the code. Applies to every change in this repo — core, UI, plugins, platform overlays, no-op artifacts, sample app and CI.
applyTo: "**"
---

# Docs stay true to the code

Heimdall is a library. Its docs are read by someone integrating it into *their* app, who cannot see
our code and will copy what the docs say verbatim. A doc that contradicts the code does not just
mislead — it produces a broken integration in someone else's project, and the bug report lands on
us looking like a library defect.

## The rule

**Any change that alters behaviour is not done until the docs that describe that behaviour are
updated in the same commit.**

Behaviour means anything a consuming app could notice: a public API symbol or signature, a Gradle
coordinate, an integration step, a default value, what is captured and what is redacted, what the
overlay does on a gesture, which platform supports what, what the no-op artifact does, and what ends
up in a release binary.

Exempt: internal refactors, renames of non-public symbols, formatting, comments, and test-only
changes — nothing a consumer can observe moved.

## Rules specific to a multiplatform library

1. **Platform claims are per-platform.** Never write "works on Android and iOS" unless it has been
   run on both. If it has only been verified on one, say so: "Android: verified. iOS: not yet
   verified." An unverified iOS claim is the most likely drift in this repo, because iOS is the
   platform we exercise least.
2. **The parity table is the source of truth for support.** `docs/platform-support.md` lists every
   feature × {Android, iOS} with one of: supported, partial (with the gap stated), not supported.
   Any change that adds, removes or degrades a feature on either platform updates that table.
3. **Every code snippet in the docs must compile.** Integration snippets are the part consumers copy.
   Prefer pointing at the sample app, which CI builds, over inline snippets. When an inline snippet
   is needed, it must match a real call site in `sample/` — if the sample would not compile with
   the snippet's code, the snippet is wrong.
4. **No-op parity is documented behaviour.** The no-op artifact must expose every public symbol of
   the real one. If a public symbol is added, removed or changed, the no-op changes in the same
   commit, and the docs say what the no-op does for it (usually: nothing, returns a neutral value).
5. **Release-safety claims need proof.** Any sentence of the form "not included in release builds",
   "no-op when disabled", "nothing is captured" must name how it was checked (APK/IPA inspected,
   test name, R8 mapping). These are the claims a consumer's security review will quote.
6. **Limits and defaults are stated with their value.** Buffer sizes, body truncation, retention,
   redacted headers — write the number and the symbol it lives in, not "a reasonable default".
7. **Known platform limitations are written down, not discovered.** E.g. if iOS crash capture only
   sees Kotlin exceptions and not signals, that goes in the plugin's doc and in the parity table.

## What to do before finishing a change

1. Search `docs/` and `README.md` for the concept you touched — the feature, the API name, the
   gesture, the default. Search for the *concept*, not just the identifier.
2. Update anything the change made **wrong**. That is the obligation. Adding prose for a new feature
   is good but secondary.
3. **Verify each surrounding claim you touch against the code** rather than trusting the paragraph
   you are editing. Drift clusters: a stale sentence usually has stale neighbours.
4. In your summary, say which docs you updated, or state that you checked and none applied. Silence
   is not evidence that the docs were fine.

Do not create a new doc file per change. Update the existing one that owns the topic.

## Where things live

| Changed | Check |
|---|---|
| Gradle coordinates, install steps, `install()`/entry-point API | `README.md`, `docs/integration.md` |
| Feature available / degraded on a platform | `docs/platform-support.md` |
| Overlay: bubble, drag, dismiss, shake-to-recall, panel navigation | `docs/overlay.md` |
| Network capture: what is recorded, redaction, body limits | `docs/plugins/network.md` |
| Database inspection: supported drivers, read/write, queries | `docs/plugins/database.md` |
| Key-value storage (DataStore / prefs) viewing and editing | `docs/plugins/storage.md` |
| Logs and crash capture | `docs/plugins/logs-crashes.md` |
| Feature-flag overrides | `docs/plugins/flags.md` |
| Writing a custom plugin, plugin API | `docs/plugins/custom.md` |
| Release safety, no-op artifact, what ships in release | `docs/release-builds.md` |
| Module graph, `expect`/`actual` boundaries, internal architecture | `docs/architecture.md` |
| A known gap or risk deliberately not fixed | `docs/TODO.md` |
| Anything a consumer must know when upgrading | `CHANGELOG.md` |

## CHANGELOG is the one history file

Unlike the design docs, `CHANGELOG.md` *is* a history, because consumers upgrade across versions
and need to know what changed. Every consumer-observable change adds an entry under
`## Unreleased`: Added / Changed / Fixed / Removed. Breaking changes are marked **Breaking** and say
what the consumer must do. All other docs describe the current library, not its history.

## Deliberately deferred work

If a change leaves a known hole — a platform left unsupported, a capture that can miss events, an
unredacted field, a decision postponed — record it in `docs/TODO.md` with the reason and the
options considered. An unwritten known gap is indistinguishable from an unnoticed bug to whoever
finds it next.

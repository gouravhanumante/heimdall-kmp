# Release builds

**Not implemented yet.** There is no no-op artifact, and no runtime "disabled" switch — every
module builds as its real self in every build type. Do not ship this into a release build; there
is currently no mechanism preventing it from doing something in one.

When this is built (tracked in `docs/TODO.md`), it will follow kmp-inspector's model:
- A `debugImplementation`/`releaseImplementation` split on Android, backed by a `-noop` artifact
  exposing the same public API with empty bodies.
- On iOS, `enabled` flags alone do not remove code — Kotlin/Native only dead-code-eliminates
  unused symbols if the consuming framework does not `export()` Heimdall. This needs to be
  proven with an inspected release `.ipa`/framework, not asserted, before this doc claims it works
  (see docs.instructions.md rule 5).

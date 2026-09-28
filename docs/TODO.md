# TODO / known gaps

Deliberately deferred work, per `.github/instructions/docs.instructions.md`.

- **iOS shake + overlay host unimplemented.** `IosShakeListener` is a stub. Lands with chunk 9
  (overlay hosts), together with deciding where the iOS overlay lives
  (`docs/architecture.md`, decision 1).
- **Back button doesn't close the panel.** Android's back press goes to the app instead.
  Needs Compose Multiplatform's `BackHandler`; not added yet.
- **Bubble position isn't saved across app restarts.** It survives hide/show within a run, but a
  new launch starts at the default position.
- **No no-op / release-safety mechanism.** See `docs/release-builds.md`. Must exist before any
  consumer is told it's safe to ship with Heimdall in the dependency graph.
- **Database, flags UI and persistence, logs/crash capture unbuilt** (chunks 5–7). `FlagStore`'s
  override logic exists but nothing uses it yet.
- **Overlay-window prototype (`docs/architecture.md`, decision 1) not attempted.** The bubble
  lives inside the wrapped root composable, so native sheets/screens outside it hide the bubble.
- **`Heimdall.install()` isn't enforced.** Recording before it throws at runtime
  (`HeimdallDatabase.requireConnection()`), not at compile time.
- **Keychain: only generic-password items.** Internet passwords, keys and certificates aren't
  listed. Most apps and Keychain wrappers use generic passwords.

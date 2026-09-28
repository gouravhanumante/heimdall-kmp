# TODO / known gaps

Deliberately deferred work, per `.github/instructions/docs.instructions.md`.

- **iOS shake + overlay host unimplemented.** `IosShakeListener` is a stub. Lands with chunk 9
  (overlay hosts), together with deciding where the iOS overlay lives
  (`docs/architecture.md`, decision 1).
- **Back handling is platform-hosted.** Android's sample uses `BackHandler` to close the SDK
  inspector first and then return from sample screens; consuming apps must connect their own
  navigation back callback to `HeimdallOverlayController.handleBack()`.
- **Bubble position isn't saved across app restarts.** It survives hide/show within a run, but a
  new launch starts at the default position.
- **No no-op / release-safety mechanism.** See `docs/release-builds.md`. Must exist before any
  consumer is told it's safe to ship with Heimdall in the dependency graph.
- **Database, flags UI and persistence, logs/crash capture** (chunks 5–7) are still not complete
  even though the core stores and panel wiring are now in progress. `FlagStore` has overrides and
  the panel can toggle them, but they remain in-memory only and are not durable across restart.
- **Database framework adapters.** `DatabaseInspector` is the shared attach/refresh contract, but
  Room, SQLDelight, and raw SQLite adapter modules still need to be implemented separately.
- **Overlay-window prototype (`docs/architecture.md`, decision 1) not attempted.** The bubble
  lives inside the wrapped root composable, so native sheets/screens outside it hide the bubble.
- **`Heimdall.install()` isn't enforced.** Recording before it throws at runtime
  (`HeimdallDatabase.requireConnection()`), not at compile time.
- **Keychain: only generic-password items.** Internet passwords, keys and certificates aren't
  listed. Most apps and Keychain wrappers use generic passwords.
- **Automatic Compose performance instrumentation.** `Heimdall.measure(...)` reports explicit
  durations, but recomposition counts, frame jank, and automatic screen render timing still need
  a Compose-specific integration with defined overhead limits.
- **Sample image requests are not yet routed through HeimdallKtor.** Coil loads them through its
  own configured fetcher, while the sample API/feed calls use the Heimdall-installed Ktor client.

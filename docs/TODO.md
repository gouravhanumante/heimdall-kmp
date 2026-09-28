# TODO / known gaps

Deliberately deferred work, per `.github/instructions/docs.instructions.md`.

- **Bubble goes behind dialogs and sheets.** `HeimdallOverlay` wraps the root composable, so
  Compose `Dialog`/`ModalBottomSheet`/`Popup` (separate windows) and native screens draw over it.
  Considered: an Android decor-view host (tried, removed — those windows still sit above the decor
  view); a per-window injector or `TYPE_APPLICATION_PANEL` window (not tried). On iOS a separate
  `UIWindow` covers this and is kept as a sample-only opt-in
  (`MainViewControllerWithNativeOverlayWindow()`), not yet run or extracted into the SDK.
- **Back handling is platform-hosted.** Android's sample uses `BackHandler` to close the SDK
  inspector first and then return from sample screens; consuming apps must connect their own
  navigation back callback to `HeimdallOverlayController.handleBack()`.
- **No no-op mirror for `heimdall-storage`, `heimdall-database-sqlite`, `heimdall-flags-firebase`.**
  `heimdall-core-noop`, `heimdall-ui-noop`, and `heimdall-network-ktor-noop` exist and are
  JVM-tested; these three collector modules don't have a `-noop` counterpart yet, so their code
  still runs if kept on a release build's classpath. See `docs/release-builds.md`.
- **iOS release swap mechanism is unresolved.** The no-op modules compile for `iosArm64`/
  `iosSimulatorArm64`, but there's no iOS equivalent of `debugImplementation`/
  `releaseImplementation` proven out yet — how a consumer picks the real framework for a debug
  scheme and the no-op one for release is still open. See `docs/release-builds.md`.
- **Database, logs/crash capture** (chunks 5–7) still need richer adapters and detail interactions.
  Flag overrides are durable with boolean, text, number, and per-flag reset controls.
- **Database framework adapters.** `DatabaseInspector` is the shared attach/refresh contract.
  `heimdall-database-sqlite`'s `SqliteFileInspector` covers Room/SQLDelight/raw SQLite by reading
  the on-disk file directly (works for any of them, since all three end up as a plain SQLite
  file) but only shows raw tables — it doesn't understand Room entities/relations or SQLDelight's
  generated queries, and it can't inspect an in-memory (`:memory:`) database. Not yet run against
  a real Room or SQLDelight database, only a hand-built fixture.
- **Firebase/remote-config flag adapter.** Android Firebase Remote Config discovery is implemented;
  iOS Firebase adapter parity and explicit type metadata for ambiguous string values remain.
- **`Heimdall.install()` isn't enforced.** Recording before it throws at runtime
  (`HeimdallDatabase.requireConnection()`), not at compile time.
- **Keychain: only generic-password items.** Internet passwords, keys and certificates aren't
  listed. Most apps and Keychain wrappers use generic passwords.
- **Automatic Compose performance instrumentation.** `Heimdall.measure(...)` reports explicit
  durations, and Android/iOS frame timing is implemented. Recomposition counts and automatic
  screen render timing still need a Compose-specific integration with defined overhead limits.
- **Runtime Compose recomposition counters.** Official compiler reports are enabled for `heimdall-ui`
  and the shared sample under `build/compose-compiler`; those are static stability facts, not
  runtime counts. A versioned optional compiler instrumentation plugin is still needed for strict
  runtime recomposition measurements.
- **Sample image requests are not yet routed through HeimdallKtor.** Coil loads them through its
  own configured fetcher, while the sample API/feed calls use the Heimdall-installed Ktor client.
- **Database search is currently snapshot filtering.** The UI filters loaded table rows on
  `Dispatchers.Default`, but true million-row search needs a paged, read-only query contract so
  filtering happens in the app database rather than after materializing every row.

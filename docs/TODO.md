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
- **Sample doesn't demonstrate the Android debug/release swap.** `sample/shared`'s Android target
  has no build-type variance in the `com.android.kotlin.multiplatform.library` DSL, so it always
  resolves the real modules for Android. The pattern itself (`debugImplementation`/
  `releaseImplementation` on a plain `com.android.application`) is standard Android/Gradle
  behavior, not something specific to Heimdall, and doesn't need proving here — but the sample
  can't show it without restructuring `sample/shared` to have real Android build-type variants.
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
- **Keychain: keys and certificates aren't listed.** Generic-password and internet-password items
  are (as of this pass); `kSecClassKey`/`kSecClassCertificate` aren't covered.
- **Automatic Compose performance instrumentation.** `Heimdall.measure(...)` reports explicit
  durations, and Android/iOS frame timing is implemented. Recomposition counts and automatic
  screen render timing still need a Compose-specific integration with defined overhead limits.
- **Runtime Compose recomposition counters.** Official compiler reports are enabled for `heimdall-ui`
  and the shared sample under `build/compose-compiler`; those are static stability facts, not
  runtime counts. A versioned optional compiler instrumentation plugin is still needed for strict
  runtime recomposition measurements. Confirmed design constraint: a Kotlin compiler plugin can
  only instrument a consumer's compilation if their build script applies it
  (`id("io.heimdall.recomposition")` or similar) — there is no way to get this "for free" from
  just adding a library dependency, the same way Compose itself needs
  `org.jetbrains.kotlin.plugin.compose` applied explicitly. Not started.
- **Sample image requests are not yet routed through HeimdallKtor.** Coil loads them through its
  own configured fetcher, while the sample API/feed calls use the Heimdall-installed Ktor client.
- **Database search has no pagination.** Search now runs live against the real table (a bound
  `LIKE` query per column, see `docs/plugins/database.md`), so it's no longer limited to whatever
  the initial snapshot happened to load — but results are still capped at 200 rows with no
  "load more" or scroll-triggered paging. Deliberately deferred: fixing "search misses rows outside
  the snapshot" was the actual correctness bug; scrolling through unbounded results is a separate,
  larger UI feature.

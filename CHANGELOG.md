# Changelog

All notable, consumer-observable changes to Heimdall are recorded here. See
`.github/instructions/docs.instructions.md` for what belongs here vs. in the design docs.

## Unreleased

### Changed
- Pinned Compose Multiplatform to `1.10.0` and Coil to `3.5.0` so Android artifacts remain
  compatible with AGP `9.0.1` and compileSdk `36`.
- **Breaking**: all stores now persist to Heimdall's own on-disk SQLite database (Android:
  `no_backup_files_dir`, excluded from auto-backup; iOS: Application Support) instead of an
  in-memory ring buffer. Data survives app restarts and crashes; `Heimdall.install(context)` must
  be called once at startup before anything is recorded — see `docs/architecture.md`.
- Every record is tagged with the app-run ("session") it happened in. `Heimdall.sessions()` lists
  past runs, `NetworkStore`/`LogStore`/`CrashStore.forSession(id)` reads one back. A session that
  had a fatal crash is flagged (`Session.crashed`).
- `NetworkStore`/`LogStore`/`CrashStore` now expose `current: StateFlow<List<_>>` (the live
  current session, newest-first) instead of a `snapshot()` function.
- `heimdall-ui`: bubble rewritten. Tap opens the panel (it didn't before); drag to the bottom
  "drop to hide" target hides it (before, any small drag left/up hid it); shake brings it back
  where it was, repeatably (before, it could never be hidden again after the first recall); it
  stays inside the screen. Panel gets a Close button and no longer lets taps through to the app.
- **Breaking**: `OverlayState`/`OverlayVisibility` removed (unused). iOS
  `discoverStandardUserDefaults()` replaced by `discoverUserDefaults()`/`discoverStorage()`.
- Storage: re-scan on Storage tab open (`StorageStore.refresh()`), UserDefaults suites, app's own
  UserDefaults keys only, iOS Keychain (generic passwords), Android Keystore aliases, one-call
  `Heimdall.discoverStorage(...)` per platform. Fixed: SharedPreferences change listeners could be
  garbage-collected (held weakly by Android); UserDefaults values could never be edited (`is
  NSString` never matches a bridged Kotlin `String`).
- Retention: history older than 24 hours is deleted (at startup and hourly while running), plus
  oldest-first caps per session (5,000 network records / 20,000 log entries / 200 crash records).
- Logs and crash records now have live panel rows; fatal crash records are shown alongside logs and
  the current session header displays a crash badge. Database snapshots expose live state and flag
  overrides have panel controls, including reset and boolean toggles. These changes are not yet
  device-verified.
- Added a bounded, session-persisted `EventStore` and `Heimdall.event(...)` for app-reported
  timeline events, plus `Heimdall.setCurrentScreen(...)` and scoped `Heimdall.screen(...)` for
  optional attribution. The panel now includes Overview and Timeline tabs.
- The shared sample is now a multi-screen harness (Network, Feed, Database, Storage, Logs) reached
  from an icon Home screen, each attributed via `Heimdall.setCurrentScreen(...)`. Network and Feed
  make real Ktor calls through `HeimdallKtor`; Feed renders remote images with Coil; Database uses
  bundled SQLite; Android Storage exercises discovered SharedPreferences.
- Added explicit `Heimdall.measure(...)` timing with bounded live performance records, Overview
  slow-operation counts, and matching timeline events. Automatic Compose recomposition metrics are
  intentionally not included yet.
- Reworked `HeimdallPanel` into a bounded floating inspector with an icon-only navigation rail,
  selected-section heading, and `Global` context fallback when no screen is attributed. The
  inspector is centered on screen.
- Bubble: on release it now springs to the nearest left/right edge (8dp inset) instead of staying
  wherever it was dropped; it tracks the finger without lag while dragging.
- Sample: added a typed Details flow that writes text, int, float, long, and boolean values to
  Android SharedPreferences; API actions now emit `SampleApi` Logcat messages; Android back closes
  the inspector before returning from sample screens.
- Inspector: Network rows open request/response detail with a copy-cURL action; Database shows table
  rows; Storage and Logs support search; Timeline is no longer a primary navigation destination.
- Inspector data presentation: Network payloads are selectable monospace code blocks; Database and
  Storage use structured tables; Logs and Crashes use contained severity records instead of raw text
  streams.
- Added shared `HeimdallDesign` tokens and improved popup behavior: 94% centered sizing, outside-tap
  dismissal, icon-only close, Network URL search, per-table Database search, searchable Flags, and
  Overview quick actions/critical issue surfaces.
- Sample database now includes `users` and `profiles` tables. Android shake recall samples at game
  rate and posts state changes to the main thread; the inspector has stable minimum dimensions and
  a device-aware light/dark shell palette.
- Database navigation now lists tables before opening one; Network detail separates status metadata
  from a full Copy cURL button, uses an icon Back action, and provides descriptive icons throughout
  the navigation rail and copy/close actions.
- Added the supplied Heimdall horn artwork as an Android-safe PNG Compose resource; the original SVG
  was rasterized because Compose Android does not decode SVG resources directly.
- Sample controls now use a shared Heimdall Material theme with horn-gold primary actions,
  watchman-blue secondary accents, consistent outlines, and rounded input/button shapes.
- Added offline Health rules and an Overview Health table. Measured crashes, network errors, and
  slow operations are reported with confidence; unsupported Compose metrics are omitted.
- Flag overrides now persist as typed values in Heimdall's own SQLite database and restore during
  installation. Official Compose compiler reports/metrics are enabled for `heimdall-ui` and the
  shared sample; runtime recomposition counters remain unavailable until an optional instrumentation
  plugin exists.
- Android sample now reports live Choreographer frame intervals to Overview/Health; frame samples
  stay in memory and are not persisted.
- Added a Sessions view for historical network/log/crash/event browsing while keeping database,
  storage, flags, and frame state live-only.
- Flags now support persisted text and number editors plus per-flag reset actions in addition to
  boolean toggles.
- Added the one-time `FlagCatalog` attachment contract so provider adapters can discover all known
  flag keys and still receive Heimdall overrides without per-key registration code.
- Added optional Android `heimdall-flags-firebase`: `FirebaseRemoteConfig.attachToHeimdall()`
  discovers keys and returns an override-aware provider without per-key definitions.
- Flags expose an optional host restart handler; the panel shows Restart app only when configured.
- iOS frame intervals now use `CADisplayLink` after `Heimdall.install(...)`; iOS shake/overlay host
  forwarding is now implemented through the sample UIKit responder host. The iOS sample defaults to
  the shared `HeimdallOverlay`; `MainViewControllerWithNativeOverlayWindow()` opts into a separate
  always-on-top `UIWindow` with passthrough hit testing.
- The shared `HeimdallOverlay` wrapper is the overlay on both platforms. Known limitation: Compose
  `Dialog`/`ModalBottomSheet`/`Popup` draw over the bubble.
- `Heimdall.install(...)` now owns Android uncaught-crash capture and frame-monitor startup; sample
  integration no longer duplicates those hooks.
- Added `DatabaseInspector`, `DatabaseStore.attach(...)`, and refresh-on-open behavior for app
  database adapters. Heimdall still never owns or migrates the app database; Room, SQLDelight, and
  raw SQLite adapter modules remain separate work.

### Added
- The Sessions tab now shows a readable UTC date/time (`formatSessionTimestamp`) instead of raw
  epoch milliseconds.
- Added `Heimdall.bubblePosition`: the bubble's resting position (as a screen fraction) now
  survives an app restart, restored on `Heimdall.install(...)`/`installInMemory()`. Previously it
  only survived hide/show within one run.
- Added `heimdall-core-noop`, `heimdall-ui-noop`, and `heimdall-network-ktor-noop`: drop-in,
  same-package no-op counterparts for release builds, wired via
  `debugImplementation`/`releaseImplementation` on Android. `Heimdall.measure`/`screen` still run
  their block; `Heimdall.flags.attach(...)` still delegates to the app's real flag provider so
  production feature flags are unaffected. `heimdall-storage`, `heimdall-database-sqlite`, and
  `heimdall-flags-firebase` don't have a no-op counterpart yet, and the iOS debug/release swap
  mechanism is unresolved — see `docs/release-builds.md`.
- Added optional `heimdall-database-sqlite`: `SqliteFileInspector`, a `DatabaseInspector` that
  reads any on-disk SQLite file by path — covers Room, SQLDelight, and raw SQLite databases with
  one adapter, since all three are a plain SQLite file underneath. Opens its own read-only
  connection; the ad-hoc query box only accepts `SELECT`/`PRAGMA`/`EXPLAIN`/`WITH`. Not usable
  with an in-memory (`:memory:`) database. See `docs/plugins/database.md`.
- `heimdall-core`: `ShakeDetector` (shared threshold/debounce logic) and `AndroidShakeListener`
  (Android accelerometer wiring). iOS listener is a stub — see `docs/TODO.md`.
- `heimdall-ui`: `HeimdallOverlay` composable — wrap a screen's root content once to get the
  draggable bubble and, on tap, the panel. `HeimdallOverlayController.recall()` brings back a
  hidden bubble.
- `heimdall-ui`: `HeimdallPanel` — tab shell for Overview / Timeline / Network / Database / Storage / Logs / Flags, with
  live network, storage, database metadata, log, crash, and flag content where a collector has
  published data.
- `sample/androidApp`: reference integration wiring `HeimdallOverlay` + `AndroidShakeListener`
  into an Activity.

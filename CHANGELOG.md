# Changelog

All notable, consumer-observable changes to Heimdall are recorded here. See
`.github/instructions/docs.instructions.md` for what belongs here vs. in the design docs.

## Unreleased

### Changed
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
- Added `DatabaseInspector`, `DatabaseStore.attach(...)`, and refresh-on-open behavior for app
  database adapters. Heimdall still never owns or migrates the app database; Room, SQLDelight, and
  raw SQLite adapter modules remain separate work.

### Added
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

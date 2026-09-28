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

### Added
- `heimdall-core`: `ShakeDetector` (shared threshold/debounce logic) and `AndroidShakeListener`
  (Android accelerometer wiring). iOS listener is a stub — see `docs/TODO.md`.
- `heimdall-ui`: `HeimdallOverlay` composable — wrap a screen's root content once to get the
  draggable bubble and, on tap, the panel. `HeimdallOverlayController.recall()` brings back a
  hidden bubble.
- `heimdall-ui`: `HeimdallPanel` — tab shell for Network / Database / Storage / Logs / Flags.
  All tab bodies are placeholders; no collector is implemented yet.
- `sample/androidApp`: reference integration wiring `HeimdallOverlay` + `AndroidShakeListener`
  into an Activity.

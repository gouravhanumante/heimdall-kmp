# Platform support

One row per feature. "Verified" means run and observed on that platform, not just compiled.

| Feature | Android | iOS |
|---|---|---|
| `heimdall-core`/`heimdall-ui` compile for the platform | Compiles | Compiles (`iosSimulatorArm64`) |
| Bubble: drag, tap to open, drag to bottom to hide | Rewritten, not compiled by me yet, not run | Shared Compose code; no iOS host yet |
| Shake-to-recall | Implemented, not run on a device. `ShakeDetector` logic unit-tested | Not implemented — `IosShakeListener` is a stub |
| Panel shell (tabs, close button) | Not run on a device | No iOS host yet |
| Overlay lives above native screens/sheets | Not decided — see `docs/architecture.md` decision 1 | Not decided — same |
| Network capture (Ktor plugin) | Implemented, JVM-tested with `MockEngine`. No UI yet | Compiles. Not run |
| Persistence (own SQLite DB, sessions, retention) | Implemented, JVM-tested (`sqlite-bundled-jvm` for host tests) | Compiles (`sqlite-bundled` for `iosArm64`/`iosSimulatorArm64`). Not run |
| Database inspector | Core `DatabaseInspector` adapter API and panel refresh exist; Room/SQLDelight/raw SQLite adapters not included | Same shared API; no framework adapters included |
| Storage: DataStore (`attachDataStore`) | Implemented, JVM-tested. No UI yet | Compiles. Not run |
| Storage: SharedPreferences / UserDefaults discovery, re-scan on tab open | Written, not compiled by me yet, not run | Written, not compiled by me yet, not run (text values editable only) |
| Storage: Android Keystore (aliases, read-only) / iOS Keychain (generic passwords, editable) | Written, not compiled by me yet, not run | Written, not compiled by me yet, not run |
| Logs viewer | Core capture and panel view implemented; not run | Shared core/UI code; not run |
| Crash capture | Android sample uncaught-handler wiring and panel view implemented; not run | No platform uncaught-handler wiring |
| Feature-flag overrides | In-memory override API and panel controls implemented; not run | Shared core/UI code; not run |
| No-op / release-safe artifact | Not implemented | Not implemented |

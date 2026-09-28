# Platform support

One row per feature. "Verified" means run and observed on that platform, not just compiled.

| Feature | Android | iOS |
|---|---|---|
| `heimdall-core`/`heimdall-ui` compile for the platform | Compiles | Compiles (`iosSimulatorArm64`) |
| Bubble: draggable, swipe-to-edge dismiss | Implemented, not run on a device | Not implemented |
| Shake-to-recall | Implemented, not run on a device. `ShakeDetector` logic unit-tested | Not implemented — `IosShakeListener` is a stub |
| Panel shell (tab navigation) | Implemented, not run on a device (empty tab bodies) | Not implemented — no host set up yet |
| Overlay lives above native screens/sheets | Not decided — see `docs/architecture.md` decision 1 | Not decided — same |
| Network capture (Ktor plugin) | Implemented, JVM-tested with `MockEngine`. No UI yet | Compiles. Not run |
| Persistence (own SQLite DB, sessions, retention) | Implemented, JVM-tested (`sqlite-bundled-jvm` for host tests) | Compiles (`sqlite-bundled` for `iosArm64`/`iosSimulatorArm64`). Not run |
| Database inspector | Not implemented | Not implemented |
| Storage: DataStore (`attachDataStore`) | Implemented, JVM-tested. No UI yet | Compiles. Not run |
| Storage: SharedPreferences / UserDefaults auto-discovery | Compiles. Not run | Compiles. Not run (text values editable only) |
| Storage: Keychain / Keystore | Not implemented | Not implemented |
| Logs viewer | Not implemented | Not implemented |
| Crash capture | Not implemented | Not implemented |
| Feature-flag overrides | Not implemented | Not implemented |
| No-op / release-safe artifact | Not implemented | Not implemented |

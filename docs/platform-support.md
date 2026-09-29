# Platform support

One row per feature. "Verified" means run and observed on that platform, not just compiled.

| Feature | Android | iOS |
|---|---|---|
| `heimdall-core`/`heimdall-ui` compile for the platform | Compiles | Compiles (`iosSimulatorArm64`) |
| Bubble: drag, tap to open, drag to bottom to hide | Rewritten, not compiled by me yet, not run | Compiles for `iosSimulatorArm64`, including the sample's iOS host (`MainViewController.kt`, verified this session); not run |
| Shake-to-recall | Implemented, not run on a device. `ShakeDetector` logic unit-tested | `IosShakeListener.notifyShakeDetected()` plus the sample's `ShakeHostViewController` responder-chain forwarding compile for `iosSimulatorArm64`; not run |
| Panel shell (tabs, close button) | Not run on a device | `heimdall-ui` and the sample's iOS host compile for `iosSimulatorArm64`; not run |
| Overlay lives above native screens/sheets | Not supported: `HeimdallOverlay` wraps the root composable, so Compose `Dialog`/`ModalBottomSheet`/`Popup` (own windows) cover the bubble | Partial: default is the same shared overlay; opt-in sample `MainViewControllerWithNativeOverlayWindow()` hosts it in a separate `UIWindow` — implemented, not run |
| Network capture (Ktor plugin) | Implemented, JVM-tested with `MockEngine`. No UI yet | Compiles. Not run |
| Persistence (own SQLite DB, sessions, retention) | Implemented, JVM-tested (`sqlite-bundled-jvm` for host tests) | Compiles (`sqlite-bundled` for `iosArm64`/`iosSimulatorArm64`). Not run |
| Database inspector | Core `DatabaseInspector` adapter API and panel refresh exist. `heimdall-database-sqlite`'s `SqliteFileInspector` covers Room/SQLDelight/raw SQLite by file path (JVM-tested, 5 tests) | Same shared API and adapter; compiles for `iosArm64`/`iosSimulatorArm64`, not run |
| Storage: DataStore (`attachDataStore`) | Implemented, JVM-tested. No UI yet | Compiles. Not run |
| Storage: SharedPreferences / UserDefaults discovery, re-scan on tab open | Written, not compiled by me yet, not run | Compiles for `iosSimulatorArm64` (verified this session); not run (text values editable only) |
| Storage: Android Keystore (aliases, read-only) / iOS Keychain (generic + internet passwords, editable) | Written, not compiled by me yet, not run | Compiles for `iosSimulatorArm64` (verified this session); not run |
| Logs viewer | Core capture and panel view implemented; not run | `heimdall-core`/`heimdall-ui` compile for `iosSimulatorArm64`; not run |
| Crash capture | Android sample uncaught-handler wiring and panel view implemented; not run | No platform uncaught-handler wiring |
| Feature-flag overrides | In-memory override API and panel controls implemented; not run | Compiles for `iosSimulatorArm64`; not run |
| Live frame timing | Android `Choreographer` monitor installed by `Heimdall.install`; not run | iOS `CADisplayLink` monitor installed by `Heimdall.install`; compiles for `iosSimulatorArm64`, not run |
| No-op / release-safe artifact | All 6 modules have a `-noop` counterpart, JVM-tested (9 tests); demonstrated only via a plain `debugImplementation`/`releaseImplementation` pattern, not proven in this repo's own Android sample (see docs/TODO.md) | All 6 compile for `iosArm64`/`iosSimulatorArm64`; the `CONFIGURATION`-env-var swap is implemented and verified end-to-end in `sample/shared` (compiles all 3 ways: unset/`Debug`/`Release`), not run on a device |

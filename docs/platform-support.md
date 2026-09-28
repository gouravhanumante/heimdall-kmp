# Platform support

One row per feature. "Verified" means run and observed on that platform, not just compiled.

| Feature | Android | iOS |
|---|---|---|
| `heimdall-core`/`heimdall-ui` compile for the platform | Compiles | Compiles (`iosSimulatorArm64`) |
| Bubble: drag, tap to open, drag to bottom to hide | Rewritten, not compiled by me yet, not run | Shared Compose code; no iOS host yet |
| Shake-to-recall | Implemented, not run on a device. `ShakeDetector` logic unit-tested | UIKit responder host implemented in sample; not run |
| Panel shell (tabs, close button) | Not run on a device | No iOS host yet |
| Overlay lives above native screens/sheets | Not supported: `HeimdallOverlay` wraps the root composable, so Compose `Dialog`/`ModalBottomSheet`/`Popup` (own windows) cover the bubble | Partial: default is the same shared overlay; opt-in sample `MainViewControllerWithNativeOverlayWindow()` hosts it in a separate `UIWindow` — implemented, not run |
| Network capture (Ktor plugin) | Implemented, JVM-tested with `MockEngine`. No UI yet | Compiles. Not run |
| Persistence (own SQLite DB, sessions, retention) | Implemented, JVM-tested (`sqlite-bundled-jvm` for host tests) | Compiles (`sqlite-bundled` for `iosArm64`/`iosSimulatorArm64`). Not run |
| Database inspector | Core `DatabaseInspector` adapter API and panel refresh exist. `heimdall-database-sqlite`'s `SqliteFileInspector` covers Room/SQLDelight/raw SQLite by file path (JVM-tested, 5 tests) | Same shared API and adapter; compiles for `iosArm64`/`iosSimulatorArm64`, not run |
| Storage: DataStore (`attachDataStore`) | Implemented, JVM-tested. No UI yet | Compiles. Not run |
| Storage: SharedPreferences / UserDefaults discovery, re-scan on tab open | Written, not compiled by me yet, not run | Written, not compiled by me yet, not run (text values editable only) |
| Storage: Android Keystore (aliases, read-only) / iOS Keychain (generic passwords, editable) | Written, not compiled by me yet, not run | Written, not compiled by me yet, not run |
| Logs viewer | Core capture and panel view implemented; not run | Shared core/UI code; not run |
| Crash capture | Android sample uncaught-handler wiring and panel view implemented; not run | No platform uncaught-handler wiring |
| Feature-flag overrides | In-memory override API and panel controls implemented; not run | Shared core/UI code; not run |
| Live frame timing | Android `Choreographer` monitor installed by `Heimdall.install`; not run | iOS `CADisplayLink` monitor installed by `Heimdall.install`; not run |
| No-op / release-safe artifact | `heimdall-core-noop`, `heimdall-ui-noop`, `heimdall-network-ktor-noop` implemented and JVM-tested (9 tests); `heimdall-storage`/`heimdall-database-sqlite`/`heimdall-flags-firebase` have none yet | Compiles for `iosArm64`/`iosSimulatorArm64`; the debug/release swap mechanism itself is unresolved, not run |

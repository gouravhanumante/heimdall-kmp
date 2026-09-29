# Developer Integration Guide

This guide covers the currently published `0.1.0-alpha02` artifacts. Heimdall is a set of
explicit integrations, not an automatic app-wide instrumentor: adding the dependency and showing
the overlay does not by itself populate every inspector tab.

## 1. Add Maven Central Artifacts

Ensure the consuming build resolves dependencies from `mavenCentral()` (usually in
`settings.gradle.kts`). The complete real bundle is `heimdall`; the matching release bundle is
`heimdall-noop`:

```kotlin
dependencies {
        debugImplementation("io.github.gouravhanumante:heimdall:0.1.0-alpha02")
        releaseImplementation("io.github.gouravhanumante:heimdall-noop:0.1.0-alpha02")
}
```

For a smaller footprint, add `heimdall-core` and `heimdall-ui`, then only the collectors your app
uses. Never put a real module and its `-noop` counterpart in the same variant. The published
group is `io.github.gouravhanumante`; the checked-in sample instead uses project dependencies to
exercise unreleased source, so its local coordinates are not consumer coordinates.

| Artifact | Provides | No-op counterpart |
|---|---|---|
| `heimdall-core` | Stores, sessions, logs, crashes, events, screen attribution, performance and flag contracts | `heimdall-core-noop` |
| `heimdall-ui` | Compose bubble, panel, inspector tabs and overlay controller | `heimdall-ui-noop` |
| `heimdall-network-ktor` | Ktor client plugin that reports requests into the network store | `heimdall-network-ktor-noop` |
| `heimdall-storage` | Platform storage discovery and DataStore attachment | `heimdall-storage-noop` |
| `heimdall-database-sqlite` | Read-only adapter for an existing on-disk SQLite database | `heimdall-database-sqlite-noop` |
| `heimdall-flags-firebase` | Android Firebase Remote Config adapter | `heimdall-flags-firebase-noop` |

The `heimdall` and `heimdall-noop` umbrella artifacts combine these modules. The Firebase adapter
is Android-only. Kotlin Multiplatform consumers use the same base coordinates; Gradle selects the
platform-specific publication. iOS release swapping is based on Xcode's `CONFIGURATION` value;
follow [release-build setup](release-builds.md) rather than copying Android variant syntax.

## 2. Install At Startup

Call `Heimdall.install(...)` once before using stores or collectors. It opens Heimdall's own
persistent database, starts a session, restores persisted flags/bubble position and installs the
platform crash hook where available. It never opens or migrates your app database.

On Android, call it from your `Application.onCreate()` and register that class with
`android:name` in the app manifest. The sample's actual entry points are
[SampleApp.kt](../sample/androidApp/src/main/kotlin/io/heimdall/sample/SampleApp.kt) and
[AndroidManifest.xml](../sample/androidApp/src/main/AndroidManifest.xml).

```kotlin
Heimdall.install(PlatformContext(this))
```

On iOS, call `Heimdall.install(PlatformContext())` when constructing the main view controller;
see [MainViewController.kt](../sample/shared/src/iosMain/kotlin/io/heimdall/sample/shared/MainViewController.kt).
Android's uncaught exceptions are captured and mark the current session crashed. iOS has no
automatic uncaught-exception hook; call `Heimdall.recordCrash(...)` for handled Kotlin exceptions.

## 3. Show The Inspector

Wrap the app's root Compose content in `HeimdallOverlay`. It draws the floating bubble and panel;
it does not install collectors or platform shake handling. See the complete [Android host](../sample/androidApp/src/main/kotlin/io/heimdall/sample/MainActivity.kt)
or [iOS host](../sample/shared/src/iosMain/kotlin/io/heimdall/sample/shared/MainViewController.kt)
for the controller lifecycle and the root-content call.

For shake-to-recall, create and retain a `HeimdallOverlayController`, then connect
`AndroidShakeListener` or `IosShakeListener` to `controller.recall()`. Android must start/stop its
listener with the Activity lifecycle. iOS needs its host `UIResponder` to forward shake events to
`IosShakeListener.notifyShakeDetected()`. The sample has working host wiring for
[Android](../sample/androidApp/src/main/kotlin/io/heimdall/sample/MainActivity.kt) and
[iOS](../sample/shared/src/iosMain/kotlin/io/heimdall/sample/shared/MainViewController.kt).
See [overlay behavior](overlay.md) for bubble gestures, dismiss behavior and platform limits.

## 4. Connect Collectors

### Network

The network tab records only requests made by Ktor clients where `HeimdallKtor` is installed.
Install it on every client to include; adding the artifact alone does not change existing clients.
The in-repo sample call is:

```kotlin
val client = remember { HttpClient { install(HeimdallKtor) } }
```

Records contain method, URL, request/response headers, status, duration and errors. Request-body
previews use Ktor's body representation and currently have no size cap; byte-array bodies are
represented by size. Text-like response bodies are retained up to 1,000,000 characters
(`maxCapturedBodyBytes`); larger text is replaced with a size note and non-text response bodies
have no readable body preview. Request headers are unredacted by default; `redactedHeaders` can
mask selected request headers. Response headers are recorded as returned. Captured content is
stored locally and may contain credentials or personal data. The
[sample client](../sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt)
exercises successful, failed and POST requests.

### Storage

After install, call `Heimdall.discoverStorage(...)` for platform stores. Android discovers
SharedPreferences and Keystore aliases; iOS discovers UserDefaults and Keychain data. Pass app
group suite names on iOS when those suites should be included. Discovery sources are refreshed
when the Storage tab opens. DataStore is different: pass Heimdall the app's already-open instance
and a long-lived coroutine scope rather than having Heimdall open the same file a second time.
Storage shows current values, not per-session history. Edit support and source limitations are
listed in [storage details](plugins/storage.md).

### Database

Attach `SqliteFileInspector` after the app has created its on-disk SQLite file. It opens a separate
read-only connection; it cannot inspect `:memory:` databases. The sample uses this call:

```kotlin
Heimdall.database.attach(SqliteFileInspector(databaseName = "demo_notes.db", path = dbPath))
```

Call `Heimdall.database.refresh("demo_notes.db")` after writes when the view should refresh
immediately. The inspector lists tables, initially loads up to 200 rows per table, supports live
search capped at 200 results, and allows only read-only `SELECT`, `PRAGMA`, `EXPLAIN` or `WITH`
queries. Blobs are shown as byte counts. See [database details](plugins/database.md).

### Logs And Crashes

Heimdall does not import Android Logcat or system logs automatically. Send app-owned log records
with `Heimdall.log(...)`; `Heimdall.recordCrash(...)` records a handled exception and its stack
trace. On Android, the uncaught-exception handler installed by `Heimdall.install(...)` records
fatal uncaught exceptions. On iOS, there is no automatic crash hook.

### Feature Flags

Register the flags the app wants to expose using `Heimdall.flags.attach(...)` with a
`FlagCatalog`/provider, or use `heimdall-flags-firebase` for Android Firebase Remote Config.
Read flags through the provider returned from `attach`; panel overrides only affect those reads.
The sample demonstrates the catalog/provider contract in its
[flags screen](../sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt).

### Events, Screens And Timing

Use `Heimdall.event(...)` for app-reported timeline entries and `Heimdall.setCurrentScreen(...)`
or the scoped `Heimdall.screen(...)` API for attribution. Heimdall does not infer screens or
events automatically. `Heimdall.measure(name) { ... }` records an explicit duration and a matching
timeline event; it does not automatically profile frames or Compose recompositions.

## 5. Understand What The User Sees

- The bubble opens the panel; drag it to the bottom dismiss target to hide it. Shake-to-recall
    works only if the platform host is wired as described above.
- Overview and Timeline show the current session's health summary and events. Network, Logs,
    Crashes and Events are session history; Storage, Database and Flags show current live state.
- Heimdall keeps the 3 most recent sessions and applies a 24-hour history retention limit. Per-store
    record caps and remaining feature limitations are documented in [platform support](platform-support.md).
- `Heimdall.enabled = false` disables recording at runtime in the real artifacts; it does not
    remove SDK code or dependencies. Use `-noop` artifacts for release builds instead.

See [release-build setup](release-builds.md), [platform support](platform-support.md), and the
[changelog](../CHANGELOG.md) before upgrading. The API is alpha and may change between releases.

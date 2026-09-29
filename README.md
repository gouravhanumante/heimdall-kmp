# Heimdall

In-app debug inspector for Kotlin Multiplatform / Compose Multiplatform apps — network,
database, storage, logs, crashes and feature-flag overrides, behind a floating bubble you can
drag away to hide and bring back with a shake.

**Status: alpha.** Android and iOS simulator builds are verified; the reference sample exercises
the overlay, session history, Ktor network capture, SQLite database inspection, storage, logs,
crashes, and feature-flag overrides. See [docs/TODO.md](docs/TODO.md) and
[docs/platform-support.md](docs/platform-support.md) for platform-specific verification status.

## Modules

| Module | Contents |
|---|---|
| `heimdall-core` | Platform-agnostic logic: shake detection, sessions, collectors, events and screen attribution. No UI. |
| `heimdall-ui` | Compose Multiplatform bubble, overview, timeline and inspector tabs. |
| `heimdall-network-ktor` | Ktor `HttpClient` plugin that reports into `Heimdall.network`. |
| `heimdall-storage` | DataStore/SharedPreferences/UserDefaults/Keystore/Keychain discovery. |
| `heimdall-database-sqlite` | `DatabaseInspector` adapter for any on-disk SQLite file (Room, SQLDelight, raw). |
| `heimdall-flags-firebase` | Firebase Remote Config flag adapter (Android). |
| `heimdall` | Recommended complete bundle: core, UI, network, storage, SQLite, and Android Firebase flags. |
| `heimdall-core-noop`, `heimdall-ui-noop`, `heimdall-network-ktor-noop`, `heimdall-storage-noop`, `heimdall-database-sqlite-noop`, `heimdall-flags-firebase-noop` | Same-API, do-nothing counterparts for release builds — see `docs/release-builds.md`. |
| `heimdall-noop` | Complete no-op bundle for release variants. |
| `sample/androidApp`, `sample/shared`, `sample/iosApp` | Reference integration wiring the modules together. |

See [docs/architecture.md](docs/architecture.md) for the module graph and the design decisions
behind it, and [docs/overlay.md](docs/overlay.md) for exactly what the bubble/panel does.

## Quick Start

Heimdall is published to Maven Central. Make sure `mavenCentral()` is in your dependency
repositories, then add the real bundle to Android debug and the no-op bundle to production release:

```kotlin
dependencies {
    debugImplementation("io.github.gouravhanumante:heimdall:0.1.0-alpha02")
    releaseImplementation("io.github.gouravhanumante:heimdall-noop:0.1.0-alpha02")
}
```

The real bundle includes core, UI, Ktor capture, storage, SQLite inspection, and the Android
Firebase Remote Config adapter. For a smaller footprint, use the individual artifacts instead;
the in-repo [integration guide](docs/integration.md) lists each coordinate and its no-op pairing.
Kotlin Multiplatform consumers use the same base coordinates; Gradle selects the platform artifact.

Install once at startup. On Android, call this from your `Application.onCreate()` and register
that `Application` class in the manifest:

```kotlin
Heimdall.install(PlatformContext(this))
```

On iOS, install with `PlatformContext()` when creating the app's main view controller. Wrap the
root composable in `HeimdallOverlay`; for the complete controller setup and platform host wiring,
use the working [Android entry point](sample/androidApp/src/main/kotlin/io/heimdall/sample/MainActivity.kt),
[Android Application](sample/androidApp/src/main/kotlin/io/heimdall/sample/SampleApp.kt), and
[iOS host](sample/shared/src/iosMain/kotlin/io/heimdall/sample/shared/MainViewController.kt) as
references. Shake-to-recall requires forwarding platform shake events to the overlay controller;
the overlay alone does not install that host behavior.

## What You Need To Connect

Adding the dependency and showing the overlay does not automatically instrument every app API.
Each collector below needs the corresponding app-side integration.

| Inspector area | What appears | What your app must do |
|---|---|---|
| Network | Ktor method, URL, timing, status, headers, and supported body previews | Install `HeimdallKtor` on each Ktor `HttpClient` whose calls you want recorded. See the [sample client](sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt). |
| Storage | Current values from supported platform stores; some values can be edited | Call `Heimdall.discoverStorage(...)` after install. Pass the existing DataStore instance to Heimdall separately; see [storage details](docs/plugins/storage.md). |
| Database | Tables, an initial row snapshot, search, and read-only queries | Attach `SqliteFileInspector` to an existing on-disk SQLite file after it exists. Call `Heimdall.database.refresh(name)` after writes you want reflected immediately. See the [sample database screen](sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt) and [database limits](docs/plugins/database.md). |
| Logs | App-reported log records, searchable by severity | Call `Heimdall.log(...)`; Heimdall does not automatically import Android Logcat or iOS system logs. |
| Crashes | Fatal Android uncaught exceptions and app-reported exceptions | Android uncaught exceptions are recorded after `Heimdall.install(...)`. Call `Heimdall.recordCrash(...)` for handled exceptions. iOS has no automatic uncaught-exception hook. |
| Feature flags | Registered flag values and local overrides | Attach a `FlagCatalog`/provider, or add the Android-only Firebase adapter. Overrides affect reads made through the returned provider; see the [sample flags screen](sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt). |
| Timeline and sessions | App-reported events, screen attribution, explicit timings, and recent launches | Call the core event/screen APIs where useful. `Heimdall.measure(...)` records a named duration; screen attribution is not inferred automatically. |

The sample's [shared app and collector exercises](sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt)
show the calls in context. The checked-in sample depends on this repository's project modules so
it can test unreleased code; its local Gradle setup is not the Maven setup shown above.

## Capture And Privacy

Network capture is opt-in per Ktor client. By default request headers are recorded without
redaction, and response headers are recorded as received. Request body previews use Ktor's body
representation and currently have no size cap; byte-array requests are shown as binary-size notes.
Text-like response bodies are kept up to 1,000,000 characters (`maxCapturedBodyBytes`); larger
text responses are represented by a size note, and non-text response bodies are not stored as
readable previews. Captured data is stored in Heimdall's on-device history, so think carefully
before enabling it against real user data.

`Heimdall.install(...)` creates Heimdall's own persistent database and starts a session; it never
opens or migrates the app's database. The most recent 3 sessions are retained, with the existing
24-hour age limit. Storage, database, and flag views are live values rather than per-session
history. See [platform support](docs/platform-support.md) for platform-by-platform verification.

## Release Builds

Use the matching `-noop` artifacts for production builds. They preserve the API so shared code
still compiles, but render no inspector and do not capture data. See
[release-build setup](docs/release-builds.md), including custom Android variants and the iOS
`CONFIGURATION`-based swap.

## Try it

```
./gradlew :sample:androidApp:installDebug
```

## Release builds

Every module above always runs its real implementation — do not add them to a release build's
dependencies as-is. Every module has a `-noop` counterpart to swap in via `releaseImplementation`
instead; see [docs/release-builds.md](docs/release-builds.md) for exactly what is and isn't
covered yet.

## License

Apache-2.0.

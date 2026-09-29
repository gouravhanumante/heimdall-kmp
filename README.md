# Heimdall

In-app debug inspector for Kotlin Multiplatform / Compose Multiplatform apps — network,
database, storage, logs, crashes and feature-flag overrides, behind a floating bubble you can
drag away to hide and bring back with a shake.

**Status: alpha.** Android and iOS simulator builds are verified; the reference sample exercises
the overlay, Ktor capture, the generic `DatabaseInspector` contract, Android SharedPreferences,
logs, events, and flag registration. It does not currently demonstrate the SQLite file adapter,
DataStore, Firebase Remote Config, crash reporting, or provider-backed flag overrides. See
[docs/TODO.md](docs/TODO.md) and
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

## Connect Collectors

Adding the dependency and showing the overlay does not instrument every app API. Connect only the
collectors your app uses. The [checked-in sample](sample/shared/src/commonMain/kotlin/io/heimdall/sample/shared/SampleApp.kt)
shows Ktor, generic database, storage, event, and log calls; its local Gradle setup uses project
modules to test unreleased code, not the Maven coordinates above.

### Ktor Network

Install the plugin on every client whose calls should appear in the Network tab. A dependency
alone does not modify existing clients:

```kotlin
val client = remember { HttpClient { install(HeimdallKtor) } }
```

The plugin records method, URL, timing, status, headers, errors, and supported body previews. See
[capture limits and privacy](docs/integration.md#network).

### Storage

Call discovery once after install. Android discovers SharedPreferences and Keystore aliases:

```kotlin
Heimdall.discoverStorage(this)
```

On iOS, call `Heimdall.discoverStorage()`; pass app-group suite names when needed. If using
Preferences DataStore, attach the already-open instance and a scope that lives as long as it:

```kotlin
Heimdall.attachDataStore(dataStore, name = "preferences", scope = appScope)
```

See [storage details](docs/plugins/storage.md) for supported sources, edit behavior, and limits.

### SQLite Database

For the `SqliteFileInspector` adapter, attach an existing on-disk SQLite file after it has been
created. On Android, for example:

```kotlin
Heimdall.database.attach(
    SqliteFileInspector(databaseName = "app.db", path = context.getDatabasePath("app.db").path),
)
```

Call `Heimdall.database.refresh("app.db")` after writes that should appear immediately. This
adapter opens a separate read-only connection and cannot inspect `:memory:` databases. The
checked-in sample instead demonstrates a custom in-memory `DatabaseInspector`; see
[database details](docs/plugins/database.md) for the file adapter and query limits.

### Logs And Crashes

Heimdall does not import Android Logcat or iOS system logs. Report app-owned records and handled
exceptions explicitly:

```kotlin
Heimdall.log(LogLevel.INFO, "Checkout", "Checkout started")
Heimdall.recordCrash(IllegalStateException("Handled checkout failure"), isFatal = false)
```

Android uncaught exceptions are captured automatically after `Heimdall.install(...)`; iOS has no
automatic uncaught-exception hook. Handled exceptions can be reported on either platform.

### Feature Flags

For your own flag system, implement `FlagCatalog` and use the provider returned by `attach` for
reads so panel overrides affect your app:

```kotlin
var newCheckoutEnabled = false
val flags = Heimdall.flags.attach(
    object : FlagCatalog {
        override fun definitions() = listOf(
            FlagDefinition("new_checkout", "New checkout", FlagValue.BoolValue(false)),
        )

        override fun get(key: String, default: FlagValue): FlagValue =
            if (key == "new_checkout") FlagValue.BoolValue(newCheckoutEnabled) else default
    },
)
val enabled = (flags.get("new_checkout", FlagValue.BoolValue(false)) as FlagValue.BoolValue).value
```

Alternatively, the optional `heimdall-flags-firebase` module provides an Android Firebase adapter:

```kotlin
val flags = FirebaseRemoteConfig.getInstance().attachToHeimdall()
val enabled = flags.get("new_checkout", FlagValue.BoolValue(false))
```

The Firebase adapter discovers keys once; provide explicit definitions for string values that are
intended to be numeric or boolean-shaped.

### Events, Screens And Timing

Call these APIs where app activity happens; Heimdall does not infer screens or events:

```kotlin
Heimdall.setCurrentScreen("Checkout")
Heimdall.event("Checkout started")
Heimdall.measure("parse_checkout") { parseCheckout(payload) }
```

`measure` records an explicit duration and timeline event; it does not profile frames or
recompositions. The [integration guide](docs/integration.md) has platform setup and deeper details.

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

## License

Apache-2.0.

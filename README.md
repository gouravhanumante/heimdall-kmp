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

## Install

Start with these two modules. They are the only required dependencies, and you do not add
Android/iOS-specific artifact names manually:

```kotlin
dependencies {
	debugImplementation("io.github.gouravhanumante:heimdall:0.1.0-alpha02")
}
```

The umbrella includes Core, UI, Ktor network capture, storage discovery, SQLite inspection, and
the Android Firebase flag adapter. Add an individual module only when you deliberately want a
smaller custom footprint:

| Need | Add |
|---|---|
| Network requests made by Ktor | `heimdall-network-ktor` |
| SharedPreferences, DataStore, UserDefaults, Keychain or Keystore | `heimdall-storage` |
| Inspect an on-disk SQLite database | `heimdall-database-sqlite` |
| Firebase Remote Config flags on Android | `heimdall-flags-firebase` |

For example, a Ktor network setup adds one more line:

```kotlin
debugImplementation("io.github.gouravhanumante:heimdall-network-ktor:0.1.0-alpha02")
```

Use the same base coordinate on Android and iOS. Kotlin Multiplatform automatically selects the
correct `android`, `iosarm64`, or `iossimulatorarm64` artifact; consumers never add those
platform-specific artifacts directly.

At app startup, install Heimdall and wrap your root Compose content:

```kotlin
Heimdall.install(PlatformContext(this)) // Android; PlatformContext() on iOS

HeimdallOverlay {
	YourAppContent()
}
```

For database inspection add `heimdall-database-sqlite`; for Firebase Remote Config add
`heimdall-flags-firebase` on Android. See [docs/integration.md](docs/integration.md) for the
complete module list and [docs/release-builds.md](docs/release-builds.md) for replacing real
modules with `-noop` artifacts in release variants.

### Optional integrations

Install `HeimdallKtor` on every Ktor client whose traffic you want to capture:

```kotlin
val client = HttpClient {
	install(HeimdallKtor)
}
```

Discover platform storage once after installing Heimdall:

```kotlin
// Android
Heimdall.discoverStorage(this)

// iOS
Heimdall.discoverStorage()
```

Attach an on-disk SQLite database using the optional database module. The path must be the path
to the app's existing database file; Heimdall opens a separate read-only connection:

```kotlin
Heimdall.database.attach(
	SqliteFileInspector(databaseName = "app.db", path = databasePath),
)
```

For Firebase Remote Config on Android, attach the adapter to your existing provider and use the
returned provider for flag reads:

```kotlin
val flags = FirebaseRemoteConfig.getInstance().attachToHeimdall()
val enabled = flags.get("new_checkout", FlagValue.BoolValue(false))
```

The core API also accepts app-reported logs, crashes, events, screen names, and explicit timing:

```kotlin
Heimdall.log(LogLevel.INFO, "Checkout", "Started checkout")
Heimdall.recordCrash(throwable, isFatal = false)
Heimdall.setCurrentScreen("Checkout")
Heimdall.measure("load_checkout") { loadCheckout() }
```

For production variants, replace each real module with the matching `-noop` artifact. The app
keeps the same call sites, but the overlay and capture implementations are absent from the
release binary. See [docs/release-builds.md](docs/release-builds.md) for Android custom variants
and the iOS `CONFIGURATION`-based swap.

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

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
| `heimdall-core-noop`, `heimdall-ui-noop`, `heimdall-network-ktor-noop`, `heimdall-storage-noop`, `heimdall-database-sqlite-noop`, `heimdall-flags-firebase-noop` | Same-API, do-nothing counterparts for release builds — see `docs/release-builds.md`. |
| `sample/androidApp`, `sample/shared`, `sample/iosApp` | Reference integration wiring the modules together. |

See [docs/architecture.md](docs/architecture.md) for the module graph and the design decisions
behind it, and [docs/overlay.md](docs/overlay.md) for exactly what the bubble/panel does.

## Install

Add only the modules your app uses. The current release is `0.1.0-alpha01`:

```kotlin
dependencies {
	debugImplementation("io.github.gouravhanumante:heimdall-core:0.1.0-alpha01")
	debugImplementation("io.github.gouravhanumante:heimdall-ui:0.1.0-alpha01")
	debugImplementation("io.github.gouravhanumante:heimdall-network-ktor:0.1.0-alpha01")
	debugImplementation("io.github.gouravhanumante:heimdall-storage:0.1.0-alpha01")
}
```

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

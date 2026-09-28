# Heimdall

In-app debug inspector for Kotlin Multiplatform / Compose Multiplatform apps — network,
database, storage, logs, crashes and feature-flag overrides, behind a floating bubble you can
drag away to hide and bring back with a shake.

**Status: pre-alpha, nothing run on a device yet.** Built so far: the overlay (bubble, tap to
open, drag to hide, shake-to-recall on Android, empty panel), Heimdall's own 24-hour history
database, Ktor network capture, and storage (DataStore, SharedPreferences, Keystore, UserDefaults,
Keychain). See [docs/TODO.md](docs/TODO.md) and [docs/platform-support.md](docs/platform-support.md).

## Modules

| Module | Contents |
|---|---|
| `heimdall-core` | Platform-agnostic logic: shake detection, sessions, collectors, events and screen attribution. No UI. |
| `heimdall-ui` | Compose Multiplatform bubble, overview, timeline and inspector tabs. |
| `heimdall-network-ktor` | Ktor `HttpClient` plugin that reports into `Heimdall.network`. |
| `heimdall-storage` | DataStore/SharedPreferences/UserDefaults/Keystore/Keychain discovery. |
| `heimdall-database-sqlite` | `DatabaseInspector` adapter for any on-disk SQLite file (Room, SQLDelight, raw). |
| `heimdall-flags-firebase` | Firebase Remote Config flag adapter (Android). |
| `heimdall-core-noop`, `heimdall-ui-noop`, `heimdall-network-ktor-noop` | Same-API, do-nothing counterparts for release builds — see `docs/release-builds.md`. |
| `sample/androidApp`, `sample/shared`, `sample/iosApp` | Reference integration wiring the modules together. |

See [docs/architecture.md](docs/architecture.md) for the module graph and the design decisions
behind it, and [docs/overlay.md](docs/overlay.md) for exactly what the bubble/panel does.

## Try it

```
./gradlew :sample:androidApp:installDebug
```

## Release builds

Every module above always runs its real implementation — do not add them to a release build's
dependencies as-is. `heimdall-core`, `heimdall-ui`, and `heimdall-network-ktor` have a `-noop`
counterpart to swap in via `releaseImplementation` instead; see
[docs/release-builds.md](docs/release-builds.md) for exactly what is and isn't covered yet.

## License


Apache-2.0.

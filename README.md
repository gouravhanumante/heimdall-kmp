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
| `sample/androidApp` | Minimal Android app wiring the two together — the integration reference. |

See [docs/architecture.md](docs/architecture.md) for the module graph and the design decisions
behind it, and [docs/overlay.md](docs/overlay.md) for exactly what the bubble/panel does.

## Try it

```
./gradlew :sample:androidApp:installDebug
```

## License

Apache-2.0.

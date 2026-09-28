# Heimdall

In-app debug inspector for Kotlin Multiplatform / Compose Multiplatform apps — network,
database, storage, logs, crashes and feature-flag overrides, behind a floating bubble you can
swipe away and bring back with a shake.

**Status: pre-alpha, milestone 1 in progress.** Overlay shell (bubble, drag, swipe-to-edge
dismiss, shake-to-recall, empty panel) is implemented for Android. iOS is scaffolded but not
functional yet — see [docs/TODO.md](docs/TODO.md) and [docs/platform-support.md](docs/platform-support.md).

## Modules

| Module | Contents |
|---|---|
| `heimdall-core` | Platform-agnostic logic: shake detection, overlay state. No UI. |
| `heimdall-ui` | Compose Multiplatform bubble + panel shell. |
| `sample/androidApp` | Minimal Android app wiring the two together — the integration reference. |

See [docs/architecture.md](docs/architecture.md) for the module graph and the design decisions
behind it, and [docs/overlay.md](docs/overlay.md) for exactly what the bubble/panel does.

## Try it

```
./gradlew :sample:androidApp:installDebug
```

## License

Apache-2.0.

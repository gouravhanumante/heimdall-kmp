# Integration

`0.1.0-alpha02` is the next release containing the complete umbrella artifacts. The recommended
coordinate is:

```kotlin
dependencies {
    debugImplementation("io.github.gouravhanumante:heimdall:0.1.0-alpha02")
}
```

The umbrella includes core, UI, Ktor network capture, storage, SQLite database inspection, and
the Android Firebase flag adapter. Individual module coordinates remain available for consumers
that need a smaller dependency footprint:

```kotlin
dependencies {
    implementation("io.github.gouravhanumante:heimdall-core:0.1.0-alpha02")
    implementation("io.github.gouravhanumante:heimdall-ui:0.1.0-alpha02")
    // add only the collector modules you use:
    implementation("io.github.gouravhanumante:heimdall-network-ktor:0.1.0-alpha02")
    implementation("io.github.gouravhanumante:heimdall-storage:0.1.0-alpha02")
    implementation("io.github.gouravhanumante:heimdall-database-sqlite:0.1.0-alpha02")
    implementation("io.github.gouravhanumante:heimdall-flags-firebase:0.1.0-alpha02") // Android only
}
```

## Setup

```kotlin
// Once, at app startup (Application.onCreate on Android; app launch on iOS)
Heimdall.install(PlatformContext(this)) // this = Context on Android; no argument's needed on iOS
```

```kotlin
// Wrap your app's root composable once
HeimdallOverlay {
    YourAppContent()
}
```

See [docs/overlay.md](overlay.md) for what the bubble/panel does, and each
`docs/plugins/*.md` for the collector modules (network, storage, database, flags).

## Release builds

Every module above always runs its real implementation. See
[docs/release-builds.md](release-builds.md) for the `-noop` counterparts and the
`debugImplementation`/`releaseImplementation` (Android) / `CONFIGURATION`-based (iOS) swap.

## Version scheme

`0.x.y-alphaNN`/`-betaNN` while the API is still moving; see [CHANGELOG.md](../CHANGELOG.md) for
what changed release to release. Breaking changes are called out there, not silently absorbed.

# Integration

**Not published to Maven Central yet.** The Gradle plugin wiring (`com.vanniktech.maven.publish`,
POM metadata, signing tasks) is in place and verified — `./gradlew :heimdall-core:tasks` lists
real `publishToMavenCentral`/`publishToMavenLocal` tasks, and
`generatePomFileFor*Publication` produces a correct POM (coordinates, license, developer, SCM) —
but nothing has actually been pushed to Central yet. That needs, separately from this repo:

- A verified Central Portal namespace for `io.github.gouravhanumante`.
- A GPG signing key (`signAllPublications()` requires one) — `gpg --full-generate-key` to create
  one, then `gpg --export-secret-keys --armor <key id>` to get the value for
  `signingInMemoryKey` below.
- A Central Portal user token (`mavenCentralUsername`/`mavenCentralPassword`) generated at
  https://central.sonatype.com/account, plus `signingInMemoryKey`/`signingInMemoryKeyPassword`
  for the GPG key above — all four go in `~/.gradle/gradle.properties`, never committed.

Once published, the coordinates will be:

```kotlin
dependencies {
    implementation("io.github.gouravhanumante:heimdall-core:0.1.0-alpha01")
    implementation("io.github.gouravhanumante:heimdall-ui:0.1.0-alpha01")
    // add only the collector modules you use:
    implementation("io.github.gouravhanumante:heimdall-network-ktor:0.1.0-alpha01")
    implementation("io.github.gouravhanumante:heimdall-storage:0.1.0-alpha01")
    implementation("io.github.gouravhanumante:heimdall-database-sqlite:0.1.0-alpha01")
    implementation("io.github.gouravhanumante:heimdall-flags-firebase:0.1.0-alpha01") // Android only
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

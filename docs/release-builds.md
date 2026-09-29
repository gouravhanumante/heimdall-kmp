# Release builds

All six modules now have a real no-op counterpart: `heimdall-core-noop`, `heimdall-ui-noop`,
`heimdall-network-ktor-noop`, `heimdall-storage-noop`, `heimdall-database-sqlite-noop`, and
`heimdall-flags-firebase-noop`. Each exposes the exact same public API as its real module — same
package, same class/function names and signatures — with every body doing nothing. Swapping one
in removes the real implementation's code and dependencies from that build variant entirely; it
is not a runtime flag, the real code simply isn't there.

## Setup (Android)

In the **consuming app's** module (not a Heimdall module), use Android's build-type-scoped
configurations to pick the real artifact for `debug` and the no-op one for `release`:

```kotlin
dependencies {
    debugImplementation("io.github.gouravhanumante:heimdall-core:VERSION")
    debugImplementation("io.github.gouravhanumante:heimdall-ui:VERSION")
    debugImplementation("io.github.gouravhanumante:heimdall-network-ktor:VERSION")
    debugImplementation("io.github.gouravhanumante:heimdall-storage:VERSION")
    debugImplementation("io.github.gouravhanumante:heimdall-database-sqlite:VERSION")
    debugImplementation("io.github.gouravhanumante:heimdall-flags-firebase:VERSION")

    releaseImplementation("io.github.gouravhanumante:heimdall-core-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-ui-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-network-ktor-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-storage-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-database-sqlite-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-flags-firebase-noop:VERSION")
}
```

**Never add both the real and the `-noop` artifact of the same module to one variant.** They
declare the same classes in the same package and will fail to link.

Call sites (`Heimdall.install(...)`, `HeimdallOverlay { ... }`, `install(HeimdallKtor)`, feature
flag reads through `Heimdall.flags.attach(...)`) don't change between variants — only which jar is
resolved does.

## What each no-op module does

- **`heimdall-core-noop`**: `Heimdall.install`/`installInMemory` do nothing; `log`/`event`/
  `recordCrash` do nothing; every store's `current` is a permanently empty `StateFlow`, `clear()`/
  `forSession(...)` are no-ops. `Heimdall.bubblePosition.save(...)` doesn't persist anything either
  — moot in practice, since `heimdall-ui-noop` never renders a bubble to have a position.
  `Heimdall.measure(...)` and `Heimdall.screen(...)` still run and
  return the block they're given — only the capture around it is removed, never the app's own
  code. `Heimdall.flags.attach(catalog)` always delegates straight to the app's real flag
  provider — there is no override store in a release build, so the app's actual feature flag
  values must never depend on this being a no-op (proved by `HeimdallNoopTest`: reverting the
  delegate to a fixed value turns that test red). Depends on nothing but
  `kotlinx-coroutines-core` — no SQLite driver ships in a release build using this.
- **`heimdall-ui-noop`**: `HeimdallOverlay { content() }` renders exactly `content()` — no bubble,
  no panel, no touch interception. `HeimdallOverlayController.acceptsOverlayTouch` always returns
  `false`. Depends on Compose runtime/ui only — no Material3, no Coil, no bundled artwork resource.
- **`heimdall-network-ktor-noop`**: `HeimdallKtor` installs on the client but never inspects a
  request or response. Proved by `HeimdallKtorNoopTest`: installing it does not change the
  response body or status the app sees.
- **`heimdall-storage-noop`**: `discoverStorage`/`discoverAndroidPreferences`/
  `discoverAndroidKeystore`/`discoverUserDefaults`/`discoverKeychain`/`attachDataStore` all do
  nothing — no file/Keychain/Keystore scanning happens.
- **`heimdall-database-sqlite-noop`**: `SqliteFileInspector`'s constructor never opens a
  connection; `snapshot()` returns an empty table list, `query(...)` returns an empty result
  rather than throwing.
- **`heimdall-flags-firebase-noop`**: `attachToHeimdall()` still reads Firebase Remote Config
  values directly and returns a `FlagProvider` that works normally — the app's real flag values
  are unaffected. Only the discovery/override side (which lives in `heimdall-core-noop`) is inert.

## iOS: picking real vs no-op

Proven in `sample/shared`, not just documented: its `build.gradle.kts` reads
`System.getenv("CONFIGURATION")` and depends on the `-noop` artifacts when it's `"Release"`,
the real ones otherwise (unset — e.g. a plain `./gradlew compileKotlinIosSimulatorArm64` —
defaults to real). This works because Xcode invokes `embedAndSignAppleFrameworkForXcode` as a
full, separate Gradle process per build configuration (see `sample/iosApp/project.yml`'s
`preBuildScripts`), with `CONFIGURATION` set in that process's environment — so a Debug build
and a Release build resolve completely different dependency graphs, not just a runtime flag.
Verified by compiling `sample/shared` for `iosSimulatorArm64` all three ways (unset, `Debug`,
`Release`) and confirming each resolves and compiles — including `SampleApp.kt` and
`MainViewController.kt`, which call straight into `Heimdall`/`HeimdallOverlay`/`IosShakeListener`
either way, proving the no-op API parity holds in practice, not just by inspection.

A consuming app's own KMP shared module can use the same pattern:

```kotlin
val useNoop = System.getenv("CONFIGURATION") == "Release"
commonMain.dependencies {
    if (useNoop) {
        api("io.github.gouravhanumante:heimdall-core-noop:VERSION")
    } else {
        api("io.github.gouravhanumante:heimdall-core:VERSION")
    }
}
```

**Limitation**: this only works for a *shared KMP module* built once per Xcode invocation, the way
`sample/shared` is. It does not (yet) give a plain `com.android.application` module a matching
Android-side demonstration in this sample — `sample/shared`'s Android target has no build-type
variance in the newer `com.android.kotlin.multiplatform.library` DSL used here, so it always
resolves the real modules for Android regardless of build type. A real consumer app that is *not*
a KMP shared module (a plain `com.android.application`, like most Android apps) doesn't have this
limitation at all: `debugImplementation`/`releaseImplementation` on the real vs. `-noop` artifacts
works exactly as documented above, since that's standard, decade-old Android Gradle behavior, not
something specific to this library.

## What is not covered yet

- **Not verified on a device or in a real release build**, Android or iOS — only JVM host tests
  and `compileAndroidMain`/`compileKotlinIosSimulatorArm64`.
- **Room/SQLDelight/raw SQLite adapter richness, Firebase iOS parity, and database search
  pagination remain separately-tracked gaps** — unrelated to release-safety, see the rest of
  `docs/TODO.md`.

## Runtime kill switch

`Heimdall.enabled = false` (real module) still exists as a defense-in-depth runtime switch —
every store's `record`/`publish` checks it — but it does not remove any code or dependency from
the binary. Use the `-noop` artifacts for that; use `enabled` only for toggling capture within a
debug build.

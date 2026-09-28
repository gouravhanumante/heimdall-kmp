# Release builds

Three modules now have a real no-op counterpart: `heimdall-core-noop`, `heimdall-ui-noop`, and
`heimdall-network-ktor-noop`. Each exposes the exact same public API as its real module — same
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

    releaseImplementation("io.github.gouravhanumante:heimdall-core-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-ui-noop:VERSION")
    releaseImplementation("io.github.gouravhanumante:heimdall-network-ktor-noop:VERSION")
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

## What is not covered yet

- **`heimdall-storage`, `heimdall-database-sqlite`, `heimdall-flags-firebase` have no `-noop`
  counterpart.** If a release build keeps one of these on the classpath, its code still runs.
  Concretely: `SqliteFileInspector`'s constructor always opens a real read-only SQLite connection;
  `DatabaseStore.attach`/`StorageStore.publish` do check `Heimdall.enabled` before doing anything
  with what's attached (see `heimdall-core`), but the adapter's own setup cost isn't eliminated.
  Until these have no-op modules, either don't add them to a release build's dependencies, or set
  `Heimdall.enabled = false` and accept that residual setup cost.
- **iOS has no build-type-scoped dependency mechanism equivalent to
  `debugImplementation`/`releaseImplementation`.** The no-op iOS targets (`iosArm64`,
  `iosSimulatorArm64`) compile for all three covered modules, but *how* a consumer picks the real
  framework for a debug scheme and the no-op one for a release scheme/archive is unresolved and
  unverified — this needs to be proven against an inspected release `.ipa`/framework before this
  doc claims it works, per `.github/instructions/docs.instructions.md` rule 5.
- **Not verified on a device or in a real release build**, Android or iOS — only JVM host tests
  (`HeimdallNoopTest`, `HeimdallKtorNoopTest`) and `compileAndroidMain`/`testAndroidHostTest`.

## Runtime kill switch

`Heimdall.enabled = false` (real module) still exists as a defense-in-depth runtime switch —
every store's `record`/`publish` checks it — but it does not remove any code or dependency from
the binary. Use the `-noop` artifacts for that; use `enabled` only for toggling capture within a
debug build.

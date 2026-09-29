# Changelog

All notable, consumer-observable changes to Heimdall are recorded here. See
`.github/instructions/docs.instructions.md` for what belongs here vs. in the design docs.

## Unreleased

### Added
- Added `heimdall` and `heimdall-noop` umbrella artifacts. Most consumers can now use one real
  dependency for debug builds and one no-op dependency for release builds; the umbrella includes
  core, UI, Ktor network capture, storage, SQLite inspection, and the Android Firebase flag adapter.

### Added
- Logs tab: severity filter chips (All, Crashes, Error, Warn, Info, Debug, Verbose) above the
  search field. Tapping a crash row now opens a detail screen showing the full stack trace
  (`CrashRecord.stackTraceText`) in a copyable monospace block — previously only the exception
  type and message were shown, and the full trace was captured but never rendered anywhere.

### Removed
- **Breaking**: automatic frame/jank monitoring (`AndroidFrameMonitor`/`IosFrameMonitor`,
  `FrameRecord`, `PerformanceStore.frames`/`recordFrame`, `HealthRules.slowFrames`, Overview's
  "N slow frames"/"Worst frame" text) is gone. It measured every frame the process drew,
  including Heimdall's own popup — since `HeimdallOverlay` draws in the same window as the app,
  opening the inspector to check for jank could itself register as the jank, with no way to tell
  the two apart. `Heimdall.measure(...)` (explicit, developer-named durations) is unaffected.
  Also removed the now-unused `MetricConfidence.COMPILER_ANALYSIS`/`ESTIMATED` enum values, which
  existed only for a planned Compose recomposition metric that was never built and has been
  decided against — see `docs/TODO.md`.

### Changed
- Overview's "Current session" raw counts (network calls, logs, timeline events) are gone — they
  duplicated Health's own numbers without the status coloring, and weren't actionable on their
  own. Health is now the single place Overview shows session counts.
- Tapping a rail icon (Network, Database) now always returns to that tab's top-level list, even
  if you were already on it drilled into a detail/table — previously tapping Network again while
  viewing a call's detail did nothing, since only `selectedTab` changed and the drilled-down
  record/table selection was untouched.
- Sessions are now labeled "Current session" / "Previous session" / "N sessions ago" instead of
  a raw, meaningless "Session &lt;database id&gt;".
- **Panel visual rework**: rail/action icons are now real vector icons (`material-icons-extended`,
  pinned to `1.7.3` — this artifact isn't versioned in lockstep with Compose Multiplatform)
  instead of plain Unicode glyphs. Every tab now consistently uses `HeimdallDesign.palette()`
  instead of `HeimdallDesign`'s raw dark-mode-only constants, so the panel actually follows the
  device light/dark setting as `docs/overlay.md` already claimed — previously only the header did;
  everything else (Overview, Network, Database, Storage, Logs, Flags, Sessions) was hardcoded to
  dark colors and would have rendered with unreadable near-white-on-white text in light mode.
  Cards/rows now have a visible border for contrast against the background, log severities have
  distinct colors (fatal/error red, warning amber, info/debug/verbose stepped down) instead of
  everything but errors sharing one flat gray, and text sizes are drawn from a shared type scale
  (`HeimdallDesign.screenTitleSize/sectionTitleSize/bodySize/labelSize/captionSize`) instead of
  one-off numbers.
- Sessions are now capped at the 3 most recent launches (`SessionRepository.MAX_RETAINED_SESSIONS`)
  — older ones are pruned on the next `Heimdall.install(...)`, independent of the existing 24-hour
  retention window. The Sessions tab highlights whichever run is currently being viewed, and every
  panel tab shows a "Viewing session N — not live" banner with a one-tap way back to live data
  while a historical session is selected.
- **Breaking**: `IosShakeListener`'s internal `motionEndedWithShake()` is now the public
  `notifyShakeDetected()` — a consumer's own `UIResponder.motionEnded` override must call this to
  forward `UIEventSubtypeMotionShake`, and `internal` never actually let that compile across the
  module boundary. Mirrored in `heimdall-core-noop`.
- Pinned Compose Multiplatform to `1.10.0` and Coil to `3.5.0` so Android artifacts remain
  compatible with AGP `9.0.1` and compileSdk `36`.
- **Breaking**: all stores now persist to Heimdall's own on-disk SQLite database (Android:
  `no_backup_files_dir`, excluded from auto-backup; iOS: Application Support) instead of an
  in-memory ring buffer. Data survives app restarts and crashes; `Heimdall.install(context)` must
  be called once at startup before anything is recorded — see `docs/architecture.md`.
- Every record is tagged with the app-run ("session") it happened in. `Heimdall.sessions()` lists
  past runs, `NetworkStore`/`LogStore`/`CrashStore.forSession(id)` reads one back. A session that
  had a fatal crash is flagged (`Session.crashed`).
- `NetworkStore`/`LogStore`/`CrashStore` now expose `current: StateFlow<List<_>>` (the live
  current session, newest-first) instead of a `snapshot()` function.
- `heimdall-ui`: bubble rewritten. Tap opens the panel (it didn't before); drag to the bottom
  "drop to hide" target hides it (before, any small drag left/up hid it); shake brings it back
  where it was, repeatably (before, it could never be hidden again after the first recall); it
  stays inside the screen. Panel gets a Close button and no longer lets taps through to the app.
- **Breaking**: `OverlayState`/`OverlayVisibility` removed (unused). iOS
  `discoverStandardUserDefaults()` replaced by `discoverUserDefaults()`/`discoverStorage()`.
- Storage: re-scan on Storage tab open (`StorageStore.refresh()`), UserDefaults suites, app's own
  UserDefaults keys only, iOS Keychain (generic passwords), Android Keystore aliases, one-call
  `Heimdall.discoverStorage(...)` per platform. Fixed: SharedPreferences change listeners could be
  garbage-collected (held weakly by Android); UserDefaults values could never be edited (`is
  NSString` never matches a bridged Kotlin `String`).
- Retention: history older than 24 hours is deleted (at startup and hourly while running), plus
  oldest-first caps per session (5,000 network records / 20,000 log entries / 200 crash records).
- Logs and crash records now have live panel rows; fatal crash records are shown alongside logs and
  the current session header displays a crash badge. Database snapshots expose live state and flag
  overrides have panel controls, including reset and boolean toggles. These changes are not yet
  device-verified.
- Added a bounded, session-persisted `EventStore` and `Heimdall.event(...)` for app-reported
  timeline events, plus `Heimdall.setCurrentScreen(...)` and scoped `Heimdall.screen(...)` for
  optional attribution. The panel now includes Overview and Timeline tabs.
- The shared sample is now a multi-screen harness (Network, Feed, Database, Storage, Logs) reached
  from an icon Home screen, each attributed via `Heimdall.setCurrentScreen(...)`. Network and Feed
  make real Ktor calls through `HeimdallKtor`; Feed renders remote images with Coil; Database uses
  bundled SQLite; Android Storage exercises discovered SharedPreferences.
- Added explicit `Heimdall.measure(...)` timing with bounded live performance records, Overview
  slow-operation counts, and matching timeline events. Automatic Compose recomposition metrics are
  intentionally not included yet.
- Reworked `HeimdallPanel` into a bounded floating inspector with an icon-only navigation rail,
  selected-section heading, and `Global` context fallback when no screen is attributed. The
  inspector is centered on screen.
- Bubble: on release it now springs to the nearest left/right edge (8dp inset) instead of staying
  wherever it was dropped; it tracks the finger without lag while dragging.
- Sample: added a typed Details flow that writes text, int, float, long, and boolean values to
  Android SharedPreferences; API actions now emit `SampleApi` Logcat messages; Android back closes
  the inspector before returning from sample screens.
- Inspector: Network rows open request/response detail with a copy-cURL action; Database shows table
  rows; Storage and Logs support search; Timeline is no longer a primary navigation destination.
- Inspector data presentation: Network payloads are selectable monospace code blocks; Database and
  Storage use structured tables; Logs and Crashes use contained severity records instead of raw text
  streams.
- Added shared `HeimdallDesign` tokens and improved popup behavior: 94% centered sizing, outside-tap
  dismissal, icon-only close, Network URL search, per-table Database search, searchable Flags, and
  Overview quick actions/critical issue surfaces.
- Sample database now includes `users` and `profiles` tables. Android shake recall samples at game
  rate and posts state changes to the main thread; the inspector has stable minimum dimensions and
  a device-aware light/dark shell palette.
- Database navigation now lists tables before opening one; Network detail separates status metadata
  from a full Copy cURL button, uses an icon Back action, and provides descriptive icons throughout
  the navigation rail and copy/close actions.
- Added the supplied Heimdall horn artwork as an Android-safe PNG Compose resource; the original SVG
  was rasterized because Compose Android does not decode SVG resources directly.
- Sample controls now use a shared Heimdall Material theme with horn-gold primary actions,
  watchman-blue secondary accents, consistent outlines, and rounded input/button shapes.
- Added offline Health rules and an Overview Health table. Measured crashes, network errors, and
  slow operations are reported with confidence; unsupported Compose metrics are omitted.
- Flag overrides now persist as typed values in Heimdall's own SQLite database and restore during
  installation. Official Compose compiler reports/metrics are enabled for `heimdall-ui` and the
  shared sample; runtime recomposition counters remain unavailable until an optional instrumentation
  plugin exists.
- Android sample now reports live Choreographer frame intervals to Overview/Health; frame samples
  stay in memory and are not persisted.
- Added a Sessions view for historical network/log/crash/event browsing while keeping database,
  storage, flags, and frame state live-only.
- Flags now support persisted text and number editors plus per-flag reset actions in addition to
  boolean toggles.
- Added the one-time `FlagCatalog` attachment contract so provider adapters can discover all known
  flag keys and still receive Heimdall overrides without per-key registration code.
- Added optional Android `heimdall-flags-firebase`: `FirebaseRemoteConfig.attachToHeimdall()`
  discovers keys and returns an override-aware provider without per-key definitions.
- Flags expose an optional host restart handler; the panel shows Restart app only when configured.
- iOS frame intervals now use `CADisplayLink` after `Heimdall.install(...)`; iOS shake/overlay host
  forwarding is now implemented through the sample UIKit responder host. The iOS sample defaults to
  the shared `HeimdallOverlay`; `MainViewControllerWithNativeOverlayWindow()` opts into a separate
  always-on-top `UIWindow` with passthrough hit testing.
- The shared `HeimdallOverlay` wrapper is the overlay on both platforms. Known limitation: Compose
  `Dialog`/`ModalBottomSheet`/`Popup` draw over the bubble.
- `Heimdall.install(...)` now owns Android uncaught-crash capture and frame-monitor startup; sample
  integration no longer duplicates those hooks.
- Added `DatabaseInspector`, `DatabaseStore.attach(...)`, and refresh-on-open behavior for app
  database adapters. Heimdall still never owns or migrates the app database; Room, SQLDelight, and
  raw SQLite adapter modules remain separate work.

### Added
- Bumped Gradle to `9.5.1` and AGP to `9.1.1` (from `9.1.0`/`9.0.1`) to match a real consuming
  app used to test integration via a Gradle composite build
  (`includeBuild` + `dependencySubstitution`) — Gradle disallows mixing AGP versions across
  included builds. Verified: full test suite, iOS compile sweep, and the sample all still pass;
  a separate test app resolves and builds a full debug APK against this Heimdall checkout with
  real usage (`Heimdall.install`, `discoverStorage`, `HeimdallOverlay`, `AndroidShakeListener`).
- `signAllPublications()` is now conditional on a `signingInMemoryKey` Gradle property being
  present, so `publishToMavenLocal` works for local testing without a GPG key — previously it
  failed outright (`Cannot perform signing task ... no configured signatory`) even for local-only
  publishing.
- `heimdall-flags-firebase`/`-noop` now use `JavadocJar.Empty()` instead of Dokka-generated docs:
  Dokka crashes (`PermittedSubclasses requires ASM9`) reading `FlagValue`'s sealed-class bytecode
  on this Kotlin/JDK combination. The javadoc jar artifact still exists (Central requires one),
  it's just empty rather than real API docs for these two modules.
- Added `LICENSE` (Apache-2.0, matching what README already claimed) and wired up
  `com.vanniktech.maven.publish` with POM metadata (license, developer, SCM) on all 12 publishable
  modules, coordinates under `io.github.gouravhanumante`, starting at `0.1.0-alpha01`. Verified:
  `generatePomFileFor*Publication` produces a correct POM and `publishToMavenCentral`/
  `publishToMavenLocal` tasks exist for every module. **Not yet published** — see
  `docs/integration.md` for what's still needed (Central Portal namespace, GPG key, user token).
- Sample's Feed screen now loads images through a Coil `ImageLoader` backed by the same
  `HttpClient` `HeimdallKtor` is installed on, so feed images show up in `Heimdall.network` instead
  of bypassing capture through Coil's own fetcher.
- iOS Keychain discovery now also lists keys (`kSecClassKey`) and certificates
  (`kSecClassCertificate`), read-only, as separate `Keychain: Keys`/`Keychain: Certificates`
  sources. Never requests key material or raw certificate data.
- Added `heimdall-storage-noop`, `heimdall-database-sqlite-noop`, and
  `heimdall-flags-firebase-noop`: every module now has a `-noop` counterpart. Compile-verified on
  Android and `iosSimulatorArm64`.
- iOS debug/release swap is implemented and verified in `sample/shared`: its `build.gradle.kts`
  picks the `-noop` artifacts when Xcode's `CONFIGURATION` env var is `"Release"` (the real ones
  otherwise), proven by compiling all three ways (unset/`Debug`/`Release`) — see
  `docs/release-builds.md`.
- **Breaking**: `DatabaseQueryRunner`/`DatabaseInspector.query` now take a `sql` string and an
  `args: List<String>` bound to `?` placeholders, instead of `sql` alone. Existing custom
  `DatabaseInspector` implementations must add the `args` parameter to their `query` override.
  The Database tab's search box now runs a live, bound query against every column of the real
  table (via `Heimdall.database.queryRunnerFor(...)`) instead of only filtering whatever rows the
  initial snapshot happened to load, so a match outside that snapshot is no longer invisible.
  Falls back to filtering the snapshot if no query runner is available. See
  `docs/plugins/database.md`.
- iOS Keychain discovery now also lists internet-password items (`kSecClassInternetPassword`),
  not just generic passwords, shown as `service / account (internet)`.
- The Sessions tab now shows a readable UTC date/time (`formatSessionTimestamp`) instead of raw
  epoch milliseconds.
- Added `Heimdall.bubblePosition`: the bubble's resting position (as a screen fraction) now
  survives an app restart, restored on `Heimdall.install(...)`/`installInMemory()`. Previously it
  only survived hide/show within one run.
- Added `heimdall-core-noop`, `heimdall-ui-noop`, and `heimdall-network-ktor-noop`: drop-in,
  same-package no-op counterparts for release builds, wired via
  `debugImplementation`/`releaseImplementation` on Android. `Heimdall.measure`/`screen` still run
  their block; `Heimdall.flags.attach(...)` still delegates to the app's real flag provider so
  production feature flags are unaffected. `heimdall-storage`, `heimdall-database-sqlite`, and
  `heimdall-flags-firebase` don't have a no-op counterpart yet, and the iOS debug/release swap
  mechanism is unresolved — see `docs/release-builds.md`.
- Added optional `heimdall-database-sqlite`: `SqliteFileInspector`, a `DatabaseInspector` that
  reads any on-disk SQLite file by path — covers Room, SQLDelight, and raw SQLite databases with
  one adapter, since all three are a plain SQLite file underneath. Opens its own read-only
  connection; the ad-hoc query box only accepts `SELECT`/`PRAGMA`/`EXPLAIN`/`WITH`. Not usable
  with an in-memory (`:memory:`) database. See `docs/plugins/database.md`.
- `heimdall-core`: `ShakeDetector` (shared threshold/debounce logic) and `AndroidShakeListener`
  (Android accelerometer wiring). iOS listener is a stub — see `docs/TODO.md`.
- `heimdall-ui`: `HeimdallOverlay` composable — wrap a screen's root content once to get the
  draggable bubble and, on tap, the panel. `HeimdallOverlayController.recall()` brings back a
  hidden bubble.
- `heimdall-ui`: `HeimdallPanel` — tab shell for Overview / Timeline / Network / Database / Storage / Logs / Flags, with
  live network, storage, database metadata, log, crash, and flag content where a collector has
  published data.
- `sample/androidApp`: reference integration wiring `HeimdallOverlay` + `AndroidShakeListener`
  into an Activity.

### Fixed
- Network detail's request/response body blocks (and several other panel search fields/lists)
  had no bounded width anywhere in their layout, so a long, unbroken line (e.g. minified JSON)
  overflowed past the popup's edge and was rendered off-screen instead of wrapping — it looked
  truncated even though the full text was captured and stored correctly. Also fixed: Logs',
  Flags', and Storage's search fields, and Flags' global action buttons, weren't full-width like
  Network's and Database's, making the tabs look inconsistent.
- **A forgotten `Heimdall.install()` call could crash real app functionality, not just fail to
  record.** `NetworkStore`/`LogStore`/`EventStore.record()` called `HeimdallDatabase.write`, which
  threw `IllegalStateException` if the database wasn't open yet — and since `NetworkStore.record`
  runs from `HeimdallKtor`'s `on(Send)` hook with no surrounding `try`/`catch`, that exception
  propagated straight out through the app's own network call. These three now no-op instead
  (`Heimdall.measure`/`screen` still run and return their block either way). Proved by
  `NotInstalledTest`: reverting the guard turns it red with exactly that exception.
- `sample/shared`'s iOS host (`MainViewController.kt`) now compiles: wrong `hitTest` override
  signature, several `CValue<T>`/`T` mismatches (`CGPoint`, `CGRect`), missing
  `@OptIn(ExperimentalForeignApi::class)`, and `addChildViewController`/
  `didMoveToParentViewController` needing an explicit import (they're extension functions in this
  Kotlin/Native UIKit binding, not members) were all fixed. Every Heimdall module, including the
  full sample, now compiles for `iosSimulatorArm64` and Android in the same tree.
- `heimdall-storage`'s iOS Keychain code imported `CFBridgingRetain`/`CFBridgingRelease` from the
  wrong package (`platform.CoreFoundation` instead of `platform.Foundation`), and a stray `/*` in
  a KDoc comment (from `` `Library/Preferences/*.plist` ``) made a Kotlin nested block comment
  swallow the rest of `IosUserDefaultsDiscovery.kt`. Neither had ever been caught, since iOS had
  never been compiled before — the whole module now compiles cleanly for `iosSimulatorArm64`.

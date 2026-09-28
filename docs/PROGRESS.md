# Progress

One page to check any time to see where Heimdall actually stands. Updated as work happens —
if this disagrees with the code, the code wins; say so and it'll get fixed here.

**Nothing has run on a device or simulator yet.** Everything below is "compiles" and, where
noted, "has unit tests", not "works".

## The 12 chunks

| # | Chunk | Status |
|---|---|---|
| 1 | Overlay shell: bubble, drag, hide, shake-to-recall, floating inspector | Rewritten, floating window now implemented; device verification remains |
| 2 | Heimdall's own database, launch sessions, 24h retention | Committed, tested |
| 3 | Network capture (Ktor): headers/bodies as-is, failed calls recorded | Committed, tested |
| 4 | Storage: DataStore, SharedPreferences, Keystore, UserDefaults, Keychain | Rewritten, not yet compiled/tested |
| 5 | Logs and crashes | In progress (recording and live UI exist in core, panel is wired in) |
| 6 | App database inspector (Room/SQLDelight/raw SQLite, live) | In progress (multiple tables, rows, attachable inspector API, refresh, and per-table search are live; framework adapters remain) |
| 7 | Flags: persist overrides, restart button | In progress (override logic and panel controls exist, persistence still not durable) |
| 8 | UI: icon rail, overview, network details, database rows, searchable storage/logs | In progress (floating inspector and tester-focused views are wired; device verification remains) |
| 9 | Showing the overlay in a real app (Android auto-inject, iOS window + shake) | Not started |
| 10 | Release safety (no-op artifact) | Not started |
| 11 | Sample app exercising every feature | In progress (Network/Feed use real Ktor calls and Coil images; Database uses bundled SQLite; Android Storage uses discovered SharedPreferences; iOS storage host wiring remains) |
| 12 | Per-feature docs | Ongoing — written as each chunk lands |
| — | Device verification, one chunk at a time | Not started |

## Right now

The last commit (`5c2ef89`) is chunks 1–4 as first built. On top of that, **uncommitted** fixes
for bugs found in chunks 1 and 4 are staged (`git status` shows them) — you asked to compile and
test these yourself before they're committed:

- Bubble: tap now opens the panel, drag-to-bottom hides it instead of "any drag left/up",
  shake restores its position, panel has a Close button and blocks touches.
- Storage: re-scans when the Storage tab opens (new SharedPreferences files, UserDefaults
  suites), adds Keychain (iOS) and Android Keystore (read-only), fixes a listener that could be
  garbage-collected and a UserDefaults type check that never matched.

**Not yet verified by anyone** — compile and run these before trusting them, especially the iOS
Keychain file (`IosKeychainDiscovery.kt`), which uses low-level CoreFoundation interop:

```
./gradlew assembleDebug \
  :heimdall-core:testAndroidHostTest \
  :heimdall-network-ktor:testAndroidHostTest \
  :heimdall-storage:testAndroidHostTest \
  :heimdall-storage:compileKotlinIosSimulatorArm64 \
  :heimdall-ui:compileKotlinIosSimulatorArm64
```

## Decisions made along the way

- History (network, logs, crashes) is tagged per launch ("session"); live state (storage, app
  database, flags) is not — see `docs/architecture.md`, "History vs. live state".
- Retention is 24 hours, not "last 20 launches" — you're using Firebase Crashlytics for
  long-term crash history, Heimdall only needs enough to debug what just happened.
- Network capture is unredacted by default (so a captured call is a working `curl` command);
  redaction is an opt-in.
- DataStore needs one line of setup (`Heimdall.attachDataStore(...)`) because DataStore can't
  have two open instances on the same file. SharedPreferences/UserDefaults/Keychain/Keystore are
  found automatically.

## Open question

**Crashes tab: should it show every crash from every launch in one list** (recommended), or only
the current launch's, with older ones behind their session bar? Not decided yet — needed before
chunk 5.

## Known gaps (see `docs/TODO.md` for the full list)

- iOS has no shake listener or overlay host yet.
- Android sample back handling closes the panel before app navigation; consuming apps still need
  to connect their own back dispatcher to the controller.
- No no-op/release-safety artifact — do not ship this in a release build yet.
- Keychain only lists generic-password items (what most apps use), not keys/certificates.

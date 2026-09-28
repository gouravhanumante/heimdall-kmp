# Storage plugin (`heimdall-storage`)

Shows the app's key/value storage **live** — current values only, never history, never split
by session (see `docs/architecture.md`, "History vs. live state").

## Setup

| Platform | Call once, after `Heimdall.install(...)` | Finds |
|---|---|---|
| Android | `Heimdall.discoverStorage(context)` | SharedPreferences, Android Keystore |
| iOS | `Heimdall.discoverStorage(appGroupSuiteNames = listOf(...))` | UserDefaults (standard + suites), Keychain |
| Both | `Heimdall.attachDataStore(dataStore, name, scope)` — once per DataStore | That DataStore |

Sources that need scanning (SharedPreferences files, UserDefaults suites, Keychain, Keystore) are
re-scanned whenever the Storage tab opens (`StorageStore.refresh()`), so things created after
startup appear too. Values of already-attached sources update live.

## Sources

| Source | Editable | Notes |
|---|---|---|
| DataStore | Yes | Needs the one line above — see below |
| SharedPreferences | Yes | Every file in `shared_prefs/` |
| Android Keystore | No | Aliases and key algorithm only; key material can't be read by design |
| UserDefaults | Text values only | App's own keys only (`persistentDomainForName`), no Apple system keys. Suites found by listing `Library/Preferences/*.plist`; app-group suites must be passed in |
| Keychain | Yes (text values) | Generic-password items only, shown as `service / account`. Non-text values show as a byte count |

## Why DataStore needs one line

DataStore allows only one open instance per file in a process. If Heimdall opened the app's
file itself, the app would crash with "There are multiple DataStores active for the same file".
So the app passes in the instance it already has, e.g. from Koin:

```kotlin
Heimdall.attachDataStore(get<DataStore<Preferences>>(), name = "auth", scope = appScope)
```

`scope` must live as long as the DataStore; panel edits are launched on it.

## Type safety when editing

The panel edits through a text field, so writes must not change a value's type — the app's own
typed read (`getBoolean`, `prefs[intPreferencesKey(..)]`) would then throw.
- **DataStore / SharedPreferences**: the write mirrors the existing value's type
  (`Boolean`/`Int`/`Long`/`Float`/`Double`/`String`); a value that doesn't parse is rejected
  (`StorageWriter.write` returns `false`). `Set<String>`/`ByteArray` can't be edited.
- **UserDefaults**: only string values can be edited. Foundation stores Bool and Int both as
  `NSNumber`, so the original type can't be recovered.
- **Keychain**: values are written back as UTF-8 text.

## Verification status

`attachDataStore`: 5 JVM tests (`DataStoreAttachmentTest`); the two type tests were confirmed to
fail against a string-only write. SharedPreferences, Keystore, UserDefaults and Keychain
discovery: written, not yet compiled by me or run on a device.

# Storage plugin (`heimdall-storage`)

Shows the app's key/value storage **live** — current values only, never history, never split
by session (see `docs/architecture.md`, "History vs. live state").

## Sources

| Source | Platform | How it's attached | Editable from the panel |
|---|---|---|---|
| `DataStore<Preferences>` | Android + iOS | **One line**: `Heimdall.attachDataStore(dataStore, name, scope)` | Yes — written as a string key |
| SharedPreferences | Android | Auto: `Heimdall.discoverAndroidPreferences(context)` | Yes — see "type safety" |
| `NSUserDefaults.standardUserDefaults` | iOS | Auto: `Heimdall.discoverStandardUserDefaults()` | Text values only |
| Custom `NSUserDefaults(suiteName:)` | iOS | Not supported | — |
| Keychain / Android Keystore | both | Not supported yet — see `docs/TODO.md` | — |

## Why DataStore needs one line

DataStore allows only one open instance per file in a process. If Heimdall opened the app's
file itself, the app would crash with "There are multiple DataStores active for the same file".
So the app passes in the instance it already has. Example (Koin):

```kotlin
single<DataStore<Preferences>> { PreferenceDataStoreFactory.createWithPath(scope, produceFile) }
    .also { /* after it's created */ }
// then, once, e.g. in Application.onCreate:
Heimdall.attachDataStore(get(), name = "auth", scope = appScope)
```

`scope` must live as long as the DataStore; panel edits are launched on it.

## Auto-discovery limits

- **SharedPreferences**: found by listing the `shared_prefs/` directory once, when
  `discoverAndroidPreferences` is called. Files created after that call are not picked up.
- **UserDefaults**: only the standard suite; iOS has no API to list suite names.
- Change listeners are never unregistered — they live as long as the app, like the stores.

## Type safety when editing

The panel edits through a text field.
- **SharedPreferences** keeps the key's original type: a `Boolean` key only accepts
  `true`/`false`, an `Int` key only an integer, and so on. An unparseable value is rejected
  (`StorageWriter.write` returns `false`) instead of being written as the wrong type, which would
  make the app's own `getBoolean` throw. `Set<String>` values can't be edited.
- **UserDefaults**: only keys whose current value is a string can be edited. Foundation stores
  Bool and Int both as `NSNumber`, so the original type can't be recovered reliably.
- **DataStore**: same rule as SharedPreferences — the write mirrors the existing key's type
  (`Boolean`/`Int`/`Long`/`Float`/`Double`/`String`), and an unparseable value is rejected.
  DataStore keys compare by name only, so writing through the wrong key type would silently
  replace the value's type and break the app's own typed read. `Set<String>`/`ByteArray` values
  can't be edited.

## Verification status

`attachDataStore`: 5 JVM tests (`DataStoreAttachmentTest`), including type preservation — the two
type tests were confirmed to fail against a string-only write. SharedPreferences and UserDefaults
discovery: compile only, not run on a device.

# Database plugin (`heimdall-database-sqlite`)

Shows the app's own SQLite database in the inspector — table list, rows, and a read-only
ad-hoc query box. Optional module; `heimdall-core`'s `DatabaseInspector` interface is the
contract, `heimdall-database-sqlite` is one adapter for it.

## Setup

Room, SQLDelight, and raw `androidx.sqlite`/`SupportSQLite` databases are all, underneath, a
plain SQLite file. `SqliteFileInspector` opens that file directly, so one adapter covers all
three — pass the same path the app already gives to `Room.databaseBuilder`, the SQLDelight
driver, or `openOrCreateDatabase`:

```kotlin
Heimdall.database.attach(
    SqliteFileInspector(databaseName = "app.db", path = context.getDatabasePath("app.db").path),
)
```

Call `attach` once, after `Heimdall.install(...)` and after the app has created the database file
(the file must already exist). Call `Heimdall.database.refresh("app.db")` after writes the app
wants reflected sooner than the next time the Database tab is opened.

**Not usable with an in-memory database** (`:memory:`): a second SQLite connection can't see
another connection's in-memory data, so there's nothing on disk to open.

## How it reads the database

`SqliteFileInspector` opens its own connection to the file, with the driver's `SQLITE_OPEN_READONLY`
flag — it never touches the app's writer connection and cannot write to the file even if asked to
(proved by `SqliteFileInspectorTest`: removing the read-only flag turns a passing test red).

- **Tables**: every table in `sqlite_master`, except SQLite/Room housekeeping tables
  (`sqlite_sequence`, `android_metadata`, `room_master_table`).
- **Rows**: up to `maxRowsPerTable` (default 200) per table, in whatever order `SELECT *` returns.
- **Ad-hoc query**: `SqliteFileInspector.query(sql)` accepts only `SELECT`, `PRAGMA`, `EXPLAIN`,
  or `WITH` statements, and only one statement at a time; anything else throws
  `IllegalArgumentException` before it reaches the connection.
- **Blob columns** show as `<blob NB>` (byte count), not raw bytes, since there is no useful text
  form. Every other type is read through SQLite's own text conversion.

## Verification status

`SqliteFileInspectorTest`: 5 JVM tests (snapshot contents, blob/null formatting, read-only
statement rejection, single-statement rejection, and the read-only-connection proof above). Not
yet run on a device or against a real Room/SQLDelight database.

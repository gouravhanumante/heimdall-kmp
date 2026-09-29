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
  This first load is a snapshot, not live — see "Search" below for what reaches the rest of the
  table.
- **Ad-hoc query**: `SqliteFileInspector.query(sql, args)` accepts only `SELECT`, `PRAGMA`,
  `EXPLAIN`, or `WITH` statements, and only one statement at a time; anything else throws
  `IllegalArgumentException` before it reaches the connection. `args` bind to `?` placeholders in
  the SQL — always prefer this over building `sql` by concatenating untrusted text into it.
- **Blob columns** show as `<blob NB>` (byte count), not raw bytes, since there is no useful text
  form. Every other type is read through SQLite's own text conversion.

## Search

Typing in a table's search box does **not** filter the snapshot rows already loaded — a match
outside the first `maxRowsPerTable` rows would be invisible if it did. Instead it calls
`Heimdall.database.queryRunnerFor(databaseName)` and runs a live, bound query against every column
(`... WHERE "col1" LIKE ? OR "col2" LIKE ? ... LIMIT 200`, one bound arg per column, `%`/`_`/`\`
in the search text escaped so they match literally). The search text is never concatenated into
the SQL string itself (`HeimdallPanel`'s `buildSearchQuery`, proved by
`DatabaseSearchQueryTest`: a SQL-injection-shaped search string never appears in the built SQL,
only as a bound argument; `SqliteFileInspectorTest` proves the same end-to-end through the bound
`?` actually reaching SQLite). Falls back to filtering the loaded snapshot rows if no query runner
was published, or if the live query fails for any reason. Results are capped at 200 rows; there is
no further pagination ("load more") yet — see `docs/TODO.md`.

## Verification status

`SqliteFileInspectorTest`: 7 JVM tests (snapshot contents, blob/null formatting, bound-argument
querying including a SQL-injection-payload proof, read-only statement rejection, single-statement
rejection, and the read-only-connection proof above). `DatabaseSearchQueryTest` (in `heimdall-ui`):
4 JVM tests for the search-query builder. Not
yet run on a device or against a real Room/SQLDelight database.

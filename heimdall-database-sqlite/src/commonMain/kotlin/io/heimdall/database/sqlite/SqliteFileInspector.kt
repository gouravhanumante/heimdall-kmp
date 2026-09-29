package io.heimdall.database.sqlite

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import io.heimdall.core.DatabaseInspector
import io.heimdall.core.DatabaseSnapshot
import io.heimdall.core.DatabaseTable

private val internalTableNames = setOf("android_metadata", "room_master_table", "sqlite_sequence")

/**
 * Inspects any on-disk SQLite database by file path. Room, SQLDelight and raw
 * androidx.sqlite/SupportSQLite databases all end up as a plain SQLite file, so one adapter
 * covers all of them — pass the same path given to `Room.databaseBuilder`, the SQLDelight
 * driver, or `openOrCreateDatabase`.
 *
 * Opens its own read-only connection; never touches the app's writer connection. Not usable
 * with an in-memory database (`:memory:`), since a second connection can't see another
 * connection's in-memory data.
 */
class SqliteFileInspector(
    override val databaseName: String,
    path: String,
    private val maxRowsPerTable: Int = 200,
) : DatabaseInspector {

    private val connection: SQLiteConnection =
        BundledSQLiteDriver().open(path, SQLITE_OPEN_READONLY or SQLITE_OPEN_FULLMUTEX)

    override fun snapshot(): DatabaseSnapshot =
        DatabaseSnapshot(databaseName, tableNames().map(::readTable))

    override fun query(sql: String, args: List<String>): DatabaseTable {
        requireReadOnlyStatement(sql)
        val statement = connection.prepare(sql)
        return try {
            args.forEachIndexed { index, arg -> statement.bindText(index + 1, arg) }
            readResult(name = "result", statement)
        } finally {
            statement.close()
        }
    }

    /** Releases the dedicated inspection connection. */
    fun close() {
        connection.close()
    }

    private fun tableNames(): List<String> {
        val statement = connection.prepare(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
        )
        return try {
            buildList {
                while (statement.step()) {
                    val name = statement.getText(0)
                    if (name !in internalTableNames) add(name)
                }
            }
        } finally {
            statement.close()
        }
    }

    private fun readTable(name: String): DatabaseTable {
        val statement = connection.prepare("SELECT * FROM \"$name\" LIMIT ?")
        return try {
            statement.bindLong(1, maxRowsPerTable.toLong())
            readResult(name, statement)
        } finally {
            statement.close()
        }
    }

    private fun readResult(name: String, statement: SQLiteStatement): DatabaseTable {
        val columnCount = statement.getColumnCount()
        val columns = List(columnCount) { statement.getColumnName(it) }
        val rows = buildList {
            while (statement.step()) {
                add(
                    List(columnCount) { index ->
                        when {
                            statement.isNull(index) -> null
                            statement.getColumnType(index) == SQLITE_DATA_BLOB ->
                                "<blob ${statement.getBlob(index).size}B>"
                            else -> statement.getText(index)
                        }
                    },
                )
            }
        }
        return DatabaseTable(name, columns, rows)
    }

    private fun requireReadOnlyStatement(sql: String) {
        val trimmed = sql.trim()
        require(trimmed.isNotEmpty()) { "Empty SQL statement" }
        require(!trimmed.removeSuffix(";").contains(';')) { "Only a single statement is allowed" }
        val keyword = trimmed.trimStart('(').substringBefore(' ').removeSuffix(";").uppercase()
        require(keyword == "SELECT" || keyword == "PRAGMA" || keyword == "EXPLAIN" || keyword == "WITH") {
            "Only read-only statements (SELECT/PRAGMA/EXPLAIN/WITH) are allowed, got: $keyword"
        }
    }
}

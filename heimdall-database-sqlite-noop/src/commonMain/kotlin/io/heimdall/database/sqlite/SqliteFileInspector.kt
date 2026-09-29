package io.heimdall.database.sqlite

import io.heimdall.core.DatabaseInspector
import io.heimdall.core.DatabaseSnapshot
import io.heimdall.core.DatabaseTable

/** Release build stand-in for `heimdall-database-sqlite`: opens nothing, reads nothing. */
class SqliteFileInspector(
    override val databaseName: String,
    path: String,
    private val maxRowsPerTable: Int = 200,
) : DatabaseInspector {
    override fun snapshot(): DatabaseSnapshot = DatabaseSnapshot(databaseName, emptyList())

    override fun query(sql: String, args: List<String>): DatabaseTable =
        DatabaseTable(name = "result", columns = emptyList(), rows = emptyList())

    fun close() = Unit
}

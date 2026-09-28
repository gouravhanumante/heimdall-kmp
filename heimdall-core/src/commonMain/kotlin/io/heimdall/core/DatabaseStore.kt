package io.heimdall.core

data class DatabaseTable(
    val name: String,
    val columns: List<String>,
    val rows: List<List<String?>>,
)

data class DatabaseSnapshot(
    val databaseName: String,
    val tables: List<DatabaseTable>,
)

fun interface DatabaseQueryRunner {
    /** Run a read-only SQL statement against the live database and return column names + rows.
     * Throws on invalid SQL or a non-SELECT statement — the collector decides what to allow. */
    fun query(sql: String): DatabaseTable
}

class DatabaseStore internal constructor() {
    private val snapshots = mutableMapOf<String, DatabaseSnapshot>()
    private val queryRunners = mutableMapOf<String, DatabaseQueryRunner>()

    fun publish(snapshot: DatabaseSnapshot, queryRunner: DatabaseQueryRunner? = null) {
        if (!Heimdall.enabled) return
        snapshots[snapshot.databaseName] = snapshot
        if (queryRunner != null) queryRunners[snapshot.databaseName] = queryRunner
    }

    fun snapshot(): List<DatabaseSnapshot> = snapshots.values.toList()

    fun queryRunnerFor(databaseName: String): DatabaseQueryRunner? = queryRunners[databaseName]
}

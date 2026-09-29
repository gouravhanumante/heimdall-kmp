package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
     * [args] are bound to `?` placeholders in order — always prefer this over concatenating
     * user-provided text into [sql], which would be a SQL injection vector. Throws on invalid SQL
     * or a non-SELECT statement — the collector decides what to allow. */
    fun query(sql: String, args: List<String>): DatabaseTable
}

interface DatabaseInspector {
    val databaseName: String

    /** Return a bounded, read-only view of the database's current tables and rows. */
    fun snapshot(): DatabaseSnapshot

    /** Run a read-only query using the app database's own driver or framework. See
     * [DatabaseQueryRunner.query] for why [args] exists. */
    fun query(sql: String, args: List<String> = emptyList()): DatabaseTable
}

class DatabaseStore internal constructor() {
    private val _current = MutableStateFlow<List<DatabaseSnapshot>>(emptyList())
    val current: StateFlow<List<DatabaseSnapshot>> = _current

    private val snapshots = linkedMapOf<String, DatabaseSnapshot>()
    private val queryRunners = linkedMapOf<String, DatabaseQueryRunner>()
    private val inspectors = linkedMapOf<String, DatabaseInspector>()

    fun publish(snapshot: DatabaseSnapshot, queryRunner: DatabaseQueryRunner? = null) {
        if (!Heimdall.enabled) return
        snapshots[snapshot.databaseName] = snapshot
        if (queryRunner != null) queryRunners[snapshot.databaseName] = queryRunner
        _current.update { snapshots.values.toList() }
    }

    fun attach(inspector: DatabaseInspector) {
        if (!Heimdall.enabled) return
        inspectors[inspector.databaseName] = inspector
        publish(
            snapshot = inspector.snapshot(),
            queryRunner = DatabaseQueryRunner(inspector::query),
        )
    }

    fun refresh(databaseName: String) {
        if (!Heimdall.enabled) return
        val inspector = inspectors[databaseName] ?: return
        publish(
            snapshot = inspector.snapshot(),
            queryRunner = DatabaseQueryRunner(inspector::query),
        )
    }

    fun refreshAll() {
        inspectors.keys.toList().forEach(::refresh)
    }

    fun snapshot(): List<DatabaseSnapshot> = _current.value

    fun queryRunnerFor(databaseName: String): DatabaseQueryRunner? = queryRunners[databaseName]
}

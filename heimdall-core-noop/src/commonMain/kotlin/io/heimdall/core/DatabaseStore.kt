package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    fun query(sql: String): DatabaseTable
}

interface DatabaseInspector {
    val databaseName: String
    fun snapshot(): DatabaseSnapshot
    fun query(sql: String): DatabaseTable
}

class DatabaseStore internal constructor() {
    val current: StateFlow<List<DatabaseSnapshot>> = MutableStateFlow(emptyList())

    /** Never calls [inspector] \u2014 the real version reads it on attach to publish a first
     * snapshot; here that would just be I/O with nothing to show it to. */
    fun attach(inspector: DatabaseInspector) = Unit

    fun publish(snapshot: DatabaseSnapshot, queryRunner: DatabaseQueryRunner? = null) = Unit
    fun refresh(databaseName: String) = Unit
    fun refreshAll() = Unit
    fun snapshot(): List<DatabaseSnapshot> = emptyList()
    fun queryRunnerFor(databaseName: String): DatabaseQueryRunner? = null
}

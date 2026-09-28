package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The current key/values of one storage source (a DataStore, SharedPreferences, …). Live state,
 * not history: never persisted or tagged with a session — see docs/architecture.md. */
data class StorageSnapshot(
    val sourceName: String,
    val entries: Map<String, String>,
)

fun interface StorageWriter {
    /** Write [value] back to [key] in the underlying store. Returns false if the write was
     * rejected (e.g. a type the source can't safely write back). */
    fun write(key: String, value: String): Boolean
}

class StorageStore internal constructor() {
    // StateFlow.update is an atomic compare-and-set, so collectors publishing from different
    // threads (a SharedPreferences listener, a DataStore flow) can't lose each other's writes.
    private val _current = MutableStateFlow<Map<String, StorageSnapshot>>(emptyMap())
    private val writers = MutableStateFlow<Map<String, StorageWriter>>(emptyMap())
    private val refreshers = MutableStateFlow<List<() -> Unit>>(emptyList())

    /** Every attached source, keyed by [StorageSnapshot.sourceName], updated live. */
    val current: StateFlow<Map<String, StorageSnapshot>> = _current

    fun publish(snapshot: StorageSnapshot, writer: StorageWriter? = null) {
        if (!Heimdall.enabled) return
        if (writer != null) writers.update { it + (snapshot.sourceName to writer) }
        _current.update { it + (snapshot.sourceName to snapshot) }
    }

    /** For collectors that must re-scan to find new sources (new prefs files, keychain items). */
    fun addRefresher(refresh: () -> Unit) {
        refreshers.update { it + refresh }
    }

    /** Called by the panel when the Storage tab opens. */
    fun refresh() {
        refreshers.value.forEach { it() }
    }

    fun snapshot(): List<StorageSnapshot> = _current.value.values.toList()

    fun writerFor(sourceName: String): StorageWriter? = writers.value[sourceName]
}

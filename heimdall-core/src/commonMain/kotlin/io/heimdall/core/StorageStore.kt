package io.heimdall.core

/** A key/value snapshot of one storage source (a DataStore, SharedPreferences, etc.), refreshed
 * by the collector each time the panel opens or the user hits refresh — not a live subscription,
 * since most KV stores don't cheaply expose "every value, whenever any of them changes". */
data class StorageSnapshot(
    val sourceName: String,
    val entries: Map<String, String>,
)

fun interface StorageWriter {
    /** Write [value] back to [key] in the underlying store. Returns false if the write was
     * rejected (e.g. the collector was opened read-only). */
    fun write(key: String, value: String): Boolean
}

class StorageStore internal constructor() {
    private val snapshots = mutableMapOf<String, StorageSnapshot>()
    private val writers = mutableMapOf<String, StorageWriter>()

    fun publish(snapshot: StorageSnapshot, writer: StorageWriter? = null) {
        if (!Heimdall.enabled) return
        snapshots[snapshot.sourceName] = snapshot
        if (writer != null) writers[snapshot.sourceName] = writer
    }

    fun snapshot(): List<StorageSnapshot> = snapshots.values.toList()

    fun writerFor(sourceName: String): StorageWriter? = writers[sourceName]
}

package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class StorageSnapshot(
    val sourceName: String,
    val entries: Map<String, String>,
)

fun interface StorageWriter {
    fun write(key: String, value: String): Boolean
}

class StorageStore internal constructor() {
    val current: StateFlow<Map<String, StorageSnapshot>> = MutableStateFlow(emptyMap())
    fun publish(snapshot: StorageSnapshot, writer: StorageWriter? = null) = Unit
    fun addRefresher(refresh: () -> Unit) = Unit
    fun refresh() = Unit
    fun snapshot(): List<StorageSnapshot> = emptyList()
    fun writerFor(sourceName: String): StorageWriter? = null
}

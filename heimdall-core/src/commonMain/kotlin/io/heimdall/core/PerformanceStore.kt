package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PerformanceRecord(
    val name: String,
    val screen: String?,
    val durationMillis: Long,
    val timestampMillis: Long,
)

class PerformanceStore internal constructor() {
    private val _current = MutableStateFlow<List<PerformanceRecord>>(emptyList())
    val current: StateFlow<List<PerformanceRecord>> = _current

    fun record(record: PerformanceRecord) {
        if (!Heimdall.enabled) return
        _current.update { (listOf(record) + it).take(HeimdallDatabase.MAX_PERFORMANCE_RECORDS) }
    }

    fun clear() {
        _current.value = emptyList()
    }
}
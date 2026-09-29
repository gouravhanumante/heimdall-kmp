package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PerformanceRecord(
    val name: String,
    val screen: String?,
    val durationMillis: Long,
    val timestampMillis: Long,
)

class PerformanceStore internal constructor() {
    val current: StateFlow<List<PerformanceRecord>> = MutableStateFlow(emptyList())
    fun record(record: PerformanceRecord) = Unit
    fun clear() = Unit
}

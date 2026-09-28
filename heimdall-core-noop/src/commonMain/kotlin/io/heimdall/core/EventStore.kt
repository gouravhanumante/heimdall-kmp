package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HeimdallEvent(
    val id: String,
    val name: String,
    val screen: String?,
    val attributes: Map<String, String>,
    val timestampMillis: Long,
)

class EventStore internal constructor() {
    val current: StateFlow<List<HeimdallEvent>> = MutableStateFlow(emptyList())
    fun record(event: HeimdallEvent) = Unit
    fun forSession(sessionId: Long): List<HeimdallEvent> = emptyList()
    fun clear() = Unit
}

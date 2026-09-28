package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NetworkRecord(
    val id: String,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val statusCode: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val startedAtMillis: Long,
    val durationMillis: Long?,
    val error: String?,
) {
    val isError: Boolean get() = error != null || (statusCode != null && statusCode >= 400)
}

class NetworkStore internal constructor() {
    val current: StateFlow<List<NetworkRecord>> = MutableStateFlow(emptyList())
    fun record(entry: NetworkRecord) = Unit
    fun forSession(sessionId: Long): List<NetworkRecord> = emptyList()
    fun clear() = Unit
}

package io.heimdall.core

/** One captured HTTP call. [requestBody]/[responseBody] are already-truncated text — truncation
 * and redaction happen at the collector (e.g. the Ktor plugin), not here, so this model stays
 * collector-agnostic per docs/architecture.md's "any HTTP client" goal. */
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

class NetworkStore internal constructor(capacity: Int = 500) {
    private val buffer = RingBuffer<NetworkRecord>(capacity)

    fun record(entry: NetworkRecord) {
        if (!Heimdall.enabled) return
        buffer.push(entry)
    }

    fun snapshot(): List<NetworkRecord> = buffer.snapshot()

    fun clear() = buffer.clear()
}

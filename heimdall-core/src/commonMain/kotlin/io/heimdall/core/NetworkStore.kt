package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

/**
 * Persists every call to Heimdall's own database (see [HeimdallDatabase]) as it happens — whether
 * or not the panel is open — and keeps a capped, newest-first in-memory copy of the *current*
 * session as a [StateFlow] so the panel updates live without polling the database.
 */
class NetworkStore internal constructor() {
    private val _current = MutableStateFlow<List<NetworkRecord>>(emptyList())
    val current: StateFlow<List<NetworkRecord>> = _current

    fun record(entry: NetworkRecord) {
        if (!Heimdall.enabled) return
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn -> insert(conn, sessionId, entry) }
        _current.update { (listOf(entry) + it).take(HeimdallDatabase.MAX_NETWORK_RECORDS_PER_SESSION) }
    }

    /** Loads a past session's calls straight from disk — not cached, since only the current
     * session needs to be live/reactive. */
    fun forSession(sessionId: Long): List<NetworkRecord> = HeimdallDatabase.read { conn ->
        conn.prepare(
            "SELECT id, method, url, request_headers, request_body, status_code, " +
                "response_headers, response_body, started_at_millis, duration_millis, error " +
                "FROM network_records WHERE session_id = ? ORDER BY started_at_millis DESC",
        ).use { stmt ->
            stmt.bindLong(1, sessionId)
            buildList { while (stmt.step()) add(readRecord(stmt)) }
        }
    }

    fun clear() {
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            conn.prepare("DELETE FROM network_records WHERE session_id = ?").use { stmt ->
                stmt.bindLong(1, sessionId)
                stmt.step()
            }
        }
        _current.value = emptyList()
    }

    private fun insert(conn: SQLiteConnection, sessionId: Long, entry: NetworkRecord) {
        conn.prepare(
            "INSERT OR REPLACE INTO network_records (id, session_id, method, url, " +
                "request_headers, request_body, status_code, response_headers, response_body, " +
                "started_at_millis, duration_millis, error) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ).use { stmt ->
            stmt.bindText(1, entry.id)
            stmt.bindLong(2, sessionId)
            stmt.bindText(3, entry.method)
            stmt.bindText(4, entry.url)
            stmt.bindText(5, encodeStringMap(entry.requestHeaders))
            bindNullableText(stmt, 6, entry.requestBody)
            bindNullableLong(stmt, 7, entry.statusCode?.toLong())
            stmt.bindText(8, encodeStringMap(entry.responseHeaders))
            bindNullableText(stmt, 9, entry.responseBody)
            stmt.bindLong(10, entry.startedAtMillis)
            bindNullableLong(stmt, 11, entry.durationMillis)
            bindNullableText(stmt, 12, entry.error)
            stmt.step()
        }
        conn.execSQL(
            "DELETE FROM network_records WHERE session_id = $sessionId AND id NOT IN " +
                "(SELECT id FROM network_records WHERE session_id = $sessionId " +
                "ORDER BY started_at_millis DESC LIMIT ${HeimdallDatabase.MAX_NETWORK_RECORDS_PER_SESSION})",
        )
    }

    private fun readRecord(stmt: androidx.sqlite.SQLiteStatement): NetworkRecord = NetworkRecord(
        id = stmt.getText(0),
        method = stmt.getText(1),
        url = stmt.getText(2),
        requestHeaders = decodeStringMap(stmt.getText(3)),
        requestBody = if (stmt.isNull(4)) null else stmt.getText(4),
        statusCode = if (stmt.isNull(5)) null else stmt.getLong(5).toInt(),
        responseHeaders = decodeStringMap(stmt.getText(6)),
        responseBody = if (stmt.isNull(7)) null else stmt.getText(7),
        startedAtMillis = stmt.getLong(8),
        durationMillis = if (stmt.isNull(9)) null else stmt.getLong(9),
        error = if (stmt.isNull(10)) null else stmt.getText(10),
    )
}

private fun bindNullableText(stmt: androidx.sqlite.SQLiteStatement, index: Int, value: String?) {
    if (value == null) stmt.bindNull(index) else stmt.bindText(index, value)
}

private fun bindNullableLong(stmt: androidx.sqlite.SQLiteStatement, index: Int, value: Long?) {
    if (value == null) stmt.bindNull(index) else stmt.bindLong(index, value)
}

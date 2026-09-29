package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class HeimdallEvent(
    val id: String,
    val name: String,
    val screen: String?,
    val attributes: Map<String, String>,
    val timestampMillis: Long,
)

class EventStore internal constructor() {
    private companion object {
        const val MAX_NAME_LENGTH = 128
        const val MAX_SCREEN_LENGTH = 128
        const val MAX_ATTRIBUTES = 32
        const val MAX_ATTRIBUTE_LENGTH = 1_024
    }

    private val _current = MutableStateFlow<List<HeimdallEvent>>(emptyList())
    val current: StateFlow<List<HeimdallEvent>> = _current

    /** No-ops (doesn't throw) if [Heimdall.install] hasn't been called yet — `Heimdall.event(...)`
     * and `Heimdall.measure(...)` must never crash the app's own code around them. */
    fun record(event: HeimdallEvent) {
        if (!Heimdall.enabled || !HeimdallDatabase.isOpen()) return
        val boundedEvent = event.copy(
            name = event.name.take(MAX_NAME_LENGTH),
            screen = event.screen?.take(MAX_SCREEN_LENGTH),
            attributes = event.attributes.entries
                .take(MAX_ATTRIBUTES)
                .associate { (key, value) -> key.take(MAX_NAME_LENGTH) to value.take(MAX_ATTRIBUTE_LENGTH) },
        )
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            insert(conn, sessionId, boundedEvent)
            HeimdallDatabase.pruneIfDue(conn, epochMillisNow(), sessionId)
        }
        _current.update { (listOf(boundedEvent) + it).take(HeimdallDatabase.MAX_EVENTS_PER_SESSION) }
    }

    fun forSession(sessionId: Long): List<HeimdallEvent> = HeimdallDatabase.read { conn ->
        conn.prepare(
            "SELECT id, name, screen, attributes, timestamp_millis FROM events " +
                "WHERE session_id = ? ORDER BY timestamp_millis DESC",
        ).use { stmt ->
            stmt.bindLong(1, sessionId)
            buildList {
                while (stmt.step()) {
                    add(
                        HeimdallEvent(
                            id = stmt.getText(0),
                            name = stmt.getText(1),
                            screen = if (stmt.isNull(2)) null else stmt.getText(2),
                            attributes = decodeStringMap(stmt.getText(3)),
                            timestampMillis = stmt.getLong(4),
                        ),
                    )
                }
            }
        }
    }

    fun clear() {
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            conn.prepare("DELETE FROM events WHERE session_id = ?").use {
                it.bindLong(1, sessionId)
                it.step()
            }
        }
        _current.value = emptyList()
    }

    private fun insert(conn: SQLiteConnection, sessionId: Long, event: HeimdallEvent) {
        conn.prepare(
            "INSERT OR REPLACE INTO events " +
                "(id, session_id, name, screen, attributes, timestamp_millis) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
        ).use { stmt ->
            stmt.bindText(1, event.id)
            stmt.bindLong(2, sessionId)
            stmt.bindText(3, event.name)
            if (event.screen == null) stmt.bindNull(4) else stmt.bindText(4, event.screen)
            stmt.bindText(5, encodeStringMap(event.attributes))
            stmt.bindLong(6, event.timestampMillis)
            stmt.step()
        }
        conn.execSQL(
            "DELETE FROM events WHERE session_id = $sessionId AND id NOT IN " +
                "(SELECT id FROM events WHERE session_id = $sessionId " +
                "ORDER BY timestamp_millis DESC LIMIT ${HeimdallDatabase.MAX_EVENTS_PER_SESSION})",
        )
    }
}
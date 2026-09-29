package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestampMillis: Long,
    val throwableText: String?,
)

/** A crash (uncaught exception) or a handled exception explicitly reported via
 * [CrashStore.record]. Kept separate from [LogStore] because crashes need to survive across the
 * process death that often follows one — see [HeimdallDatabase.writeBeforeCrash], which
 * [CrashStore.record] uses instead of the normal write path. */
data class CrashRecord(
    val id: String,
    val isFatal: Boolean,
    val exceptionType: String,
    val message: String?,
    val stackTraceText: String,
    val timestampMillis: Long,
)

class LogStore internal constructor() {
    private val _current = MutableStateFlow<List<LogEntry>>(emptyList())
    val current: StateFlow<List<LogEntry>> = _current

    /** No-ops (doesn't throw) if [Heimdall.install] hasn't been called yet — a forgotten install()
     * must never crash an otherwise-working `Heimdall.log(...)` call site. */
    fun record(entry: LogEntry) {
        if (!Heimdall.enabled || !HeimdallDatabase.isOpen()) return
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            insert(conn, sessionId, entry)
            HeimdallDatabase.pruneIfDue(conn, epochMillisNow(), sessionId)
        }
        _current.update { (listOf(entry) + it).take(HeimdallDatabase.MAX_LOG_ENTRIES_PER_SESSION) }
    }

    fun forSession(sessionId: Long): List<LogEntry> = HeimdallDatabase.read { conn ->
        conn.prepare(
            "SELECT level, tag, message, timestamp_millis, throwable_text FROM log_entries " +
                "WHERE session_id = ? ORDER BY timestamp_millis DESC",
        ).use { stmt ->
            stmt.bindLong(1, sessionId)
            buildList {
                while (stmt.step()) {
                    add(
                        LogEntry(
                            level = LogLevel.valueOf(stmt.getText(0)),
                            tag = stmt.getText(1),
                            message = stmt.getText(2),
                            timestampMillis = stmt.getLong(3),
                            throwableText = if (stmt.isNull(4)) null else stmt.getText(4),
                        ),
                    )
                }
            }
        }
    }

    fun clear() {
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            conn.prepare("DELETE FROM log_entries WHERE session_id = ?").use { it.bindLong(1, sessionId); it.step() }
        }
        _current.value = emptyList()
    }

    private fun insert(conn: SQLiteConnection, sessionId: Long, entry: LogEntry) {
        conn.prepare(
            "INSERT INTO log_entries (session_id, level, tag, message, timestamp_millis, throwable_text) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
        ).use { stmt ->
            stmt.bindLong(1, sessionId)
            stmt.bindText(2, entry.level.name)
            stmt.bindText(3, entry.tag)
            stmt.bindText(4, entry.message)
            stmt.bindLong(5, entry.timestampMillis)
            bindNullableText(stmt, 6, entry.throwableText)
            stmt.step()
        }
        conn.execSQL(
            "DELETE FROM log_entries WHERE session_id = $sessionId AND id NOT IN " +
                "(SELECT id FROM log_entries WHERE session_id = $sessionId " +
                "ORDER BY timestamp_millis DESC LIMIT ${HeimdallDatabase.MAX_LOG_ENTRIES_PER_SESSION})",
        )
    }
}

class CrashStore internal constructor() {
    private val _current = MutableStateFlow<List<CrashRecord>>(emptyList())
    val current: StateFlow<List<CrashRecord>> = _current

    /** Uses [HeimdallDatabase.writeBeforeCrash] (blocking, no coroutine scope assumed) since a
     * fatal crash calls this from the thread that's about to die. */
    fun record(entry: CrashRecord) {
        if (!Heimdall.enabled) return
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.writeBeforeCrash { conn -> insert(conn, sessionId, entry) }
        if (entry.isFatal) SessionRepository.markCrashed(sessionId)
        _current.update { (listOf(entry) + it).take(HeimdallDatabase.MAX_CRASH_RECORDS_PER_SESSION) }
    }

    fun forSession(sessionId: Long): List<CrashRecord> = HeimdallDatabase.read { conn ->
        conn.prepare(
            "SELECT id, is_fatal, exception_type, message, stack_trace_text, timestamp_millis " +
                "FROM crash_records WHERE session_id = ? ORDER BY timestamp_millis DESC",
        ).use { stmt ->
            stmt.bindLong(1, sessionId)
            buildList {
                while (stmt.step()) {
                    add(
                        CrashRecord(
                            id = stmt.getText(0),
                            isFatal = stmt.getLong(1) != 0L,
                            exceptionType = stmt.getText(2),
                            message = if (stmt.isNull(3)) null else stmt.getText(3),
                            stackTraceText = stmt.getText(4),
                            timestampMillis = stmt.getLong(5),
                        ),
                    )
                }
            }
        }
    }

    fun clear() {
        val sessionId = Heimdall.currentSessionId
        HeimdallDatabase.write { conn ->
            conn.prepare("DELETE FROM crash_records WHERE session_id = ?").use { it.bindLong(1, sessionId); it.step() }
        }
        _current.value = emptyList()
    }

    private fun insert(conn: SQLiteConnection, sessionId: Long, entry: CrashRecord) {
        conn.prepare(
            "INSERT OR REPLACE INTO crash_records (id, session_id, is_fatal, exception_type, " +
                "message, stack_trace_text, timestamp_millis) VALUES (?, ?, ?, ?, ?, ?, ?)",
        ).use { stmt ->
            stmt.bindText(1, entry.id)
            stmt.bindLong(2, sessionId)
            stmt.bindBoolean(3, entry.isFatal)
            stmt.bindText(4, entry.exceptionType)
            bindNullableText(stmt, 5, entry.message)
            stmt.bindText(6, entry.stackTraceText)
            stmt.bindLong(7, entry.timestampMillis)
            stmt.step()
        }
        conn.execSQL(
            "DELETE FROM crash_records WHERE session_id = $sessionId AND id NOT IN " +
                "(SELECT id FROM crash_records WHERE session_id = $sessionId " +
                "ORDER BY timestamp_millis DESC LIMIT ${HeimdallDatabase.MAX_CRASH_RECORDS_PER_SESSION})",
        )
    }
}

private fun bindNullableText(stmt: SQLiteStatement, index: Int, value: String?) {
    if (value == null) stmt.bindNull(index) else stmt.bindText(index, value)
}

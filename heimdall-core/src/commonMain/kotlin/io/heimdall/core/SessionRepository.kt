package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Session bookkeeping: starting one, pruning history older than
 * [HeimdallDatabase.RETENTION_MILLIS] or beyond [MAX_RETAINED_SESSIONS], and reading sessions
 * back for the panel's session picker. */
internal object SessionRepository {

    /** Keeps the session picker a short, memorable list (current + a couple of recent runs)
     * instead of growing forever. */
    const val MAX_RETAINED_SESSIONS = 3

    fun startNewSession(startedAtMillis: Long): Long = HeimdallDatabase.write { conn ->
        conn.prepare("INSERT INTO sessions (started_at_millis, crashed) VALUES (?, 0)").use { stmt ->
            stmt.bindLong(1, startedAtMillis)
            stmt.step()
        }
        val id = conn.lastInsertRowId()
        HeimdallDatabase.pruneIfDue(conn, startedAtMillis, currentSessionId = id)
        pruneToMaxSessions(conn)
        id
    }

    /** Drops every session beyond the [MAX_RETAINED_SESSIONS] most recent, along with its
     * network/log/crash/event rows — there's no FK cascade on this schema, so each table is
     * cleared explicitly. */
    private fun pruneToMaxSessions(conn: SQLiteConnection) {
        val staleIds = conn.prepare(
            "SELECT id FROM sessions ORDER BY id DESC LIMIT -1 OFFSET $MAX_RETAINED_SESSIONS",
        ).use { stmt ->
            buildList { while (stmt.step()) add(stmt.getLong(0)) }
        }
        if (staleIds.isEmpty()) return
        val idList = staleIds.joinToString(",")
        conn.execSQL("DELETE FROM network_records WHERE session_id IN ($idList)")
        conn.execSQL("DELETE FROM log_entries WHERE session_id IN ($idList)")
        conn.execSQL("DELETE FROM crash_records WHERE session_id IN ($idList)")
        conn.execSQL("DELETE FROM events WHERE session_id IN ($idList)")
        conn.execSQL("DELETE FROM sessions WHERE id IN ($idList)")
    }

    fun markCrashed(sessionId: Long) {
        HeimdallDatabase.writeBeforeCrash { conn ->
            conn.prepare("UPDATE sessions SET crashed = 1 WHERE id = ?").use { stmt ->
                stmt.bindLong(1, sessionId)
                stmt.step()
            }
        }
    }

    fun listSessions(): List<Session> = HeimdallDatabase.read { conn ->
        conn.prepare("SELECT id, started_at_millis, crashed FROM sessions ORDER BY id DESC").use { stmt ->
            buildList {
                while (stmt.step()) {
                    add(Session(id = stmt.getLong(0), startedAtMillis = stmt.getLong(1), crashed = stmt.getLong(2) != 0L))
                }
            }
        }
    }
}

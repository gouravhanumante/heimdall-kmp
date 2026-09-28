package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Session bookkeeping: starting one, evicting the oldest beyond [HeimdallDatabase.MAX_SESSIONS],
 * and reading them back for the panel's session picker. */
internal object SessionRepository {

    fun startNewSession(startedAtMillis: Long): Long = HeimdallDatabase.write { conn ->
        conn.prepare("INSERT INTO sessions (started_at_millis, crashed) VALUES (?, 0)").use { stmt ->
            stmt.bindLong(1, startedAtMillis)
            stmt.step()
        }
        val id = conn.lastInsertRowId()
        evictOldSessions(conn)
        id
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

    private fun evictOldSessions(conn: androidx.sqlite.SQLiteConnection) {
        conn.prepare(
            "DELETE FROM sessions WHERE id IN (SELECT id FROM sessions ORDER BY id DESC LIMIT -1 OFFSET ?)",
        ).use { stmt ->
            stmt.bindLong(1, HeimdallDatabase.MAX_SESSIONS.toLong())
            stmt.step()
        }
        // No foreign keys configured, so a session's rows in other tables need an explicit
        // cleanup pass rather than a cascade delete.
        conn.execSQL("DELETE FROM network_records WHERE session_id NOT IN (SELECT id FROM sessions)")
        conn.execSQL("DELETE FROM log_entries WHERE session_id NOT IN (SELECT id FROM sessions)")
        conn.execSQL("DELETE FROM crash_records WHERE session_id NOT IN (SELECT id FROM sessions)")
    }
}

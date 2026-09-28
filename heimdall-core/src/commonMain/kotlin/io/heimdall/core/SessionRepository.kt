package io.heimdall.core

/** Session bookkeeping: starting one, pruning history older than
 * [HeimdallDatabase.RETENTION_MILLIS], and reading sessions back for the panel's session picker. */
internal object SessionRepository {

    fun startNewSession(startedAtMillis: Long): Long = HeimdallDatabase.write { conn ->
        conn.prepare("INSERT INTO sessions (started_at_millis, crashed) VALUES (?, 0)").use { stmt ->
            stmt.bindLong(1, startedAtMillis)
            stmt.step()
        }
        val id = conn.lastInsertRowId()
        HeimdallDatabase.pruneIfDue(conn, startedAtMillis, currentSessionId = id)
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
}

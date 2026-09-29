package io.heimdall.core

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Heimdall's own SQLite database — never the app's. One [BundledSQLiteDriver] connection, guarded
 * by [mutex]: the bundled driver's connections are documented as not thread-safe, and every
 * collector (network, logs, crashes) can call in from a different thread at once.
 *
 * [prepare]/[execSQL] on this connection are synchronous on every Heimdall target (see the
 * androidx.sqlite reference: `Cmn` = common, non-suspend), so [write]/[read] use `runBlocking`
 * purely as a cross-platform mutual-exclusion lock, not because real suspension happens inside.
 */
internal object HeimdallDatabase {
    const val RETENTION_MILLIS = 24L * 60 * 60 * 1000
    const val MAX_NETWORK_RECORDS_PER_SESSION = 5_000
    const val MAX_LOG_ENTRIES_PER_SESSION = 20_000
    const val MAX_CRASH_RECORDS_PER_SESSION = 200
    const val MAX_EVENTS_PER_SESSION = 5_000
    const val MAX_PERFORMANCE_RECORDS = 1_000
    private const val PRUNE_INTERVAL_MILLIS = 60L * 60 * 1000

    private val mutex = Mutex()
    private var connection: SQLiteConnection? = null
    private var lastPrunedAtMillis: Long? = null

    fun openForApp(context: PlatformContext) {
        open(resolveDatabasePath(context, "heimdall.db"))
    }

    /** In-memory, no crash-survival guarantee — the entire point of the real database. For tests
     * and previews only; see docs/architecture.md. */
    fun openInMemory() {
        open(":memory:")
    }

    private fun open(path: String) {
        if (connection != null) return
        val conn = BundledSQLiteDriver().open(path)
        createSchema(conn)
        connection = conn
    }

    private fun createSchema(conn: SQLiteConnection) {
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "started_at_millis INTEGER NOT NULL, " +
                "crashed INTEGER NOT NULL DEFAULT 0)",
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS network_records (" +
                "id TEXT PRIMARY KEY, session_id INTEGER NOT NULL, method TEXT NOT NULL, " +
                "url TEXT NOT NULL, request_headers TEXT NOT NULL, request_body TEXT, " +
                "status_code INTEGER, response_headers TEXT NOT NULL, response_body TEXT, " +
                "started_at_millis INTEGER NOT NULL, duration_millis INTEGER, error TEXT)",
        )
        conn.execSQL("CREATE INDEX IF NOT EXISTS idx_network_session ON network_records(session_id, started_at_millis)")
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS log_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, " +
                "level TEXT NOT NULL, tag TEXT NOT NULL, message TEXT NOT NULL, " +
                "timestamp_millis INTEGER NOT NULL, throwable_text TEXT)",
        )
        conn.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_session ON log_entries(session_id, timestamp_millis)")
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS crash_records (" +
                "id TEXT PRIMARY KEY, session_id INTEGER NOT NULL, is_fatal INTEGER NOT NULL, " +
                "exception_type TEXT NOT NULL, message TEXT, stack_trace_text TEXT NOT NULL, " +
                "timestamp_millis INTEGER NOT NULL)",
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS events (" +
                "id TEXT PRIMARY KEY, session_id INTEGER NOT NULL, name TEXT NOT NULL, " +
                "screen TEXT, attributes TEXT NOT NULL, timestamp_millis INTEGER NOT NULL)",
        )
        conn.execSQL("CREATE INDEX IF NOT EXISTS idx_events_session ON events(session_id, timestamp_millis)")
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS flag_overrides (" +
                "key TEXT PRIMARY KEY, value_type TEXT NOT NULL, value TEXT NOT NULL)",
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS bubble_position (" +
                "id INTEGER PRIMARY KEY, x_fraction REAL NOT NULL, y_fraction REAL NOT NULL)",
        )
    }

    /** For collectors on a normal thread — network capture, logging, session bookkeeping. */
    fun <T> write(block: (SQLiteConnection) -> T): T {
        val conn = requireConnection()
        return runBlocking { mutex.withLock { block(conn) } }
    }

    fun <T> read(block: (SQLiteConnection) -> T): T = write(block)

    /** Same lock, explicit name: called from a crash handler on a thread about to die, where the
     * write must land before the process actually exits. See docs/plugins/logs-crashes.md. */
    fun writeBeforeCrash(block: (SQLiteConnection) -> Unit) {
        val conn = connection ?: return
        runBlocking { mutex.withLock { block(conn) } }
    }

    fun isOpen(): Boolean = connection != null

    /** Deletes history older than [RETENTION_MILLIS]. Must be called inside [write]. Runs at most
     * once per [PRUNE_INTERVAL_MILLIS], so a process that stays alive for days still gets pruned. */
    fun pruneIfDue(conn: SQLiteConnection, nowMillis: Long, currentSessionId: Long) {
        val last = lastPrunedAtMillis
        if (last != null && nowMillis - last < PRUNE_INTERVAL_MILLIS) return
        lastPrunedAtMillis = nowMillis
        val cutoff = nowMillis - RETENTION_MILLIS
        conn.execSQL("DELETE FROM network_records WHERE started_at_millis < $cutoff")
        conn.execSQL("DELETE FROM log_entries WHERE timestamp_millis < $cutoff")
        conn.execSQL("DELETE FROM crash_records WHERE timestamp_millis < $cutoff")
        conn.execSQL("DELETE FROM events WHERE timestamp_millis < $cutoff")
        // A launch older than the cutoff stays while it still has recent rows, or is the current one.
        conn.execSQL(
            "DELETE FROM sessions WHERE started_at_millis < $cutoff AND id != $currentSessionId " +
                "AND id NOT IN (SELECT session_id FROM network_records) " +
                "AND id NOT IN (SELECT session_id FROM log_entries) " +
                "AND id NOT IN (SELECT session_id FROM crash_records) " +
                "AND id NOT IN (SELECT session_id FROM events)",
        )
    }

    private fun requireConnection(): SQLiteConnection =
        connection ?: error("Heimdall.install(context) must be called before recording anything")

    /** Test-only: drops every row without closing the connection, so each test starts clean. */
    fun clearAllForTests() {
        lastPrunedAtMillis = null
        write { conn ->
            conn.execSQL("DELETE FROM bubble_position")
            conn.execSQL("DELETE FROM crash_records")
            conn.execSQL("DELETE FROM events")
            conn.execSQL("DELETE FROM flag_overrides")
            conn.execSQL("DELETE FROM log_entries")
            conn.execSQL("DELETE FROM network_records")
            conn.execSQL("DELETE FROM sessions")
        }
    }

    /** Test-only: closes the connection and forgets it, simulating "install() never called" for a
     * test that needs that state — otherwise, once any test in the process opens the (shared,
     * singleton) database, it stays open for every test that runs after it. */
    fun closeForTests() {
        connection?.close()
        connection = null
        lastPrunedAtMillis = null
    }
}

internal fun SQLiteConnection.lastInsertRowId(): Long {
    prepare("SELECT last_insert_rowid()").use { stmt ->
        stmt.step()
        return stmt.getLong(0)
    }
}

internal inline fun <T> androidx.sqlite.SQLiteStatement.use(block: (androidx.sqlite.SQLiteStatement) -> T): T {
    try {
        return block(this)
    } finally {
        close()
    }
}

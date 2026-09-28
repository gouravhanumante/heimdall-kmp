package io.heimdall.core

/**
 * Single entry point every collector reports into and the panel reads from. `enabled` is the one
 * runtime kill switch that exists today — every store's `record`/`publish` call checks it. It
 * does **not** remove any code from a release binary; see docs/release-builds.md for what that
 * would take and why this alone isn't it.
 */
object Heimdall {
    var enabled: Boolean = true

    val network = NetworkStore()
    val logs = LogStore()
    val crashes = CrashStore()
    val storage = StorageStore()
    val database = DatabaseStore()
    val flags = FlagStore()

    /** A session is one process run — see [Session]. Call once at app start, before anything
     * else on this object; nothing before this has anywhere to persist to. */
    var currentSessionId: Long = 0
        private set

    /** Call once, in `Application.onCreate` (Android) or at the top of app startup (iOS). Opens
     * Heimdall's own on-disk database (never the app's) and starts a new session. */
    fun install(context: PlatformContext) {
        if (HeimdallDatabase.isOpen()) return
        HeimdallDatabase.openForApp(context)
        currentSessionId = SessionRepository.startNewSession(epochMillisNow())
    }

    /** For tests/previews: an in-memory database, no [PlatformContext] required. Not a
     * substitute for [install] in a real app — see docs/architecture.md. */
    fun installInMemory() {
        if (HeimdallDatabase.isOpen()) return
        HeimdallDatabase.openInMemory()
        currentSessionId = SessionRepository.startNewSession(epochMillisNow())
    }

    fun sessions(): List<Session> = SessionRepository.listSessions()

    /** Called by a platform crash handler, on the thread that's about to die — see
     * docs/plugins/logs-crashes.md for why this can't wait for the normal write path. */
    fun markCurrentSessionCrashed() {
        SessionRepository.markCrashed(currentSessionId)
    }
}

internal expect fun epochMillisNow(): Long


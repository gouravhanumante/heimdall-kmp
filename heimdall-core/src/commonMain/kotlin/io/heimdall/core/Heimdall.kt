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
    val events = EventStore()
    val performance = PerformanceStore()
    val storage = StorageStore()
    val database = DatabaseStore()
    val flags = FlagStore()

    /** A session is one process run — see [Session]. Call once at app start, before anything
     * else on this object; nothing before this has anywhere to persist to. */
    var currentSessionId: Long = 0
        private set

    var currentScreen: String? = null
        private set

    /** Call once, in `Application.onCreate` (Android) or at the top of app startup (iOS). Opens
     * Heimdall's own on-disk database (never the app's) and starts a new session. */
    fun install(context: PlatformContext) {
        if (HeimdallDatabase.isOpen()) return
        HeimdallDatabase.openForApp(context)
        currentSessionId = SessionRepository.startNewSession(epochMillisNow())
        flags.restorePersisted()
        installPlatformHooks(context)
    }

    /** For tests/previews: an in-memory database, no [PlatformContext] required. Not a
     * substitute for [install] in a real app — see docs/architecture.md. */
    fun installInMemory() {
        if (HeimdallDatabase.isOpen()) return
        HeimdallDatabase.openInMemory()
        currentSessionId = SessionRepository.startNewSession(epochMillisNow())
        flags.restorePersisted()
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (!enabled) return
        logs.record(
            LogEntry(
                level = level,
                tag = tag,
                message = message,
                timestampMillis = epochMillisNow(),
                throwableText = throwable?.stackTraceToString(),
            ),
        )
    }

    fun setCurrentScreen(screen: String?) {
        currentScreen = screen
    }

    fun <T> screen(name: String, block: () -> T): T {
        val previous = currentScreen
        currentScreen = name
        return try {
            block()
        } finally {
            currentScreen = previous
        }
    }

    fun event(name: String, attributes: Map<String, String> = emptyMap(), screen: String? = currentScreen) {
        events.record(
            HeimdallEvent(
                id = "${epochMillisNow()}-$name-${attributes.hashCode()}",
                name = name,
                screen = screen,
                attributes = attributes,
                timestampMillis = epochMillisNow(),
            ),
        )
    }

    fun <T> measure(
        name: String,
        screen: String? = currentScreen,
        block: () -> T,
    ): T {
        val startedAt = epochMillisNow()
        return try {
            block()
        } finally {
            val durationMillis = (epochMillisNow() - startedAt).coerceAtLeast(0)
            performance.record(
                PerformanceRecord(
                    name = name.take(128),
                    screen = screen?.take(128),
                    durationMillis = durationMillis,
                    timestampMillis = epochMillisNow(),
                ),
            )
            event(
                name = "Performance: $name",
                attributes = mapOf("duration_ms" to durationMillis.toString()),
                screen = screen,
            )
        }
    }

    fun recordCrash(
        throwable: Throwable,
        isFatal: Boolean = true,
        tag: String = "Heimdall",
    ) {
        if (!enabled) return
        crashes.record(
            CrashRecord(
                id = "${epochMillisNow()}-${throwable::class.simpleName ?: "Throwable"}-${throwable.hashCode()}",
                isFatal = isFatal,
                exceptionType = throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable",
                message = throwable.message,
                stackTraceText = throwable.stackTraceToString(),
                timestampMillis = epochMillisNow(),
            ),
        )
        log(level = LogLevel.ERROR, tag = tag, message = throwable.message ?: throwable::class.simpleName ?: "Crash", throwable = throwable)
    }

    fun sessions(): List<Session> = SessionRepository.listSessions()

    /** Called by a platform crash handler, on the thread that's about to die — see
     * docs/plugins/logs-crashes.md for why this can't wait for the normal write path. */
    fun markCurrentSessionCrashed() {
        SessionRepository.markCrashed(currentSessionId)
    }
}

internal expect fun epochMillisNow(): Long


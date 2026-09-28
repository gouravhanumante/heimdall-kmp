package io.heimdall.core

/**
 * Release build stand-in for `heimdall-core`: same public API, no capture, no storage, no I/O.
 * Swap in via `releaseImplementation` in place of the real artifact — see docs/release-builds.md.
 * Never depend on both at once: they share the `io.heimdall.core` package and will collide.
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
    val bubblePosition = BubblePositionStore()

    var currentSessionId: Long = 0
        private set

    var currentScreen: String? = null
        private set

    fun install(context: PlatformContext) = Unit

    fun installInMemory() = Unit

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = Unit

    fun setCurrentScreen(screen: String?) {
        currentScreen = screen
    }

    /** Still runs [block] and tracks [currentScreen] around it — only the capture is a no-op,
     * app behaviour that depends on this call must not change. */
    fun <T> screen(name: String, block: () -> T): T {
        val previous = currentScreen
        currentScreen = name
        return try {
            block()
        } finally {
            currentScreen = previous
        }
    }

    fun event(name: String, attributes: Map<String, String> = emptyMap(), screen: String? = currentScreen) = Unit

    /** Still runs and returns [block] — only the duration capture is a no-op. */
    fun <T> measure(
        name: String,
        screen: String? = currentScreen,
        block: () -> T,
    ): T = block()

    fun recordCrash(
        throwable: Throwable,
        isFatal: Boolean = true,
        tag: String = "Heimdall",
    ) = Unit

    fun sessions(): List<Session> = emptyList()

    fun markCurrentSessionCrashed() = Unit
}

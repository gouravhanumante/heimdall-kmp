package io.heimdall.core

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestampMillis: Long,
    val throwableText: String?,
)

/** A crash (uncaught exception) or a handled exception explicitly reported via
 * [CrashStore.recordNonFatal]. Kept separate from [LogStore] because crashes need to survive
 * across the process death that often follows one — persistence is not implemented yet, see
 * docs/TODO.md, so today this is in-memory only and a fatal crash's own record is lost with it. */
data class CrashRecord(
    val id: String,
    val isFatal: Boolean,
    val exceptionType: String,
    val message: String?,
    val stackTraceText: String,
    val timestampMillis: Long,
)

class LogStore internal constructor(capacity: Int = 2_000) {
    private val buffer = RingBuffer<LogEntry>(capacity)

    fun record(entry: LogEntry) {
        if (!Heimdall.enabled) return
        buffer.push(entry)
    }

    fun snapshot(): List<LogEntry> = buffer.snapshot()

    fun clear() = buffer.clear()
}

class CrashStore internal constructor(capacity: Int = 200) {
    private val buffer = RingBuffer<CrashRecord>(capacity)

    fun record(entry: CrashRecord) {
        if (!Heimdall.enabled) return
        buffer.push(entry)
    }

    fun snapshot(): List<CrashRecord> = buffer.snapshot()

    fun clear() = buffer.clear()
}

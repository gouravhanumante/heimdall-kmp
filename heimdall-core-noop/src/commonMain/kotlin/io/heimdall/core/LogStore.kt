package io.heimdall.core

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestampMillis: Long,
    val throwableText: String?,
)

data class CrashRecord(
    val id: String,
    val isFatal: Boolean,
    val exceptionType: String,
    val message: String?,
    val stackTraceText: String,
    val timestampMillis: Long,
)

class LogStore internal constructor() {
    val current: kotlinx.coroutines.flow.StateFlow<List<LogEntry>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    fun record(entry: LogEntry) = Unit
    fun forSession(sessionId: Long): List<LogEntry> = emptyList()
    fun clear() = Unit
}

class CrashStore internal constructor() {
    val current: kotlinx.coroutines.flow.StateFlow<List<CrashRecord>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    fun record(entry: CrashRecord) = Unit
    fun forSession(sessionId: Long): List<CrashRecord> = emptyList()
    fun clear() = Unit
}

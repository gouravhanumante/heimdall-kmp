package io.heimdall.core

private object AndroidPlatformHooks {
    var installed = false
    var frameMonitor: AndroidFrameMonitor? = null
}

internal actual fun installPlatformHooks(context: PlatformContext) {
    if (AndroidPlatformHooks.installed) return
    AndroidPlatformHooks.installed = true

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Heimdall.recordCrash(throwable, isFatal = true, tag = "UncaughtException")
        Heimdall.markCurrentSessionCrashed()
        defaultHandler?.uncaughtException(thread, throwable)
    }

    AndroidPlatformHooks.frameMonitor = AndroidFrameMonitor().also { it.start() }
}
package io.heimdall.core

private var frameMonitor: IosFrameMonitor? = null

internal actual fun installPlatformHooks(context: PlatformContext) {
	if (frameMonitor != null) return
	frameMonitor = IosFrameMonitor().also { it.start() }
}
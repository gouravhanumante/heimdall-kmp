package io.heimdall.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DisplayLinkTarget(private val onFrame: (Double) -> Unit) : NSObject() {
    @ObjCAction
    fun displayLinkDidFire(displayLink: CADisplayLink) {
        onFrame(displayLink.timestamp)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFrameMonitor {
    private val target = DisplayLinkTarget(::recordFrame)
    private var displayLink: CADisplayLink? = null
    private var previousTimestamp: Double? = null

    fun start() {
        if (displayLink != null) return
        previousTimestamp = null
        displayLink = CADisplayLink.displayLinkWithTarget(
            target = target,
            selector = NSSelectorFromString("displayLinkDidFire:"),
        ).also { it.addToRunLoop(NSRunLoop.mainRunLoop, NSDefaultRunLoopMode) }
    }

    fun stop() {
        displayLink?.invalidate()
        displayLink = null
        previousTimestamp = null
    }

    private fun recordFrame(timestamp: Double) {
        val previous = previousTimestamp
        if (previous != null) {
            Heimdall.performance.recordFrame(
                FrameRecord(
                    durationMillis = ((timestamp - previous) * 1000.0).toLong().coerceAtLeast(0),
                    timestampMillis = epochMillisNow(),
                ),
            )
        }
        previousTimestamp = timestamp
    }
}
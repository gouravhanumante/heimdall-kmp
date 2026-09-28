package io.heimdall.core

import android.view.Choreographer

/** Live Android frame interval monitor. It is not persisted or tagged as session history. */
class AndroidFrameMonitor {
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var previousFrameNanos: Long? = null
    private val callback: Choreographer.FrameCallback = Choreographer.FrameCallback { frameTimeNanos: Long ->
        if (!running) return@FrameCallback
        val previous = previousFrameNanos
        if (previous != null) {
            Heimdall.performance.recordFrame(
                FrameRecord(
                    durationMillis = ((frameTimeNanos - previous) / 1_000_000L).coerceAtLeast(0),
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }
        previousFrameNanos = frameTimeNanos
        choreographer.postFrameCallback(callback)
    }

    fun start() {
        if (running) return
        running = true
        previousFrameNanos = null
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        running = false
        previousFrameNanos = null
        choreographer.removeFrameCallback(callback)
    }
}
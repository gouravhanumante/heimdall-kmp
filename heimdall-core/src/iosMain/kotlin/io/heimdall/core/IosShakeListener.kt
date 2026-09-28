package io.heimdall.core

class IosShakeListener(private val onShake: () -> Unit) {
    private var active = false

    fun start() {
        active = true
    }

    fun stop() {
        active = false
    }

    internal fun motionEndedWithShake() {
        if (active) onShake()
    }
}

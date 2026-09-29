package io.heimdall.core

/** Android has a system accelerometer service to poll; iOS has no equivalent, so the host's own
 * `UIResponder.motionEnded` override must forward `UIEventSubtypeMotionShake` here — see
 * `ShakeHostViewController` in the iOS sample. */
class IosShakeListener(private val onShake: () -> Unit) {
    private var active = false

    fun start() {
        active = true
    }

    fun stop() {
        active = false
    }

    /** Call from the host's `motionEnded(_:with:)` when `motion == .motionShake`. */
    fun notifyShakeDetected() {
        if (active) onShake()
    }
}

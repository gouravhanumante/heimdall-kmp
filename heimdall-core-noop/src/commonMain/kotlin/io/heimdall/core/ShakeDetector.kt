package io.heimdall.core

/** Release build stand-in: never feeds real accelerometer samples in, never calls [onShake]. */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val accelerationThreshold: Double = DEFAULT_THRESHOLD_G,
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MS,
) {
    fun onSensorEvent(x: Double, y: Double, z: Double, nowMillis: Long) = Unit

    companion object {
        const val DEFAULT_THRESHOLD_G = 2.7
        const val DEFAULT_MIN_INTERVAL_MS = 1_000L
    }
}

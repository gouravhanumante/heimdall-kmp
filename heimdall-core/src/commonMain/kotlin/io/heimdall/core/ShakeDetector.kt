package io.heimdall.core

/**
 * Detects a device shake from raw accelerometer samples so the overlay can be recalled after
 * being dismissed. Threshold/debounce logic lives here (shared, unit-testable); each platform
 * only has to feed it accelerometer readings — see [ShakeDetector.Companion] platform factories.
 */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val accelerationThreshold: Double = DEFAULT_THRESHOLD_G,
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MS,
) {
    // Long.MIN_VALUE, not 0, so an event at nowMillis == 0 isn't mistaken for a repeat of a
    // "previous" shake that never happened.
    // null, not a sentinel timestamp: subtracting from one would risk overflow (or a 0 sentinel
    // would misfire on an event whose nowMillis is also 0 — both happened here, see the test).
    private var lastShakeAtMillis: Long? = null

    /** Feed one accelerometer sample, in G-forces per axis, with its wall-clock timestamp. */
    fun onSensorEvent(x: Double, y: Double, z: Double, nowMillis: Long) {
        val gForce = sqrt(x * x + y * y + z * z) - EARTH_GRAVITY_G
        if (gForce <= accelerationThreshold) return
        val previous = lastShakeAtMillis
        if (previous != null && nowMillis - previous < minIntervalMillis) return
        lastShakeAtMillis = nowMillis
        onShake()
    }

    companion object {
        const val DEFAULT_THRESHOLD_G = 2.7
        const val DEFAULT_MIN_INTERVAL_MS = 1_000L
        private const val EARTH_GRAVITY_G = 1.0
    }
}

private fun sqrt(value: Double): Double = kotlin.math.sqrt(value)

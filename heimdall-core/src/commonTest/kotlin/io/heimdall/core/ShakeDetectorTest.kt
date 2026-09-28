package io.heimdall.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ShakeDetectorTest {

    @Test
    fun `a single hard jolt triggers exactly one callback`() {
        var shakeCount = 0
        val detector = ShakeDetector(onShake = { shakeCount++ })

        detector.onSensorEvent(x = 5.0, y = 0.0, z = 0.0, nowMillis = 0)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `normal handling jitter below threshold does not trigger`() {
        var shakeCount = 0
        val detector = ShakeDetector(onShake = { shakeCount++ })

        // Resting on a table reads ~1g on one axis; walking/handling jitter rarely exceeds ~1.5g
        // total. Anything under the threshold must stay silent, or every phone in a pocket would
        // pop the bubble back open.
        detector.onSensorEvent(x = 1.2, y = 0.1, z = 0.1, nowMillis = 0)

        assertEquals(0, shakeCount)
    }

    @Test
    fun `two jolts within the debounce window count as one shake`() {
        var shakeCount = 0
        val detector = ShakeDetector(onShake = { shakeCount++ }, minIntervalMillis = 1_000L)

        detector.onSensorEvent(x = 5.0, y = 0.0, z = 0.0, nowMillis = 0)
        detector.onSensorEvent(x = 5.0, y = 0.0, z = 0.0, nowMillis = 200)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `a jolt after the debounce window triggers again`() {
        var shakeCount = 0
        val detector = ShakeDetector(onShake = { shakeCount++ }, minIntervalMillis = 1_000L)

        detector.onSensorEvent(x = 5.0, y = 0.0, z = 0.0, nowMillis = 0)
        detector.onSensorEvent(x = 5.0, y = 0.0, z = 0.0, nowMillis = 1_500)

        assertEquals(2, shakeCount)
    }
}

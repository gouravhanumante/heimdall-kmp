package io.heimdall.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Feeds the device's accelerometer into a [ShakeDetector]. Android-only: reads [Sensor.TYPE_ACCELEROMETER]. */
class AndroidShakeListener(
    context: Context,
    onShake: () -> Unit,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = ShakeDetector(onShake)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // SensorManager reports acceleration in m/s^2; ShakeDetector's thresholds are in G.
            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
            detector.onSensorEvent(gX.toDouble(), gY.toDouble(), gZ.toDouble(), System.currentTimeMillis())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (accelerometer == null) return
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}

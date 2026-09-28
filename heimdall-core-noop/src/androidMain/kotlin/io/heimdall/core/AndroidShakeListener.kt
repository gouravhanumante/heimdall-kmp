package io.heimdall.core

import android.content.Context

/** Release build stand-in: never reads the accelerometer, never calls [onShake]. */
class AndroidShakeListener(
    context: Context,
    onShake: () -> Unit,
) {
    fun start() = Unit
    fun stop() = Unit
}

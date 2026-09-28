package io.heimdall.core

/** Release build stand-in: never calls [onShake]. */
class IosShakeListener(onShake: () -> Unit) {
    fun start() = Unit
    fun stop() = Unit
}

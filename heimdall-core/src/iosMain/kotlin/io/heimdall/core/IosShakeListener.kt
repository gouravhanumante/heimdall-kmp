package io.heimdall.core

/**
 * iOS shake capture is deferred — see docs/TODO.md. `UIEventTypeMotion` (`motionEnded`) needs a
 * `UIWindow`/responder-chain hook, which is tangled up with the overlay-window prototype (see
 * docs/architecture.md, decision 1). Rather than half-wire this now, both land together.
 */
class IosShakeListener {
    fun start() = Unit
    fun stop() = Unit
}

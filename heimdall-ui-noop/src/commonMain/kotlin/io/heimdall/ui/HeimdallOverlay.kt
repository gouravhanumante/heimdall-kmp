package io.heimdall.ui

import androidx.compose.runtime.Composable

/**
 * Release build stand-in for `heimdall-ui`: renders [content] and nothing else. See
 * docs/release-builds.md. Swap in via `releaseImplementation` in place of the real artifact —
 * never depend on both at once, they share the `io.heimdall.ui` package.
 */
class HeimdallOverlayController {
    fun recall() = Unit

    fun handleBack(): Boolean = false

    /** There is no bubble or panel to claim a touch, so the app receives all of them. */
    fun acceptsOverlayTouch(x: Float, y: Float): Boolean = false
}

@Composable
fun HeimdallOverlay(
    controller: HeimdallOverlayController = HeimdallOverlayController(),
    content: @Composable () -> Unit,
) {
    content()
}

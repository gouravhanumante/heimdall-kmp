package io.heimdall.core

/** Where the bubble currently is, so a swipe-to-edge dismiss and shake-to-recall can agree on state. */
enum class OverlayVisibility {
    Visible,
    DismissedToEdge,
}

/** Shared, platform-agnostic state for the bubble/panel — drag position and visibility. */
class OverlayState {
    var visibility: OverlayVisibility = OverlayVisibility.Visible
        private set

    var offsetX: Float = 0f
    var offsetY: Float = 0f

    fun dismissToEdge() {
        visibility = OverlayVisibility.DismissedToEdge
    }

    fun recall() {
        visibility = OverlayVisibility.Visible
    }
}

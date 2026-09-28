package io.heimdall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Call [recall] from a platform shake listener to bring a hidden bubble back. Safe to call from
 * any thread, and any number of times. */
class HeimdallOverlayController {
    internal var bubbleVisible by mutableStateOf(true)
        private set
    internal var panelOpen by mutableStateOf(false)
        private set
    private var bubbleBounds: Rect? = null

    fun recall() {
        bubbleVisible = true
    }

    internal fun openPanel() {
        panelOpen = true
    }

    internal fun closePanel() {
        panelOpen = false
    }

    fun handleBack(): Boolean {
        if (!panelOpen) return false
        panelOpen = false
        return true
    }

    fun acceptsOverlayTouch(x: Float, y: Float): Boolean =
        panelOpen || bubbleBounds?.contains(Offset(x, y)) == true

    internal fun updateBubbleBounds(bounds: Rect) {
        bubbleBounds = bounds
    }

    internal fun hide() {
        bubbleVisible = false
    }
}

/**
 * Wrap a screen's root composable in this once. It draws the bubble and, when tapped, the panel;
 * it captures no data itself — each collector reports in separately (see docs/architecture.md).
 */
@Composable
fun HeimdallOverlay(
    controller: HeimdallOverlayController = remember { HeimdallOverlayController() },
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Always composed, even while hidden or behind the panel, so the bubble keeps its position.
        HeimdallBubbleLayer(
            visible = controller.bubbleVisible && !controller.panelOpen,
            onTap = { controller.openPanel() },
            onHide = { controller.hide() },
            onBoundsChanged = controller::updateBubbleBounds,
        )

        if (controller.panelOpen) {
            HeimdallPanel(onClose = { controller.closePanel() })
        }
    }
}

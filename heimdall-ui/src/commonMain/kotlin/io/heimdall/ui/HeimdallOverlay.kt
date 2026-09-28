package io.heimdall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Call [recall] from a platform shake listener to bring a hidden bubble back. Safe to call from
 * any thread, and any number of times. */
class HeimdallOverlayController {
    internal var bubbleVisible by mutableStateOf(true)
        private set

    fun recall() {
        bubbleVisible = true
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
    var panelOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Always composed, even while hidden or behind the panel, so the bubble keeps its position.
        HeimdallBubbleLayer(
            visible = controller.bubbleVisible && !panelOpen,
            onTap = { panelOpen = true },
            onHide = { controller.hide() },
        )

        if (panelOpen) {
            HeimdallPanel(onClose = { panelOpen = false })
        }
    }
}

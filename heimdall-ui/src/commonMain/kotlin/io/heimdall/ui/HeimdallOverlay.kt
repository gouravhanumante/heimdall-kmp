package io.heimdall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrap a screen's root composable in this once. It draws the bubble and, when tapped, the panel;
 * it captures no data itself — each collector reports in separately (see docs/architecture.md).
 * Call [HeimdallOverlayController.recall] from a platform shake listener to bring a
 * swiped-away bubble back.
 */
class HeimdallOverlayController {
    internal var recallRequested by mutableStateOf(0)
        private set

    fun recall() {
        recallRequested++
    }
}

@Composable
fun HeimdallOverlay(
    controller: HeimdallOverlayController = remember { HeimdallOverlayController() },
    content: @Composable () -> Unit,
) {
    var bubbleDismissed by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(false) }

    // Recomposes when recall() bumps the counter, regardless of what triggered dismissal.
    val recallToken = controller.recallRequested
    val bubbleVisible = !bubbleDismissed

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (bubbleVisible) {
            HeimdallBubble(
                onTap = { panelOpen = true },
                onDismissedToEdge = { bubbleDismissed = true },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }

        if (panelOpen) {
            HeimdallPanel(onClose = { panelOpen = false })
        }
    }

    // Every bump of recallToken means "bring the bubble back", including the first (harmless).
    if (recallToken > 0) {
        bubbleDismissed = false
    }
}

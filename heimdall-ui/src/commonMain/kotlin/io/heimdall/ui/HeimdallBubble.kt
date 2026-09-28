package io.heimdall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * The always-on-top draggable bubble. Dragging it past [dismissEdgeThresholdDp] of either screen
 * edge slides it fully off-screen (see [HeimdallOverlayState.dismissToEdge]); a shake brings it
 * back at its last position — the shake sensor itself is wired per-platform, not here.
 */
@Composable
fun HeimdallBubble(
    onTap: () -> Unit,
    onDismissedToEdge: () -> Unit,
    modifier: Modifier = Modifier,
    dismissEdgeThresholdDp: Float = 24f,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E1B4B))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    },
                    onDragEnd = {
                        val edgeDistancePx = dismissEdgeThresholdDp * density
                        if (offsetX <= -edgeDistancePx || offsetY <= -edgeDistancePx) {
                            onDismissedToEdge()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "H", color = Color.White)
    }
}

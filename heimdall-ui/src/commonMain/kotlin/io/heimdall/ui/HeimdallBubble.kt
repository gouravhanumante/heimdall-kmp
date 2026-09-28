package io.heimdall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val BubbleSize = 56.dp
private val HideZoneHeight = 140.dp
private val BubbleColor = Color(0xFF1E1B4B)
private val HideTargetIdle = Color(0xCC111111)
private val HideTargetActive = Color(0xFFDC2626)

/**
 * Full-screen layer that draws only the bubble; it has no pointer handling of its own, so touches
 * outside the bubble reach the app underneath. Dropping the bubble in the bottom [HideZoneHeight]
 * calls [onHide]; its position is kept across hide/show, so a recall brings it back where it was.
 */
@Composable
internal fun HeimdallBubbleLayer(
    visible: Boolean,
    onTap: () -> Unit,
    onHide: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bubblePx = with(density) { BubbleSize.toPx() }
        val maxX = (constraints.maxWidth - bubblePx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - bubblePx).coerceAtLeast(0f)
        val hideLineY = constraints.maxHeight - with(density) { HideZoneHeight.toPx() }
        val defaultPosition = Offset(maxX, maxY * 0.7f)

        // null until first dragged, so the default follows screen size changes (rotation).
        var position by remember { mutableStateOf<Offset?>(null) }
        var positionBeforeDrag by remember { mutableStateOf<Offset?>(null) }
        var dragging by remember { mutableStateOf(false) }
        val latestOnTap by rememberUpdatedState(onTap)
        val latestOnHide by rememberUpdatedState(onHide)

        fun clamp(offset: Offset) = Offset(offset.x.coerceIn(0f, maxX), offset.y.coerceIn(0f, maxY))
        fun isOverHideZone(offset: Offset) = offset.y + bubblePx / 2 > hideLineY

        val shown = clamp(position ?: defaultPosition)

        if (visible) {
            if (dragging) {
                HideTarget(
                    active = isOverHideZone(shown),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                )
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(shown.x.roundToInt(), shown.y.roundToInt()) }
                    .size(BubbleSize)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(BubbleColor)
                    .pointerInput(Unit) { detectTapGestures(onTap = { latestOnTap() }) }
                    .pointerInput(maxX, maxY, hideLineY) {
                        detectDragGestures(
                            onDragStart = {
                                positionBeforeDrag = clamp(position ?: defaultPosition)
                                dragging = true
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                position = clamp((position ?: defaultPosition) + amount)
                            },
                            onDragEnd = {
                                dragging = false
                                if (isOverHideZone(clamp(position ?: defaultPosition))) {
                                    position = positionBeforeDrag
                                    latestOnHide()
                                }
                            },
                            onDragCancel = {
                                dragging = false
                                position = positionBeforeDrag
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "H", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun HideTarget(active: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (active) 72.dp else 60.dp)
                .clip(CircleShape)
                .background(if (active) HideTargetActive else HideTargetIdle),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", color = Color.White, fontSize = 22.sp)
        }
        Text(
            text = "Drop to hide · shake to bring back",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HideTargetIdle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

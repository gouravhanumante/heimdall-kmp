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
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.heimdall.core.BubblePosition
import io.heimdall.core.Heimdall
import io.heimdall.ui.generated.resources.Res
import io.heimdall.ui.generated.resources.heimdall_horn_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val BubbleSize = 56.dp
private val BubbleEdgeInset = 8.dp
private val HideZoneHeight = 140.dp
private val BubbleColor = HeimdallDesign.primaryContainer
private val HideTargetIdle = HeimdallDesign.surface
private val HideTargetActive = HeimdallDesign.error

/**
 * Full-screen layer that draws only the bubble; it has no pointer handling of its own, so touches
 * outside the bubble reach the app underneath. Dropping the bubble in the bottom [HideZoneHeight]
 * calls [onHide]; otherwise it springs to the nearest side edge. Its position is kept across
 * hide/show and, once [Heimdall.install] has been called, across app restarts too.
 */
@Composable
internal fun HeimdallBubbleLayer(
    visible: Boolean,
    onTap: () -> Unit,
    onHide: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bubblePx = with(density) { BubbleSize.toPx() }
        val maxX = (constraints.maxWidth - bubblePx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - bubblePx).coerceAtLeast(0f)
        val hideLineY = constraints.maxHeight - with(density) { HideZoneHeight.toPx() }
        val edgeInset = with(density) { BubbleEdgeInset.toPx() }
        val defaultPosition = Offset(maxX - edgeInset, maxY * 0.7f)

        // null until first dragged (or restored from a past run), so the default follows screen
        // size changes (rotation). A restored position is converted to pixels once, at this
        // composition's current size — same limitation an in-run drag already has on rotation.
        var restPosition by remember {
            mutableStateOf(
                Heimdall.bubblePosition.current.value?.let { Offset(it.xFraction * maxX, it.yFraction * maxY) },
            )
        }
        var dragPosition by remember { mutableStateOf<Offset?>(null) }
        var positionBeforeDrag by remember { mutableStateOf<Offset?>(null) }
        var dragging by remember { mutableStateOf(false) }
        val latestOnTap by rememberUpdatedState(onTap)
        val latestOnHide by rememberUpdatedState(onHide)

        fun clamp(offset: Offset) = Offset(offset.x.coerceIn(0f, maxX), offset.y.coerceIn(0f, maxY))
        fun isOverHideZone(offset: Offset) = offset.y + bubblePx / 2 > hideLineY
        fun snapToEdge(offset: Offset): Offset {
            val x = if (offset.x + bubblePx / 2 < constraints.maxWidth / 2f) edgeInset else maxX - edgeInset
            val y = offset.y.coerceIn(edgeInset, (maxY - edgeInset).coerceAtLeast(edgeInset))
            return clamp(Offset(x, y))
        }

        val target = clamp(dragPosition ?: restPosition ?: defaultPosition)
        // snap() while dragging so the bubble tracks the finger; the spring is only for the release.
        val shown by animateOffsetAsState(
            targetValue = target,
            animationSpec = if (dragging) {
                snap()
            } else {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            },
            label = "bubblePosition",
        )

        if (visible) {
            if (dragging) {
                HideTarget(
                    active = isOverHideZone(target),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                )
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(shown.x.roundToInt(), shown.y.roundToInt()) }
                    .size(BubbleSize)
                    .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
                    .shadow(if (dragging) 16.dp else 10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(BubbleColor)
                    .pointerInput(Unit) { detectTapGestures(onTap = { latestOnTap() }) }
                    .pointerInput(maxX, maxY, hideLineY) {
                        detectDragGestures(
                            onDragStart = {
                                val start = clamp(restPosition ?: defaultPosition)
                                positionBeforeDrag = start
                                dragPosition = start
                                dragging = true
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragPosition = clamp((dragPosition ?: defaultPosition) + amount)
                            },
                            onDragEnd = {
                                val released = dragPosition ?: positionBeforeDrag ?: defaultPosition
                                dragging = false
                                dragPosition = null
                                if (isOverHideZone(released)) {
                                    restPosition = positionBeforeDrag
                                    latestOnHide()
                                } else {
                                    val snapped = snapToEdge(released)
                                    restPosition = snapped
                                    if (maxX > 0f && maxY > 0f) {
                                        Heimdall.bubblePosition.save(BubblePosition(snapped.x / maxX, snapped.y / maxY))
                                    }
                                }
                            },
                            onDragCancel = {
                                dragging = false
                                dragPosition = null
                                restPosition = positionBeforeDrag
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.heimdall_horn_logo),
                    contentDescription = "Heimdall",
                    modifier = Modifier.fillMaxSize(),
                )
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
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
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

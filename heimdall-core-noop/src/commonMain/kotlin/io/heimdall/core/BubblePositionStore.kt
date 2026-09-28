package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BubblePosition(val xFraction: Float, val yFraction: Float)

class BubblePositionStore internal constructor() {
    val current: StateFlow<BubblePosition?> = MutableStateFlow(null)
    fun save(position: BubblePosition) = Unit
}

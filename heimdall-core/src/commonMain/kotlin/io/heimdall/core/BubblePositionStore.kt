package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Where the floating bubble was last left, as a fraction of the screen (0f..1f per axis) so a
 * restore still makes sense after a rotation or on a different device. */
data class BubblePosition(val xFraction: Float, val yFraction: Float)

class BubblePositionStore internal constructor() {
    private val _current = MutableStateFlow<BubblePosition?>(null)
    val current: StateFlow<BubblePosition?> = _current

    fun save(position: BubblePosition) {
        _current.value = position
        if (!HeimdallDatabase.isOpen()) return
        HeimdallDatabase.write { conn ->
            conn.prepare(
                "INSERT OR REPLACE INTO bubble_position (id, x_fraction, y_fraction) VALUES (1, ?, ?)",
            ).use { stmt ->
                stmt.bindDouble(1, position.xFraction.toDouble())
                stmt.bindDouble(2, position.yFraction.toDouble())
                stmt.step()
            }
        }
    }

    internal fun restorePersisted() {
        _current.value = HeimdallDatabase.read { conn ->
            conn.prepare("SELECT x_fraction, y_fraction FROM bubble_position WHERE id = 1").use { stmt ->
                if (stmt.step()) BubblePosition(stmt.getDouble(0).toFloat(), stmt.getDouble(1).toFloat()) else null
            }
        }
    }
}

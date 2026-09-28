package io.heimdall.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BubblePositionStoreTest {

    @BeforeTest
    fun setUp() {
        Heimdall.installInMemory()
        HeimdallDatabase.clearAllForTests()
    }

    @Test
    fun `a saved position survives a fresh restorePersisted, as a later launch would see it`() {
        Heimdall.bubblePosition.save(BubblePosition(xFraction = 0.9f, yFraction = 0.3f))

        // Simulates the next launch reading from disk, not from the in-memory value just set.
        val store = BubblePositionStore()
        store.restorePersisted()

        assertEquals(BubblePosition(0.9f, 0.3f), store.current.value)
    }

    @Test
    fun `with nothing ever saved, a fresh restore finds nothing`() {
        val store = BubblePositionStore()
        store.restorePersisted()

        assertNull(store.current.value)
    }

    @Test
    fun `saving again replaces the previous position rather than adding a row`() {
        Heimdall.bubblePosition.save(BubblePosition(0.9f, 0.3f))
        Heimdall.bubblePosition.save(BubblePosition(0.05f, 0.6f))

        val store = BubblePositionStore()
        store.restorePersisted()

        assertEquals(BubblePosition(0.05f, 0.6f), store.current.value)
    }
}

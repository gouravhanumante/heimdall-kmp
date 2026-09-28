package io.heimdall.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeimdallNoopTest {

    @Test
    fun `measure still runs and returns the block`() {
        var ran = false
        val result = Heimdall.measure("thing") {
            ran = true
            42
        }
        assertTrue(ran)
        assertEquals(42, result)
    }

    @Test
    fun `screen still runs the block and restores the previous screen after`() {
        Heimdall.setCurrentScreen("Home")
        val seenInsideBlock = Heimdall.screen("Details") { Heimdall.currentScreen }
        assertEquals("Details", seenInsideBlock)
        assertEquals("Home", Heimdall.currentScreen)
    }

    /** The one no-op that must never just return a fixed value: an app's real feature flags are
     * read through this in production, so it must always reach the app's own provider. */
    @Test
    fun `attach always delegates to the app's own flag provider`() {
        val realCatalog = FlagCatalog { key, default ->
            if (key == "new_checkout") FlagValue.BoolValue(true) else default
        }
        val provider = Heimdall.flags.attach(object : FlagCatalog {
            override fun definitions(): List<FlagDefinition> = emptyList()
            override fun get(key: String, default: FlagValue): FlagValue = realCatalog.get(key, default)
        })

        val value = provider.get("new_checkout", FlagValue.BoolValue(false))

        assertEquals(FlagValue.BoolValue(true), value)
    }

    @Test
    fun `no store ever reports having captured anything`() {
        Heimdall.log(LogLevel.ERROR, "tag", "message")
        Heimdall.event("thing")
        Heimdall.recordCrash(RuntimeException("boom"))

        assertTrue(Heimdall.logs.current.value.isEmpty())
        assertTrue(Heimdall.events.current.value.isEmpty())
        assertTrue(Heimdall.crashes.current.value.isEmpty())
        assertTrue(Heimdall.sessions().isEmpty())
    }
}

private fun FlagCatalog(get: (String, FlagValue) -> FlagValue): FlagCatalog = object : FlagCatalog {
    override fun definitions(): List<FlagDefinition> = emptyList()
    override fun get(key: String, default: FlagValue): FlagValue = get(key, default)
}

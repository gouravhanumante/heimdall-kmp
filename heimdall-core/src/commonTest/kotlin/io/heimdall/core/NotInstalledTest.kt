package io.heimdall.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotInstalledTest {

    @AfterTest
    fun tearDown() {
        // Restore normal state for every other test that runs in this process afterward.
        Heimdall.installInMemory()
    }

    @Test
    fun `log, event, and network record calls do not throw before install, and record nothing`() {
        HeimdallDatabase.closeForTests()
        val logsBefore = Heimdall.logs.current.value.size
        val eventsBefore = Heimdall.events.current.value.size
        val networkBefore = Heimdall.network.current.value.size

        Heimdall.log(LogLevel.INFO, "tag", "message")
        Heimdall.event("thing")
        Heimdall.network.record(
            NetworkRecord(
                id = "1",
                method = "GET",
                url = "https://x",
                requestHeaders = emptyMap(),
                requestBody = null,
                statusCode = 200,
                responseHeaders = emptyMap(),
                responseBody = null,
                startedAtMillis = 0,
                durationMillis = 10,
                error = null,
            ),
        )

        // Reaching here without an exception is half the point — a forgotten install() must
        // never crash a real network call or log call made through these paths.
        assertEquals(logsBefore, Heimdall.logs.current.value.size)
        assertEquals(eventsBefore, Heimdall.events.current.value.size)
        assertEquals(networkBefore, Heimdall.network.current.value.size)
    }

    @Test
    fun `measure and screen still run their block and return normally before install`() {
        HeimdallDatabase.closeForTests()

        val measured = Heimdall.measure("thing") { 42 }
        val screened = Heimdall.screen("Details") { "ran" }

        assertEquals(42, measured)
        assertEquals("ran", screened)
    }
}

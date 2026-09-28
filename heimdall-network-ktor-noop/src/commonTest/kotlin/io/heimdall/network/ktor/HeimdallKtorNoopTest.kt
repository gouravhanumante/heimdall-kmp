package io.heimdall.network.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HeimdallKtorNoopTest {

    /** Proves the no-op plugin is a true pass-through: the app's real request/response must be
     * unaffected by it being installed. */
    @Test
    fun `installing the plugin does not change the response the app sees`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("hello", headers = headersOf(HttpHeaders.ContentType, "text/plain"))
                }
            }
            install(HeimdallKtor)
        }

        val response = client.get("https://example.com")

        assertEquals("hello", response.bodyAsText())
        assertEquals(200, response.status.value)
    }
}

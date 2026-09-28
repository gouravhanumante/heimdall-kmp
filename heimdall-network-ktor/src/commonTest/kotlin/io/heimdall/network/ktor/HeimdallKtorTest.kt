package io.heimdall.network.ktor

import io.heimdall.core.Heimdall
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeimdallKtorTest {

    @BeforeTest
    fun setUp() {
        Heimdall.installInMemory() // no-op after the first test in the process, which is fine
    }

    @AfterTest
    fun tearDown() {
        Heimdall.network.clear()
        Heimdall.enabled = true
    }

    private fun mockClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Headers = headersOf(HttpHeaders.ContentType, "application/json"),
        body: String = """{"ok":true}""",
    ) = HttpClient(MockEngine) {
        install(HeimdallKtor)
        engine {
            addHandler { respond(content = body, status = status, headers = headers) }
        }
    }

    @Test
    fun `a successful call is recorded with method, url and status`() = runTest {
        val client = mockClient()

        client.get("https://api.example.com/products") {
            header("Authorization", "Bearer secret-token")
        }

        val record = Heimdall.network.current.value.single()
        assertEquals("GET", record.method)
        assertEquals("https://api.example.com/products", record.url)
        assertEquals(200, record.statusCode)
        assertTrue(!record.isError)
    }

    @Test
    fun `the Authorization header is redacted by default`() = runTest {
        val client = mockClient()

        client.get("https://api.example.com/products") {
            header("Authorization", "Bearer secret-token")
        }

        val record = Heimdall.network.current.value.single()
        assertEquals(REDACTED_VALUE, record.requestHeaders["Authorization"])
    }

    @Test
    fun `a JSON body is captured even if the caller never reads it`() = runTest {
        val client = mockClient(body = """{"id":42}""")

        client.get("https://api.example.com/products/42") // response body never read by the caller

        val record = Heimdall.network.current.value.single()
        assertEquals("""{"id":42}""", record.responseBody)
    }

    @Test
    fun `the caller can still read the body normally after capture`() = runTest {
        val client = mockClient(body = """{"id":42}""")

        val response = client.get("https://api.example.com/products/42")

        assertEquals("""{"id":42}""", response.bodyAsText())
    }

    @Test
    fun `a binary content type is not captured as text`() = runTest {
        val client = mockClient(
            headers = headersOf(HttpHeaders.ContentType, "image/png"),
            body = "not-really-a-png",
        )

        client.get("https://api.example.com/avatar.png")

        val record = Heimdall.network.current.value.single()
        assertNull(record.responseBody)
    }

    @Test
    fun `a 500 status is flagged as an error`() = runTest {
        val client = mockClient(status = HttpStatusCode.InternalServerError)

        client.get("https://api.example.com/products")

        val record = Heimdall.network.current.value.single()
        assertTrue(record.isError)
    }

    @Test
    fun `nothing is recorded while Heimdall is disabled`() = runTest {
        Heimdall.enabled = false
        val client = mockClient()

        client.get("https://api.example.com/products")

        assertTrue(Heimdall.network.current.value.isEmpty())
    }

    @Test
    fun `a connection failure is recorded as an error, not silently dropped`() = runTest {
        val client = HttpClient(MockEngine) {
            install(HeimdallKtor)
            engine {
                addHandler { throw RuntimeException("Connection reset") }
            }
        }

        runCatching { client.get("https://api.example.com/products") }

        val record = Heimdall.network.current.value.single()
        assertNull(record.statusCode)
        assertEquals("Connection reset", record.error)
        assertTrue(record.isError)
    }
}

package io.heimdall.core

import kotlin.test.Test
import kotlin.test.assertEquals

class StringMapCodecTest {

    @Test
    fun `round-trips an empty map`() {
        assertEquals(emptyMap(), decodeStringMap(encodeStringMap(emptyMap())))
    }

    @Test
    fun `round-trips ordinary headers`() {
        val headers = mapOf("Content-Type" to "application/json", "X-Request-Id" to "abc123")
        assertEquals(headers, decodeStringMap(encodeStringMap(headers)))
    }

    @Test
    fun `round-trips a value containing the delimiter character and digits`() {
        // A naive split-on-":" or split-on-length-as-string codec would break on exactly this:
        // a value that itself looks like "digits:colon".
        val headers = mapOf("X-Weird" to "12:34:not-a-length-prefix")
        assertEquals(headers, decodeStringMap(encodeStringMap(headers)))
    }

    @Test
    fun `round-trips a value containing newlines`() {
        val headers = mapOf("X-Multiline" to "line one\nline two\nline three")
        assertEquals(headers, decodeStringMap(encodeStringMap(headers)))
    }

    @Test
    fun `round-trips an empty string value`() {
        val headers = mapOf("X-Empty" to "")
        assertEquals(headers, decodeStringMap(encodeStringMap(headers)))
    }
}

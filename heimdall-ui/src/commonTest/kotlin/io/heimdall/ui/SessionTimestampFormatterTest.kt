package io.heimdall.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTimestampFormatterTest {

    @Test
    fun `unix epoch`() {
        assertEquals("1970-01-01 00:00 UTC", formatSessionTimestamp(0L))
    }

    @Test
    fun `just before the epoch stays on the previous day, not 1969-12-32 or similar`() {
        assertEquals("1969-12-31 23:59 UTC", formatSessionTimestamp(-60_000L))
    }

    @Test
    fun `a leap day is recognised`() {
        // 2024-02-29 12:00:00 UTC
        assertEquals("2024-02-29 12:00 UTC", formatSessionTimestamp(1_709_208_000_000L))
    }

    @Test
    fun `an ordinary recent timestamp`() {
        // 2026-01-15 08:30:00 UTC
        assertEquals("2026-01-15 08:30 UTC", formatSessionTimestamp(1_768_465_800_000L))
    }
}

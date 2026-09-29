package io.heimdall.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RetentionTest {

    private val hour = 60L * 60 * 1000

    @BeforeTest
    fun setUp() {
        Heimdall.installInMemory()
        HeimdallDatabase.clearAllForTests()
    }

    private fun insertCall(sessionId: Long, id: String, startedAtMillis: Long) = HeimdallDatabase.write { conn ->
        conn.prepare(
            "INSERT INTO network_records (id, session_id, method, url, request_headers, response_headers, started_at_millis) " +
                "VALUES (?, ?, 'GET', 'https://x', '', '', ?)",
        ).use { stmt ->
            stmt.bindText(1, id)
            stmt.bindLong(2, sessionId)
            stmt.bindLong(3, startedAtMillis)
            stmt.step()
        }
    }

    private fun callIds(): List<String> = HeimdallDatabase.read { conn ->
        conn.prepare("SELECT id FROM network_records ORDER BY id").use { stmt ->
            buildList { while (stmt.step()) add(stmt.getText(0)) }
        }
    }

    @Test
    fun `on the next launch, calls older than 24 hours are gone and newer ones stay`() {
        val now = epochMillisNow()
        val yesterdaysLaunch = SessionRepository.startNewSession(now - 30 * hour)
        insertCall(yesterdaysLaunch, "30h-ago", now - 30 * hour)
        insertCall(yesterdaysLaunch, "23h-ago", now - 23 * hour)

        SessionRepository.startNewSession(now)

        assertEquals(listOf("23h-ago"), callIds())
    }

    @Test
    fun `a launch older than 24 hours stays while it still has recent data`() {
        val now = epochMillisNow()
        val longRunningLaunch = SessionRepository.startNewSession(now - 30 * hour)
        insertCall(longRunningLaunch, "23h-ago", now - 23 * hour)

        val today = SessionRepository.startNewSession(now)

        assertEquals(listOf(today, longRunningLaunch), SessionRepository.listSessions().map { it.id })
    }

    @Test
    fun `a launch whose data is all older than 24 hours disappears`() {
        val now = epochMillisNow()
        val oldLaunch = SessionRepository.startNewSession(now - 30 * hour)
        insertCall(oldLaunch, "30h-ago", now - 30 * hour)

        val today = SessionRepository.startNewSession(now)

        assertEquals(listOf(today), SessionRepository.listSessions().map { it.id })
    }

    @Test
    fun `only the most recent MAX_RETAINED_SESSIONS launches are kept, even within the retention window`() {
        val now = epochMillisNow()
        val first = SessionRepository.startNewSession(now)
        insertCall(first, "first-launch-call", now)
        val second = SessionRepository.startNewSession(now + 1)
        val third = SessionRepository.startNewSession(now + 2)
        val fourth = SessionRepository.startNewSession(now + 3)

        assertEquals(listOf(fourth, third, second), SessionRepository.listSessions().map { it.id })
        assertEquals(emptyList(), callIds())
    }
}

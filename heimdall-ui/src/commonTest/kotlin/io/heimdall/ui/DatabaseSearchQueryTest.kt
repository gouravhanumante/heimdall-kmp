package io.heimdall.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseSearchQueryTest {

    @Test
    fun `binds one placeholder per column rather than inlining the search text`() {
        val (sql, args) = buildSearchQuery("users", listOf("name", "email"), "ada")

        assertEquals(
            "SELECT * FROM \"users\" WHERE \"name\" LIKE ? ESCAPE '\\' OR \"email\" LIKE ? ESCAPE '\\' LIMIT 200",
            sql,
        )
        assertEquals(listOf("%ada%", "%ada%"), args)
    }

    /** The whole point of binding: a search string shaped like a SQL injection attempt must never
     * end up inside the SQL text itself, only as bound (inert) data. */
    @Test
    fun `a SQL-injection-shaped search string never reaches the SQL text`() {
        val payload = "'; DROP TABLE users; --"

        val (sql, args) = buildSearchQuery("users", listOf("name"), payload)

        assertFalse(sql.contains(payload))
        assertFalse(sql.contains("DROP TABLE"))
        assertTrue(args.single().contains(payload))
    }

    @Test
    fun `percent and underscore in the search text are escaped so they match literally`() {
        val (_, args) = buildSearchQuery("users", listOf("name"), "50%_off")

        assertEquals(listOf("%50\\%\\_off%"), args)
    }

    @Test
    fun `a quote in a column or table name is doubled, not left to break out of the identifier`() {
        val (sql, _) = buildSearchQuery("weird\"table", listOf("weird\"col"), "x")

        assertEquals(
            "SELECT * FROM \"weird\"\"table\" WHERE \"weird\"\"col\" LIKE ? ESCAPE '\\' LIMIT 200",
            sql,
        )
    }
}

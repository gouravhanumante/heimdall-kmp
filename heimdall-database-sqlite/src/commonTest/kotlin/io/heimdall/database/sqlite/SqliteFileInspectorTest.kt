package io.heimdall.database.sqlite

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqliteFileInspectorTest {

    private val path = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "heimdall-db-test-${Random.nextLong()}.db").toString()
    private var inspector: SqliteFileInspector? = null

    @AfterTest
    fun tearDown() {
        inspector?.close()
    }

    private fun seedDatabase() {
        val writer = BundledSQLiteDriver().open(path)
        try {
            writer.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT NOT NULL, avatar BLOB)")
            writer.execSQL("INSERT INTO users (id, name, avatar) VALUES (1, 'Ada', X'0102')")
            writer.execSQL("INSERT INTO users (id, name, avatar) VALUES (2, 'Grace', NULL)")
        } finally {
            writer.close()
        }
    }

    @Test
    fun `snapshot lists tables with rows and hides internal tables`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        val snapshot = inspector!!.snapshot()

        assertEquals(listOf("users"), snapshot.tables.map { it.name })
        val users = snapshot.tables.single()
        assertEquals(listOf("id", "name", "avatar"), users.columns)
        assertEquals(listOf("1", "Ada", "<blob 2B>"), users.rows[0])
        assertEquals(listOf("2", "Grace", null), users.rows[1])
    }

    @Test
    fun `query binds args instead of concatenating them into the SQL`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        val result = inspector!!.query("SELECT name FROM users WHERE name = ?", listOf("Ada"))

        assertEquals(listOf(listOf("Ada")), result.rows)
    }

    /** A value shaped like a SQL injection payload must be treated as inert data when bound as an
     * arg, not as SQL \u2014 the users table must still exist and be queryable afterward. */
    @Test
    fun `a bound arg shaped like a SQL injection payload is treated as literal data`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        val result = inspector!!.query(
            "SELECT name FROM users WHERE name = ?",
            listOf("x'; DROP TABLE users; --"),
        )

        assertEquals(emptyList(), result.rows)
        assertEquals(2, inspector!!.snapshot().tables.single().rows.size)
    }

    @Test
    fun `query runs a read-only statement`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        val result = inspector!!.query("SELECT name FROM users WHERE id = 1")

        assertEquals(listOf("name"), result.columns)
        assertEquals(listOf(listOf("Ada")), result.rows)
    }

    @Test
    fun `query rejects a write statement`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        assertFailsWith<IllegalArgumentException> { inspector!!.query("DELETE FROM users") }
    }

    @Test
    fun `query rejects multiple statements`() {
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        assertFailsWith<IllegalArgumentException> {
            inspector!!.query("SELECT 1; DELETE FROM users")
        }
    }

    @Test
    fun `the inspector connection cannot write even without the guard`() {
        // Proves read-only comes from the connection's open flags, not just the SQL-keyword
        // check above: a raw prepare()/step() bypassing requireReadOnlyStatement must still fail.
        seedDatabase()
        inspector = SqliteFileInspector(databaseName = "app.db", path = path)

        assertTrue(runCatching { inspector!!.query("PRAGMA user_version = 7") }.isFailure)
    }
}

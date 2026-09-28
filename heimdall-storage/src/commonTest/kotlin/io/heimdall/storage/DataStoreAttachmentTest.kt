package io.heimdall.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataStoreAttachmentTest {

    @BeforeTest
    fun setUp() {
        Heimdall.installInMemory()
    }

    private fun newDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val path = uniqueTempPath()
        return PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path })
    }

    private fun uniqueTempPath(): Path {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        return tempDir / "heimdall-test-${Random.nextLong()}.preferences_pb"
    }

    /** DataStore does real file I/O on its own internal dispatcher, not one this test controls —
     * `runTest`'s virtual clock can't be used to wait for it. Polling with a bounded real timeout
     * is the honest way to synchronize with genuinely async, uncontrolled work: if the value
     * never arrives, the test still fails, it just doesn't race to find out. */
    private suspend fun awaitSnapshotWhere(
        sourceName: String,
        condition: (StorageSnapshot) -> Boolean = { true },
    ): StorageSnapshot = withTimeout(5_000) {
        while (true) {
            val snapshot = Heimdall.storage.snapshot().firstOrNull { it.sourceName == sourceName }
            if (snapshot != null && condition(snapshot)) return@withTimeout snapshot
            delay(10)
        }
        @Suppress("UNREACHABLE_CODE") error("unreachable")
    }

    @Test
    fun `an existing value is published on attach`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val dataStore = newDataStore(scope)
        dataStore.edit { it[stringPreferencesKey("theme")] = "dark" }

        Heimdall.attachDataStore(dataStore, name = "an-existing-value-is-published", scope = scope)

        val snapshot = awaitSnapshotWhere("an-existing-value-is-published")
        assertEquals("dark", snapshot.entries["theme"])
        scope.cancel()
    }

    @Test
    fun `a later change to the DataStore is republished`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val dataStore = newDataStore(scope)
        Heimdall.attachDataStore(dataStore, name = "a-later-change-is-republished", scope = scope)
        awaitSnapshotWhere("a-later-change-is-republished")

        dataStore.edit { it[stringPreferencesKey("theme")] = "light" }

        val snapshot = awaitSnapshotWhere("a-later-change-is-republished") { it.entries["theme"] == "light" }
        assertEquals("light", snapshot.entries["theme"])
        scope.cancel()
    }

    @Test
    fun `the published writer edits the DataStore back`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val dataStore = newDataStore(scope)
        Heimdall.attachDataStore(dataStore, name = "the-writer-edits-back", scope = scope)
        awaitSnapshotWhere("the-writer-edits-back")

        val writer = Heimdall.storage.writerFor("the-writer-edits-back")
        assertNotNull(writer)
        assertTrue(writer.write("theme", "midnight"))

        val snapshot = awaitSnapshotWhere("the-writer-edits-back") { it.entries["theme"] == "midnight" }
        assertEquals("midnight", snapshot.entries["theme"])
        scope.cancel()
    }

    @Test
    fun `editing an Int key keeps it an Int, so the app's own typed read still works`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val dataStore = newDataStore(scope)
        dataStore.edit { it[intPreferencesKey("launch_count")] = 3 }
        Heimdall.attachDataStore(dataStore, name = "int-key-keeps-type", scope = scope)
        awaitSnapshotWhere("int-key-keeps-type")

        assertTrue(Heimdall.storage.writerFor("int-key-keeps-type")!!.write("launch_count", "7"))

        val snapshot = awaitSnapshotWhere("int-key-keeps-type") { it.entries["launch_count"] == "7" }
        assertEquals("7", snapshot.entries["launch_count"])
        assertEquals(7, dataStore.data.first()[intPreferencesKey("launch_count")])
        scope.cancel()
    }

    @Test
    fun `a non-number for an Int key is rejected instead of written`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val dataStore = newDataStore(scope)
        dataStore.edit { it[intPreferencesKey("launch_count")] = 3 }
        Heimdall.attachDataStore(dataStore, name = "int-key-rejects-text", scope = scope)
        awaitSnapshotWhere("int-key-rejects-text")

        assertFalse(Heimdall.storage.writerFor("int-key-rejects-text")!!.write("launch_count", "lots"))

        assertEquals(3, dataStore.data.first()[intPreferencesKey("launch_count")])
        scope.cancel()
    }
}


package io.heimdall.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Watches an existing `DataStore<Preferences>` your app already opened and publishes every
 * change into `Heimdall.storage`. Takes the instance, never opens its own: DataStore does not
 * allow two open instances on the same file, so Heimdall opening a second one would crash the
 * app — see docs/plugins/storage.md.
 *
 * [scope] should live at least as long as the DataStore itself (e.g. the same scope the app
 * created the DataStore with); Heimdall does not close it, and edits made from the panel are
 * launched on it too, since [StorageWriter.write] is not a suspend function.
 */
fun Heimdall.attachDataStore(dataStore: DataStore<Preferences>, name: String, scope: CoroutineScope) {
    dataStore.data
        .onEach { preferences ->
            val entries = preferences.asMap().entries.associate { (key, value) -> key.name to value.toString() }
            storage.publish(
                StorageSnapshot(sourceName = name, entries = entries),
                writer = StorageWriter { key, newValue ->
                    val typedWrite = typedWriteFor(preferences, key, newValue) ?: return@StorageWriter false
                    scope.launch { dataStore.edit(typedWrite) }
                    true
                },
            )
        }
        .launchIn(scope)
}

/** Keys compare by name only, so writing through the wrong key type silently replaces e.g. an
 * Int with a String and the app's own typed read then throws. Mirror the existing value's type. */
private fun typedWriteFor(
    preferences: Preferences,
    key: String,
    newValue: String,
): ((MutablePreferences) -> Unit)? = when (preferences.asMap().entries.firstOrNull { it.key.name == key }?.value) {
    is Boolean -> newValue.toBooleanStrictOrNull()?.let { v -> { it[booleanPreferencesKey(key)] = v } }
    is Int -> newValue.toIntOrNull()?.let { v -> { it[intPreferencesKey(key)] = v } }
    is Long -> newValue.toLongOrNull()?.let { v -> { it[longPreferencesKey(key)] = v } }
    is Float -> newValue.toFloatOrNull()?.let { v -> { it[floatPreferencesKey(key)] = v } }
    is Double -> newValue.toDoubleOrNull()?.let { v -> { it[doublePreferencesKey(key)] = v } }
    is String, null -> { prefs -> prefs[stringPreferencesKey(key)] = newValue }
    else -> null // Set<String>, ByteArray: no single-text-field representation
}

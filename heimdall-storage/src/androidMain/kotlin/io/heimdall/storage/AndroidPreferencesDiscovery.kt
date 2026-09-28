package io.heimdall.storage

import android.content.Context
import android.content.SharedPreferences
import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import java.io.File

/**
 * Finds every SharedPreferences file the app has already created (by listing `shared_prefs/`,
 * the only way to enumerate them — Android has no "list all preference files" API) and publishes
 * each one, live, into `Heimdall.storage`. No name has to be known up front, unlike DataStore:
 * `getSharedPreferences(name, MODE_PRIVATE)` is safe to call any number of times for the same
 * file, so there's no single-writer conflict to avoid here.
 *
 * Call once, after `Heimdall.install(context)`. Files created *after* this call are not picked
 * up — see docs/plugins/storage.md.
 */
fun Heimdall.discoverAndroidPreferences(context: Context) {
    val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
    val prefsFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: return

    for (file in prefsFiles) {
        val name = file.nameWithoutExtension
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        publishSnapshot(name, prefs)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, _ -> publishSnapshot(name, changed) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Deliberately never unregistered: this listener's lifetime is meant to match the app's
        // own, same as the SharedPreferences instance itself — see docs/plugins/storage.md.
    }
}

private fun Heimdall.publishSnapshot(name: String, prefs: SharedPreferences) {
    val entries = prefs.all.entries.associate { (key, value) -> key to value.toString() }
    storage.publish(
        StorageSnapshot(sourceName = name, entries = entries),
        writer = StorageWriter { key, newValue -> writeTypedValue(prefs, key, newValue) },
    )
}

/** SharedPreferences has no single "put" — writing a new value as the wrong type (e.g.
 * `putString` on a key the app reads with `getBoolean`) throws a `ClassCastException` for the
 * app the next time it reads that key. The panel only ever offers a text field, so this recovers
 * the original type from the existing value and parses back into it, rather than trusting the
 * type implied by whatever the panel happened to send. */
private fun writeTypedValue(prefs: SharedPreferences, key: String, newValue: String): Boolean {
    val editor = prefs.edit()
    when (val existing = prefs.all[key]) {
        is Boolean -> editor.putBoolean(key, newValue.toBooleanStrictOrNull() ?: return false)
        is Int -> editor.putInt(key, newValue.toIntOrNull() ?: return false)
        is Long -> editor.putLong(key, newValue.toLongOrNull() ?: return false)
        is Float -> editor.putFloat(key, newValue.toFloatOrNull() ?: return false)
        is String, null -> editor.putString(key, newValue)
        // Set<String> has no sensible single-text-field representation; edit unsupported.
        else -> return false
    }
    editor.apply()
    return true
}

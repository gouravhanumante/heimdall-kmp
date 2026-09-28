package io.heimdall.storage

import android.content.Context
import android.content.SharedPreferences
import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import java.io.File
import java.security.KeyStore

/** One call for everything Android can find on its own: SharedPreferences and Keystore aliases. */
fun Heimdall.discoverStorage(context: Context) {
    discoverAndroidPreferences(context)
    discoverAndroidKeystore()
}

/**
 * Finds every SharedPreferences file by listing `shared_prefs/` (Android has no API to list them)
 * and publishes each one, live. Re-scans when the Storage tab opens, so files created later are
 * picked up too.
 */
fun Heimdall.discoverAndroidPreferences(context: Context) {
    val appContext = context.applicationContext
    val attached = mutableSetOf<String>()
    // SharedPreferences holds change listeners weakly; without these strong references they get
    // garbage-collected and live updates silently stop.
    val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    fun scan() {
        synchronized(attached) {
            val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
            val prefsFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: return
            for (file in prefsFiles) {
                val name = file.nameWithoutExtension
                if (!attached.add(name)) continue
                val prefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                publishSnapshot(name, prefs)
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, _ -> publishSnapshot(name, changed) }
                listeners += listener
                prefs.registerOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    scan()
    storage.addRefresher(::scan)
}

/** Lists Android Keystore aliases, read-only. Key material in the Keystore can never be read back
 * by design, so each entry shows only the key's algorithm. */
fun Heimdall.discoverAndroidKeystore() {
    fun publish() {
        val entries = runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.aliases().toList().associateWith { alias ->
                val algorithm = runCatching { keyStore.getKey(alias, null)?.algorithm }.getOrNull()
                "${algorithm ?: "unknown"} key — material not readable"
            }
        }.getOrDefault(emptyMap())
        storage.publish(StorageSnapshot(sourceName = "Android Keystore", entries = entries))
    }

    publish()
    storage.addRefresher(::publish)
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

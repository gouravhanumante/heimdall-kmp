package io.heimdall.storage

import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

/**
 * Publishes the app's standard `NSUserDefaults` suite into `Heimdall.storage`, live. A
 * custom-named suite (`NSUserDefaults(suiteName:)`) isn't auto-discovered — iOS has no API to
 * enumerate suite names that exist on disk, the same limitation DataStore has on Android, just
 * for a different reason. See docs/TODO.md.
 */
@OptIn(ExperimentalForeignApi::class)
fun Heimdall.discoverStandardUserDefaults() {
    val defaults = NSUserDefaults.standardUserDefaults

    publishUserDefaultsSnapshot(defaults)

    NSNotificationCenter.defaultCenter.addObserverForName(
        name = NSUserDefaultsDidChangeNotification,
        `object` = defaults,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> publishUserDefaultsSnapshot(defaults) }
    // Deliberately never removed, matching the SharedPreferences listener on Android — see
    // docs/plugins/storage.md.
}

@OptIn(ExperimentalForeignApi::class)
private fun Heimdall.publishUserDefaultsSnapshot(defaults: NSUserDefaults) {
    val raw = defaults.dictionaryRepresentation()
    val entries = raw.entries.associate { (key, value) -> key.toString() to value.toString() }
    storage.publish(
        StorageSnapshot(sourceName = "NSUserDefaults.standard", entries = entries),
        // Text values only: Foundation boxes Bool/Int/Float/Double all as NSNumber, and there is
        // no reliable way to tell a stored Bool apart from a stored Int from the boxed value
        // alone — guessing wrong would silently turn a Bool the app reads with
        // `boolForKey:` into a different type. Rejecting is safer than corrupting it; see
        // docs/plugins/storage.md.
        writer = StorageWriter { key, value ->
            val isText = defaults.objectForKey(key) is platform.Foundation.NSString
            if (isText) defaults.setObject(value, forKey = key)
            isText
        },
    )
}

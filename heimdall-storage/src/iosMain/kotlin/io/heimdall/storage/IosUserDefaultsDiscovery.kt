package io.heimdall.storage

import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification
import platform.Foundation.NSUserDomainMask

/** One call for everything iOS can find on its own: UserDefaults (standard + suites) and Keychain. */
fun Heimdall.discoverStorage(appGroupSuiteNames: List<String> = emptyList()) {
    discoverUserDefaults(appGroupSuiteNames)
    discoverKeychain()
}

/**
 * Publishes the app's standard UserDefaults plus every custom suite, live. Suites are found by
 * listing `Library/Preferences/*.plist`; app-group suites live in the group container instead, so
 * pass their names in [appGroupSuiteNames]. Re-scans when the Storage tab opens.
 */
fun Heimdall.discoverUserDefaults(appGroupSuiteNames: List<String> = emptyList()) {
    val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return
    val attached = mutableSetOf<String>()

    fun attach(domain: String, defaults: NSUserDefaults, sourceName: String) {
        if (!attached.add(domain)) return
        publishDomain(domain, defaults, sourceName)
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUserDefaultsDidChangeNotification,
            `object` = defaults,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> publishDomain(domain, defaults, sourceName) }
    }

    fun scan() {
        attach(bundleId, NSUserDefaults.standardUserDefaults, "UserDefaults")
        val suites = (listPreferencesSuites() - bundleId) + appGroupSuiteNames
        for (suite in suites) {
            val defaults = NSUserDefaults(suiteName = suite)
            attach(suite, defaults, "UserDefaults: $suite")
        }
    }

    scan()
    storage.addRefresher(::scan)
}

@OptIn(ExperimentalForeignApi::class)
private fun listPreferencesSuites(): Set<String> {
    val library = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: return emptySet()
    val files = NSFileManager.defaultManager.contentsOfDirectoryAtPath("$library/Preferences", error = null)
        ?: return emptySet()
    return files.mapNotNull { (it as? String)?.takeIf { name -> name.endsWith(".plist") }?.removeSuffix(".plist") }.toSet()
}

/** Reads only [domain]'s own keys. `dictionaryRepresentation()` would also include Apple's global
 * keys (`AppleLanguages`, `NS…`), which aren't the app's data. */
private fun Heimdall.publishDomain(domain: String, defaults: NSUserDefaults, sourceName: String) {
    val raw = defaults.persistentDomainForName(domain).orEmpty()
    val entries = raw.entries.associate { (key, value) -> key.toString() to value.toString() }
    storage.publish(
        StorageSnapshot(sourceName = sourceName, entries = entries),
        // Text values only: Foundation boxes Bool and Int both as NSNumber, so the original type
        // can't be recovered, and guessing wrong would break the app's own typed read.
        writer = StorageWriter { key, value ->
            // Kotlin/Native bridges a stored NSString to kotlin.String, so check String, not NSString.
            val isText = defaults.objectForKey(key) is String
            if (isText) defaults.setObject(value, forKey = key)
            isText
        },
    )
}

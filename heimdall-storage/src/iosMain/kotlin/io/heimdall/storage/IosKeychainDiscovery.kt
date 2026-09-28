package io.heimdall.storage

import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import io.heimdall.core.StorageWriter
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRef
import platform.CoreFoundation.CFBridgingRelease
import platform.CoreFoundation.CFBridgingRetain
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private data class KeychainItem(val service: String?, val account: String?, val value: String)

/**
 * Lists the app's generic-password Keychain items (what most apps and Keychain wrappers store),
 * shown as "service / account", and lets the panel edit their values. Re-scans when the Storage
 * tab opens. Other item classes (internet passwords, keys, certificates) are not listed.
 */
@OptIn(ExperimentalForeignApi::class)
fun Heimdall.discoverKeychain() {
    fun publish() {
        val items = readGenericPasswords()
        val byLabel = items.associateBy { "${it.service ?: "—"} / ${it.account ?: "—"}" }
        storage.publish(
            StorageSnapshot(sourceName = "Keychain", entries = byLabel.mapValues { it.value.value }),
            writer = StorageWriter { label, newValue ->
                val item = byLabel[label] ?: return@StorageWriter false
                val updated = updateGenericPassword(item.service, item.account, newValue)
                if (updated) publish()
                updated
            },
        )
    }

    publish()
    storage.addRefresher(::publish)
}

@OptIn(ExperimentalForeignApi::class)
private fun readGenericPasswords(): List<KeychainItem> = memScoped {
    val query = mutableCfDictionary(
        kSecClass to kSecClassGenericPassword,
        kSecMatchLimit to kSecMatchLimitAll,
        kSecReturnAttributes to kCFBooleanTrue,
        kSecReturnData to kCFBooleanTrue,
    )
    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query, result.ptr)
    CFRelease(query)
    // errSecItemNotFound (no items yet) is the common non-success case, not an error.
    if (status != errSecSuccess) return@memScoped emptyList()

    val array: CFArrayRef = result.value?.reinterpret() ?: return@memScoped emptyList()
    val items = (0 until CFArrayGetCount(array)).mapNotNull { index ->
        val dict: CFDictionaryRef = CFArrayGetValueAtIndex(array, index)?.reinterpret() ?: return@mapNotNull null
        val data = bridge(CFDictionaryGetValue(dict, kSecValueData)) as? NSData
        KeychainItem(
            service = bridge(CFDictionaryGetValue(dict, kSecAttrService)) as? String,
            account = bridge(CFDictionaryGetValue(dict, kSecAttrAccount)) as? String,
            value = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
                ?: "«${data?.length ?: 0} bytes, not text»",
        )
    }
    CFRelease(array)
    items
}

@OptIn(ExperimentalForeignApi::class)
private fun updateGenericPassword(service: String?, account: String?, newValue: String): Boolean {
    val newData = NSString.create(string = newValue).dataUsingEncoding(NSUTF8StringEncoding) ?: return false
    // CFBridgingRetain hands back +1 references; every one is released below.
    val serviceRef = service?.let { CFBridgingRetain(it) }
    val accountRef = account?.let { CFBridgingRetain(it) }
    val dataRef = CFBridgingRetain(newData)

    val query = mutableCfDictionary(kSecClass to kSecClassGenericPassword)
    if (serviceRef != null) CFDictionaryAddValue(query, kSecAttrService, serviceRef)
    if (accountRef != null) CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
    val attributes = mutableCfDictionary(kSecValueData to dataRef)

    val status = SecItemUpdate(query, attributes)

    CFRelease(query)
    CFRelease(attributes)
    serviceRef?.let { CFRelease(it) }
    accountRef?.let { CFRelease(it) }
    CFRelease(dataRef)
    return status == errSecSuccess
}

@OptIn(ExperimentalForeignApi::class)
private fun mutableCfDictionary(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>): CFMutableDictionaryRef {
    val dict = CFDictionaryCreateMutable(null, pairs.size.toLong(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)!!
    for ((key, value) in pairs) CFDictionaryAddValue(dict, key, value)
    return dict
}

/** CF → Kotlin object. Retains first because CFBridgingRelease consumes a reference we don't own. */
@OptIn(ExperimentalForeignApi::class)
private fun bridge(value: COpaquePointer?): Any? = value?.let { CFBridgingRelease(CFRetain(it)) }

package io.heimdall.storage

import io.heimdall.core.Heimdall
import io.heimdall.core.StorageSnapshot
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.CFStringRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Security.SecCertificateCopyCommonName
import platform.Security.SecCertificateRef
import platform.Security.SecItemCopyMatching
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrLabel
import platform.Security.kSecClass
import platform.Security.kSecClassCertificate
import platform.Security.kSecClassKey
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnRef
import platform.Security.kSecValueRef

/**
 * Lists Keychain keys and certificates, read-only — no key material or full certificate data is
 * ever requested, only identifying attributes. Published as separate, non-editable sources so the
 * editable password sources (see [discoverKeychain]) keep their existing write semantics.
 */
@OptIn(ExperimentalForeignApi::class)
fun Heimdall.discoverKeychainKeysAndCertificates() {
    fun publish() {
        storage.publish(StorageSnapshot(sourceName = "Keychain: Keys", entries = readKeys()))
        storage.publish(StorageSnapshot(sourceName = "Keychain: Certificates", entries = readCertificates()))
    }

    publish()
    storage.addRefresher(::publish)
}

@OptIn(ExperimentalForeignApi::class)
private fun readKeys(): Map<String, String> = memScoped {
    val query = mutableCfDictionary(
        kSecClass to kSecClassKey,
        kSecMatchLimit to kSecMatchLimitAll,
        kSecReturnAttributes to kCFBooleanTrue,
    )
    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query, result.ptr)
    CFRelease(query)
    if (status != errSecSuccess) return@memScoped emptyMap()

    val array: CFArrayRef = result.value?.reinterpret() ?: return@memScoped emptyMap()
    val entries = (0 until CFArrayGetCount(array)).mapNotNull { index ->
        val dict: CFDictionaryRef = CFArrayGetValueAtIndex(array, index)?.reinterpret() ?: return@mapNotNull null
        val tag = bridge(CFDictionaryGetValue(dict, kSecAttrApplicationTag)) as? String
        val label = bridge(CFDictionaryGetValue(dict, kSecAttrLabel)) as? String
        val keyClass = bridge(CFDictionaryGetValue(dict, kSecAttrKeyClass))?.toString()
        val keyType = bridge(CFDictionaryGetValue(dict, kSecAttrKeyType))?.toString()
        val sizeInBits = bridge(CFDictionaryGetValue(dict, kSecAttrKeySizeInBits))?.toString()
        val name = tag ?: label ?: "key-${index + 1}"
        name to "class=${keyClass ?: "?"} type=${keyType ?: "?"} size=${sizeInBits ?: "?"}bits"
    }
    CFRelease(array)
    entries.toMap()
}

@OptIn(ExperimentalForeignApi::class)
private fun readCertificates(): Map<String, String> = memScoped {
    val query = mutableCfDictionary(
        kSecClass to kSecClassCertificate,
        kSecMatchLimit to kSecMatchLimitAll,
        kSecReturnAttributes to kCFBooleanTrue,
        kSecReturnRef to kCFBooleanTrue,
    )
    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query, result.ptr)
    CFRelease(query)
    if (status != errSecSuccess) return@memScoped emptyMap()

    val array: CFArrayRef = result.value?.reinterpret() ?: return@memScoped emptyMap()
    val entries = (0 until CFArrayGetCount(array)).mapNotNull { index ->
        val dict: CFDictionaryRef = CFArrayGetValueAtIndex(array, index)?.reinterpret() ?: return@mapNotNull null
        val label = bridge(CFDictionaryGetValue(dict, kSecAttrLabel)) as? String
        val certificate: SecCertificateRef? = CFDictionaryGetValue(dict, kSecValueRef)?.reinterpret()
        val commonName = certificate?.let(::commonNameOf)
        val name = commonName ?: label ?: "certificate-${index + 1}"
        name to (commonName ?: label ?: "Certificate")
    }
    CFRelease(array)
    entries.toMap()
}

@OptIn(ExperimentalForeignApi::class)
private fun commonNameOf(certificate: SecCertificateRef): String? = memScoped {
    val nameRef = alloc<CPointerVarOf<CPointer<CFStringRefVar>>>()
    val status = SecCertificateCopyCommonName(certificate, nameRef.ptr.reinterpret())
    if (status != errSecSuccess) return@memScoped null
    bridge(nameRef.value?.reinterpret()) as? String
}

@OptIn(ExperimentalForeignApi::class)
private fun mutableCfDictionary(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>): platform.CoreFoundation.CFMutableDictionaryRef {
    val dict = CFDictionaryCreateMutable(null, pairs.size.toLong(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)!!
    for ((key, value) in pairs) CFDictionaryAddValue(dict, key, value)
    return dict
}

@OptIn(ExperimentalForeignApi::class)
private fun bridge(value: kotlinx.cinterop.COpaquePointer?): Any? =
    value?.let { CFBridgingRelease(platform.CoreFoundation.CFRetain(it)) }

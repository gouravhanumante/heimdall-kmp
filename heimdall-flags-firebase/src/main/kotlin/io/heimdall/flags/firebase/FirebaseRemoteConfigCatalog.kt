package io.heimdall.flags.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.heimdall.core.FlagCatalog
import io.heimdall.core.FlagDefinition
import io.heimdall.core.FlagProvider
import io.heimdall.core.FlagValue

/**
 * Discovers Firebase Remote Config keys once and delegates reads back to Firebase when Heimdall
 * has no local override. Type discovery uses conservative value-shape inference; callers can pass
 * explicit definitions when a string is intentionally numeric or boolean-shaped.
 */
class FirebaseRemoteConfigCatalog(
    private val remoteConfig: FirebaseRemoteConfig,
    explicitDefinitions: List<FlagDefinition> = emptyList(),
) : FlagCatalog {
    private val explicit = explicitDefinitions.associateBy { it.key }

    override fun definitions(): List<FlagDefinition> = remoteConfig.all.keys.map { key ->
        explicit[key] ?: FlagDefinition(key, key, inferredValue(remoteConfig.all.getValue(key)))
    }

    override fun get(key: String, default: FlagValue): FlagValue = when (default) {
        is FlagValue.BoolValue -> FlagValue.BoolValue(remoteConfig.getBoolean(key))
        is FlagValue.TextValue -> FlagValue.TextValue(remoteConfig.getString(key))
        is FlagValue.NumberValue -> FlagValue.NumberValue(remoteConfig.getDouble(key))
    }

    private fun inferredValue(value: com.google.firebase.remoteconfig.FirebaseRemoteConfigValue): FlagValue {
        val text = value.asString()
        return when {
            text.equals("true", ignoreCase = true) -> FlagValue.BoolValue(true)
            text.equals("false", ignoreCase = true) -> FlagValue.BoolValue(false)
            text.toDoubleOrNull() != null -> FlagValue.NumberValue(text.toDouble())
            else -> FlagValue.TextValue(text)
        }
    }
}

fun FirebaseRemoteConfig.attachToHeimdall(
    explicitDefinitions: List<FlagDefinition> = emptyList(),
): FlagProvider = io.heimdall.core.Heimdall.flags.attach(
    FirebaseRemoteConfigCatalog(this, explicitDefinitions),
)
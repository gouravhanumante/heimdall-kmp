package io.heimdall.flags.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.heimdall.core.FlagCatalog
import io.heimdall.core.FlagDefinition
import io.heimdall.core.FlagProvider
import io.heimdall.core.FlagValue
import io.heimdall.core.Heimdall

/**
 * Release build stand-in for `heimdall-flags-firebase`. Reads through to Firebase exactly like
 * the real module — [Heimdall.flags.attach] always delegates when there's no override store to
 * consult, so the app's real flag values are unaffected by this being a no-op.
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
): FlagProvider = Heimdall.flags.attach(
    FirebaseRemoteConfigCatalog(this, explicitDefinitions),
)

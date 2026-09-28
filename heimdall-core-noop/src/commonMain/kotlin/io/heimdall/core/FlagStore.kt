package io.heimdall.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class FlagValue {
    data class BoolValue(val value: Boolean) : FlagValue()
    data class TextValue(val value: String) : FlagValue()
    data class NumberValue(val value: Double) : FlagValue()
}

data class FlagDefinition(val key: String, val label: String, val default: FlagValue)

fun interface FlagProvider {
    fun get(key: String, default: FlagValue): FlagValue
}

interface FlagCatalog : FlagProvider {
    fun definitions(): List<FlagDefinition>
}

class FlagStore internal constructor() {
    val current: StateFlow<Map<String, FlagValue>> = MutableStateFlow(emptyMap())

    var restartHandler: (() -> Unit)? = null

    fun requestRestart() = Unit

    fun register(definition: FlagDefinition) = Unit

    fun definitions(): List<FlagDefinition> = emptyList()

    /** There is no override store in a release build, so every read must still reach [catalog]
     * unchanged \u2014 the app's real flag values must never depend on this being a no-op. */
    fun attach(catalog: FlagCatalog): FlagProvider {
        catalog.definitions().forEach(::register)
        return HeimdallFlagOverride(catalog, this)
    }

    fun overrideFor(key: String): FlagValue? = null

    fun setOverride(key: String, value: FlagValue?) = Unit

    fun resetAll() = Unit
}

/** Always delegates to [delegate] \u2014 there is no override store to consult. */
class HeimdallFlagOverride(
    private val delegate: FlagProvider,
    private val store: FlagStore = Heimdall.flags,
) : FlagProvider {
    override fun get(key: String, default: FlagValue): FlagValue = delegate.get(key, default)
}

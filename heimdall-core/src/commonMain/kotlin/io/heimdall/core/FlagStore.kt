package io.heimdall.core

/** A flag/entitlement value. Kept to three primitive shapes so the panel can render a sensible
 * editor (switch / text field / number field) without needing per-app type knowledge. */
sealed class FlagValue {
    data class BoolValue(val value: Boolean) : FlagValue()
    data class TextValue(val value: String) : FlagValue()
    data class NumberValue(val value: Double) : FlagValue()
}

data class FlagDefinition(val key: String, val label: String, val default: FlagValue)

/** What your app already has: something that answers "what's the value of flag X". Heimdall
 * decorates one of these — see [HeimdallFlagOverride] — it never replaces it. */
fun interface FlagProvider {
    fun get(key: String, default: FlagValue): FlagValue
}

class FlagStore internal constructor() {
    private val definitions = mutableMapOf<String, FlagDefinition>()
    private val overrides = mutableMapOf<String, FlagValue>()

    /** Declare a flag so it shows in the panel even before it's ever been read. The panel can
     * only show flags declared this way — see docs/plugins/flags.md. */
    fun register(definition: FlagDefinition) {
        definitions[definition.key] = definition
    }

    fun definitions(): List<FlagDefinition> = definitions.values.toList()

    fun overrideFor(key: String): FlagValue? = overrides[key]

    fun setOverride(key: String, value: FlagValue?) {
        if (value == null) overrides.remove(key) else overrides[key] = value
    }

    fun resetAll() = overrides.clear()
}

/**
 * Decorates an app's existing [FlagProvider]: an override set from the panel wins, otherwise the
 * read falls through to [delegate] untouched. Overrides live only in [store] (in-memory) — no
 * persistence across process death yet, see docs/TODO.md.
 */
class HeimdallFlagOverride(
    private val delegate: FlagProvider,
    private val store: FlagStore = Heimdall.flags,
) : FlagProvider {
    override fun get(key: String, default: FlagValue): FlagValue {
        if (!Heimdall.enabled) return delegate.get(key, default)
        return store.overrideFor(key) ?: delegate.get(key, default)
    }
}

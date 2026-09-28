package io.heimdall.core

import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

/** A provider that can expose its known keys once so the panel can discover them automatically. */
interface FlagCatalog : FlagProvider {
    fun definitions(): List<FlagDefinition>
}

class FlagStore internal constructor() {
    private val _current = MutableStateFlow<Map<String, FlagValue>>(emptyMap())
    val current: StateFlow<Map<String, FlagValue>> = _current

    private val definitions = mutableMapOf<String, FlagDefinition>()
    private val overrides = mutableMapOf<String, FlagValue>()

    var restartHandler: (() -> Unit)? = null

    fun requestRestart() {
        restartHandler?.invoke()
    }

    /** Declare a flag so it shows in the panel even before it's ever been read. The panel can
     * only show flags declared this way — see docs/plugins/flags.md. */
    fun register(definition: FlagDefinition) {
        definitions[definition.key] = definition
        _current.update { overrides.toMap() }
    }

    internal fun restorePersisted() {
        val persisted = HeimdallDatabase.read { conn ->
            conn.prepare("SELECT key, value_type, value FROM flag_overrides").use { statement ->
                buildMap {
                    while (statement.step()) {
                        val key = statement.getText(0)
                        decode(statement.getText(1), statement.getText(2))?.let { put(key, it) }
                    }
                }
            }
        }
        overrides.clear()
        overrides.putAll(persisted)
        _current.value = overrides.toMap()
    }

    fun definitions(): List<FlagDefinition> = definitions.values.toList()

    /** Register a provider's catalog and return a provider whose overrides win at read time. */
    fun attach(catalog: FlagCatalog): FlagProvider {
        catalog.definitions().forEach(::register)
        return HeimdallFlagOverride(catalog, this)
    }

    fun overrideFor(key: String): FlagValue? = overrides[key]

    fun setOverride(key: String, value: FlagValue?) {
        if (value == null) overrides.remove(key) else overrides[key] = value
        if (HeimdallDatabase.isOpen()) HeimdallDatabase.write { conn ->
            if (value == null) {
                conn.prepare("DELETE FROM flag_overrides WHERE key = ?").use {
                    it.bindText(1, key)
                    it.step()
                }
            } else {
                conn.prepare(
                    "INSERT OR REPLACE INTO flag_overrides (key, value_type, value) VALUES (?, ?, ?)",
                ).use {
                    it.bindText(1, key)
                    it.bindText(2, value.typeName())
                    it.bindText(3, value.encode())
                    it.step()
                }
            }
        }
        _current.update { overrides.toMap() }
    }

    fun resetAll() {
        overrides.clear()
        if (HeimdallDatabase.isOpen()) HeimdallDatabase.write { conn -> conn.execSQL("DELETE FROM flag_overrides") }
        _current.update { emptyMap() }
    }

    private fun decode(type: String, value: String): FlagValue? = when (type) {
        "bool" -> value.toBooleanStrictOrNull()?.let(FlagValue::BoolValue)
        "text" -> FlagValue.TextValue(value)
        "number" -> value.toDoubleOrNull()?.let(FlagValue::NumberValue)
        else -> null
    }
}

private fun FlagValue.typeName(): String = when (this) {
    is FlagValue.BoolValue -> "bool"
    is FlagValue.TextValue -> "text"
    is FlagValue.NumberValue -> "number"
}

private fun FlagValue.encode(): String = when (this) {
    is FlagValue.BoolValue -> value.toString()
    is FlagValue.TextValue -> value
    is FlagValue.NumberValue -> value.toString()
}

/**
 * Decorates an app's existing [FlagProvider]: an override set from the panel wins, otherwise the
 * read falls through to [delegate] untouched. Overrides are persisted in Heimdall's own database
 * and restored by [Heimdall.install].
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

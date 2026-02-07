package net.mcbrawls.api.registry

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.random.Random

/**
 * A basic string-object registry.
 */
open class Registry<T : Any> {
    private val entries = mutableListOf<T>()
    private val keys = mutableListOf<String>()
    private val keyToEntryMap = mutableMapOf<String, T>()
    private val entryToKeyMap = mutableMapOf<T, String>()

    /**
     * The amount of registered entries in this registry.
     */
    val size: Int get() = entries.size

    /**
     * The default value of this registry.
     */
    open val defaultValue: T? = null

    /**
     * The codec for this registry.
     */
    val codec: Codec<T> = Codec.STRING.flatXmap(
        { id ->
            val entry = this[id]
            entry?.let(DataResult<T>::success) ?: DataResult.error { "Key not in registry: $id" }
        },
        { entry ->
            val id = this[entry]
            id?.let(DataResult<String>::success) ?: DataResult.error { "Object not in registry: $entry" }
        },
    )

    /**
     * Registers [entry] to the registry under [key].
     * @return the passed [entry]
     */
    fun <O : T> register(key: String, entry: O): O {
        if (keys.contains(key)) {
            throw UnsupportedOperationException("Key $key already registered")
        }

        entries.add(entry)
        keys.add(key)

        keyToEntryMap[key] = entry
        entryToKeyMap[entry] = key

        return entry
    }

    /**
     * @return the value that is assigned [key], or `null` if it is not registered
     */
    open operator fun get(key: String): T? {
        return keyToEntryMap[key]
    }

    /**
     * @return the key assigned to [entry], or `null` if it is not registered
     */
    operator fun get(entry: T): String? {
        return entryToKeyMap[entry]
    }

    /**
     * @return the value that is assigned to the index [index], or null if one is not present
     */
    operator fun get(index: Int): T? {
        return entries.getOrNull(index)
    }

    /**
     * @return the index of the entry
     */
    fun indexOf(entry: T): Int {
        return entries.indexOf(entry)
    }

    /**
     * Suggests all keys in this registry to [builder].
     */
    fun suggestKeys(builder: SuggestionsBuilder, filter: ((Map.Entry<String, T>) -> Boolean)? = null): CompletableFuture<Suggestions> {
        val filtered = if (filter != null) keyToEntryMap.filter(filter).keys else keys
        filtered.forEach(builder::suggest)
        return builder.buildFuture()
    }

    fun collectEntries(): List<T> {
        return entries.toList()
    }

    fun collectKeys(): List<String> {
        return keys.toList()
    }

    /**
     * Performs [action] on every registered entry.
     */
    fun forEach(action: Consumer<T>) {
        entries.forEach(action)
    }

    /**
     * Performs [action] on every registered entry.
     */
    fun forEachEntry(action: BiConsumer<String, T>) {
        keyToEntryMap.forEach(action)
    }

    /**
     * Filters the entries list to the given [predicate].
     * @return a new list
     */
    fun firstOrNull(predicate: (T) -> Boolean): T? {
        return entries.firstOrNull(predicate)
    }

    /**
     * Returns a random entry of the registry.
     */
    fun random(random: Random = Random, filter: ((T) -> Boolean)? = null): T? {
        return (if (filter != null) entries.filter(filter) else entries).randomOrNull(random)
    }
}

package net.mcbrawls.api.registry

/**
 * An object holdable in a registry that caches a supplied object.
 */
class RegistryEntrySupplier<T : Any>(
    /**
     * The factory to create this object.
     */
    private val entryFactory: () -> T
) {
    /**
     * The cached entry.
     */
    private var _entry: T = refreshEntry()

    /**
     * The entry of this supplier.
     */
    val entry: T get() = _entry

    /**
     * Recreates [_entry] from [entryFactory].
     * @return the new entry
     */
    fun refreshEntry(): T {
        _entry = entryFactory()
        return _entry
    }

    companion object {
        /**
         * Refreshes all entries in this registry.
         */
        fun <T : Any> BasicRegistry<RegistryEntrySupplier<T>>.refreshEntries() {
            forEach(RegistryEntrySupplier<T>::refreshEntry)
        }

        /**
         * Returns the entry inside of the supplier assigned to [key].
         * @return a registry entry
         */
        fun <T : Any> BasicRegistry<RegistryEntrySupplier<T>>.getEntry(key: String) : T? {
            return this[key]?._entry
        }
    }
}

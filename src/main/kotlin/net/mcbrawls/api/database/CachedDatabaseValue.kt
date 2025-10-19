package net.mcbrawls.api.database

import kotlinx.coroutines.runBlocking

/**
 * A database value that does not need to be fetched every time it is required.
 * This object tracks changes to the database value as changes to the database are made.
 */
class CachedDatabaseValue<T : Any?>(
    /**
     * The default value.
     */
    defaultValue: T,

    /**
     * The function to select the true value from the database.
     */
    private val selector: Selector<T>,

    /**
     * Called after initial initialization.
     */
    private val validator: ValueModifier<T>? = null,
) {
    @field:Volatile
    var initializedOnce: Boolean = false
        private set

    /**
     * The value of this instance.
     */
    @field:Volatile
    private var value: T = defaultValue

    fun get(): T {
        if (!initializedOnce) {
            runBlocking {
                refresh()

                if (validator != null) {
                    modify(validator)
                }
            }
        }

        return value
    }

    /**
     * Modifies the value locally and on the database according to the function provided.
     * @return the new local value
     */
    suspend fun modify(modifier: ValueModifier<T>): T {
        val newValue = modifier.modify { value }
        value = newValue
        return newValue
    }

    /**
     * Refreshes the current value from the database.
     */
    suspend fun refresh() {
        value = selector.select()
        initializedOnce = true
    }

    fun interface ValueModifier<T> {
        /**
         * Calculates the change to the database (for local storage) and makes the database change.
         * @return the new cached value
         */
        suspend fun modify(currentValue: () -> T): T
    }

    fun interface Selector<T> {
        /**
         * Selects the value from the database.
         * @return the deferred result
         */
        suspend fun select(): T
    }
}

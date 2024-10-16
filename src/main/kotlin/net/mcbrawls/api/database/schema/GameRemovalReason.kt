package net.mcbrawls.api.database.schema

import net.mcbrawls.api.generateEnumSqlType

/**
 * The reason for which a game instance was removed.
 */
enum class GameRemovalReason(val id: String) {
    /**
     * The game ended naturally.
     */
    ENDED("ended"),

    /**
     * The game instance was removed using a command.
     */
    COMMAND("command"),

    /**
     * The game instance threw an exception in its tick.
     */
    EXCEPTION("exception"),

    /**
     * The game instance was marked as invalid during its lifecycle.
     */
    INVALID("invalid"),

    /**
     * The game instance was inactive for too long.
     */
    INACTIVE("inactive");

    companion object {
        val BY_ID = entries.associateBy(GameRemovalReason::id)

        val sqlType: String = generateEnumSqlType(BY_ID.keys)

        fun fromId(id: Any) = BY_ID[id.toString()] ?: throw IllegalArgumentException("Unknown removal reason: $id")
    }
}

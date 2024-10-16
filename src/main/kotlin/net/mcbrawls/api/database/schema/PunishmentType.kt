package net.mcbrawls.api.database.schema;

import net.mcbrawls.api.generateEnumSqlType

/**
 * A type of offence.
 */
enum class PunishmentType(
    val id: String,

    /**
     * Whether this offence type can be timed.
     */
    val canHaveDuration: Boolean,

    /**
     * Whether this offence kicks the offender.
     */
    val kickOffender: Boolean
) {
    WARN("warn", canHaveDuration = false, kickOffender = false),
    MUTE("mute", canHaveDuration = true, kickOffender = false),
    KICK("kick", canHaveDuration = false, kickOffender = true),
    BAN("ban", canHaveDuration = true, kickOffender = true);

    companion object {
        val BY_ID = entries.associateBy(PunishmentType::id)

        val sqlType: String = generateEnumSqlType(BY_ID.keys)

        fun fromId(id: Any) = BY_ID[id.toString()] ?: throw IllegalArgumentException("Unknown punishment type: $id")
    }
}

package net.mcbrawls.api.database.schema;

import net.mcbrawls.api.generateEnumSqlType

enum class DbChatMode(val id: String) {
    LOCAL("local"),
    PARTY("party"),
    TEAM("team"),
    MESSAGE("message"),
    STAFF("staff"),
    PARTNER("partner");

    companion object {
        val BY_ID = entries.associateBy(DbChatMode::id)

        val sqlType: String = generateEnumSqlType(BY_ID.keys)

        fun fromId(id: Any) = BY_ID[id.toString()] ?: throw IllegalArgumentException("Unknown chat mode: $id")
    }
}

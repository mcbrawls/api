package net.mcbrawls.api.database.type

import net.mcbrawls.api.database.SqlHelper

enum class DbChatMode(val id: String) {
    LOCAL("local"),
    PARTY("party"),
    TOURNAMENT("tournament"),
    TEAM("team"),
    MESSAGE("message"),
    STAFF("staff"),
    PARTNER("partner");

    companion object {
        val BY_ID = entries.associateBy(DbChatMode::id)

        val sqlType: String = SqlHelper.generateEnumSqlType(BY_ID.keys)

        fun fromId(id: Any) = BY_ID[id.toString()] ?: throw IllegalArgumentException("Unknown chat mode: $id")
    }
}

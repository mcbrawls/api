package net.mcbrawls.api.database

object SqlHelper {
    fun generateEnumSqlType(ids: Collection<String>): String {
        val joined = ids.joinToString(transform = { "'$it'" })
        return "enum($joined)"
    }
}

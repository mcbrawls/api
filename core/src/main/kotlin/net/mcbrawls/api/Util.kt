package net.mcbrawls.api

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.update

fun <T : Table> T.insertOrUpdate(
    insertBody: T.(UpdateBuilder<*>) -> Unit,
    updateWhere: (() -> Op<Boolean>)? = null,
    updateLimit: Int? = null,
    block: T.(UpdateStatement) -> Unit
): Int {
    val result = insertIgnore(insertBody)
    val insertedCount = result.insertedCount
    return if (insertedCount > 0) {
        insertedCount
    } else {
        if (updateWhere != null) {
            update(where = updateWhere, limit = updateLimit, body = block)
        } else {
            update(
                limit = updateLimit,
                body = block
            )
        }
    }
}

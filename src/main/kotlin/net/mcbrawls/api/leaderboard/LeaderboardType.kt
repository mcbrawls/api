package net.mcbrawls.api.leaderboard

import net.mcbrawls.api.database.schema.StatisticEvents
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Query
import kotlin.reflect.KClass

data class LeaderboardType(
    /**
     * The title of this leaderboard.
     */
    val title: String,

    /**
     * Provides the leaderboard results.
     */
    val queryFactory: Transaction.() -> LeaderboardQueryFactory,
) {
    val id: String by lazy { LeaderboardTypes[this]!! }

    data class LeaderboardQueryFactory(
        private val query: Query,
        private val valueExpression: Expression<*>,
        val playerIdColumn: Column<String> = StatisticEvents.playerId,
    ) {
        fun createQuery(limit: Int? = null, offset: Long? = null): Query {
            query.groupBy(playerIdColumn).orderBy(valueExpression, SortOrder.DESC)

            if (limit != null) {
                query.limit(limit).offset(offset ?: 0)
            }

            return query
        }

        fun with(queryFunction: (Query) -> Query): LeaderboardQueryFactory {
            return LeaderboardQueryFactory(
                queryFunction.invoke(query),
                valueExpression
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getRowResult(row: ResultRow, clazz: KClass<T>): T? {
            return row[valueExpression] as? T
        }
    }
}

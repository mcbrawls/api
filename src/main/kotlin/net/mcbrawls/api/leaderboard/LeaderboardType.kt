package net.mcbrawls.api.leaderboard

import net.mcbrawls.api.database.schema.StatisticEvents
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import kotlin.reflect.KClass

data class LeaderboardType(
    /**
     * The title of this leaderboard.
     */
    val title: String,

    /**
     * Provides the leaderboard results.
     */
    val queryFactory: Transaction.() -> LeaderboardQueryFactory
) {
    val id: String by lazy { LeaderboardTypes[this]!! }

    data class LeaderboardQueryFactory(
        private val query: Query,
        private val valueExpression: Expression<*>
    ) {
        fun createQuery(limit: Int? = null, offset: Long? = null): Query {
            query
                .groupBy(StatisticEvents.playerId)
                .orderBy(valueExpression, SortOrder.DESC)

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

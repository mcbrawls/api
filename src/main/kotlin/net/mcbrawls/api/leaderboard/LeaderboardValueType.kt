package net.mcbrawls.api.leaderboard

import kotlinx.serialization.Serializable
import net.mcbrawls.api.database.schema.ExperienceEntries
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.leaderboard.LeaderboardType.LeaderboardQueryFactory
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select

@Serializable
enum class LeaderboardValueType(
    val leaderboardQuery: Transaction.() -> LeaderboardQueryFactory
) {
    EXPERIENCE_SUM(
        {
            val valueExpression = ExperienceEntries.experienceAmount.sum().alias("value")
            LeaderboardQueryFactory(
                ExperienceEntries
                    .innerJoin(StatisticEvents)
                    .select(
                        StatisticEvents.playerId,
                        valueExpression
                    ),
                valueExpression,
            )
        }
    ),

    EVENT_COUNT(
        {
            val valueExpression = StatisticEvents.playerId.count().alias("value")
            LeaderboardQueryFactory(
                StatisticEvents
                    .select(
                        StatisticEvents.playerId,
                        valueExpression
                    ),
                valueExpression,
            )
        }
    );

    fun query(transaction: Transaction): LeaderboardQueryFactory {
        return leaderboardQuery.invoke(transaction)
    }
}

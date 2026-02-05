package net.mcbrawls.api.database.leaderboard

import kotlinx.serialization.Serializable
import net.mcbrawls.api.database.schema.ExperienceEntries
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.database.leaderboard.LeaderboardType.LeaderboardQueryFactory
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select

@Serializable
enum class LeaderboardValueType(
    val leaderboardQuery: Transaction.() -> LeaderboardType.LeaderboardQueryFactory
) {
    EXPERIENCE_SUM(
        {
            val valueExpression = ExperienceEntries.experienceAmount.sum().alias("value")
            LeaderboardType.LeaderboardQueryFactory(
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
            LeaderboardType.LeaderboardQueryFactory(
                StatisticEvents
                    .select(
                        StatisticEvents.playerId,
                        valueExpression
                    ),
                valueExpression,
            )
        }
    );

    fun query(transaction: Transaction): LeaderboardType.LeaderboardQueryFactory {
        return leaderboardQuery.invoke(transaction)
    }
}

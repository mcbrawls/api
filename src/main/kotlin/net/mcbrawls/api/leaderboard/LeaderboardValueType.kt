package net.mcbrawls.api.leaderboard

import net.mcbrawls.api.database.schema.ExperienceEntries
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.leaderboard.LeaderboardType.LeaderboardQueryFactory
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.sum

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
                        ExperienceEntries.playerId,
                        valueExpression
                    )
                    .groupBy(ExperienceEntries.playerId),
                valueExpression,
                ExperienceEntries.playerId,
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
                    )
                    .groupBy(StatisticEvents.playerId),
                valueExpression
            )
        }
    );

    fun query(transaction: Transaction): LeaderboardQueryFactory {
        return leaderboardQuery.invoke(transaction)
    }
}

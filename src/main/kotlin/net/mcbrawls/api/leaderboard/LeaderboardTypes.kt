@file:OptIn(ExperimentalTime::class)

package net.mcbrawls.api.leaderboard

import net.mcbrawls.api.database.CaseWhenNoElse.Companion.caseNoElse
import net.mcbrawls.api.database.schema.ExperienceEntries
import net.mcbrawls.api.database.schema.MasteryQuests
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.leaderboard.LeaderboardType.LeaderboardQueryFactory
import net.mcbrawls.api.registry.BasicRegistry
import org.jetbrains.exposed.v1.core.Count
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.coalesce
import org.jetbrains.exposed.v1.core.div
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.core.times
import org.jetbrains.exposed.v1.jdbc.select
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

object LeaderboardTypes : BasicRegistry<LeaderboardType>() {
    val TOTAL_EXPERIENCE = register(
        "total_experience",
        LeaderboardType("Total Experience Leaderboard") {
            val valueExpression = ExperienceEntries.experienceAmount.sum().alias("value")
            LeaderboardQueryFactory(
                ExperienceEntries
                    .select(
                        ExperienceEntries.playerId,
                        valueExpression
                    ),
                valueExpression,
                ExperienceEntries.playerId,
            )
        }
    )

    val TOTAL_MASTERY_EXPERIENCE = register(
        "total_mastery_experience",
        LeaderboardType("Total Mastery Experience Leaderboard") {
            val valueExpression = MasteryQuests.rewardMasteryXp.sum().alias("value")
            LeaderboardQueryFactory(
                MasteryQuests
                    .select(
                        MasteryQuests.playerId,
                        valueExpression
                    ),
                valueExpression,
                MasteryQuests.playerId,
            )
        }
    )

    val DODGEBOLT_HIT_RATIO = register(
        "dodgebolt_hit_ratio",
        LeaderboardType(
            "Dodgebolt Shots Hit / Fired Ratio Leaderboard",
            createRatio(LeaderboardGameType.DODGEBOLT, "arrow_hit", "arrow_fired", 30)
        )
    )

    val DODGEBOLT_KILL_DEATH_RATIO = register(
        "dodgebolt_kill_death_ratio",
        LeaderboardType(
            "Dodgebolt Kill / Death Ratio Leaderboard",
            createRatio(LeaderboardGameType.DODGEBOLT, "kill", "death", 30)
        )
    )

    val OCTOBER_2024_GIVEAWAY = register(
        "october_2024_giveaway",
        LeaderboardType("Kills Leaderboard (7th Oct - 7th Nov) (discord.mcbrawls.net)") {
            val factory = LeaderboardValueType.EVENT_COUNT.query(this)

            val zone = ZoneOffset.UTC
            val startDate = LocalDateTime.of(2024, 10, 7, 0, 0).toInstant(zone).toKotlinInstant()
            val endDate = LocalDateTime.of(2024, 11, 7, 23, 59, 59).toInstant(zone).toKotlinInstant()

            factory.with { query ->
                query.where { (StatisticEvents.causeId eq "kill") and (StatisticEvents.timestamp.between(startDate, endDate) ) }
            }
        }
    )

    fun createStatisticsQuery(
        gameType: LeaderboardGameType?,
        causeId: String,
        valueType: LeaderboardValueType = LeaderboardValueType.EVENT_COUNT
    ): Transaction.() -> LeaderboardQueryFactory {
        return {
            val function = valueType.leaderboardQuery
            val factory = function.invoke(this)
            factory.with { query ->
                val causeCheck = StatisticEvents.causeId eq causeId
                if (gameType != null) {
                    query.where { (StatisticEvents.gameType eq gameType.id) and causeCheck }
                } else {
                    query.where { causeCheck }
                }
            }
        }
    }

    private fun createRatio(gameType: LeaderboardGameType, numeratorCauseId: String, denominatorCauseId: String, denominatorMinimum: Long): Transaction.() -> LeaderboardQueryFactory {
        return {
            val literalZero = intLiteral(0)
            val literalOne = intLiteral(1)

            val numeratorCount = Count(caseNoElse<Int>().andWhen((StatisticEvents.causeId eq numeratorCauseId), literalOne))
            val denominatorCount = Count(caseNoElse<Int>().andWhen((StatisticEvents.causeId eq denominatorCauseId), literalOne))

            val value = coalesce(numeratorCount / denominatorCount, literalZero) * 100

            val query = StatisticEvents
                .select(StatisticEvents.playerId, value)
                .where { StatisticEvents.gameType eq gameType.id }
                .having { denominatorCount greater denominatorMinimum }

            LeaderboardQueryFactory(query, value)
        }
    }
}

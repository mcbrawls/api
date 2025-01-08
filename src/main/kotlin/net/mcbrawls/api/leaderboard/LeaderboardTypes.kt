package net.mcbrawls.api.leaderboard

import kotlinx.datetime.toKotlinInstant
import net.mcbrawls.api.database.CaseWhenNoElse.Companion.caseNoElse
import net.mcbrawls.api.database.schema.ExperienceEntries
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.leaderboard.LeaderboardType.LeaderboardQueryFactory
import net.mcbrawls.api.registry.BasicRegistry
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.SqlExpressionBuilder.coalesce
import org.jetbrains.exposed.sql.SqlExpressionBuilder.div
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.sum
import java.time.LocalDateTime
import java.time.ZoneOffset

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
                valueExpression
            )
        }
    )

    val DODGEBOLT_ROUNDS_WON = register(
        "dodgebolt_rounds_won",
        LeaderboardType(
            "Dodgebolt Rounds Won Leaderboard",
            createStatisticsQuery(LeaderboardGameType.DODGEBOLT, "round_win")
        )
    )

    val DODGEBOLT_GAMES_WON = register(
        "dodgebolt_games_won",
        LeaderboardType(
            "Dodgebolt Games Won Leaderboard",
            createStatisticsQuery(LeaderboardGameType.DODGEBOLT, "win")
        )
    )

    val DODGEBOLT_KILLS = register(
        "dodgebolt_kills",
        LeaderboardType(
            "Dodgebolt Kills Leaderboard",
            createStatisticsQuery(LeaderboardGameType.DODGEBOLT, "kill")
        )
    )

    val DODGEBOLT_DEATHS = register(
        "dodgebolt_deaths",
        LeaderboardType(
            "Dodgebolt Deaths Leaderboard",
            createStatisticsQuery(LeaderboardGameType.DODGEBOLT, "death")
        )
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

    val OLD_RISE_SURVIVAL = register(
        "old_rise_survival",
        LeaderboardType(
            "Rise Survival Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE, "outlive")
        )
    )

    val OLD_RISE_ROUNDS_WON = register(
        "old_rise_rounds_won",
        LeaderboardType(
            "Rise Rounds Won Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE, "round_win")
        )
    )

    val OLD_RISE_DEATHS = register(
        "old_rise_deaths",
        LeaderboardType(
            "Rise Deaths Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE, "death")
        )
    )

    val OLD_RISE_POWDER_FLOORS = register(
        "old_rise_powder_floors",
        LeaderboardType(
            "Rise Floor Drops Survived Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE, "powder_floors")
        )
    )

    val ROCKETS_FIRED = register(
        "rockets_fired",
        LeaderboardType(
            "Rockets Fired Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "rocket_fired")
        )
    )

    val ROCKETS_HIT = register(
        "rockets_hit",
        LeaderboardType(
            "Direct Rockets Hit Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "rocket_hit")
        )
    )

    val ROCKET_SPLEEF_SURVIVAL = register(
        "rocket_spleef_survival",
        LeaderboardType(
            "Rocket Spleef Survival Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "outlive")
        )
    )

    val ROCKET_SPLEEF_KILLS = register(
        "rocket_spleef_kills",
        LeaderboardType(
            "Rocket Spleef Kills Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "kill")
        )
    )

    val ROCKET_SPLEEF_KILL_ASSISTS = register(
        "rocket_spleef_kill_assists",
        LeaderboardType(
            "Rocket Spleef Kill Assists Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "kill_assist")
        )
    )

    val ROCKET_SPLEEF_DEATHS = register(
        "rocket_spleef_deaths",
        LeaderboardType(
            "Rocket Spleef Deaths Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "death")
        )
    )

    val ROCKET_SPLEEF_PLACEMENT = register(
        "rocket_spleef_placement",
        LeaderboardType(
            "Rocket Spleef Placement Leaderboard",
            createStatisticsQuery(LeaderboardGameType.ROCKET_SPLEEF, "placement", LeaderboardValueType.EXPERIENCE_SUM)
        )
    )

    val RISE_CAPTURE_TIMES_CAPTURED = register(
        "rise_capture_times_captured",
        LeaderboardType(
            "Rise Capture Times Captured Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "capture")
        )
    )

    val RISE_CAPTURE_TIMES_COLLECTED = register(
        "rise_capture_times_collected",
        LeaderboardType(
            "Rise Capture Times Collected Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "capture_collected")
        )
    )

    val RISE_CAPTURE_TIMES_CAPTURE_LOST = register(
        "rise_capture_times_capture_lost",
        LeaderboardType(
            "Rise Capture Times Capture Lost Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "capture_lost")
        )
    )

    val RISE_CAPTURE_GAMES_WON = register(
        "rise_capture_games_won",
        LeaderboardType(
            "Rise Capture Games Won Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "win")
        )
    )

    val RISE_CAPTURE_GAMES_LOST = register(
        "rise_capture_games_lost",
        LeaderboardType(
            "Rise Capture Games Lost Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "loss")
        )
    )

    val RISE_CAPTURE_KILLS = register(
        "rise_capture_kills",
        LeaderboardType(
            "Rise Capture Kills Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "kill")
        )
    )

    val RISE_CAPTURE_FALLS = register(
        "rise_capture_falls",
        LeaderboardType(
            "Rise Capture Falls Leaderboard",
            createStatisticsQuery(LeaderboardGameType.RISE_CAPTURE, "fall")
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

    private fun createStatisticsQuery(
        gameType: LeaderboardGameType,
        causeId: String,
        valueType: LeaderboardValueType = LeaderboardValueType.EVENT_COUNT
    ): Transaction.() -> LeaderboardQueryFactory {
        return {
            val function = valueType.leaderboardQuery
            val factory = function.invoke(this)
            factory.with { query ->
                query.where { (StatisticEvents.gameType eq gameType.id) and (StatisticEvents.causeId eq causeId) }
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

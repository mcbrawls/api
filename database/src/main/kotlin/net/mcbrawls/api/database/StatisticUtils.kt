package net.mcbrawls.api.database

import net.mcbrawls.api.database.leaderboard.LeaderboardGameType
import net.mcbrawls.api.database.leaderboard.LeaderboardType
import net.mcbrawls.api.database.leaderboard.LeaderboardTypes
import net.mcbrawls.api.database.leaderboard.LeaderboardValueType
import net.mcbrawls.api.database.response.Leaderboard
import net.mcbrawls.api.database.response.LeaderboardEntry
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

object StatisticUtils {
    suspend fun createLeaderboard(
        database: Database,
        gameType: LeaderboardGameType?,
        cause: String,
        valueType: LeaderboardValueType,
        limit: Int?,
        offset: Long?
    ): Leaderboard {
        val factory = LeaderboardTypes.createStatisticsQuery(gameType, cause, valueType)
        val entries = createLeaderboardEntries(database, factory, limit, offset)

        val id = if (gameType != null) {
            "stats_${cause}_${gameType.id}"
        } else {
            "stats_$cause"
        }

        val leaderboard = Leaderboard(id, id, entries)
        return leaderboard
    }

    suspend fun createLeaderboardEntries(
        database: Database,
        factorySupplier: suspend JdbcTransaction.() -> LeaderboardType.LeaderboardQueryFactory,
        limit: Int?,
        offset: Long?
    ): List<LeaderboardEntry> {
        return suspendTransaction(database, statement = transaction@{
            buildList {
                val factory = factorySupplier.invoke(this@transaction)
                val query = factory.createQuery(limit, offset)
                query.forEachIndexed { index, row ->
                    val uuidString = row[factory.playerIdColumn]
                    val uuid = UUID.fromString(uuidString)

                    val value = factory.getRowResult(row, Number::class) ?: return@forEachIndexed
                    add(LeaderboardEntry(uuid, index + 1, value.toLong()))
                }
            }
        })
    }
}

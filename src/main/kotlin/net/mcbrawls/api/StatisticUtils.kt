package net.mcbrawls.api

import kotlinx.coroutines.Dispatchers
import net.mcbrawls.api.leaderboard.LeaderboardGameType
import net.mcbrawls.api.leaderboard.LeaderboardType
import net.mcbrawls.api.leaderboard.LeaderboardTypes
import net.mcbrawls.api.leaderboard.LeaderboardValueType
import net.mcbrawls.api.response.Leaderboard
import net.mcbrawls.api.response.LeaderboardEntry
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
        factorySupplier: Transaction.() -> LeaderboardType.LeaderboardQueryFactory,
        limit: Int?,
        offset: Long?
    ): List<LeaderboardEntry> {
        return newSuspendedTransaction(Dispatchers.IO, db = database) transaction@{
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
        }
    }
}

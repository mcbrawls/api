package net.mcbrawls.api.database.response

import kotlinx.serialization.Serializable

/**
 * A complete leaderboard.
 */
@Serializable
data class Leaderboard(
    /**
     * The id of this leaderboard.
     */
    val id: String,

    /**
     * The displayed title of this leaderboard.
     */
    val title: String,

    /**
     * The entries of this leaderboard.
     */
    val entries: List<LeaderboardEntry>,
)

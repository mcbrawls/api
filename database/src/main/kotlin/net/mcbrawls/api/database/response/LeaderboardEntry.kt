package net.mcbrawls.api.database.response

import kotlinx.serialization.Serializable
import net.mcbrawls.api.SerializableUUID

/**
 * A response for leaderboard-related queries.
 */
@Serializable
data class LeaderboardEntry(
    /**
     * The uuid of the associated player.
     */
    val uuid: SerializableUUID,

    /**
     * The position of the associated player on the leaderboard.
     */
    val position: Int,

    /**
     * The value on the leaderboard.
     */
    val value: Long
)

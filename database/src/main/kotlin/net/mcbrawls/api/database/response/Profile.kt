package net.mcbrawls.api.database.response

import kotlinx.serialization.Serializable
import net.mcbrawls.api.SerializableUUID

/**
 * A response for profile-related queries.
 */
@Serializable
data class Profile(
    /**
     * The UUID of the associated player.
     */
    val uuid: SerializableUUID,

    /**
     * The rank of the associated player.
     */
    val rank: Rank,

    /**
     * The amount of experience for the associated player.
     */
    val experience: Int
)

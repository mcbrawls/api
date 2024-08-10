package net.mcbrawls.api.response

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.api.CustomCodecs
import java.util.UUID

/**
 * A response for leaderboard-related queries.
 */
data class LeaderboardResponse(
    /**
     * The uuid of the associated player.
     */
    val uuid: UUID,

    /**
     * The position of the associated player on the leaderboard.
     */
    val position: Int,

    /**
     * The value on the leaderboard.
     */
    val value: Int
) {
    companion object {
        /**
         * The codec for this class.
         */
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                CustomCodecs.UUID.fieldOf("uuid").forGetter(LeaderboardResponse::uuid),
                Codec.INT.fieldOf("position").forGetter(LeaderboardResponse::position),
                Codec.INT.fieldOf("value").forGetter(LeaderboardResponse::value)
            ).apply(instance, ::LeaderboardResponse)
        }
    }
}

package net.mcbrawls.api.response

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.api.CustomCodecs
import net.mcbrawls.api.Rank
import java.util.UUID

/**
 * A response for profile-related queries.
 */
data class ProfileResponse(
    /**
     * The UUID of the associated player.
     */
    val playerId: UUID,

    /**
     * The rank of the associated player.
     */
    val rank: Rank,

    /**
     * The amount of experience for the associated player.
     */
    val experience: Int
) {
    companion object {
        /**
         * The codec for this class.
         */
        val CODEC = RecordCodecBuilder.create { instance ->
            instance.group(
                CustomCodecs.UUID.fieldOf("uuid").forGetter(ProfileResponse::playerId),
                Rank.CODEC.fieldOf("rank").forGetter(ProfileResponse::rank),
                Codec.INT.fieldOf("experience").forGetter(ProfileResponse::experience)
            ).apply(instance, ::ProfileResponse)
        }
    }
}

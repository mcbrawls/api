package net.mcbrawls.api.response

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.api.CustomCodecs
import java.util.UUID

/**
 * A response for total experience counts.
 */
data class TotalExperienceResponse(
    /**
     * The uuid of the associated player.
     */
    val uuid: UUID,

    /**
     * The total experience earned (lifetime).
     */
    val totalExperience: Int
) {
    companion object {
        /**
         * The codec of this class.
         */
        val CODEC: Codec<TotalExperienceResponse> = RecordCodecBuilder.create { instance ->
            instance.group(
                CustomCodecs.UUID.fieldOf("uuid").forGetter(TotalExperienceResponse::uuid),
                Codec.INT.fieldOf("totalExperience").forGetter(TotalExperienceResponse::totalExperience)
            ).apply(instance, ::TotalExperienceResponse)
        }
    }
}

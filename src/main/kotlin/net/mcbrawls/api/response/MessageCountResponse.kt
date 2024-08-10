package net.mcbrawls.api.response

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

/**
 * A response for message counts.
 */
data class MessageCountResponse(
    /**
     * The amount of unfiltered, local messages sent.
     */
    val local: Int,

    /**
     * The amount of messages filtered from any chat mode.
     */
    val filtered: Int
) {
    companion object {
        /**
         * The codec of this class.
         */
        val CODEC: Codec<MessageCountResponse> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("local").forGetter(MessageCountResponse::local),
                Codec.INT.fieldOf("filtered").forGetter(MessageCountResponse::filtered)
            ).apply(instance, ::MessageCountResponse)
        }
    }
}

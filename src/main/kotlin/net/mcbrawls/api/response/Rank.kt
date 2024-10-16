package net.mcbrawls.api.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Ranks on the MC Brawls Minecraft server.
 */
@Serializable(with = Rank.Serializer::class)
enum class Rank(private val id: String) {
    ADMIN("admin"),
    BUILDER("builder"),
    MODERATOR("moderator"),
    MCCIT("mccit"),
    PARTNER("partner"),
    STAFF("staff"),
    DEFAULT("default");

    companion object {
        val BY_ID = entries.associateBy(Rank::id)

        fun fromId(id: String): Rank = BY_ID[id] ?: throw IllegalArgumentException("Unknown rank: $id")
    }

    object Serializer : KSerializer<Rank> {
        override val descriptor = PrimitiveSerialDescriptor("Rank", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = Rank.fromId(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: Rank) = encoder.encodeString(value.id)
    }
}

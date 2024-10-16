package net.mcbrawls.api.database.schema;

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mcbrawls.api.generateEnumSqlType

@Serializable(with = PartnerStatus.Serializer::class)
enum class PartnerStatus(val id: String) {
    ACTIVE("active"),
    CANCELLED("cancelled");

    companion object {
        val BY_ID = entries.associateBy(PartnerStatus::id)

        val sqlType: String = generateEnumSqlType(BY_ID.keys)

        fun fromId(id: Any) = BY_ID[id.toString()] ?: throw IllegalArgumentException("Unknown partner status: $id")
    }

    object Serializer : KSerializer<PartnerStatus> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PartnerStatus", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: PartnerStatus) = encoder.encodeString(value.id)

        override fun deserialize(decoder: Decoder): PartnerStatus = PartnerStatus.fromId(decoder.decodeString())
    }
}

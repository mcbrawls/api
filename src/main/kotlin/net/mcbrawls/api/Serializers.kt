package net.mcbrawls.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UniqueId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = decoder.decodeString().toUUIDFromUndashed()

    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())

    private fun String.isUndashedUUID(): Boolean {
        return matches(Regex("^[a-fA-F0-9]{32}$"))
    }

    private fun String.toUUIDFromUndashed(): UUID {
        if (!isUndashedUUID()) return UUID.fromString(this)

        val dashed = "${substring(0, 8)}-${substring(8, 12)}-${substring(12, 16)}-${substring(16, 20)}-${substring(20)}"
        return UUID.fromString(dashed)
    }

    init {
        println("123e4567e89b12d3a456426655440000".toUUIDFromUndashed())
    }
}

typealias SerializableUUID = @Serializable(with = UUIDSerializer::class) UUID

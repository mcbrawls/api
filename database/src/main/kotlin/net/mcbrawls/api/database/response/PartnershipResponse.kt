package net.mcbrawls.api.database.response

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.mcbrawls.api.SerializableUUID
import net.mcbrawls.api.database.type.PartnerStatus

@Serializable
data class PartnershipResponse(
    val name: String,
    val status: PartnerStatus,
    val createdAt: LocalDate,
    val partnerProfiles: Set<Profile>,
    val discordId: String,
) {
    @Serializable
    data class Profile(
        val name: String,
        val uuid: SerializableUUID,
        val discordId: String,
    )
}

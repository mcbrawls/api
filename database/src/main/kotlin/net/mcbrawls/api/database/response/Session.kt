@file:OptIn(ExperimentalTime::class)

package net.mcbrawls.api.database.response

import kotlinx.serialization.Serializable
import net.mcbrawls.api.SerializableUUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Session(
    val uuid: SerializableUUID,
    val start: Instant,
    val end: Instant,
    val gamesPlayed: Long
)

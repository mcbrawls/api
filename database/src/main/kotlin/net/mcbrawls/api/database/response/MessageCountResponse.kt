package net.mcbrawls.api.database.response

import kotlinx.serialization.Serializable

/**
 * A response for message counts.
 */
@Serializable
data class MessageCountResponse(
    /**
     * The amount of unfiltered, local messages sent.
     */
    val local: Long,

    /**
     * The amount of messages filtered from any chat mode.
     */
    val filtered: Long
)

package net.mcbrawls.api

import com.mojang.serialization.Codec

/**
 * Ranks on the MC Brawls Minecraft server.
 */
enum class Rank {
    ADMIN,
    BUILDER,
    MODERATOR,
    MCCIT,
    PARTNER,
    STAFF,
    DEFAULT;

    companion object {
        /**
         * The codec for this class.
         */
        val CODEC = Codec.STRING.xmap({ Rank.valueOf(it.uppercase()) }, { it.toString().lowercase() })
    }
}
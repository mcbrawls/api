@file:Suppress("unused")

package net.mcbrawls.api.database.schema

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import net.mcbrawls.api.generateEnumSqlType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update

private const val PLAYER_ID_KEY = "player_id"
private const val UUID_VARCHAR_LENGTH = 36

fun <T : Table> T.insertOrUpdate(
    insertBody: T.(UpdateBuilder<*>) -> Unit,
    updateWhere: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
    updateLimit: Int? = null,
    updateBody: T.(UpdateStatement) -> Unit
): Int {
    val result = insertIgnore(insertBody)
    val insertedCount = result.insertedCount
    return if (insertedCount > 0) {
        insertedCount
    } else {
        update(
            where = updateWhere,
            limit = updateLimit,
            body = updateBody
        )
    }
}

val jsonConfig = Json {
    ignoreUnknownKeys = true
}

object ApiKeys : Table("ApiKeys") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val apiKey = varchar("api_key", 32)

    override val primaryKey = PrimaryKey(playerId, apiKey)
}

object ChatLogs : Table("ChatLogs") {
    val logId = integer("log_id").autoIncrement()
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val worldId = varchar("world_id", 255)
    val contemporaryUsername = varchar("contemporary_username", 16)
    val message = text("message")
    val recipients = json<JsonArray>("recipients", jsonConfig)
    val chatMode = customEnumeration("chat_mode", DbChatMode.sqlType, DbChatMode::fromId, DbChatMode::id)
    val chatResult = customEnumeration("chat_result", ChatResult.sqlType, ChatResult::fromId, ChatResult::id)
    val gameInstanceUuid = varchar("game_instance_uuid", UUID_VARCHAR_LENGTH).nullable()
    val partyLeaderUuid = varchar("party_leader_uuid", UUID_VARCHAR_LENGTH).nullable()
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(logId)
}

object Friends : Table("Friends") {
    val initiator = reference("initiator", Players.playerId)
    val recipient = reference("recipient", Players.playerId)
    val since = timestamp("since").defaultExpression(CurrentTimestamp)
    val acceptedAt = timestamp("accepted_at").nullable()

    override val primaryKey = PrimaryKey(initiator, recipient)
}

object GameInstances : Table("GameInstances") {
    val uuid = varchar("uuid", UUID_VARCHAR_LENGTH)
    val gameType = varchar("game_type", 100).default("unknown")
    val participants = json<JsonArray>("participants", jsonConfig)
    val additionalData = json<JsonArray>("additional_data", jsonConfig).nullable()
    val removalReason = customEnumeration("removal_reason", GameRemovalReason.sqlType, GameRemovalReason::fromId, GameRemovalReason::id)
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at")

    override val primaryKey = PrimaryKey(uuid)
}

object GameParticipants : Table("GameParticipants") {
    val playerId = reference("player_id", Players.playerId)
    val instanceUuid = reference("instance_uuid", GameInstances.uuid)

    override val primaryKey = PrimaryKey(playerId, instanceUuid)
}

object IgnoredPlayers : Table("IgnoredPlayers") {
    val receiver = reference("receiver", Players.playerId)
    val target = reference("target", Players.playerId)

    override val primaryKey = PrimaryKey(receiver, target)
}

object IpAddresses : Table("IpAddresses") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val address = varchar("address", 16)

    override val primaryKey = PrimaryKey(playerId, address)
}

object Medals : Table("Medals") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val medalId = varchar("medal_id", 100)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(playerId, medalId)
}

object Partnerships : Table("Partnerships") {
    val partnerName = text("partner_name")
    val status = customEnumeration("status", PartnerStatus.sqlType, PartnerStatus::fromId, PartnerStatus::id)
    val createdDate = date("created_date")
    val discordId = varchar("discord_id", 19)
    val partneredUsers = text("partnered_users")
    val partneredUserIds = text("partnered_user_ids")
    val partneredPlayerIds = text("partnered_player_ids")
    val notes = text("notes").nullable()
}

object PlayerCosmetics : Table("PlayerCosmetics") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val title = varchar("title", 100).nullable()

    override val primaryKey = PrimaryKey(playerId)
}

object PlayerReports : Table("PlayerReports") {
    val reportId = integer("report_id").autoIncrement()
    val reporterId = reference("reporter_id", Players.playerId).index()
    val offenderId = reference("offender_id", Players.playerId).index()
    val reason = text("reason")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val resolvedAt = timestamp("resolved_at").nullable()

    override val primaryKey = PrimaryKey(reporterId)
}

object Players : Table("Players") {
    val playerId = varchar(PLAYER_ID_KEY, UUID_VARCHAR_LENGTH)
    val firstJoin = timestamp("first_join")
    val lastJoin = timestamp("last_join").nullable()

    override val primaryKey = PrimaryKey(playerId)
}

object PlayerSettings : Table("PlayerSettings") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val settingKey = varchar("setting_key", 50)
    val settingValue = varchar("setting_value", 50)

    override val primaryKey = PrimaryKey(playerId, settingKey)
}

object Punishments : Table("Punishments") {
    val punishmentId = integer("punishment_id").autoIncrement()
    val playerId = reference(PLAYER_ID_KEY, Players.playerId)
    val contemporaryName = varchar("contemporary_name", 16)
    val officerId = varchar("officer_id", UUID_VARCHAR_LENGTH).nullable()
    val punishmentType = customEnumeration("punishment_type", PunishmentType.sqlType, PunishmentType::fromId, PunishmentType::id)
    val reason = text("reason").nullable()
    val playerMadeAwareAt = timestamp("player_made_aware_at").nullable()
    val acknowledgedAt = timestamp("acknowledged_at").nullable()
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val duration = long("duration").nullable()

    override val primaryKey = PrimaryKey(punishmentId)
}

object Purchases : Table("Purchases") {
    val transactionId = varchar("transaction_id", 40)
    val playerId = reference(PLAYER_ID_KEY, Players.playerId).index()
    val packageId = integer("package_id")
    val packageExpiry = integer("package_expiry")
    val packageName = text("package_name")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(transactionId, packageId)
}

object Sessions : Table("Sessions") {
    val playerId = reference(PLAYER_ID_KEY, Players.playerId).index()
    val start = timestamp("start")
    val end = timestamp("end")
}

object StatisticEvents : Table("StatisticEvents") {
    val eventId = integer("event_id").autoIncrement()
    val playerId = reference(PLAYER_ID_KEY, Players.playerId).index()
    val causeId = text("cause_id")
    val gameType = varchar("game_type", 100).nullable()
    val gameUuid = varchar("game_uuid", UUID_VARCHAR_LENGTH).nullable()
    val experienceAmount = integer("experience_amount")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(eventId)
}

object LuckPermsPlayers : Table("luckperms_players") {
    val uuid = varchar("uuid", UUID_VARCHAR_LENGTH)
    val username = varchar("username", 16).index()
    val primaryGroup = varchar("primary_group", 36)

    override val primaryKey = PrimaryKey(uuid)
}

package net.mcbrawls.api

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mcbrawls.api.database.BrawlsDatabaseFactory
import net.mcbrawls.api.database.CachedDatabaseValue
import net.mcbrawls.api.database.schema.ApiKeys
import net.mcbrawls.api.database.schema.ChatLogs
import net.mcbrawls.api.database.schema.ChatResult
import net.mcbrawls.api.database.schema.DbChatMode
import net.mcbrawls.api.database.schema.GameInstances
import net.mcbrawls.api.database.schema.GameParticipants
import net.mcbrawls.api.database.schema.LuckPermsPlayers
import net.mcbrawls.api.database.schema.Partnerships
import net.mcbrawls.api.database.schema.Sessions
import net.mcbrawls.api.database.schema.StatisticEvents
import net.mcbrawls.api.leaderboard.LeaderboardTypes
import net.mcbrawls.api.response.Leaderboard
import net.mcbrawls.api.response.LeaderboardEntry
import net.mcbrawls.api.response.MessageCountResponse
import net.mcbrawls.api.response.PartnershipResponse
import net.mcbrawls.api.response.Profile
import net.mcbrawls.api.response.Rank
import net.mcbrawls.api.response.Session
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * TODO cache fetched data ([CachedDatabaseValue])
 */
fun main(args: Array<String>) {
    val logger: Logger = LoggerFactory.getLogger("Main")

    logger.info("Starting server")

    val port = args.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException("Port not provided (args[0])")
    val databaseSchema = args.getOrNull(1) ?: throw IllegalArgumentException("Database schema not provided (args[1])")
    val permissionsDatabaseSchema = args.getOrNull(2) ?: throw IllegalArgumentException("Permissions database schema not provided (args[2])")

    val database = BrawlsDatabaseFactory.createDatabase(databaseSchema)
    val permissionsDatabase = BrawlsDatabaseFactory.createDatabase(permissionsDatabaseSchema)

    embeddedServer(Netty, port) {
        install(Authentication) {
            // authentication
            basic("auth-basic") {
                realm = "Access to the '/' path"
                validate { credentials ->
                    val id = credentials.name
                    val apiKey = credentials.password

                    val exists = transaction(database) {
                        ApiKeys.select(
                            (ApiKeys.playerId eq id) and (ApiKeys.apiKey eq apiKey)
                        ).any()
                    }

                    if (exists) {
                        UserIdPrincipal(id)
                    } else {
                        null
                    }
                }
            }
        }

        install(SwaggerUI) {
            swagger {
                withCredentials = true
                onlineSpecValidator()
            }

            info {
                title = "MC Brawls API"
                description = "Official API for MC Brawls."
            }

            server {
                url = "https://api.mcbrawls.net"
                description = "MC Brawls API"
            }

            security {
                securityScheme("BasicAuth") {
                    type = AuthType.HTTP
                    scheme = AuthScheme.BASIC
                }

                defaultSecuritySchemeNames("BasicAuth")
                defaultUnauthorizedResponse {
                    description = "Invalid API credentials"
                }
            }
        }

        // routes
        routing {
            route("/v2") {
                route("api.json") {
                    openApiSpec()
                }

                route("docs") {
                    swaggerUI("/v2/api.json")
                }

                get("") {
                    call.respond(
                        HttpStatusCode.OK,
                        "MC Brawls API https://api.mcbrawls.net - Docs: https://api.mcbrawls.net/docs"
                    )
                }

                authenticate("auth-basic") {
                    get("/chat_statistics", {
                        description = "Gets chat statistics for all players who have played on MC Brawls."

                        response {
                            HttpStatusCode.OK to {
                                description = "Successful request."
                                body<List<MessageCountResponse>>()
                            }
                        }
                    }) {
                        val (localMessageCount, filteredMessageCount) = transaction(database) {
                            val localExpression =
                                (ChatLogs.chatMode eq DbChatMode.LOCAL) and (ChatLogs.chatResult eq ChatResult.SUCCESS)
                            val filteredExpression = ChatLogs.chatResult eq ChatResult.FILTERED_PROFANITY

                            val localMessageCount = ChatLogs.select(ChatLogs.logId).where(localExpression).count()
                            val filteredMessageCount = ChatLogs.select(ChatLogs.logId).where(filteredExpression).count()

                            localMessageCount to filteredMessageCount
                        }

                        val response = MessageCountResponse(localMessageCount, filteredMessageCount)

                        call.respondJson(Json.encodeToString(response))
                    }

                    get("/chat_statistics/{uuid}", {
                        description = "Gets chat statistics for the specified player UUID."

                        request {
                            pathParameter<String>("uuid")
                        }

                        objectResponse<MessageCountResponse>()
                    }) {
                        val uuidString = call.parameters["uuid"]

                        if (uuidString == null) {
                            call.respond(HttpStatusCode.BadRequest, "Not a valid uuid: null")
                            return@get
                        }

                        try {
                            UUID.fromString(uuidString)
                        } catch (exception: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, "Not a valid uuid: $uuidString")
                            return@get
                        }

                        val (localMessageCount, filteredMessageCount) = transaction(database) {
                            val playerIdCheck = ChatLogs.playerId eq uuidString
                            val localExpression =
                                (ChatLogs.chatMode eq DbChatMode.LOCAL) and (ChatLogs.chatResult eq ChatResult.SUCCESS)
                            val filteredExpression = ChatLogs.chatResult eq ChatResult.FILTERED_PROFANITY

                            val localMessageCount =
                                ChatLogs.select(ChatLogs.logId).where(playerIdCheck and localExpression).count()
                            val filteredMessageCount =
                                ChatLogs.select(ChatLogs.logId).where(playerIdCheck and filteredExpression).count()

                            localMessageCount to filteredMessageCount
                        }

                        val response = MessageCountResponse(localMessageCount, filteredMessageCount)

                        call.respondJson(Json.encodeToString(response))
                    }

                    get("/leaderboards", {
                        description = "Retrieves all leaderboard ids."

                        objectResponse<List<String>>()
                    }) {
                        val leaderboardIds = LeaderboardTypes.collectKeys()
                        call.respondJson(Json.encodeToString(leaderboardIds))
                    }

                    get("/leaderboards/{board}", {
                        description = "Retrieves the leaderboard for the given leaderboard type."

                        request {
                            pathParameter<String>("board") {
                                description = "The type of leaderboard."
                            }

                            pathParameter<String>("limit") {
                                description = "The maximum amount of entries to display."
                            }

                            pathParameter<String>("offset") {
                                description = "The offset index for the entries to display from. Requires limit."
                            }
                        }

                        objectResponse<List<Leaderboard>>()
                    }) {
                        val limit = call.parameters["limit"]?.toIntOrNull()
                        val offset = call.parameters["offset"]?.toLongOrNull()

                        val boardType = runCatching {
                            val boardId = call.parameters["board"]!!
                            LeaderboardTypes[boardId]!!
                        }.getOrElse {
                            call.respond(HttpStatusCode.BadRequest, "Not a valid leaderboard name")
                            return@get
                        }

                        val entries = transaction(database) {
                            val transaction = this
                            buildList {
                                val factory = boardType.queryFactory.invoke(transaction)
                                val query = factory.createQuery(limit, offset)
                                query.forEachIndexed { index, row ->
                                    val uuid = runCatching {
                                        val uuidString = row[StatisticEvents.playerId]
                                        UUID.fromString(uuidString)
                                    }.getOrElse {
                                        return@forEachIndexed
                                    }

                                    val value = factory.getRowResult(row, Number::class) ?: return@forEachIndexed
                                    add(LeaderboardEntry(uuid, index + 1, value.toLong()))
                                }
                            }
                        }

                        val leaderboard = Leaderboard(boardType.id, boardType.title, entries)
                        call.respondJson(Json.encodeToString(leaderboard))
                    }

                    get("ranks", {
                        description = "Gets all ranks."

                        objectResponse<Set<Rank>>()
                    }) {
                        val json = Json.encodeToString(Rank.entries.toSet())
                        call.respondJson(json)
                    }

                    get("/profile/{uuid}", {
                        description = "Gets some basic info about the players profile."
                        request {
                            pathParameter<String>("uuid")
                        }

                        objectResponse<Profile>()
                    }) {
                        val uuidParameter = call.parameters["uuid"]
                        val (uuidString, uuid) = runCatching {
                            val uuidString = uuidParameter!!
                            uuidString to UUID.fromString(uuidString)
                        }.getOrElse {
                            call.respond(HttpStatusCode.BadRequest, "Not a valid uuid")
                            return@get
                        }

                        val rank = transaction(permissionsDatabase) {
                            val groupResult = LuckPermsPlayers
                                .select(LuckPermsPlayers.primaryGroup)
                                .where { LuckPermsPlayers.uuid eq uuidString }
                                .singleOrNull()?.getOrNull(LuckPermsPlayers.primaryGroup)

                            if (groupResult == null) {
                                null
                            } else {
                                Rank.fromId(groupResult)
                            }
                        } ?: Rank.DEFAULT

                        val experience = transaction(database) {
                            val sum = StatisticEvents.experienceAmount.sum()
                            StatisticEvents
                                .select(sum)
                                .where { StatisticEvents.playerId eq uuidString }
                                .singleOrNull()?.getOrNull(sum)
                        } ?: 0

                        val response = Profile(uuid, rank, experience)
                        val json = Json.encodeToString(response)

                        call.respondJson(json)
                    }

                    get("sessions", {
                        description = "Gets session info."
                        objectResponse<List<Session>>()
                    }) {
                        val sessionInformation = transaction(database) {
                            val gameInstanceCount = GameInstances.uuid.count().alias("games_played")
                            Sessions
                                .leftJoin(
                                    GameParticipants,
                                    additionalConstraint = { GameParticipants.playerId eq Sessions.playerId }
                                )
                                .leftJoin(
                                    GameInstances,
                                    additionalConstraint = {
                                        val on = GameInstances.uuid eq GameParticipants.instanceUuid
                                        val sessionCheck = GameInstances.startedAt.greaterEq(Sessions.start) and GameInstances.endedAt.lessEq(Sessions.end)
                                        on and sessionCheck
                                    }
                                )

                                .select(Sessions.playerId, Sessions.start, Sessions.end, gameInstanceCount)

                                .groupBy(Sessions.playerId, Sessions.start, Sessions.end)
                                .orderBy(Sessions.end, SortOrder.DESC)

                                .map { row ->
                                    val playerId = row[Sessions.playerId]
                                    val uuid = UUID.fromString(playerId)

                                    val start = row[Sessions.start]
                                    val end = row[Sessions.end]
                                    val count = row[gameInstanceCount]

                                    Session(uuid, start, end, count)
                                }
                        }

                        val json = Json.encodeToString(sessionInformation)

                        call.respondJson(json)
                    }

                    get("partnerships", {
                        description = "Gets all partnerships."
                        objectResponse<Set<PartnershipResponse>>()
                    }) {
                        val partnerships = transaction(database) {
                            Partnerships.selectAll()
                                .mapNotNull { row ->
                                    runCatching {
                                        val name = row[Partnerships.partnerName]
                                        val status = row[Partnerships.status]
                                        val createdDate = row[Partnerships.createdDate]
                                        val discordId = row[Partnerships.discordId]

                                        val partneredNames = row[Partnerships.partneredUsers].split(",")
                                        val partneredUuids =
                                            row[Partnerships.partneredPlayerIds].split(",").mapNotNull { uuidString ->
                                                runCatching {
                                                    UUID.fromString(uuidString)
                                                }.getOrNull()
                                            }
                                        val partneredDiscordIds = row[Partnerships.partneredUserIds].split(",")

                                        val partnerProfiles = partneredNames.mapIndexed { index, partnerName ->
                                            val uuid = partneredUuids[index]
                                            val discordUserId = partneredDiscordIds[index]
                                            PartnershipResponse.Profile(partnerName, uuid, discordUserId)
                                        }.toSet()

                                        PartnershipResponse(name, status, createdDate, partnerProfiles, discordId)
                                    }.getOrNull()
                                }
                        }

                        val json = Json.encodeToString(partnerships)
                        call.respondJson(json)
                    }
                }

                route("{...}") {
                    handle {
                        call.respond(HttpStatusCode.NotFound, "Requested route was not found. See docs at /v2/docs.")
                    }
                }
            }

            route("{...}") {
                handle {
                    call.respond(HttpStatusCode.NotFound, "Page not found. Should you be using /v2?")
                }
            }
        }
    }.start(wait = true)
}

suspend fun ApplicationCall.respondJson(json: String) {
    response.headers.append(HttpHeaders.ContentType, "application/json")
    respondText(json)
}

inline fun <reified T> OpenApiRoute.objectResponse() {
    response {
        HttpStatusCode.OK to {
            description = "Successful request."
            body<T>()
        }

        HttpStatusCode.InternalServerError to {
            description = "An unexpected error happened on the server."
        }
    }
}

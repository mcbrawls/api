package net.mcbrawls.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import dev.andante.codex.encodeQuick
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.mcbrawls.api.database.CachedDatabaseValue
import net.mcbrawls.api.database.DatabaseController
import net.mcbrawls.api.database.PermissionDatabaseController
import net.mcbrawls.api.database.PreparedStatementBuilder
import net.mcbrawls.api.leaderboard.LeaderboardTypes
import net.mcbrawls.api.response.LeaderboardResponse
import net.mcbrawls.api.response.MessageCountResponse
import net.mcbrawls.api.response.ProfileResponse
import net.mcbrawls.api.response.TotalExperienceResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.sql.PreparedStatement
import java.util.UUID

private val logger: Logger = LoggerFactory.getLogger("Main")

/**
 * TODO cache fetched data ([CachedDatabaseValue])
 */
fun main() {
    logger.info("Connecting to database.")
    runBlocking { DatabaseController.connect() }
    runBlocking { PermissionDatabaseController.connect() }
    logger.info("Connected.")

    logger.info("Starting server")
    embeddedServer(Netty, port = 37612) {
        install(Authentication) {
            // authentication
            basic("auth-basic") {
                realm = "Access to the '/' path"
                validate { credentials ->
                    val id = credentials.name
                    val apiKey = credentials.password
                    val result = DatabaseController.executePrepared(
                        { prepareStatement("SELECT * FROM ApiKeys WHERE player_id = ? AND api_key = ?") },
                        { builder ->
                            builder.addNext(id, PreparedStatement::setString)
                            builder.addNext(apiKey, PreparedStatement::setString)
                        },
                        PreparedStatement::executeQuery
                    )

                    if (result.next()) {
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
                version = this::class.java.`package`.implementationVersion // somehow this works
                description = "Official API for the MC Brawls Minecraft server."
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
            route("api.json") {
                openApiSpec()
            }

            route("docs") {
                swaggerUI("/api.json")
            }

            get("/") {
                call.respond(HttpStatusCode.OK, "MC Brawls API https://api.mcbrawls.net - Docs: https://api.mcbrawls.net/docs")
            }

            authenticate("auth-basic") {
                get("/experience", {
                    description = "Gets the experience of every player who has played on MC Brawls."

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<List<TotalExperienceResponse>>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    // execute query
                    val result = DatabaseController.executeStatement {
                        executeQuery(
                            """
                                SELECT player_id, SUM(experience_amount) AS total_experience
                                FROM StatisticEvents
                                GROUP BY player_id
                                ORDER BY total_experience DESC
                            """.trimIndent()
                        )
                    }
                    // obtain results
                    val uuidToExperienceMap: Map<UUID, Int> = buildMap {
                        while (result.next()) {
                            try {
                                val playerId = UUID.fromString(result.getString("player_id"))
                                val totalExperience = result.getInt("total_experience")
                                this[playerId] = totalExperience
                            } catch (_: Exception) {
                            }
                        }
                    }
                    // compile
                    val responses = uuidToExperienceMap.map { (id, xp) -> TotalExperienceResponse(id, xp) }
                    val json = TotalExperienceResponse.CODEC.listOf().encodeQuick(JsonOps.INSTANCE, responses)

                    if (json == null) {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }
                    // respond
                    call.respondJson(json)
                }

                get("/experience/{uuid}", {
                    description = "Gets the total experience for the specified player UUID."
                    request {
                        pathParameter<String>("uuid")
                    }

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<TotalExperienceResponse>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    val uuidString = call.parameters["uuid"]
                    val uuid = try {
                        UUID.fromString(uuidString)
                    } catch (exception: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, "Not a valid uuid: $uuidString")
                        return@get
                    }
                    // execute query
                    val result = DatabaseController.executePrepared(
                        {
                            prepareStatement(
                                """
                                SELECT SUM(experience_amount) AS total_experience
                                FROM StatisticEvents
                                WHERE player_id = ?
                            """.trimIndent()
                            )
                        },
                        { builder ->
                            builder.addNext(uuid, PreparedStatementBuilder::setStatementUuid)
                        },
                        PreparedStatement::executeQuery
                    )
                    // obtain results
                    val experience = if (result.next()) {
                        try {
                            result.getInt("total_experience")
                        } catch (exception: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                            return@get
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Player not found: $uuid")
                        return@get
                    }
                    // compile
                    val response = TotalExperienceResponse(uuid, experience)
                    val json = TotalExperienceResponse.CODEC.encodeQuick(JsonOps.INSTANCE, response)

                    if (json == null) {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }
                    // respond
                    call.respondJson(json)
                }

                get("/chat_statistics", {
                    description = "Gets chat statistics for all players who have played on MC Brawls."

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<List<MessageCountResponse>>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    // execute query
                    val result = DatabaseController.executeStatement {
                        executeQuery(
                            """
                                SELECT
                                    SUM(chat_mode = 'local' AND chat_result = 'success') AS local_message_count,
                                    SUM(chat_result = 'filtered_profanity') AS filtered_message_count
                                FROM ChatLogs
                            """.trimIndent()
                        )
                    }
                    // obtain results
                    result.next()
                    val localMessageCount = result.getInt("local_message_count")
                    val filteredMessageCount = result.getInt("filtered_message_count")
                    // compile
                    val response = MessageCountResponse(localMessageCount, filteredMessageCount)
                    val json = MessageCountResponse.CODEC.encodeQuick(JsonOps.INSTANCE, response)

                    if (json == null) {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }
                    // respond
                    call.respondJson(json)
                }

                get("/chat_statistics/{uuid}", {
                    description = "Gets chat statistics for the specified player UUID."

                    request {
                        pathParameter<String>("uuid")
                    }

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<MessageCountResponse>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    val uuidString = call.parameters["uuid"]
                    val uuid = try {
                        UUID.fromString(uuidString)
                    } catch (exception: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, "Not a valid uuid: $uuidString")
                        return@get
                    }
                    // execute query
                    val result = DatabaseController.executePrepared(
                        {
                            prepareStatement(
                                """
                                    SELECT
                                        SUM(player_id = ? AND chat_mode = 'local' AND chat_result = 'success') AS local_message_count,
                                        SUM(player_id = ? AND chat_result = 'filtered_profanity') AS filtered_message_count
                                    FROM ChatLogs
                                """.trimIndent()
                            )
                        },
                        { builder ->
                            builder.addNext(uuid, PreparedStatementBuilder::setStatementUuid)
                            builder.addNext(uuid, PreparedStatementBuilder::setStatementUuid)
                        },
                        PreparedStatement::executeQuery
                    )
                    // obtain results
                    result.next()
                    val localMessageCount = result.getInt("local_message_count")
                    val filteredMessageCount = result.getInt("filtered_message_count")
                    // compile
                    val response = MessageCountResponse(localMessageCount, filteredMessageCount)
                    val json = MessageCountResponse.CODEC.encodeQuick(JsonOps.INSTANCE, response)

                    if (json == null) {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }
                    // respond
                    call.respondJson(json)
                }

                get("/leaderboards/{board}", {
                    description = "Retrieves the leaderboard for the given leaderboard type."

                    request {
                        pathParameter<String>("board") {
                            description = "The type of leaderboard. This can be rockets_fired, total_experience, dodgebolt_rounds_won, old_rise_powder_floors and dodgebolt_hit_ratio"
                        }
                    }

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<List<LeaderboardResponse>>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    val boardName = call.parameters["board"]
                    val boardType = LeaderboardTypes[boardName.toString()] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "Not a valid leaderboard name: $boardName")
                        return@get
                    }
                    val result = DatabaseController.executeStatement(boardType.resultProvider)
                    val results = buildList {
                        while (result.next()) {
                            add(LeaderboardResponse(
                                runCatching { UUID.fromString(result.getString("player_id")) }.getOrElse {
                                    call.respond(HttpStatusCode.InternalServerError, "An exception occurred on the server")
                                    return@get
                                },
                                size + 1,
                                result.getInt("value")
                            )
                            )
                        }
                    }
                    val json = LeaderboardResponse.CODEC.listOf().encodeQuick(JsonOps.INSTANCE, results)
                    if (json == null) {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }

                    call.respondJson(json)
                }

                get("/profile/{uuid}", {
                    description = "Gets some basic info about the players profile."
                    request {
                        pathParameter<String>("uuid")
                    }

                    response {
                        HttpStatusCode.OK to {
                            description = "Successful request."
                            body<ProfileResponse>()
                        }

                        HttpStatusCode.InternalServerError to {
                            description = "An unexpected error happened on the server."
                        }
                    }
                }) {
                    val playerId = runCatching { call.parameters["uuid"]?.let(UUID::fromString) }.getOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "Invalid specified player UUID!")
                        return@get
                    }

                    val rankResult = PermissionDatabaseController.executePrepared(
                        {
                            prepareStatement(
                                """
                                    SELECT primary_group FROM luckperms_players WHERE uuid = ?
                                """.trimIndent()
                            )
                        },
                        { builder ->
                            builder.addNext(playerId, PreparedStatementBuilder::setStatementUuid)
                        },
                        PreparedStatement::executeQuery
                    )

                    rankResult.next()
                    val rank = runCatching { Rank.valueOf(rankResult.getString("primary_group").uppercase()) }.getOrNull() ?: Rank.DEFAULT

                    val experienceResult = DatabaseController.executePrepared(
                        {
                            prepareStatement(
                                """
                                SELECT SUM(experience_amount) AS total_experience
                                FROM StatisticEvents
                                WHERE player_id = ?
                            """.trimIndent()
                            )
                        },
                        { builder ->
                            builder.addNext(playerId, PreparedStatementBuilder::setStatementUuid)
                        },
                        PreparedStatement::executeQuery
                    )
                    experienceResult.next()
                    val experience = runCatching { experienceResult.getInt("total_experience") }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest, "That player does not exist!")
                        return@get
                    }

                    val response = ProfileResponse(playerId, rank, experience)
                    val json = ProfileResponse.CODEC.encodeQuick(JsonOps.INSTANCE, response) ?: run {
                        call.respond(HttpStatusCode.InternalServerError, "An exception occured on the server")
                        return@get
                    }
                    call.respondJson(json)
                }
            }
        }
    }.start(wait = true)
}

@Suppress("DeferredResultUnused")
inline fun runAsync(crossinline block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.async { block.invoke(this) }
}

/**
 * Retrieves a file from the run directory.
 */
fun file(path: String): File {
    return Path.of(path).toFile()
}

suspend fun ApplicationCall.respondJson(element: JsonElement) {
    respondText(element.toJsonString())
}

fun JsonElement.toJsonString(prettyPrint: Boolean = false): String {
    val gson = GsonBuilder()
    if (prettyPrint) {
        gson.setPrettyPrinting()
    }
    return gson.create().toJson(this)
}

fun File.toJson(): JsonElement? {
    if (exists() && isFile) {
        return reader().use(JsonParser::parseReader)
    }

    return null
}

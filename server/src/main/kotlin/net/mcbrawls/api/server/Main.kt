@file:OptIn(ExperimentalTime::class)

package net.mcbrawls.api.server

import com.zaxxer.hikari.util.Credentials
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

object Main {
    private val logger: Logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting API server")

        val port = args.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException("Port not provided (args[0])")

        val schema = System.getenv("DATABASE_SCHEMA") ?: error("No database schema")
        val permissionsSchema = System.getenv("DATABASE_SCHEMA_PERMS") ?: error("No permissions database schema")

        val databaseUrl = System.getenv("DATABASE_URL") ?: error("No db url")
        val databaseUser = System.getenv("DATABASE_USERNAME") ?: error("No db user")
        val databasePass = System.getenv("DATABASE_PASSWORD") ?: error("No db pass")
        val credentials = Credentials.of(databaseUser, databasePass)

        val auth = System.getenv("NO_AUTH") == null

        ApiServer(databaseUrl, credentials, schema, permissionsSchema).start(port, auth)
    }
}

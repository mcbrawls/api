package net.mcbrawls.api.database

import com.mojang.serialization.JsonOps
import net.mcbrawls.api.file
import net.mcbrawls.api.runAsync
import net.mcbrawls.api.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import kotlin.concurrent.thread

/**
 * Manages the remote LuckPerms MySQL database.
 */
object PermissionDatabaseController : DatabaseExecutable {
    private val logger: Logger = LoggerFactory.getLogger("Database Controller")

    private const val DATABASE_CONFIG_PATH = "database_config.json"

    var database: AuthenticatableDatabase = loadConfiguration()

    init {
        runAsync { connect() }

        thread(name = "LuckPerms Database Heartbeat") {
            // start heartbeat
            runAsync {
                while (true) {
                    Thread.sleep(5000)
                    connectionHeartbeat()
                }
            }
        }
    }

    /**
     * Verifies that the connection is valid after a 5 second timeout.
     */
    private suspend fun connectionHeartbeat() {
        // check 1 second timeout
        if (!database.isConnectionValid(3)) {
            logger.warn("No database connection found. Attempting to connect to the Brawls central database.")

            // try reconnect
            try {
                database.connect()
                logger.info("Connected to Brawls central database.")
            } catch (exception: Exception) {
                logger.error("Something went wrong connecting to the database", exception)
            }
        }
    }

    /**
     * Loads the configuration from disk.
     */
    fun loadConfiguration(): AuthenticatableDatabase {
        val file = file(DATABASE_CONFIG_PATH)
        try {
            val json = file.toJson()
            val connectionInfo = DatabaseController.ConnectionInfo.CODEC.decode(JsonOps.INSTANCE, json).result().orElseThrow().first
            return AuthenticatableDatabase(
                connectionInfo.address,
                connectionInfo.permissionDatabase,
                "mysql",
                connectionInfo.user,
                connectionInfo.password
            ).also { database = it }
        } catch (exception: Exception) {
            logger.error("Could not load database information", exception)
            throw exception
        }
    }

    override suspend fun <T> execute(action: (Connection) -> T): T {
        return database.execute(action) ?: throw NullPointerException("Database was null: $this")
    }

    /**
     * Connects the database.
     */
    suspend fun connect() {
        database.connect()
    }

    /**
     * Disconnects the database.
     */
    fun disconnect(): Boolean? {
        return database.disconnect()
    }
}

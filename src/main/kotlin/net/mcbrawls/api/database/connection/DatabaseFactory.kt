package net.mcbrawls.api.database.connection

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.util.Credentials
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
    fun create(
        url: String,
        credentials: Credentials,
        builder: HikariConfig.() -> Unit,
    ): Database {
        val config = HikariConfig()

        config.jdbcUrl = url
        config.credentials = credentials

        builder.invoke(config)

        val source = HikariDataSource(config)
        val database = Database.connect(source)

        return database
    }
}

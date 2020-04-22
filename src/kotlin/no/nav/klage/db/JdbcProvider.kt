package no.nav.klage.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.klage.config.Configuration
import javax.sql.DataSource

object ConnectionPool {
    private val config = HikariConfig()

    init {
        val configuration = Configuration().db
        config.jdbcUrl = configuration.dbUrl
        config.username = configuration.dbUsername
        config.password = configuration.dbPassword
        config.maximumPoolSize = configuration.dbMaximumPoolSize
        config.minimumIdle = configuration.dbMinimumIdle
        config.connectionTimeout = configuration.dbConnectionTimeout
    }

    fun getDataSource(): DataSource {
        return HikariDataSource(config)
    }
}
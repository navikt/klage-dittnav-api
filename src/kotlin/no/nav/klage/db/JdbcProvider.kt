package no.nav.klage.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.klage.ApplicationProperties
import javax.sql.DataSource

object ConnectionPool {
    private val config = HikariConfig()

    init {
        val applicationProperties = ApplicationProperties()
        config.jdbcUrl = applicationProperties.dbUrl
        config.username = applicationProperties.dbUsername
        config.password = applicationProperties.dbPassword
        config.maximumPoolSize = 4
        config.minimumIdle = 0
        config.connectionTimeout = 1000
    }

    fun getDataSource(): DataSource {
        return HikariDataSource(config)
    }
}
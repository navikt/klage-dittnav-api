package no.nav.klage.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.klage.ApplicationProperties
import no.nav.klage.SingletonHolder

class ConnectionPool private constructor(application: ApplicationProperties) {
    var dataSource: HikariDataSource

    init {
        val config = HikariConfig()
        config.jdbcUrl = application.dbUrl
        config.username = application.dbUsername
        config.password = application.dbPassword
        config.schema = "klage"
        config.maximumPoolSize = 10
        config.minimumIdle = 2
        config.connectionTimeout = 1000

        dataSource = HikariDataSource(config)
    }

    companion object : SingletonHolder<ConnectionPool, ApplicationProperties>(::ConnectionPool)
}
package no.nav.klage.db

import com.zaxxer.hikari.HikariConfig
import no.nav.klage.ApplicationProperties
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import javax.sql.DataSource

class ConnectionPool private constructor(application: ApplicationProperties) {
    private val config = HikariConfig()

    init {
        config.jdbcUrl = application.dbUrl
        config.schema = "public"
        config.maximumPoolSize = 4
        config.minimumIdle = 0
        config.connectionTimeout = 1000
    }

    companion object {
        fun getDataSourceForUser(): DataSource {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                ConnectionPool(ApplicationProperties()).config,
                "dev",
                "klage-user"
            );
        }

        fun getDataSourceForAdmin(): DataSource {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                ConnectionPool(ApplicationProperties()).config,
                "dev",
                "klage-admin"
            );
        }
    }
}
package no.nav.klage.db

import com.zaxxer.hikari.HikariConfig
import no.nav.klage.ApplicationProperties
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import javax.sql.DataSource

class ConnectionPool private constructor(application: ApplicationProperties) {
    private val config = HikariConfig()

    init {
        config.jdbcUrl = application.dbUrl
        config.maximumPoolSize = 4
        config.minimumIdle = 0
        config.connectionTimeout = 1000
    }

    companion object {
        fun getDataSourceForUser(): DataSource {
            val applicationProperties = ApplicationProperties()
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                ConnectionPool(applicationProperties).config,
                applicationProperties.vaultMountPath,
                "klage-user"
            );
        }

        fun getDataSourceForAdmin(): DataSource {
            val applicationProperties = ApplicationProperties()
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                ConnectionPool(applicationProperties).config,
                applicationProperties.vaultMountPath,
                "klage-admin"
            );
        }
    }
}
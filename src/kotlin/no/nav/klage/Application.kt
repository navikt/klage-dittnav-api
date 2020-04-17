package no.nav.klage

import no.nav.klage.common.configurePrometheusMeterRegistry
import no.nav.klage.db.ConnectionPool
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

private val log = LoggerFactory.getLogger("klage-dittnav-api.Application")

fun main() {
    //Connect to db
    Database.connect(ConnectionPool.getDataSource())

    runDatabaseMigrationOnStartup()

    val prometheusRegistry = configurePrometheusMeterRegistry()

    val applicationState = ApplicationState()

    val applicationServer = createHttpServer(applicationState = applicationState, prometheusRegistry = prometheusRegistry)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationState.initialized = false
        applicationServer.stop(5000, 5000)
    })

    applicationServer.start(wait = true)
}

private fun runDatabaseMigrationOnStartup() {
    log.debug("Trying to run database migration")
    val flyway = Flyway()
    flyway.dataSource = ConnectionPool.getDataSource()
    flyway.migrate()
    log.debug("Database migration complete")
}


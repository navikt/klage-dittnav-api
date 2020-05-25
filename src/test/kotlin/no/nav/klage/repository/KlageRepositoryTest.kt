package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Tema
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageRepositoryTest {
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    @BeforeAll
    fun initDb() {
        val datasource = JdbcConnectionPool.create(jdbcUrl, "sa", "")
        val statement = datasource.connection.createStatement()
        statement.execute("CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP;")
        val config = ClassicConfiguration()
        config.dataSource = datasource
        val flyway = Flyway(config)
        flyway.migrate()

        Database.connect(datasource)
    }

    @Test
    fun `klage inserted`() {
        val klageRepository = KlageRepository()
        transaction {
            val klage = Klage(
                foedselsnummer = "123455667",
                fritekst = "lkdfjals",
                tema = Tema.AAP,
                vedtaksdato = LocalDate.now()
            )

            val nyKlage = klageRepository.createKlage(klage)
            Assertions.assertEquals(1, nyKlage.id)
        }
    }
}

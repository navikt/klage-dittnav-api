package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Tema
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageRepositoryTest {
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    private lateinit var klageRepository: KlageRepository
    private lateinit var datasource: DataSource

    @BeforeAll
    fun initDb() {
        klageRepository = KlageRepository()

        datasource = JdbcConnectionPool.create(jdbcUrl, "sa", "")
        val statement = datasource.connection.createStatement()
        statement.execute("CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP;")
        val config = ClassicConfiguration()
        config.dataSource = datasource
        val flyway = Flyway(config)
        flyway.migrate()

        Database.connect(datasource)
    }

    @Test
    fun `get inserted klage from db`() {
        transaction {
            klageRepository.createKlage(klage1)

            val hentetKlage = klageRepository.getKlageById(1)
            Assertions.assertEquals(fritekst, hentetKlage.fritekst)
        }
    }

    private val fritekst = "fritekst"

    private val klage1 = Klage(
        foedselsnummer = "123455667",
        fritekst = fritekst,
        tema = Tema.AAP,
        vedtaksdato = LocalDate.now()
    )
}

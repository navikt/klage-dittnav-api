package no.nav.klage.repository

import no.nav.klage.clients.createShortCircuitWebClient
import no.nav.klage.domain.Klage
import no.nav.klage.domain.Tema
import no.nav.klage.domain.VedleggWrapper
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.mock.web.MockMultipartFile
import java.nio.charset.Charset
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedleggRepositoryTest {
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    private lateinit var vedleggRepository: VedleggRepository
    private lateinit var klageRepository: KlageRepository
    private lateinit var datasource: DataSource

    @BeforeAll
    fun initDb() {
        vedleggRepository = VedleggRepository(createShortCircuitWebClient(okJsonResponse))
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
    fun `stores klage with vedlegg`() {
        val nyKlage = transaction {
            val klage = klageRepository.createKlage(klage1)
            vedleggRepository.putVedlegg(klage.id!!, vedlegg1)
            klageRepository.getKlageById(klage.id!!)
        }

        Assertions.assertFalse(nyKlage.vedlegg.isNullOrEmpty())
        val vedlegg = nyKlage.vedlegg!![0]

        Assertions.assertEquals("0000-0000-0000-0000", vedlegg.ref)
    }

    @Language("json")
    private val okJsonResponse = """
        {
          "id": "0000-0000-0000-0000"
        }
    """.trimIndent()

    private val klage1 = Klage(
        foedselsnummer = "123455667",
        fritekst = "fritekst",
        tema = Tema.AAP,
        vedtaksdato = LocalDate.now()
    )

    private val vedlegg1 = VedleggWrapper(
        tittel = "tittel",
        type = "type",
        content = MockMultipartFile("vedlegg.txt", "file".toByteArray(Charset.defaultCharset()))
    )
}

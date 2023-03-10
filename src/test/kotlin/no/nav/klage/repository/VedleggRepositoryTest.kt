package no.nav.klage.repository

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.KlagevedleggDAO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.springframework.mock.web.MockMultipartFile
import java.nio.charset.Charset
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedleggRepositoryTest {
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL"

    private lateinit var vedleggRepository: VedleggRepository
    private lateinit var klageRepository: KlageRepository
    private lateinit var datasource: DataSource

    @BeforeAll
    fun initDb() {
        vedleggRepository = VedleggRepository()
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
        val vedleggExternalRef = "externalRef"
        val nyKlage = transaction {
            val klage = klageRepository.createKlage(klage1)
            vedleggRepository.storeKlagevedlegg(klage.id!!, vedlegg1, vedleggExternalRef)
            klageRepository.getKlageById(klage.id!!)
        }

        Assertions.assertFalse(nyKlage.vedlegg.isEmpty())
        val vedlegg = nyKlage.vedlegg[0]

        Assertions.assertEquals(vedleggExternalRef, vedlegg.ref)
    }

    @AfterAll
    fun cleanup() {
        transaction {
            KlagevedleggDAO.all().forEach { x -> x.delete() }
            KlageDAO.all().forEach { x -> x.delete() }
        }
    }

    private val klage1 = Klage(
        foedselsnummer = "123455667",
        fritekst = "fritekst",
        tema = Tema.AAP,
        language = LanguageEnum.NB,
        innsendingsytelse = Innsendingsytelse.ALDERSPENSJON,
        hasVedlegg = false,
    )

    private val vedlegg1 =
        MockMultipartFile("vedlegg.txt", "vedlegg.txt", "txt", "file".toByteArray(Charset.defaultCharset()))
}

package no.nav.klage.repository

import no.nav.klage.domain.*
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.klage.KlageFullInput
import no.nav.klage.domain.klage.KlageInput
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
    private lateinit var klageRepository: OldKlageRepository
    private lateinit var datasource: DataSource

    @BeforeAll
    fun initDb() {
        vedleggRepository = VedleggRepository()
        klageRepository = OldKlageRepository()

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
    fun `stores klage with vedlegg from KlageInput`() {
        val vedleggExternalRef = "externalRef"
        val nyKlage = transaction {
            val klage = klageRepository.createKlage(klageInput, bruker)
            vedleggRepository.storeKlagevedlegg(klage.id, vedlegg1, vedleggExternalRef)
            klageRepository.getKlageById(klage.id.toString())
        }

        Assertions.assertFalse(nyKlage.vedlegg.isEmpty())
        val vedlegg = nyKlage.vedlegg[0]

        Assertions.assertEquals(vedleggExternalRef, vedlegg.ref)
    }

    @Test
    fun `stores klage with vedlegg from KlageFullInput`() {
        val vedleggExternalRef = "externalRef"
        val nyKlage = transaction {
            val klage = klageRepository.createKlage(klageFullInput, bruker)
            vedleggRepository.storeKlagevedlegg(klage.id, vedlegg1, vedleggExternalRef)
            klageRepository.getKlageById(klage.id.toString())
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
    
    private val klageInput = KlageInput(
        internalSaksnummer = null, 
        innsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER
    )

    private val klageFullInput = KlageFullInput(
        internalSaksnummer = null,
        innsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER,
        checkboxesSelected = setOf(),
        userSaksnummer = null,
        language = LanguageEnum.NB,
        vedtakDate = null,
        fritekst = "",
        hasVedlegg = false
    )
    
    private val bruker = Bruker(
        navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
        adresse = null,
        kontaktinformasjon = null,
        folkeregisteridentifikator = Identifikator(type = "", identifikasjonsnummer = ""),
        tokenExpires = null
    )

    private val vedlegg1 =
        MockMultipartFile("vedlegg.txt", "vedlegg.txt", "txt", "file".toByteArray(Charset.defaultCharset()))
}

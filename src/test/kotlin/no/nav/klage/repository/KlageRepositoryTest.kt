package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.titles.Innsendingsytelse
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.time.Instant
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageRepositoryTest {
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL"

    private val exampleFritekst = "fritekst"
    private val exampleFritekst2 = "fritekst2"
    private val fnr = "12345678910"
    private val draftStatus = KlageAnkeStatus.DRAFT
    private val exampleTema = Tema.AAP
    private val exampleInnsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER
    private val exampleInternalSaksnummer = "123456"
    private val exampleModifiedByUser = Instant.parse("2020-11-12T09:35:39.727803600Z")
    private val exampleModifiedByUser2 = exampleModifiedByUser.plusSeconds(100)

    private val innsendingsytelseAndInternalSaksnummer = "innsendingsytelse and internalSaksnummer"
    private val innsendingsytelseAndNoInternalSaksnummer = "innsendingsytelse and no internalSaksnummer"

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

    @BeforeEach
    fun cleanup() {
        transaction {
            KlageDAO.all().forEach { x -> x.delete() }
        }
    }

    @Test
    fun `should get correct klage based on internalSaksnummer and innsendingsytelse`() {
        transaction {
            createDBEntries()

            val hentetKlage = klageRepository.getLatestKlageDraft(
                fnr,
                exampleTema,
                exampleInternalSaksnummer,
                exampleInnsendingsytelse
            )
            Assertions.assertEquals(innsendingsytelseAndInternalSaksnummer, hentetKlage?.fritekst)
        }
    }

    @Test
    fun `should get correct klage based on no internalSaksnummer and innsendingsytelse`() {
        transaction {
            createDBEntries()

            val hentetKlage = klageRepository.getLatestKlageDraft(
                fnr = fnr,
                tema = exampleTema,
                internalSaksnummer = null,
                innsendingsytelse = exampleInnsendingsytelse
            )
            Assertions.assertEquals(innsendingsytelseAndNoInternalSaksnummer, hentetKlage?.fritekst)
        }
    }

    @Test
    fun `should get latest klage`() {
        transaction {
            createTwoSimilarEntries()


            val hentetKlage = klageRepository.getLatestKlageDraft(
                fnr,
                exampleTema,
                exampleInternalSaksnummer,
                exampleInnsendingsytelse
            )
            Assertions.assertEquals(exampleFritekst2, hentetKlage?.fritekst)
        }
    }

    @Test
    fun `updateFritekst works as expected`() {
        transaction {
            createDBEntryWithYtelse()

            val klage = klageRepository.getDraftKlagerByFnr(fnr)[0]
            klageRepository.updateFritekst(klage.id.toString(), exampleFritekst2)
            val output = klageRepository.getKlageById(klage.id.toString()).fritekst

            Assertions.assertEquals(exampleFritekst2, output)
        }
    }

    @AfterAll
    fun cleanupAfter() {
        transaction {
            KlageDAO.all().forEach { x -> x.delete() }
        }
    }

    private fun createTwoSimilarEntries() {
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst
            tema = exampleTema.name
            innsendingsytelse = exampleInnsendingsytelse.name
            internalSaksnummer = exampleInternalSaksnummer
            modifiedByUser = exampleModifiedByUser
        }

        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst2
            tema = exampleTema.name
            innsendingsytelse = exampleInnsendingsytelse.name
            internalSaksnummer = exampleInternalSaksnummer
            modifiedByUser = exampleModifiedByUser2
        }
    }

    private fun createDBEntries() {
        //innsendingsytelse and internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = innsendingsytelseAndInternalSaksnummer
            tema = exampleTema.name
            innsendingsytelse = exampleInnsendingsytelse.name
            internalSaksnummer = exampleInternalSaksnummer
        }

        //innsendingsytelse and no internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = innsendingsytelseAndNoInternalSaksnummer
            tema = exampleTema.name
            innsendingsytelse = exampleInnsendingsytelse.name
        }
    }

    private fun createDBEntryWithYtelse() {
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst
            tema = exampleTema.name
            innsendingsytelse = exampleInnsendingsytelse.name
        }
    }

}

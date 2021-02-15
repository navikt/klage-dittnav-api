package no.nav.klage.repository

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.klage.KlageStatus
import no.nav.klage.domain.titles.TitleEnum
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
    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    private val exampleFritekst = "fritekst"
    private val exampleFritekst2 = "fritekst2"
    private val fnr = "12345678910"
    private val draftStatus = KlageStatus.DRAFT
    private val exampleTema = Tema.AAP
    private val exampleTitleKey = TitleEnum.ARBEIDSAVKLARINGSPENGER
    private val exampleInternalSaksnummer = "123456"
    private val exampleModifiedByUser = Instant.parse("2020-11-12T09:35:39.727803600Z")
    private val exampleModifiedByUser2 = exampleModifiedByUser.plusSeconds(100)

    private val titleKeyAndInternalSaksnummer = "titleKey and internalSaksnummer"
    private val titleKeyAndNoInternalSaksnummer = "titleKey and no internalSaksnummer"
    private val noTitleKeyAndInternalSaksnummer = "no titleKey and internalSaksnummer"
    private val noTitleKeyAndNoInternalSaksnummer = "no titleKey and no internalSaksnummer"

    private val klage1 = Klage(
        foedselsnummer = fnr,
        fritekst = exampleFritekst,
        tema = Tema.AAP,
        language = LanguageEnum.NB,
        titleKey = TitleEnum.ALDERSPENSJON
    )


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
    fun `get inserted klage from db`() {
        transaction {
            KlageDAO.all().forEach { x -> x.delete() }
            klageRepository.createKlage(klage1)

            val hentetKlage = klageRepository.getKlageById(1)
            Assertions.assertEquals(exampleFritekst, hentetKlage.fritekst)
        }
    }

    @Nested
    inner class GetLatestDraft {
        @Test
        fun `should get correct klage based on internalSaksnummer and titleKey`() {
            transaction {
                createDBEntries()

                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    exampleInternalSaksnummer,
                    exampleTitleKey
                )
                Assertions.assertEquals(titleKeyAndInternalSaksnummer, hentetKlage?.fritekst)
            }
        }

        @Test
        fun `should get correct klage based on internalSaksnummer and no titleKey`() {
            transaction {
                createDBEntries()

                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    exampleInternalSaksnummer,
                    null
                )
                Assertions.assertEquals(noTitleKeyAndInternalSaksnummer, hentetKlage?.fritekst)
            }
        }

        @Test
        fun `should get correct klage based on no internalSaksnummer and titleKey`() {
            transaction {
                createDBEntries()

                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    null,
                    exampleTitleKey
                )
                Assertions.assertEquals(titleKeyAndNoInternalSaksnummer, hentetKlage?.fritekst)
            }
        }

        @Test
        fun `should get correct klage based on no internalSaksnummer and no titleKey`() {
            transaction {
                createDBEntries()

                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    null,
                    null
                )
                Assertions.assertEquals(noTitleKeyAndNoInternalSaksnummer, hentetKlage?.fritekst)
            }
        }

        @Test
        fun `should get latest klage`() {
            transaction {
                createTwoSimilarEntries()


                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    exampleInternalSaksnummer,
                    exampleTitleKey
                )
                Assertions.assertEquals(exampleFritekst2, hentetKlage?.fritekst)
            }
        }

        @Test
        fun `should get klage with titleKey based on ytelse in KlageDAO and titleKey in request`() {
            transaction {
                createDBEntryWithYtelse()

                val hentetKlage = klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                    fnr,
                    exampleTema,
                    null,
                    exampleTitleKey
                )
                Assertions.assertEquals(exampleTitleKey, hentetKlage?.titleKey)
            }
        }
    }

    private fun createTwoSimilarEntries() {
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst
            tema = exampleTema.name
            titleKey = exampleTitleKey.name
            internalSaksnummer = exampleInternalSaksnummer
            modifiedByUser = exampleModifiedByUser
        }

        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst2
            tema = exampleTema.name
            titleKey = exampleTitleKey.name
            internalSaksnummer = exampleInternalSaksnummer
            modifiedByUser = exampleModifiedByUser2
        }
    }

    private fun createDBEntries() {
        //titleKey and internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = titleKeyAndInternalSaksnummer
            tema = exampleTema.name
            titleKey = exampleTitleKey.name
            internalSaksnummer = exampleInternalSaksnummer
        }

        //titleKey and no internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = titleKeyAndNoInternalSaksnummer
            tema = exampleTema.name
            titleKey = exampleTitleKey.name
        }

        //no titleKey and internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = noTitleKeyAndInternalSaksnummer
            tema = exampleTema.name
            internalSaksnummer = exampleInternalSaksnummer
        }

        //no titleKey and no internalSaksnummer
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = noTitleKeyAndNoInternalSaksnummer
            tema = exampleTema.name
        }
    }

    private fun createDBEntryWithYtelse() {
        KlageDAO.new {
            foedselsnummer = fnr
            status = draftStatus.name
            fritekst = exampleFritekst
            tema = exampleTema.name
            ytelse = exampleTitleKey.nb
        }
    }

}

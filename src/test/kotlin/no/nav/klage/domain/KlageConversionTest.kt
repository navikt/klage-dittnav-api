package no.nav.klage.domain

import no.nav.klage.domain.klage.*
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.repository.KlageRepository
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageConversionTest {
    private val exampleId = 1
    private val exampleFnr = "12345678910"
    private val exampleFritekst = "fritekst"
    private val exampleTema = Tema.FOR
    private val exampleYtelse = "Alderspensjon"
    private val exampleYtelse2 = "Barnepensjon"
    private val exampleModifiedByUser = Instant.parse("2020-11-12T09:35:39.727803600Z")
    private val exampleStatus = KlageAnkeStatus.DRAFT
    private val exampleTitleKey = TitleEnum.ALDERSPENSJON

    private val jdbcUrl = "jdbc:h2:mem:test_mem;MODE=PostgreSQL"

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

    @Nested
    inner class KlageDAOToKlage {
        @Nested
        inner class CheckboxesSelectedListBasedOnCheckboxesSelectedString {
            @Test
            fun `should populate checkboxesSelected in Klage based on checkboxesSelected string in KlageDAO`() {
                val klageInDB = templateKlage
                val expectedOutput = klageInDB.copy(
                    checkboxesSelected = setOf(
                        CheckboxEnum.AVSLAG_PAA_SOKNAD,
                        CheckboxEnum.FOR_LITE_UTBETALT
                    )
                )

                verifyCheckboxesSelectedConversionToKlageFromKlageDAO(
                    expectedOutput,
                    klageInDB,
                    "FOR_LITE_UTBETALT,AVSLAG_PAA_SOKNAD"
                )
            }
        }

        @Nested
        inner class TitleKey {
            @Test
            fun `should populate titleKey in Klage based on ytelse and no titleKey in KlageDAO`() {
                val expectedOutput = templateKlage

                transaction {
                    val inputKlageDAO = KlageDAO.new {
                        foedselsnummer = exampleFnr
                        status = exampleStatus.name
                        fritekst = exampleFritekst
                        tema = exampleTema.name
                        ytelse = exampleYtelse
                        language = LanguageEnum.NB.name
                    }

                    val result = inputKlageDAO.toKlage().copy(id = exampleId, modifiedByUser = exampleModifiedByUser)

                    assertEquals(expectedOutput, result)
                }
            }

            @Test
            fun `should populate titleKey in Klage based on no ytelse and titleKey in KlageDAO`() {
                val expectedOutput = templateKlage

                transaction {
                    val inputKlageDAO = KlageDAO.new {
                        foedselsnummer = exampleFnr
                        status = exampleStatus.name
                        fritekst = exampleFritekst
                        tema = exampleTema.name
                        language = LanguageEnum.NB.name
                        titleKey = exampleTitleKey.name
                    }

                    val result = inputKlageDAO.toKlage().copy(id = exampleId, modifiedByUser = exampleModifiedByUser)

                    assertEquals(expectedOutput, result)
                }
            }

            @Test
            fun `should populate correct titleKey in Klage based on wrong ytelse and titleKey in KlageDAO`() {
                val expectedOutput = templateKlage

                transaction {
                    val inputKlageDAO = KlageDAO.new {
                        foedselsnummer = exampleFnr
                        status = exampleStatus.name
                        fritekst = exampleFritekst
                        tema = exampleTema.name
                        language = LanguageEnum.NB.name
                        ytelse = exampleYtelse2
                        titleKey = exampleTitleKey.name
                    }

                    val result = inputKlageDAO.toKlage().copy(id = exampleId, modifiedByUser = exampleModifiedByUser)

                    assertEquals(expectedOutput, result)
                }
            }
        }
    }

    @Nested
    inner class KlageToKlageDAO {
        @Test
        fun `should populate checkboxesSelected in KlageDAO based on checkboxesSelected in Klage`() {
            val inputKlage = templateKlage.copy(
                checkboxesSelected = setOf(CheckboxEnum.FOR_LITE_UTBETALT, CheckboxEnum.AVSLAG_PAA_SOKNAD)
            )

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertEquals("FOR_LITE_UTBETALT,AVSLAG_PAA_SOKNAD", result.checkBoxesSelected)
            }
        }
    }

    @AfterAll
    fun cleanup() {
        transaction {
            KlageDAO.all().forEach { x -> x.delete() }
        }
    }

    private fun verifyCheckboxesSelectedConversionToKlageFromKlageDAO(
        expectedOutput: Klage,
        klageInDB: Klage,
        checkboxesSelectedString: String? = null
    ) {
        transaction {
            val inputKlageDAO = KlageDAO.new {
                foedselsnummer = klageInDB.foedselsnummer
                status = klageInDB.status.name
                fritekst = klageInDB.fritekst
                tema = klageInDB.tema.name
                checkBoxesSelected = checkboxesSelectedString
                titleKey = klageInDB.titleKey.name
            }

            val result = inputKlageDAO.toKlage().copy(id = exampleId, modifiedByUser = exampleModifiedByUser)

            assertEquals(expectedOutput, result)
        }
    }


    private val templateKlage = Klage(
        id = exampleId,
        foedselsnummer = exampleFnr,
        status = exampleStatus,
        fritekst = exampleFritekst,
        tema = exampleTema,
        modifiedByUser = exampleModifiedByUser,
        language = LanguageEnum.NB,
        titleKey = exampleTitleKey
    )
}
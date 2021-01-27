package no.nav.klage.domain

import no.nav.klage.domain.klage.*
import no.nav.klage.repository.KlageRepository
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageConversionTest {
    private val exampleId = 1
    private val foedselsnummer = "12345678910"
    private val fritekst = "fritekst"
    private val tema = Tema.FOR
    private val ytelse = "Ytelse"
    private val modifiedByUser = Instant.parse("2020-11-12T09:35:39.727803600Z")

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
                ytelse = klageInDB.ytelse
                checkBoxesSelected = checkboxesSelectedString
            }

            val result = inputKlageDAO.toKlage().copy(id = exampleId, modifiedByUser = modifiedByUser)

            assertEquals(expectedOutput, result)
        }
    }

    private val templateKlage = Klage(
        id = exampleId,
        foedselsnummer = foedselsnummer,
        status = KlageStatus.DRAFT,
        fritekst = fritekst,
        tema = tema,
        ytelse = ytelse,
        modifiedByUser = modifiedByUser
    )
}
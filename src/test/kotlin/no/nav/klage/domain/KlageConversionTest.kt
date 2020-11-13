package no.nav.klage.domain

import no.nav.klage.domain.klage.*
import no.nav.klage.repository.KlageRepository
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KlageConversionTest {
    private val exampleId = 1
    private val foedselsnummer = "12345678910"
    private val fritekst = "fritekst"
    private val tema = Tema.FOR
    private val ytelse = "Ytelse"
    private val earlierVedtakWithDate = "Tidligere vedtak - 04.11.2020"
    private val earlierVedtakWithDateVersion2 = "Tidligere vedtak - 11.08.2004"
    private val earlierVedtak = "Tidligere vedtak"
    private val latestVedtak = "Siste vedtak"
    private val vedtakDate = LocalDate.of(2020, 11, 4)
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
        inner class VedtakBasedOnVedtakTypeAndVedtakDate {

            @Test
            fun `should populate earlier vedtak with date in Klage based on vedtakType and vedtakDate in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtakType = VedtakType.EARLIER, vedtakDate = vedtakDate)
                val expectedOutput = klageInDB.copy(vedtak = earlierVedtakWithDate)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate earlier vedtak without date in Klage based on vedtakType in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtakType = VedtakType.EARLIER)
                val expectedOutput = klageInDB.copy(vedtak = earlierVedtak)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate latest vedtak without date in Klage based on vedtakType in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtakType = VedtakType.LATEST)
                val expectedOutput = klageInDB.copy(vedtak = latestVedtak)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate latest vedtak without date in Klage based on vedtakType and vedtakDate in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtakType = VedtakType.LATEST, vedtakDate = vedtakDate)
                val expectedOutput = klageInDB.copy(vedtak = latestVedtak)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate earlier vedtak with date in Klage based on vedtakType and vedtakDate in KlageDAO, ignoring vedtak in KlageDAO`() {
                val klageInDB = templateKlage.copy(
                    vedtak = earlierVedtakWithDateVersion2,
                    vedtakType = VedtakType.EARLIER,
                    vedtakDate = vedtakDate
                )
                val expectedOutput = klageInDB.copy(vedtak = earlierVedtakWithDate)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

        }

        @Nested
        inner class VedtakTypeAndVedtakDateBasedOnVedtak {

            @Test
            fun `should populate vedtakDate and vedtakType in Klage based on earlier vedtak with date in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtak = earlierVedtakWithDate)
                val expectedOutput = klageInDB.copy(vedtakType = VedtakType.EARLIER, vedtakDate = vedtakDate)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate vedtakType in Klage based on earlier vedtak without date in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtak = earlierVedtak)
                val expectedOutput = klageInDB.copy(vedtakType = VedtakType.EARLIER)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate vedtakType in Klage based on latest vedtak without date in KlageDAO`() {
                val klageInDB = templateKlage.copy(vedtak = latestVedtak)
                val expectedOutput = klageInDB.copy(vedtakType = VedtakType.LATEST)

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }

            @Test
            fun `should populate vedtak, vedtakDate and vedtakType in Klage based on corresponding date and type values in KlageDAO, ignoring existing vedtak`() {
                val klageInDB = templateKlage.copy(
                    vedtakType = VedtakType.EARLIER,
                    vedtakDate = vedtakDate,
                    vedtak = earlierVedtakWithDateVersion2
                )
                val expectedOutput = klageInDB.copy(
                    vedtakType = VedtakType.EARLIER,
                    vedtakDate = vedtakDate,
                    vedtak = earlierVedtakWithDate
                )

                verifyConversionToKlageFromKlageDAO(expectedOutput, klageInDB)
            }
        }
    }

    @Nested
    inner class KlageToKlageDAO {
        @Test
        fun `should populate vedtakType and vedtakDate in KlageDAO based on earlier vedtak with date in Klage`() {
            val inputKlage = templateKlage.copy(vedtak = earlierVedtakWithDate)

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertEquals(VedtakType.EARLIER.name, result.vedtakType)
                assertEquals(vedtakDate, result.vedtakDate)
                assertNull(result.vedtak)
            }
        }

        @Test
        fun `should populate vedtakType in KlageDAO based on earlier vedtak without date in Klage`() {
            val inputKlage = templateKlage.copy(vedtak = earlierVedtak)

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertEquals(VedtakType.EARLIER.name, result.vedtakType)
                assertNull(result.vedtakDate)
                assertNull(result.vedtak)
            }
        }

        @Test
        fun `should populate vedtakType in KlageDAO based on latest vedtak in Klage`() {
            val inputKlage = templateKlage.copy(vedtak = latestVedtak)

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertEquals(VedtakType.LATEST.name, result.vedtakType)
                assertNull(result.vedtakDate)
                assertNull(result.vedtak)
            }
        }

        @Test
        fun `should populate vedtakType and vedtakDate in KlageDAO based on corresponding values in Klage, ignoring vedtak`() {
            val inputKlage = templateKlage.copy(
                vedtak = earlierVedtakWithDateVersion2,
                vedtakType = VedtakType.EARLIER,
                vedtakDate = vedtakDate
            )

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertEquals(VedtakType.EARLIER.name, result.vedtakType)
                assertEquals(vedtakDate, result.vedtakDate)
                assertNull(result.vedtak)
            }
        }

        @Test
        fun `should populate vedtakDate in KlageDAO based on corresponding value in Klage, ignoring vedtak`() {
            val inputKlage = templateKlage.copy(vedtak = earlierVedtakWithDate, vedtakDate = vedtakDate)

            transaction {
                val result = KlageDAO.new {
                    fromKlage(inputKlage)
                }

                assertNull(result.vedtakType)
                assertEquals(vedtakDate, result.vedtakDate)
                assertNull(result.vedtak)
            }
        }
    }

    private fun verifyConversionToKlageFromKlageDAO(expectedOutput: Klage, klageInDB: Klage) {
        transaction {
            val inputKlageDAO = KlageDAO.new {
                foedselsnummer = klageInDB.foedselsnummer
                status = klageInDB.status.name
                fritekst = klageInDB.fritekst
                tema = klageInDB.tema.name
                ytelse = klageInDB.ytelse
                vedtak = klageInDB.vedtak
                vedtakType = klageInDB.vedtakType?.name
                vedtakDate = klageInDB.vedtakDate
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
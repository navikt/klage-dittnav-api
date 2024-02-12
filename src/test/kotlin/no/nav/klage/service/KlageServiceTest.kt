package no.nav.klage.service


import io.mockk.mockk
import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.repository.KlageRepository
import no.nav.klage.repository.KlankeRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@ActiveProfiles("dbtest")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KlageServiceTest {

    companion object {
        @Container
        @JvmField
        val postgreSQLContainer: TestPostgresqlContainer = TestPostgresqlContainer.instance
    }

    private val exampleFritekst = "fritekst"
    private val exampleFritekst2 = "fritekst2"
    private val fnr = "12345678910"
    private val draftStatus = KlageAnkeStatus.DRAFT
    private val exampleTema = Tema.AAP
    private val exampleInnsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER
    private val exampleInternalSaksnummer = "123456"
    private val exampleModifiedByUser = LocalDateTime.now()
    private val exampleModifiedByUser2 = exampleModifiedByUser.plusSeconds(100)

    private val innsendingsytelseAndInternalSaksnummer = "innsendingsytelse and internalSaksnummer"
    private val innsendingsytelseAndNoInternalSaksnummer = "innsendingsytelse and no internalSaksnummer"

    @Autowired
    private lateinit var klageRepository: KlageRepository

    @Autowired
    private lateinit var klankeRepository: KlankeRepository

    private lateinit var klageService: KlageService

    @BeforeEach
    fun cleanup() {
        klageRepository.deleteAll()

        klageService = KlageService(
            klankeRepository = klankeRepository,
            klageRepository = klageRepository,
            klageAnkeMetrics = mockk(),
            vedleggMetrics = mockk(),
            kafkaProducer = mockk(),
            fileClient = mockk(),
            validationService = mockk(relaxed = true),
            kafkaInternalEventService = mockk(),
            klageDittnavPdfgenService = mockk(),
        )
    }

    @Test
    fun `should get correct klage based on internalSaksnummer and innsendingsytelse`() {
        createDBEntries()

        val hentetKlage = klageService.getLatestKlageDraft(
            Bruker(
                navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
                adresse = null,
                folkeregisteridentifikator = Identifikator(type = "fnr", "12345678910"),
                kontaktinformasjon = null,
            ),
            exampleTema,
            exampleInternalSaksnummer,
            exampleInnsendingsytelse
        )
        assertEquals(innsendingsytelseAndInternalSaksnummer, hentetKlage?.fritekst)
    }

    @Test
    fun `should get correct klage based on no internalSaksnummer and innsendingsytelse`() {
        createDBEntries()

        val hentetKlage = klageService.getLatestKlageDraft(
            Bruker(
                navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
                adresse = null,
                folkeregisteridentifikator = Identifikator(type = "fnr", "12345678910"),
                kontaktinformasjon = null,
            ),
            tema = exampleTema,
            internalSaksnummer = null,
            innsendingsytelse = exampleInnsendingsytelse
        )
        assertEquals(innsendingsytelseAndNoInternalSaksnummer, hentetKlage?.fritekst)
    }

    @Test
    fun `should get latest klage`() {
        createTwoSimilarEntries()

        val hentetKlage = klageService.getLatestKlageDraft(
            Bruker(
                navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
                adresse = null,
                folkeregisteridentifikator = Identifikator(type = "fnr", "12345678910"),
                kontaktinformasjon = null,
            ),
            exampleTema,
            exampleInternalSaksnummer,
            exampleInnsendingsytelse
        )
        assertEquals(exampleFritekst2, hentetKlage?.fritekst)
    }

    @Test
    fun `updateFritekst works as expected`() {
        createDBEntryWithYtelse()

        val klage = klageService.getKlageDraftsByFnr(
            bruker = Bruker(
                navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
                adresse = null,
                folkeregisteridentifikator = Identifikator(type = "fnr", "12345678910"),
                kontaktinformasjon = null,
            )
        ).first()
        klageService.updateFritekst(klankeId = klage.id, fritekst = exampleFritekst2, bruker = mockk(relaxed = true))
        val output = klageService.getKlanke(klankeId = klage.id, bruker = mockk(relaxed = true)).fritekst

        assertEquals(exampleFritekst2, output)
    }

    private fun createTwoSimilarEntries() {
        val now = LocalDateTime.now()

        klageRepository.save(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
                tema = exampleTema,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                internalSaksnummer = exampleInternalSaksnummer,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = exampleModifiedByUser,
            )
        )

        klageRepository.save(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst2,
                status = draftStatus,
                tema = exampleTema,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                internalSaksnummer = exampleInternalSaksnummer,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = exampleModifiedByUser2,
            )
        )
    }

    private fun createDBEntries() {
        var now = LocalDateTime.now()

        //innsendingsytelse and internalSaksnummer
        klageRepository.save(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndInternalSaksnummer,
                status = draftStatus,
                tema = exampleTema,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                internalSaksnummer = exampleInternalSaksnummer,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
            )
        )

        now = LocalDateTime.now()

        //innsendingsytelse and no internalSaksnummer
        klageRepository.save(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndNoInternalSaksnummer,
                status = draftStatus,
                tema = exampleTema,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                internalSaksnummer = null,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
            )
        )
    }

    private fun createDBEntryWithYtelse() {
        val now = LocalDateTime.now()
        klageRepository.save(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
                tema = exampleTema,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                internalSaksnummer = null,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
            )
        )
    }

}
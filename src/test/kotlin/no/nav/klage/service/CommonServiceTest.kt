package no.nav.klage.service


import io.mockk.mockk
import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.repository.KlankeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@ActiveProfiles("dbtest")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommonServiceTest {

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
    private lateinit var klankeRepository: KlankeRepository

    private lateinit var commonService: CommonService

    @BeforeEach
    fun cleanup() {
        klankeRepository.deleteAll()

        commonService = CommonService(
            klankeRepository = klankeRepository,
            validationService = mockk(relaxed = true),
            kafkaInternalEventService = mockk(),
            klageAnkeMetrics = mockk(),
            vedleggMetrics = mockk(),
            kafkaProducer = mockk(),
            klageDittnavPdfgenService = mockk(),
            documentService = mockk()
        )
    }

    @Test
    fun `should get correct klage based on internalSaksnummer and innsendingsytelse`() {
        createDBEntries()

        val hentetKlage = commonService.getLatestKlankeDraft(
            foedselsnummer = "12345678910",
            internalSaksnummer = exampleInternalSaksnummer,
            innsendingsytelse = exampleInnsendingsytelse,
            type = Type.KLAGE,
        )
        assertEquals(innsendingsytelseAndInternalSaksnummer, hentetKlage?.fritekst)
    }

    @Test
    fun `should get correct klage based on no internalSaksnummer and innsendingsytelse`() {
        createDBEntries()

        val hentetKlage = commonService.getLatestKlankeDraft(
            foedselsnummer = "12345678910",
            internalSaksnummer = null,
            innsendingsytelse = exampleInnsendingsytelse,
            type = Type.KLAGE,
        )
        assertEquals(innsendingsytelseAndNoInternalSaksnummer, hentetKlage?.fritekst)
    }

    @Test
    fun `should get latest klage`() {
        createTwoSimilarEntries()

        val hentetKlage = commonService.getLatestKlankeDraft(
            foedselsnummer = "12345678910",
            internalSaksnummer = exampleInternalSaksnummer,
            innsendingsytelse = exampleInnsendingsytelse,
            type = Type.KLAGE,
        )
        assertEquals(exampleFritekst2, hentetKlage?.fritekst)
    }

    @Test
    fun `updateFritekst works as expected`() {
        createDBEntryWithYtelse()

        val klage = klankeRepository.findAll().first()
        commonService.updateFritekst(klankeId = klage.id, fritekst = exampleFritekst2, foedselsnummer = "12345678910")
        val output = commonService.getKlanke(klankeId = klage.id, foedselsnummer = "12345678910").fritekst

        assertEquals(exampleFritekst2, output)
    }

    private fun createTwoSimilarEntries() {
        val now = LocalDateTime.now()

        klankeRepository.save(
            Klanke(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
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
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )

        klankeRepository.save(
            Klanke(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst2,
                status = draftStatus,
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
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )
    }

    private fun createDBEntries() {
        var now = LocalDateTime.now()

        //innsendingsytelse and internalSaksnummer
        klankeRepository.save(
            Klanke(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndInternalSaksnummer,
                status = draftStatus,
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
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )

        now = LocalDateTime.now()

        //innsendingsytelse and no internalSaksnummer
        klankeRepository.save(
            Klanke(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndNoInternalSaksnummer,
                status = draftStatus,
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
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )
    }

    private fun createDBEntryWithYtelse() {
        val now = LocalDateTime.now()
        klankeRepository.save(
            Klanke(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
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
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )
    }

}
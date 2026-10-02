package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.createTestKlanke
import no.nav.klage.db.PostgresIntegrationTestBase
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.KlankeMarkedCompletedEvent
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Type
import no.nav.klage.domain.exception.SectionedValidationErrorWithDetailsException
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Sak
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.repository.KlankeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("dbtest")
@DataJpaTest
class CommonServiceTest : PostgresIntegrationTestBase() {
    private val exampleFritekst = "fritekst"
    private val exampleFritekst2 = "fritekst2"
    private val fnr = "12345678910"
    private val draftStatus = KlageAnkeStatus.DRAFT
    private val exampleInnsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER
    private val exampleInternalSaksnummer = "123456"
    private val exampleModifiedByUser = LocalDateTime.now()
    private val exampleModifiedByUser2 = exampleModifiedByUser.plusSeconds(100)

    private val innsendingsytelseAndInternalSaksnummer = "innsendingsytelse and internalSaksnummer"
    private val innsendingsytelseAndNoInternalSaksnummer = "innsendingsytelse and no internalSaksnummer"

    private val klageLookupClient: KlageLookupClient = mockk()
    private val safselvbetjeningService: SafSelvbetjeningService = mockk()
    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val validationService: ValidationService = mockk(relaxed = true)

    @Autowired
    private lateinit var klankeRepository: KlankeRepository

    private lateinit var commonService: CommonService

    @BeforeEach
    fun cleanup() {
        klankeRepository.deleteAll()

        commonService =
            CommonService(
                klankeRepository = klankeRepository,
                validationService = validationService,
                kafkaInternalEventService = mockk(),
                klageAnkeMetrics = mockk(relaxed = true),
                vedleggMetrics = mockk(relaxed = true),
                klageDittnavPdfgenService = mockk(),
                documentService = mockk(),
                klageLookupClient = klageLookupClient,
                tokenUtil = mockk(),
                safSelvbetjeningService = safselvbetjeningService,
                applicationEventPublisher = applicationEventPublisher,
            )
    }

    @Test
    fun `finalizeKlanke marks klanke as SENDING and publishes event without sending to Kafka`() {
        val klanke = klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.DRAFT))
        every { safselvbetjeningService.getUsersDocumentTemas(userIdent = any()) } returns emptyList()

        val finalized = commonService.finalizeKlanke(klankeId = klanke.id)

        val updated = klankeRepository.findById(klanke.id).get()
        assertEquals(KlageAnkeStatus.SENDING, updated.status)
        assertNotNull(updated.markedCompleted)
        assertEquals(updated.markedCompleted, updated.modifiedByUser)
        assertEquals(finalized.modifiedByUser, updated.modifiedByUser)
        verify(exactly = 1) { applicationEventPublisher.publishEvent(KlankeMarkedCompletedEvent(klankeId = klanke.id)) }
    }

    @Test
    fun `finalizeKlanke validates content and does not finalize invalid klanke`() {
        val klanke = klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.DRAFT))
        every { safselvbetjeningService.getUsersDocumentTemas(userIdent = any()) } returns emptyList()
        every { validationService.validateKlanke(klanke = any()) } throws
            SectionedValidationErrorWithDetailsException(title = "Validation error", sections = emptyList())

        assertThrows<SectionedValidationErrorWithDetailsException> {
            commonService.finalizeKlanke(klankeId = klanke.id)
        }

        val unchanged = klankeRepository.findById(klanke.id).get()
        assertEquals(KlageAnkeStatus.DRAFT, unchanged.status)
        assertNull(unchanged.markedCompleted)
        verify(exactly = 1) { validationService.validateKlanke(klanke = match { it.id == klanke.id }) }
        verify(exactly = 0) { applicationEventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `finalizeKlanke on already SENDING klanke returns early without new event`() {
        val markedCompleted = LocalDateTime.now().minusMinutes(5)
        val klanke =
            klankeRepository.save(
                createTestKlanke(
                    status = KlageAnkeStatus.SENDING,
                    markedCompleted = markedCompleted,
                    modifiedByUser = markedCompleted,
                ),
            )

        val finalized = commonService.finalizeKlanke(klankeId = klanke.id)

        assertEquals(markedCompleted.toLocalDate(), finalized.finalizedDate)
        assertEquals(markedCompleted, klankeRepository.findById(klanke.id).get().markedCompleted)
        verify(exactly = 0) { applicationEventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `markKlankeAsDone sets DONE and keeps markedCompleted and modifiedByUser`() {
        val markedCompleted = LocalDateTime.now().minusMinutes(5)
        val klanke =
            klankeRepository.save(
                createTestKlanke(
                    status = KlageAnkeStatus.SENDING,
                    markedCompleted = markedCompleted,
                    modifiedByUser = markedCompleted,
                ),
            )

        commonService.markKlankeAsDone(klankeId = klanke.id)

        val updated = klankeRepository.findById(klanke.id).get()
        assertEquals(KlageAnkeStatus.DONE, updated.status)
        assertEquals(markedCompleted, updated.markedCompleted)
        assertEquals(markedCompleted, updated.modifiedByUser)
    }

    @Test
    fun `markKlankeAsDone does not change klanke that is not SENDING`() {
        val klanke = klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.DELETED))

        commonService.markKlankeAsDone(klankeId = klanke.id)

        assertEquals(KlageAnkeStatus.DELETED, klankeRepository.findById(klanke.id).get().status)
    }

    @Test
    fun `findKlankeIdsToSend returns only SENDING klanker`() {
        val sending = klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.SENDING))
        klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.DONE))
        klankeRepository.save(createTestKlanke(status = KlageAnkeStatus.DRAFT))

        assertEquals(listOf(sending.id), commonService.findKlankeIdsToSend())
    }

    @Test
    fun `should get correct klage based on internalSaksnummer and innsendingsytelse`() {
        createDBEntries()

        val hentetKlage =
            commonService.getLatestKlankeDraft(
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

        val hentetKlage =
            commonService.getLatestKlankeDraft(
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

        val hentetKlage =
            commonService.getLatestKlankeDraft(
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
        commonService.updateFritekst(klankeId = klage.id, fritekst = exampleFritekst2)
        every { safselvbetjeningService.getUsersDocumentTemas(userIdent = any()) } returns emptyList()
        val output = commonService.getKlanke(klankeId = klage.id).fritekst

        assertEquals(exampleFritekst2, output)
    }

    private fun createTwoSimilarEntries() {
        val now = LocalDateTime.now()

        klankeRepository.save(
            Klanke(
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                sak =
                    Sak(
                        sakstype = "FAGSAK",
                        fagsaksystem = "FS123",
                        fagsakid = exampleInternalSaksnummer,
                    ),
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = exampleModifiedByUser,
                type = Type.KLAGE,
                caseIsAtKA = null,
                fullmektigFoedselsnummer = null,
            ),
        )

        klankeRepository.save(
            Klanke(
                foedselsnummer = fnr,
                fritekst = exampleFritekst2,
                status = draftStatus,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                sak =
                    Sak(
                        sakstype = "FAGSAK",
                        fagsaksystem = "FS123",
                        fagsakid = exampleInternalSaksnummer,
                    ),
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = exampleModifiedByUser2,
                type = Type.KLAGE,
                caseIsAtKA = null,
                fullmektigFoedselsnummer = null,
            ),
        )
    }

    private fun createDBEntries() {
        var now = LocalDateTime.now()

        // innsendingsytelse and internalSaksnummer
        klankeRepository.save(
            Klanke(
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndInternalSaksnummer,
                status = draftStatus,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                sak =
                    Sak(
                        sakstype = "FAGSAK",
                        fagsaksystem = "FS123",
                        fagsakid = exampleInternalSaksnummer,
                    ),
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
                type = Type.KLAGE,
                caseIsAtKA = null,
                fullmektigFoedselsnummer = null,
            ),
        )

        now = LocalDateTime.now()

        // innsendingsytelse and no internalSaksnummer
        klankeRepository.save(
            Klanke(
                foedselsnummer = fnr,
                fritekst = innsendingsytelseAndNoInternalSaksnummer,
                status = draftStatus,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                sak = null,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
                type = Type.KLAGE,
                caseIsAtKA = null,
                fullmektigFoedselsnummer = null,
            ),
        )
    }

    private fun createDBEntryWithYtelse() {
        val now = LocalDateTime.now()
        klankeRepository.save(
            Klanke(
                foedselsnummer = fnr,
                fritekst = exampleFritekst,
                status = draftStatus,
                userSaksnummer = null,
                journalpostId = null,
                vedtakDate = null,
                sak = null,
                language = LanguageEnum.NB,
                innsendingsytelse = exampleInnsendingsytelse,
                hasVedlegg = false,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = now,
                modifiedByUser = now,
                type = Type.KLAGE,
                caseIsAtKA = null,
                fullmektigFoedselsnummer = null,
            ),
        )
    }
}

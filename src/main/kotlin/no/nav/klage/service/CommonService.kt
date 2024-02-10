package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.*
import java.time.*
import java.util.*

abstract class CommonService(
    val klankeRepository: KlankeRepository,
    private val fileClient: FileClient,
    private val validationService: ValidationService,
    private val kafkaInternalEventService: KafkaInternalEventService,
) {

    companion object {

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getKlanke(klankeId: UUID, bruker: Bruker): Klanke {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke, bruker = bruker)
        return klanke
    }

    fun validateAccess(klankeId: UUID, bruker: Bruker) {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = klanke, bruker = bruker)
    }

    fun getDraftKlankerByFnr(bruker: Bruker): List<Klanke> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        return klankeRepository.findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
    }

    private fun getLatestDraftKlageByParams(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
    ): Klanke? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val klanke =
            getLatestKlankeDraft(
                fnr = fnr,
                tema = tema,
                internalSaksnummer = internalSaksnummer,
                innsendingsytelse = innsendingsytelse,
            )
        return if (klanke != null) {
            validationService.validateKlankeAccess(klanke = klanke, bruker = bruker)
            klanke
        } else null
    }

    private fun getLatestKlankeDraft(
        fnr: String,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse
    ): Klanke? {
        return klankeRepository.findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }
    }

    fun getJournalpostId(klankeId: UUID, bruker: Bruker): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke, false)
        validationService.validateKlankeAccess(klanke, bruker)
        return klanke.journalpostId
    }

    fun updateFritekst(klankeId: UUID, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.fritekst = fritekst
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateUserSaksnummer(klankeId: UUID, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.userSaksnummer = userSaksnummer
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateVedtakDate(klankeId: UUID, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.vedtakDate = vedtakDate
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateJournalpostId(klankeId: UUID, journalpostId: String, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.journalpostId = journalpostId
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.journalpostId = journalpostId
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    private fun getAndValidateAccess(klankeId: UUID, bruker: Bruker): Klanke {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, bruker = bruker)
        return existingKlanke
    }

    fun updateHasVedlegg(klankeId: UUID, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.hasVedlegg = hasVedlegg
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateStatus(klankeId: UUID, status: KlageAnkeStatus, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.status = status
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateStatusWithoutValidation(klankeId: UUID, status: KlageAnkeStatus): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.status = status
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun deleteKlanke(klankeId: UUID, bruker: Bruker) {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.status = KlageAnkeStatus.DELETED
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

    fun getJournalpostIdWithoutValidation(klankeId: UUID): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        return klanke.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String) {
        updateJournalpostIdWithoutValidation(klankeId, journalpostId)
        kafkaInternalEventService.publishEvent(
            Event(
                klageAnkeId = klankeId.toString(),
                name = "journalpostId",
                id = klankeId.toString(),
                data = journalpostId,
            )
        )
    }

    fun setPdfDownloadedWithoutAccessValidation(klankeId: UUID, pdfDownloaded: LocalDateTime?) {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)

        existingKlanke.pdfDownloaded = pdfDownloaded
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

}

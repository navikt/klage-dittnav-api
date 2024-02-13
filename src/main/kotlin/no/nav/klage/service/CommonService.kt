package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Event
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

abstract class CommonService(
    private val klankeRepository: KlankeRepository,
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

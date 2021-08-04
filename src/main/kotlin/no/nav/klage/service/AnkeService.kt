package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.anke.AnkeView
import no.nav.klage.domain.anke.isLonnskompensasjon
import no.nav.klage.domain.anke.toAnke
import no.nav.klage.domain.ankevedlegg.toAnkeVedleggView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.repository.AnkeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
@Transactional
class AnkeService(
    private val ankeRepository: AnkeRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService,
    private val ankeVedleggService: AnkeVedleggService
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"
    }

    fun getAnke(ankeId: Int, bruker: Bruker): AnkeView {
        val anke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.toAnkeView(bruker, anke.status === KlageAnkeStatus.DRAFT)
    }

    fun createAnke(anke: AnkeView, bruker: Bruker): AnkeView {
        if (anke.fullmaktsgiver != null) {
            brukerService.verifyFullmakt(anke.tema, anke.fullmaktsgiver)
        }

        return ankeRepository
            .createAnke(anke.toAnke(bruker, KlageAnkeStatus.DRAFT))
            .toAnkeView(bruker)
            .also {
                val temaReport = if (anke.isLonnskompensasjon()) {
                    LOENNSKOMPENSASJON_GRAFANA_TEMA
                } else {
                    anke.tema.toString()
                }
                klageAnkeMetrics.incrementAnkerInitialized(temaReport)
            }
    }

    fun updateAnke(anke: AnkeView, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeById(anke.id)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        ankeRepository
            .updateAnke(anke.toAnke(bruker))
            .toAnkeView(bruker, false)
    }

    fun getLatestDraftAnkeByParams(
        bruker: Bruker,
        internalSaksnummer: String,
        fullmaktsgiver: String?
    ): AnkeView {
        val fnr = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer

        val anke =
            ankeRepository.getLatestDraftAnkeByFnrAndInternalSaksnummer(
                fnr,
                internalSaksnummer
            )
        if (anke != null) {
            validationService.validateAnkeAccess(anke, bruker)
            return anke.toAnkeView(bruker, false)
        }
        throw AnkeNotFoundException()
    }

    fun getJournalpostId(ankeId: Int, bruker: Bruker): String? {
        val anke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.journalpostId
    }

    fun deleteAnke(ankeId: Int, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        ankeRepository.deleteAnke(ankeId)
    }

    fun Anke.toAnkeView(bruker: Bruker, expandAnkeVedleggToAnkeVedleggView: Boolean = true): AnkeView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return AnkeView(
            id!!,
            fritekst,
            tema,
            status,
            modifiedDateTime,
            vedlegg.map {
                if (expandAnkeVedleggToAnkeVedleggView) {
                    ankeVedleggService.expandAnkeVedleggToAnkeVedleggView(
                        it,
                        bruker
                    )
                } else {
                    it.toAnkeVedleggView("")
                }
            },
            journalpostId,
            finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedDateTime.toLocalDate() else null,
            vedtakDate = vedtakDate,
            internalSaksnummer = internalSaksnummer,
            fullmaktsgiver = fullmektig?.let { foedselsnummer },
            language = language,
            titleKey = titleKey,
            ytelse = titleKey.nb
        )
    }

    fun getAnkePdf(ankeId: Int, bruker: Bruker): ByteArray {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        requireNotNull(existingAnke.journalpostId)
        return fileClient.getKlageAnkeFile(existingAnke.journalpostId)
    }


}
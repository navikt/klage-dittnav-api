package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.anke.AnkeView
import no.nav.klage.domain.anke.NewAnkeRequest
import no.nav.klage.domain.anke.toAnke
import no.nav.klage.domain.ankevedlegg.toAnkeVedleggView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.AvailableAnkeNotFoundException
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
    private val ankeVedleggService: AnkeVedleggService,
    private val availableAnkeService: AvailableAnkeService
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"
    }

    fun getAnke(internalSaksnummer: String, bruker: Bruker): AnkeView {
        val anke = ankeRepository.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.toAnkeView(bruker, anke.status === KlageAnkeStatus.DRAFT)
    }

    fun createAnke(input: NewAnkeRequest, bruker: Bruker): AnkeView {
        try {
            val existingAnke = ankeRepository.getAnkeByInternalSaksnummer(input.ankeInternalSaksnummer)
            return existingAnke.toAnkeView(bruker)
        } catch (e: AnkeNotFoundException) {
            val availableAnke = availableAnkeService.getAvailableAnke(input.ankeInternalSaksnummer, bruker)
            val anke = Anke(
                foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
                fritekst = "",
                tema = availableAnke.tema,
                vedtakDate = availableAnke.vedtakDate,
                internalSaksnummer = availableAnke.internalSaksnummer,
                language = input.language
            )

            return ankeRepository
                .createAnke(anke)
                .toAnkeView(bruker)
                .also {
                    klageAnkeMetrics.incrementAnkerInitialized(anke.tema.toString())
                }
        }
    }

    fun updateAnke(anke: AnkeView, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeByInternalSaksnummer(anke.ankeInternalSaksnummer)
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

    fun getJournalpostId(internalSaksnummer: String, bruker: Bruker): String? {
        val anke = ankeRepository.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.journalpostId
    }

    fun deleteAnke(internalSaksnummer: String, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        ankeRepository.deleteAnke(internalSaksnummer)
    }

    fun Anke.toAnkeView(bruker: Bruker, expandAnkeVedleggToAnkeVedleggView: Boolean = true): AnkeView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return AnkeView(
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
            ankeInternalSaksnummer = internalSaksnummer,
            fullmaktsgiver = fullmektig?.let { foedselsnummer },
            language = language
        )
    }

    fun getAnkePdf(internalSaksnummer: String, bruker: Bruker): ByteArray {
        val existingAnke = ankeRepository.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        requireNotNull(existingAnke.journalpostId)
        return fileClient.getKlageAnkeFile(existingAnke.journalpostId)
    }


}
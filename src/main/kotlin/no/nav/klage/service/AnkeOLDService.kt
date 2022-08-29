package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.ankeOLD.AnkeOLD
import no.nav.klage.domain.ankeOLD.AnkeOLDView
import no.nav.klage.domain.ankeOLD.NewAnkeOLDRequest
import no.nav.klage.domain.ankeOLD.toAnke
import no.nav.klage.domain.ankevedleggOLD.toAnkeVedleggView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.repository.AnkeRepositoryOLD
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*

@Service
@Transactional
class AnkeOLDService(
    private val ankeRepositoryOLD: AnkeRepositoryOLD,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService,
    private val ankeOLDVedleggService: AnkeOLDVedleggService,
    private val availableAnkeService: AvailableAnkeService
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"
    }

    fun getAnke(internalSaksnummer: String, bruker: Bruker): AnkeOLDView {
        val anke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatusOLD(anke, false)
        validationService.validateAnkeAccessOLD(anke, bruker)
        return anke.toAnkeView(bruker, anke.status === KlageAnkeStatus.DRAFT)
    }

    fun createAnke(input: NewAnkeOLDRequest, bruker: Bruker): AnkeOLDView {
        try {
            val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(input.ankeInternalSaksnummer)
            return existingAnke.toAnkeView(bruker)
        } catch (e: AnkeNotFoundException) {
            val availableAnke = availableAnkeService.getAvailableAnke(input.ankeInternalSaksnummer, bruker)
            val ankeOLD = AnkeOLD(
                foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
                fritekst = "",
                tema = availableAnke.tema,
                vedtakDate = availableAnke.vedtakDate,
                internalSaksnummer = availableAnke.internalSaksnummer,
                language = input.language
            )

            return ankeRepositoryOLD
                .createAnke(ankeOLD)
                .toAnkeView(bruker)
                .also {
                    klageAnkeMetrics.incrementAnkerInitialized(ankeOLD.tema.toString())
                }
        }
    }

    fun updateAnke(anke: AnkeOLDView, bruker: Bruker) {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(anke.ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        ankeRepositoryOLD
            .updateAnke(anke.toAnke(bruker))
            .toAnkeView(bruker, false)
    }

    fun updateFritekst(ankeInternalSaksnummer: String, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        return ankeRepositoryOLD
            .updateFritekst(ankeInternalSaksnummer, fritekst)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun updateVedtakDate(ankeInternalSaksnummer: String, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        return ankeRepositoryOLD
            .updateVedtakDate(ankeInternalSaksnummer, vedtakDate)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun getLatestDraftAnkeByParams(
        bruker: Bruker,
        internalSaksnummer: String,
        fullmaktsgiver: String?
    ): AnkeOLDView {
        val fnr = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer

        val anke =
            ankeRepositoryOLD.getLatestDraftAnkeByFnrAndInternalSaksnummer(
                fnr,
                internalSaksnummer
            )
        if (anke != null) {
            validationService.validateAnkeAccessOLD(anke, bruker)
            return anke.toAnkeView(bruker, false)
        }
        throw AnkeNotFoundException()
    }

    fun getJournalpostId(internalSaksnummer: String, bruker: Bruker): String? {
        val anke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatusOLD(anke, false)
        validationService.validateAnkeAccessOLD(anke, bruker)
        return anke.journalpostId
    }

    fun deleteAnke(internalSaksnummer: String, bruker: Bruker) {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        ankeRepositoryOLD.deleteAnke(internalSaksnummer)
    }

    fun AnkeOLD.toAnkeView(bruker: Bruker, expandAnkeVedleggToAnkeVedleggView: Boolean = true): AnkeOLDView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return AnkeOLDView(
            fritekst,
            tema,
            status,
            modifiedDateTime,
            vedlegg.map {
                if (expandAnkeVedleggToAnkeVedleggView) {
                    ankeOLDVedleggService.expandAnkeVedleggToAnkeVedleggView(
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
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke, false)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        requireNotNull(existingAnke.journalpostId)
        return fileClient.getKlageAnkeFile(existingAnke.journalpostId)
    }


}
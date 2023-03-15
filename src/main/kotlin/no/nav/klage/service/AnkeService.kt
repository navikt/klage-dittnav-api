package no.nav.klage.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenAnkeInput
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Event
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.*
import no.nav.klage.domain.exception.KlageIsFinalizedException
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.util.*

@Service
@Transactional
class AnkeService(
    private val ankeRepository: AnkeRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val vedleggService: VedleggService,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService,
    private val kafkaInternalEventService: KafkaInternalEventService,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        val objectMapper: ObjectMapper = jacksonObjectMapper()
    }

    fun getAnke(ankeId: UUID, bruker: Bruker): AnkeView {
        val anke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.toAnkeView(bruker = bruker)
    }

    fun validateAccess(ankeId: UUID, bruker: Bruker) {
        val anke = ankeRepository.getAnkeById(ankeId)
        validationService.validateAnkeAccess(anke, bruker)
    }

    fun getDraftAnkerByFnr(bruker: Bruker): List<AnkeView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val anker = ankeRepository.getDraftAnkerByFnr(fnr)
        return anker.map { it.toAnkeView(bruker) }
    }

    private fun getLatestDraftAnkeByParams(
        bruker: Bruker,
        innsendingsytelse: Innsendingsytelse,
    ): AnkeView? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val anke =
            ankeRepository.getLatestAnkeDraft(
                fnr = fnr,
                innsendingsytelse = innsendingsytelse
            )
        if (anke != null) {
            validationService.validateAnkeAccess(anke, bruker)
            return anke.toAnkeView(bruker)
        }
        return null
    }

    private fun createAnkeViewAndUpdateMetrics(input: Anke, bruker: Bruker): AnkeView {
        return input.toAnkeView(bruker)
            .also {
                val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
                    LOENNSKOMPENSASJON_GRAFANA_TEMA
                } else {
                    input.innsendingsytelse.toTema().toString()
                }
                klageAnkeMetrics.incrementAnkerInitialized(temaReport)
            }
    }

    fun createAnke(input: AnkeFullInput, bruker: Bruker): AnkeView {
        return createAnkeViewAndUpdateMetrics(
            input = ankeRepository.createAnke(ankeFullInput = input, bruker = bruker),
            bruker = bruker
        )
    }

    fun createAnke(input: AnkeInput, bruker: Bruker): AnkeView {
        return createAnkeViewAndUpdateMetrics(
            input = ankeRepository.createAnke(ankeInput = input, bruker = bruker),
            bruker = bruker
        )
    }

    fun getDraftOrCreateAnke(input: AnkeInput, bruker: Bruker): AnkeView {
        val existingAnke = getLatestDraftAnkeByParams(
            bruker = bruker,
            innsendingsytelse = input.innsendingsytelse,
        )

        return existingAnke ?: createAnke(
            input = input,
            bruker = bruker,
        )
    }

    fun updateFritekst(ankeId: UUID, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateFritekst(ankeId, fritekst)
            .toAnkeView(bruker)
            .modifiedByUser
    }

    fun updateUserSaksnummer(ankeId: UUID, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateUserSaksnummer(ankeId, userSaksnummer)
            .toAnkeView(bruker)
            .modifiedByUser
    }

    fun updateEnhetsnummer(ankeId: UUID, enhetsnummer: String?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateEnhetsnummer(ankeId, enhetsnummer)
            .toAnkeView(bruker)
            .modifiedByUser
    }

    fun updateVedtakDate(ankeId: UUID, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateVedtakDate(ankeId, vedtakDate)
            .toAnkeView(bruker)
            .modifiedByUser
    }

    fun updateHasVedlegg(ankeId: UUID, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateHasVedlegg(ankeId, hasVedlegg)
            .toAnkeView(bruker)
            .modifiedByUser
    }

    fun deleteAnke(ankeId: UUID, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        ankeRepository.updateStatus(ankeId, KlageAnkeStatus.DELETED)
    }

    fun createAnkePdfWithFoersteside(ankeId: UUID, bruker: Bruker): ByteArray? {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        validationService.validateAnke(existingAnke)

        klageDittnavPdfgenService.createAnkePdfWithFoersteside(
            createPdfWithFoerstesideInput(existingAnke, bruker)
        ).also {
            setPdfDownloadedWithoutAccessValidation(ankeId, Instant.now())
            return it
        }
    }

    private fun setPdfDownloadedWithoutAccessValidation(ankeId: UUID, pdfDownloaded: Instant?) {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        ankeRepository.updatePdfDownloaded(ankeId, pdfDownloaded)
    }

    fun Anke.toAnkeView(bruker: Bruker): AnkeView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return AnkeView(
            id = id.toString(),
            fritekst = fritekst ?: "",
            status = status,
            modifiedByUser = modifiedDateTime,
            vedtakDate = vedtakDate,
            userSaksnummer = userSaksnummer,
            language = language,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = hasVedlegg,
            enhetsnummer = enhetsnummer,
            vedlegg = vedlegg.map { it.toVedleggView() },
            journalpostId = journalpostId,
            finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedDateTime.toLocalDate() else null,
            internalSaksnummer = internalSaksnummer,
        )
    }

    fun createPdfWithFoerstesideInput(anke: Anke, bruker: Bruker): OpenAnkeInput {
        return OpenAnkeInput(
            foedselsnummer = anke.foedselsnummer,
            navn = bruker.navn,
            fritekst = anke.fritekst!!,//should already be validated
            userSaksnummer = anke.userSaksnummer,
            internalSaksnummer = anke.internalSaksnummer,
            vedtakDate = anke.vedtakDate,
            innsendingsytelse = anke.innsendingsytelse,
            enhetsnummer = anke.enhetsnummer!!,//should already be validated
            language = anke.language,
            hasVedlegg = anke.vedlegg.isNotEmpty() || anke.hasVedlegg,
        )
    }

    fun getJournalpostId(ankeId: UUID, bruker: Bruker): String? {
        val anke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(anke, false)
        validationService.validateAnkeAccess(anke, bruker)
        return anke.journalpostId
    }

    fun setJournalpostIdWithoutValidation(ankeId: UUID, journalpostId: String) {
        ankeRepository.updateJournalpostId(ankeId, journalpostId)
        kafkaInternalEventService.publishEvent(
            Event(
                klageAnkeId = ankeId.toString(),
                name = "journalpostId",
                id = ankeId.toString(),
                data = journalpostId,
            )
        )
    }

    fun finalizeAnke(ankeId: UUID, bruker: Bruker): Instant {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(
            anke = existingAnke,
            includeFinalized = false
        )

        if (existingAnke.isFinalized()) {
            return existingAnke.modifiedByUser
                ?: throw KlageIsFinalizedException("No modified date after finalize anke")
        }

        validationService.validateAnkeAccess(existingAnke, bruker)
        validationService.validateAnke(existingAnke)

        val updatedAnke = ankeRepository.updateStatus(ankeId, KlageAnkeStatus.DONE)
        kafkaProducer.sendToKafka(createAggregatedAnke(bruker, updatedAnke))
        registerFinalizedMetrics(updatedAnke)

        val ankeIdAsString = ankeId.toString()
        logger.debug(
            "Anke {} med tema {} er sendt inn",
            ankeIdAsString,
            existingAnke.tema.name,
        )

        //TODO new exception
        return updatedAnke.modifiedByUser ?: throw KlageIsFinalizedException("No modified date after finalize anke")
    }

    private fun registerFinalizedMetrics(anke: Anke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(anke.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            anke.tema.toString()
        }
        klageAnkeMetrics.incrementAnkerFinalized(temaReport)

        if (anke.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (anke.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }
        vedleggMetrics.registerNumberOfVedleggPerUser(anke.vedlegg.size.toDouble())
    }

    private fun createAggregatedAnke(
        bruker: Bruker,
        anke: Anke,
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(anke.vedtakDate)

        return AggregatedKlageAnke(
            id = anke.id.toString(),
            fornavn = bruker.navn.fornavn,
            mellomnavn = bruker.navn.mellomnavn ?: "",
            etternavn = bruker.navn.etternavn,
            vedtak = vedtak ?: "",
            dato = ZonedDateTime.ofInstant(anke.modifiedByUser, ZoneOffset.UTC).toLocalDate(),
            begrunnelse = sanitizeText(anke.fritekst!!),
            identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = anke.tema.name,
            ytelse = anke.innsendingsytelse.nb,
            vedlegg = anke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userSaksnummer = anke.userSaksnummer,
            internalSaksnummer = anke.internalSaksnummer,
            enhetsnummer = anke.enhetsnummer,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.ANKE,
        )
    }

    fun getAnkePdf(ankeId: UUID, bruker: Bruker): ByteArray {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        requireNotNull(existingAnke.journalpostId)
        return fileClient.getKlageAnkeFile(existingAnke.journalpostId)
    }

    fun getJournalpostIdWithoutValidation(ankeId: UUID): String? {
        val anke = ankeRepository.getAnkeById(ankeId)
        return anke.journalpostId
    }

}

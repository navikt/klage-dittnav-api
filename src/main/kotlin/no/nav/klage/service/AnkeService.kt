package no.nav.klage.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenAnkeInput
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.*
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.util.getLogger
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
        return anke.toAnkeView(bruker, anke.status === KlageAnkeStatus.DRAFT)
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

    fun getLatestDraftAnkeByParams(
        bruker: Bruker,
        titleKey: TitleEnum,
    ): AnkeView? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val anke =
            ankeRepository.getLatestDraftAnkeByFnrTitleKey(
                fnr,
                titleKey
            )
        if (anke != null) {
            validationService.validateAnkeAccess(anke, bruker)
            return anke.toAnkeView(bruker, true)
        }
        return null
    }

    fun createAnke(anke: AnkeFullInput, bruker: Bruker): AnkeView {
        return ankeRepository
            .createAnke(anke.toAnke(bruker))
            .toAnkeView(bruker)
    }

    fun createAnke(input: AnkeInput, bruker: Bruker): AnkeView {
        return ankeRepository
            .createAnke(input.toAnke(bruker))
            .toAnkeView(bruker)
    }

    fun getDraftOrCreateAnke(input: AnkeInput, bruker: Bruker): AnkeView {
        val existingAnke = getLatestDraftAnkeByParams(
            bruker = bruker,
            titleKey = input.titleKey,
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
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun updateUserSaksnummer(ankeId: UUID, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateUserSaksnummer(ankeId, userSaksnummer)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun updateEnhetsnummer(ankeId: UUID, enhetsnummer: String?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateEnhetsnummer(ankeId, enhetsnummer)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun updateVedtakDate(ankeId: UUID, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateVedtakDate(ankeId, vedtakDate)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun updateHasVedlegg(ankeId: UUID, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankeRepository
            .updateHasVedlegg(ankeId, hasVedlegg)
            .toAnkeView(bruker, false)
            .modifiedByUser
    }

    fun deleteAnke(ankeId: UUID, bruker: Bruker) {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        ankeRepository.deleteAnke(ankeId)
    }

    fun createAnkePdfWithFoersteside(ankeId: UUID, bruker: Bruker): ByteArray? {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        validationService.validateAnke(existingAnke)

        return klageDittnavPdfgenService.createAnkePdfWithFoersteside(
            createPdfWithFoerstesideInput(existingAnke, bruker)
        )
    }

    fun Anke.toAnkeView(bruker: Bruker, expandVedleggToVedleggView: Boolean = true): AnkeView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return AnkeView(
            id = id.toString(),
            fritekst = fritekst,
            tema = tema,
            status = status,
            modifiedByUser = modifiedDateTime,
            vedtakDate = vedtakDate,
            userSaksnummer = userSaksnummer,
            language = language,
            titleKey = titleKey,
            hasVedlegg = hasVedlegg,
        )
    }

    fun createPdfWithFoerstesideInput(anke: Anke, bruker: Bruker): OpenAnkeInput {
        return OpenAnkeInput(
            foedselsnummer = anke.foedselsnummer,
            navn = bruker.navn,
            fritekst = anke.fritekst!!,//should already be validated
            userSaksnummer = anke.userSaksnummer,
            vedtakDate = anke.vedtakDate,
            titleKey = anke.titleKey,
            tema = anke.tema,
            enhetsnummer = anke.enhetsnummer!!,//should already be validated
            language = anke.language,
            hasVedlegg = anke.hasVedlegg,
        )
    }

}

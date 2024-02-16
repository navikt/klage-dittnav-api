package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.controller.view.*
import no.nav.klage.domain.anke.AnkeFullInput
import no.nav.klage.domain.anke.AnkeInput
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.domain.toHeartBeatServerSentEvent
import no.nav.klage.domain.toServerSentEvent
import no.nav.klage.service.AnkeService
import no.nav.klage.service.BrukerService
import no.nav.klage.service.CommonService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*

@RestController
@Tag(name = "anker")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api/anker")
class AnkeController(
    private val brukerService: BrukerService,
    private val ankeService: AnkeService,
    private val vedleggService: VedleggService,
    private val kafkaEventClient: KafkaEventClient,
    private val commonService: CommonService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping
    fun getAnker(): List<AnkeView> {
        val bruker = brukerService.getBruker()
        logger.debug("Get anker for user is requested.")
        secureLogger.debug(
            "Get anker for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getAnkeDraftsByFnr(bruker).map {
            it.toAnkeView()
        }
    }

    @GetMapping("/{ankeId}")
    fun getAnke(
        @PathVariable ankeId: UUID
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Get anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return (commonService.getKlanke(ankeId, bruker) as Anke).toAnkeView()
    }

    @GetMapping("/{ankeId}/journalpostid")
    fun getJournalpostId(
        @PathVariable ankeId: UUID
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. AnkeId: {}", ankeId)
        secureLogger.debug(
            "Get journalpost id is requested. AnkeId: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return commonService.getJournalpostId(ankeId, bruker)
    }

    @GetMapping("/{ankeId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable ankeId: UUID
    ): Flux<ServerSentEvent<String>> {
        val bruker = brukerService.getBruker()
        kotlin.runCatching {
            commonService.validateAccess(ankeId, bruker)
        }.onFailure {
            throw AnkeNotFoundException()
        }
        logger.debug("Journalpostid events called for ankeId: {}", ankeId)
        //https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream: Flux<ServerSentEvent<String>> = Flux.interval(Duration.ofSeconds(10))
            .takeWhile { true }
            .map { tick -> tick.toHeartBeatServerSentEvent() }

        return kafkaEventClient.getEventPublisher()
            .mapNotNull { event -> jsonToEvent(event.data()) }
            .filter { it.klageAnkeId == ankeId.toString() }
            .mapNotNull { it.toServerSentEvent() }
            .mergeWith(heartbeatStream)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnke(
        @RequestBody anke: AnkeFullInput, response: HttpServletResponse
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create anke is requested.")
        secureLogger.debug(
            "Create anke is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.createAnke(anke, bruker).toAnkeView()
    }

    @PutMapping
    fun createOrGetAnke(
        @RequestBody ankeInput: AnkeInput,
        response: HttpServletResponse
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create or update anke for user is requested.")
        secureLogger.debug(
            "Create or update anke for user is requested. Fnr: {}, innsendingsytelse: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            ankeInput.innsendingsytelse
        )

        return ankeService.getDraftOrCreateAnke(ankeInput, bruker).toAnkeView()
    }

    @PutMapping("/{ankeId}/fritekst")
    fun updateFritekst(
        @PathVariable ankeId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke fritekst is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke fritekst is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateFritekst(ankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{ankeId}/usersaksnummer")
    fun updateUserSaksnummer(
        @PathVariable ankeId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke userSaksnummer is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke userSaksnummer is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateUserSaksnummer(ankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{ankeId}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable ankeId: UUID,
        @RequestBody input: DateInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke vedtakDate is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke vedtakDate is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateVedtakDate(ankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{ankeId}/hasvedlegg")
    fun updateHasVedlegg(
        @PathVariable ankeId: UUID,
        @RequestBody input: BooleanInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke hasvedlegg is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke hasvedlegg is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateHasVedlegg(ankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{ankeId}/enhetsnummer")
    fun updateEnhetsnummer(
        @PathVariable ankeId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke enhetsnummer is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke enhetsnummer is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = ankeService.updateEnhetsnummer(ankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @DeleteMapping("/{ankeId}")
    fun deleteAnke(@PathVariable ankeId: UUID) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Delete anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        commonService.deleteKlanke(ankeId, bruker)
    }

    @PostMapping("/{ankeId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeAnke(
        @PathVariable ankeId: UUID
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Finalize anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val finalizedLocalDateTime = ankeService.finalizeAnke(ankeId = ankeId, bruker = bruker)
        return mapOf(
            "finalizedDate" to finalizedLocalDateTime.toLocalDate().toString(),
            "modifiedByUser" to finalizedLocalDateTime.toString()
        )
    }

    @PostMapping(value = ["/{ankeId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToAnke(
        @PathVariable ankeId: UUID,
        @RequestParam vedlegg: MultipartFile
    ): VedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to klage is requested. AnkeId: {}", ankeId)
        secureLogger.debug(
            "Add Vedlegg to anke is requested. AnkeId: {}, fnr: {} ",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return vedleggService.addAnkevedlegg(
            ankeId = ankeId,
            multipart = vedlegg,
            bruker = bruker
        ).toVedleggView()
    }

    @DeleteMapping("/{ankeId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable ankeId: UUID,
        @PathVariable vedleggId: UUID
    ) {
        val bruker = brukerService.getBruker()
        logger.debug(
            "Delete vedlegg from anke is requested. AnkeId: {}, VedleggId: {}",
            ankeId,
            vedleggId
        )
        secureLogger.debug(
            "Delete vedlegg from anke is requested. AnkeId: {}, vedleggId: {}, fnr: {} ",
            ankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!vedleggService.deleteVedleggFromKlanke(
                klankeId = ankeId,
                vedleggId = vedleggId,
                bruker = bruker
            )
        ) {
            //TODO is there a reason for this choice of exception?
            throw AnkeNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{ankeId}/vedlegg/{vedleggId}")
    fun getVedleggFromAnke(
        @PathVariable ankeId: UUID,
        @PathVariable vedleggId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug(
            "Get vedlegg to anke is requested. AnkeId: {} - VedleggId: {}",
            ankeId,
            vedleggId
        )
        secureLogger.debug(
            "Vedlegg from anke is requested. AnkeId: {}, vedleggId: {}, fnr: {} ",
            ankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = vedleggService.getVedleggFromAnke(ankeId = ankeId, vedleggId = vedleggId, bruker = bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "vedlegg.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }

    @ResponseBody
    @GetMapping("/{ankeId}/pdf")
    fun getAnkePdf(
        @PathVariable ankeId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke pdf is requested. KlageId: {}", ankeId)
        secureLogger.debug(
            "Get anke-pdf is requested. AnkeId: {}, fnr: {} ",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = ankeService.getAnkePdf(ankeId = ankeId, bruker = bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "anke.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }

    @ResponseBody
    @GetMapping("/{ankeId}/pdf/innsending")
    fun getAnkePdfForPrint(
        @PathVariable ankeId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke pdf for print is requested. AnkeId: {}", ankeId)
        secureLogger.debug(
            "Get anke pdf for print is requested. AnkeId: {}, fnr: {} ",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = ankeService.createAnkePdfWithFoersteside(ankeId, bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "anke.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }
}

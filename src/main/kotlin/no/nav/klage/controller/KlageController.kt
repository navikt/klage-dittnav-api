package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.controller.view.*
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.klage.KlageFullInput
import no.nav.klage.domain.klage.KlageInput
import no.nav.klage.controller.view.KlageView
import no.nav.klage.controller.view.VedleggView
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.service.BrukerService
import no.nav.klage.service.CommonService
import no.nav.klage.service.KlageService
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
import java.util.UUID

@RestController
@Tag(name = "klager")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api/klager")
class KlageController(
    private val brukerService: BrukerService,
    private val klageService: KlageService,
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
    fun getKlager(): List<KlageView> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klager for user is requested.")
        secureLogger.debug(
            "Get klager for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getKlageDraftsByFnr(bruker).map {
            it.toKlageView()
        }
    }

    @GetMapping("/{klageId}")
    fun getKlage(
        @PathVariable klageId: UUID
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Get klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return (commonService.getKlanke(klageId, bruker) as Klage).toKlageView()
    }

    @GetMapping("/{klageId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klageId: UUID
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get journalpost id is requested. KlageId: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return commonService.getJournalpostId(klageId, bruker)
    }

    @GetMapping("/{klageId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable klageId: UUID
    ): Flux<ServerSentEvent<String>> {
        val bruker = brukerService.getBruker()
        kotlin.runCatching {
            commonService.validateAccess(klageId, bruker)
        }.onFailure {
            throw KlageNotFoundException()
        }
        logger.debug("Journalpostid events called for klageId: $klageId")
        //https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream: Flux<ServerSentEvent<String>> = Flux.interval(Duration.ofSeconds(10))
            .takeWhile { true }
            .map { tick -> tick.toHeartBeatServerSentEvent() }

        return kafkaEventClient.getEventPublisher()
            .mapNotNull { event -> jsonToEvent(event.data()) }
            .filter { it.klageAnkeId == klageId.toString() }
            .mapNotNull { it.toServerSentEvent() }
            .mergeWith(heartbeatStream)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlage(
        @RequestBody klage: KlageFullInput, response: HttpServletResponse
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Create klage is requested.")
        secureLogger.debug(
            "Create klage is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.createKlage(klage, bruker).toKlageView()
    }

    @PutMapping
    fun createOrGetKlage(
        @RequestBody klageInput: KlageInput,
        response: HttpServletResponse
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Create or update klage for user is requested.")
        secureLogger.debug(
            "Create or update klage for user is requested. Fnr: {}, innsendingsytelse: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            klageInput.innsendingsytelse
        )

        return klageService.getDraftOrCreateKlage(klageInput, bruker).toKlageView()
    }

    @PutMapping("/{klageId}/fritekst")
    fun updateFritekst(
        @PathVariable klageId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage fritekst is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage fritekst is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateFritekst(klageId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klageId}/usersaksnummer")
    fun updateUserSaksnummer(
        @PathVariable klageId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage userSaksnummer is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage userSaksnummer is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateUserSaksnummer(klageId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klageId}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable klageId: UUID,
        @RequestBody input: DateInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage vedtakDate is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage vedtakDate is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateVedtakDate(klageId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klageId}/hasvedlegg")
    fun updateHasVedlegg(
        @PathVariable klageId: UUID,
        @RequestBody input: BooleanInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage hasVedlegg is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage hasVedlegg is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateHasVedlegg(klageId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klageId}/checkboxesselected")
    fun updateCheckboxesSelected(
        @PathVariable klageId: UUID,
        @RequestBody input: CheckboxesSelectedInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage checkboxesSelected is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage checkboxesSelected is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = klageService.updateCheckboxesSelected(
            klageId = klageId,
            checkboxesSelected = input.value,
            bruker = bruker
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @DeleteMapping("/{klageId}")
    fun deleteKlage(@PathVariable klageId: UUID) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Delete klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        commonService.deleteKlanke(klankeId = klageId, bruker = bruker)
    }

    @PostMapping("/{klageId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlage(
        @PathVariable klageId: UUID
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Finalize klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val finalizedLocalDateTime = klageService.finalizeKlage(klageId, bruker)
        return mapOf(
            "finalizedDate" to finalizedLocalDateTime.toLocalDate().toString(),
            "modifiedByUser" to finalizedLocalDateTime.toString()
        )
    }

    @PostMapping(value = ["/{klageId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlage(
        @PathVariable klageId: UUID,
        @RequestParam vedlegg: MultipartFile
    ): VedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to klage is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Add Vedlegg to klage is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return vedleggService.addKlagevedlegg(
            klageId = klageId,
            multipart = vedlegg,
            bruker = bruker
        ).toVedleggView()
    }

    @DeleteMapping("/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: UUID,
        @PathVariable vedleggId: UUID
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete vedlegg from klage is requested. KlageId: {}, VedleggId: {}", klageId, vedleggId)
        secureLogger.debug(
            "Delete vedlegg from klage is requested. KlageId: {}, vedleggId: {}, fnr: {} ",
            klageId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!vedleggService.deleteVedleggFromKlage(klageId, vedleggId, bruker)) {
            throw KlageNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{klageId}/vedlegg/{vedleggId}")
    fun getVedleggFromKlage(
        @PathVariable klageId: UUID,
        @PathVariable vedleggId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to klage is requested. KlageId: {} - VedleggId: {}", klageId, vedleggId)
        secureLogger.debug(
            "Vedlegg from klage is requested. KlageId: {}, vedleggId: {}, fnr: {} ",
            klageId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = vedleggService.getVedleggFromKlage(klageId, vedleggId, bruker)

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
    @GetMapping("/{klageId}/pdf")
    fun getKlagePdf(
        @PathVariable klageId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage pdf is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get klage pdf is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = klageService.getKlagePdf(klageId, bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "klage.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }

    @ResponseBody
    @GetMapping("/{klageId}/pdf/innsending")
    fun getKlagePdfForPrint(
        @PathVariable klageId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage pdf for print is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get klage pdf for print is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = klageService.createKlagePdfWithFoersteside(klageId, bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "klage.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }

}

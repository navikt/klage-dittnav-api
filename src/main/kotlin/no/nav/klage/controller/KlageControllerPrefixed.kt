package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.exception.UpdateMismatchException
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.domain.klage.KlageInput
import no.nav.klage.domain.klage.KlageViewIdAsString
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.domain.toHeartBeatServerSentEvent
import no.nav.klage.domain.toServerSentEvent
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.service.BrukerService
import no.nav.klage.service.KlageService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.servlet.http.HttpServletResponse

@RestController
@Tag(name = "klager-prefixed")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api/klager")
class KlageControllerPrefixed(
    private val brukerService: BrukerService,
    private val klageService: KlageService,
    private val vedleggService: VedleggService,
    private val kafkaEventClient: KafkaEventClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping
    fun getKlager(): List<KlageViewIdAsString> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klager for user is requested.")
        secureLogger.debug(
            "Get klager for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getDraftKlagerByFnrIdAsString(bruker)
    }

    /**
     * Get possible draft. Might be null.
     */
    @GetMapping("/draft")
    fun getDraftKlageByQuery(
        @RequestParam tema: Tema,
        @RequestParam ytelse: String?,
        @RequestParam internalSaksnummer: String?,
        @RequestParam fullmaktsgiver: String?,
        @RequestParam titleKey: TitleEnum?
    ): KlageViewIdAsString? {
        val bruker = brukerService.getBruker()
        logger.debug("Get draft klage for user is requested.")
        secureLogger.debug(
            "Get draft klage for user is requested. Fnr: {}, tema: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema
        )
        return klageService.getLatestDraftKlageByParamsIdAsString(
            bruker,
            tema,
            internalSaksnummer,
            fullmaktsgiver,
            titleKey,
            ytelse
        )
    }

    @GetMapping("/{klageId}")
    fun getKlage(
        @PathVariable klageId: String
    ): KlageViewIdAsString {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Get klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getKlageIdAsString(klageId.toInt(), bruker)
    }

    @GetMapping("/{klageId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klageId: String
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get journalpost id is requested. KlageId: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getJournalpostId(klageId.toInt(), bruker)
    }

    @Unprotected
    @GetMapping("/{klageId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable klageId: String
    ): Flux<ServerSentEvent<String>> {
        val bruker = brukerService.getBruker()
        kotlin.runCatching {
            klageService.validateAccess(Integer.valueOf(klageId), bruker)
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
            .filter { it.klageId == klageId }
            .mapNotNull { it.toServerSentEvent() }
            .mergeWith(heartbeatStream)
            .also {
                logger.debug("event stream debug: {}", it)
            }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlage(
        @RequestBody klage: KlageViewIdAsString, response: HttpServletResponse
    ): KlageViewIdAsString {
        val bruker = brukerService.getBruker()
        logger.debug("Create klage is requested.")
        secureLogger.debug(
            "Create klage is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.createKlageIdAsString(klage, bruker)
    }

    @PutMapping("/{klageId}")
    fun updateKlage(
        @PathVariable klageId: String,
        @RequestBody klage: KlageViewIdAsString,
        response: HttpServletResponse
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Update klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Update klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (klage.id != klageId) {
            throw UpdateMismatchException("Id in klage does not match resource id")
        }
        klageService.updateKlageIdAsString(klage, bruker)
    }

    @PutMapping()
    fun createOrGetKlage(
        @RequestBody klageInput: KlageInput,
        response: HttpServletResponse
    ): KlageViewIdAsString {
        val bruker = brukerService.getBruker()
        logger.debug("Create or update klage for user is requested.")
        secureLogger.debug(
            "Create or update klage for user is requested. Fnr: {}, tema: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            klageInput.tema
        )

        return klageService.getDraftOrCreateKlage(klageInput, bruker)
    }

    @DeleteMapping("/{klageId}")
    fun deleteKlage(@PathVariable klageId: String) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Delete klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        klageService.deleteKlage(klageId.toInt(), bruker)
    }

    @PostMapping("/{klageId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlage(
        @PathVariable klageId: String
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Finalize klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val finalizedInstant = klageService.finalizeKlage(klageId.toInt(), bruker)
        val zonedDateTime = ZonedDateTime.ofInstant(finalizedInstant, ZoneId.of("Europe/Oslo"))
        return mapOf(
            "finalizedDate" to zonedDateTime.toLocalDate().toString(),
            "modifiedByUser" to zonedDateTime.toLocalDateTime().toString()
        )
    }

    @PostMapping(value = ["/{klageId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlage(
        @PathVariable klageId: String,
        @RequestParam vedlegg: MultipartFile
    ): VedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to klage is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Add Vedlegg to klage is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val temporaryVedlegg = vedleggService.addVedlegg(klageId.toInt(), vedlegg, bruker)
        return vedleggService.expandVedleggToVedleggView(temporaryVedlegg, bruker)
    }

    @DeleteMapping("/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: String,
        @PathVariable vedleggId: Int
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete vedlegg from klage is requested. KlageId: {}, VedleggId: {}", klageId, vedleggId)
        secureLogger.debug(
            "Delete vedlegg from klage is requested. KlageId: {}, vedleggId: {}, fnr: {} ",
            klageId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!vedleggService.deleteVedlegg(klageId.toInt(), vedleggId, bruker)) {
            throw KlageNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{klageId}/vedlegg/{vedleggId}")
    fun getVedleggFromKlage(
        @PathVariable klageId: String,
        @PathVariable vedleggId: Int
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to klage is requested. KlageId: {} - VedleggId: {}", klageId, vedleggId)
        secureLogger.debug(
            "Vedlegg from klage is requested. KlageId: {}, vedleggId: {}, fnr: {} ",
            klageId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = vedleggService.getVedlegg(vedleggId, bruker)

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
        @PathVariable klageId: String
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage pdf is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get klage pdf is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = klageService.getKlagePdf(klageId.toInt(), bruker)

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

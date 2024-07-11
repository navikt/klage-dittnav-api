package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.controller.view.*
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.domain.toHeartBeatServerSentEvent
import no.nav.klage.domain.toServerSentEvent
import no.nav.klage.service.BrukerService
import no.nav.klage.service.CommonService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getResourceThatWillBeDeleted
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import reactor.core.publisher.Flux
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.time.Duration
import java.util.*

@RestController
@Tag(name = "klanker")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api/klanker")
class KlankeController(
    private val brukerService: BrukerService,
    private val vedleggService: VedleggService,
    private val kafkaEventClient: KafkaEventClient,
    private val commonService: CommonService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping("/{klankeId}")
    fun getKlanke(
        @PathVariable klankeId: UUID
    ): KlankeView {
        val bruker = brukerService.getBruker()
        logger.debug("Get klanke is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Get klanke is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return commonService.getKlanke(klankeId, bruker).toKlankeView()
    }

    @GetMapping("/{klankeId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klankeId: UUID
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. KlankeId: {}", klankeId)
        secureLogger.debug(
            "Get journalpost id is requested. KlankeId: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return commonService.getJournalpostId(klankeId, bruker)
    }

    @GetMapping("/{klankeId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable klankeId: UUID
    ): Flux<ServerSentEvent<String>> {
        val bruker = brukerService.getBruker()
        kotlin.runCatching {
            commonService.validateAccess(klankeId, bruker)
        }.onFailure {
            throw KlankeNotFoundException()
        }
        logger.debug("Journalpostid events called for klankeId: {}", klankeId)
        //https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream: Flux<ServerSentEvent<String>> = Flux.interval(Duration.ofSeconds(10))
            .takeWhile { true }
            .map { tick -> tick.toHeartBeatServerSentEvent() }

        return kafkaEventClient.getEventPublisher()
            .mapNotNull { event -> jsonToEvent(event.data()) }
            .filter { it.klageAnkeId == klankeId.toString() }
            .mapNotNull { it.toServerSentEvent() }
            .mergeWith(heartbeatStream)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlanke(
        @RequestBody klankeFullInput: KlankeFullInput, response: HttpServletResponse
    ): KlankeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create klanke is requested.")
        secureLogger.debug(
            "Create klanke is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return commonService.createKlanke(klankeFullInput, bruker).toKlankeView()
    }

    @PutMapping
    fun createOrGetKlanke(
        @RequestBody klankeMinimalInput: KlankeMinimalInput,
        response: HttpServletResponse
    ): KlankeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create or update klanke for user is requested.")
        secureLogger.debug(
            "Create or update klanke for user is requested. Fnr: {}, innsendingsytelse: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            klankeMinimalInput.innsendingsytelse
        )

        return commonService.getDraftOrCreateKlanke(klankeMinimalInput, bruker = bruker).toKlankeView()
    }

    @PutMapping("/{klankeId}/fritekst")
    fun updateFritekst(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klanke fritekst is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Update klanke fritekst is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateFritekst(klankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/usersaksnummer")
    fun updateUserSaksnummer(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInputNullable,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klanke userSaksnummer is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Update klanke userSaksnummer is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateUserSaksnummer(klankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable klankeId: UUID,
        @RequestBody input: DateInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klanke vedtakDate is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Update klanke vedtakDate is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateVedtakDate(klankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/hasvedlegg")
    fun updateHasVedlegg(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klanke hasVedlegg is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Update klanke hasVedlegg is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateHasVedlegg(klankeId, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/checkboxesselected")
    fun updateCheckboxesSelected(
        @PathVariable klankeId: UUID,
        @RequestBody input: CheckboxesSelectedInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update klanke checkboxesSelected is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Update klanke checkboxesSelected is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateCheckboxesSelected(
            klankeId = klankeId,
            checkboxesSelected = input.value,
            bruker = bruker
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @Deprecated("No longer in use")
    @PutMapping("/{klankeId}/enhetsnummer")
    fun updateEnhetsnummer(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInputNullable,
        response: HttpServletResponse
    ) {
        logger.debug("Deprecated update klankeId enhetsnummer is requested. Returning 200.")
    }

    @PutMapping("/{klankeId}/caseisatka")
    fun updateCaseIsAtKA(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("updateCaseIsAtKA is requested. Id: {}", klankeId)
        secureLogger.debug(
            "updateCaseIsAtKA is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = commonService.updateCaseIsAtKA(
            klankeId = klankeId,
            caseIsAtKA = input.value,
            bruker = bruker
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @DeleteMapping("/{klankeId}")
    fun deleteKlanke(@PathVariable klankeId: UUID) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete klanke is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Delete klanke is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        commonService.deleteKlanke(klankeId = klankeId, bruker = bruker)
    }

    @PostMapping("/{klankeId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlanke(
        @PathVariable klankeId: UUID
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize klanke is requested. Id: {}", klankeId)
        secureLogger.debug(
            "Finalize klanke is requested. Id: {}, fnr: {}",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val finalizedLocalDateTime = commonService.finalizeKlanke(klankeId, bruker)
        return mapOf(
            "finalizedDate" to finalizedLocalDateTime.toLocalDate().toString(),
            "modifiedByUser" to finalizedLocalDateTime.toString()
        )
    }

    @PostMapping(value = ["/{klankeId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlanke(
        @PathVariable klankeId: UUID,
        @RequestParam vedlegg: MultipartFile
    ): VedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to klanke is requested. KlankeId: {}", klankeId)
        secureLogger.debug(
            "Add Vedlegg to klanke is requested. KlankeId: {}, fnr: {} ",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return vedleggService.addKlankevedlegg(
            klankeId = klankeId,
            multipart = vedlegg,
            bruker = bruker
        ).toVedleggView()
    }

    @DeleteMapping("/{klankeId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete vedlegg from klanke is requested. KlankeId: {}, VedleggId: {}", klankeId, vedleggId)
        secureLogger.debug(
            "Delete vedlegg from klanke is requested. KlankeId: {}, vedleggId: {}, fnr: {} ",
            klankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!vedleggService.deleteVedleggFromKlanke(
                klankeId = klankeId,
                vedleggId = vedleggId,
                bruker = bruker
            )
        ) {
            throw KlankeNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{klankeId}/vedlegg/{vedleggId}")
    fun getVedleggFromKlanke(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID
    ): ResponseEntity<Resource> {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to klanke is requested. KlankeId: {} - VedleggId: {}", klankeId, vedleggId)
        secureLogger.debug(
            "Vedlegg from klanke is requested. KlankeId: {}, vedleggId: {}, fnr: {} ",
            klankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val fileResource = vedleggService.getVedleggFromKlanke(klankeId, vedleggId, bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + (fileResource.filename ?: "vedlegg.pdf"))
        return ResponseEntity.ok()
            .headers(responseHeaders)
            .contentLength(fileResource.file.length())
            .body(getResourceThatWillBeDeleted(fileResource))
    }

    @ResponseBody
    @GetMapping("/{klankeId}/vedlegg/{vedleggId}/signedurl")
    fun getVedleggFromKlankeSignedUrl(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID
    ): ModelAndView {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to klanke is requested. KlankeId: {} - VedleggId: {}", klankeId, vedleggId)
        secureLogger.debug(
            "Vedlegg from klanke is requested. KlankeId: {}, vedleggId: {}, fnr: {} ",
            klankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val url = vedleggService.getVedleggFromKlankeAsSignedUrl(klankeId, vedleggId, bruker)
        return ModelAndView("redirect:$url")
    }

    @ResponseBody
    @GetMapping("/{klankeId}/pdf")
    fun getKlankePdf(
        @PathVariable klankeId: UUID
    ): ResponseEntity<Resource> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klanke pdf is requested. KlankeId: {}", klankeId)
        secureLogger.debug(
            "Get klanke pdf is requested. KlankeId: {}, fnr: {} ",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val (pathToMergedDocument, title) = commonService.getKlankePdf(klankeId, bruker)
        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.APPLICATION_PDF
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$title.pdf\"")

        return ResponseEntity.ok()
            .headers(responseHeaders)
            .contentLength(pathToMergedDocument.toFile().length())
            .body(
                object : FileSystemResource(pathToMergedDocument) {
                    override fun getInputStream(): InputStream {
                        return object : FileInputStream(pathToMergedDocument.toFile()) {
                            override fun close() {
                                super.close()
                                //Override to do this after client has downloaded file
                                Files.delete(file.toPath())
                            }
                        }
                    }
                })
    }

    @ResponseBody
    @GetMapping("/{klankeId}/pdf/innsending")
    fun getKlankePdfForPrint(
        @PathVariable klankeId: UUID
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klanke pdf for print is requested. KlankeId: {}", klankeId)
        secureLogger.debug(
            "Get klanke pdf for print is requested. KlankeId: {}, fnr: {} ",
            klankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val klanke = commonService.getKlanke(klankeId = klankeId, bruker = bruker)

        val content = commonService.createKlankePdfWithFoersteside(klankeId, bruker)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "${klanke.type.name.lowercase()}.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }

}

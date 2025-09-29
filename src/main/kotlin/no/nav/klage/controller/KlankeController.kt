package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.controller.view.*
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.domain.toHeartBeatServerSentEvent
import no.nav.klage.domain.toServerSentEvent
import no.nav.klage.service.BrukerService
import no.nav.klage.service.CommonService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
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
    private val tokenUtil: TokenUtil,
    private val brukerService: BrukerService,
    private val vedleggService: VedleggService,
    private val kafkaEventClient: KafkaEventClient,
    private val commonService: CommonService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @GetMapping("/{klankeId}")
    fun getKlanke(
        @PathVariable klankeId: UUID
    ): KlankeView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Get klanke is requested. Id: {}", klankeId)
        return commonService.getKlanke(klankeId = klankeId, foedselsnummer = brukerIdent).toKlankeView()
    }

    @GetMapping("/{klankeId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klankeId: UUID
    ): String? {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Get journalpost id is requested. KlankeId: {}", klankeId)
        return commonService.getJournalpostId(klankeId = klankeId, foedselsnummer = brukerIdent)
    }

    @GetMapping("/{klankeId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable klankeId: UUID
    ): Flux<ServerSentEvent<String>> {
        val brukerIdent = tokenUtil.getSubject()
        kotlin.runCatching {
            commonService.validateAccess(klankeId = klankeId, foedselsnummer = brukerIdent)
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
        @RequestBody klankeFullInput: KlankeFullInput
    ): KlankeView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Create klanke is requested.")
        return commonService.createKlanke(input = klankeFullInput, foedselsnummer = brukerIdent).toKlankeView()
    }

    @PutMapping
    fun createOrGetKlanke(
        @RequestBody klankeMinimalInput: KlankeMinimalInput,
    ): KlankeView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Create or update klanke for user is requested.")
        return commonService.getDraftOrCreateKlanke(input = klankeMinimalInput, foedselsnummer = brukerIdent).toKlankeView()
    }

    @PutMapping("/{klankeId}/fritekst")
    fun updateFritekst(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInput,
    ): EditedView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Update klanke fritekst is requested. Id: {}", klankeId)
        val modifiedByUser = commonService.updateFritekst(
            klankeId = klankeId,
            fritekst = input.value,
            foedselsnummer = brukerIdent
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/usersaksnummer")
    fun updateUserSaksnummer(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInputNullable,
    ): EditedView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Update klanke userSaksnummer is requested. Id: {}", klankeId)
        val modifiedByUser = commonService.updateUserSaksnummer(
            klankeId = klankeId,
            userSaksnummer = input.value,
            foedselsnummer = brukerIdent
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable klankeId: UUID,
        @RequestBody input: DateInput,
    ): EditedView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Update klanke vedtakDate is requested. Id: {}", klankeId)
        val modifiedByUser = commonService.updateVedtakDate(
            klankeId = klankeId,
            vedtakDate = input.value,
            foedselsnummer = brukerIdent,
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/hasvedlegg")
    fun updateHasVedlegg(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
    ): EditedView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Update klanke hasVedlegg is requested. Id: {}", klankeId)
        val modifiedByUser = commonService.updateHasVedlegg(
            klankeId = klankeId,
            hasVedlegg = input.value,
            foedselsnummer = brukerIdent,
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{klankeId}/caseisatka")
    fun updateCaseIsAtKA(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
    ): EditedView {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("updateCaseIsAtKA is requested. Id: {}", klankeId)
        val modifiedByUser = commonService.updateCaseIsAtKA(
            klankeId = klankeId,
            caseIsAtKA = input.value,
            foedselsnummer = brukerIdent,
        )
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @DeleteMapping("/{klankeId}")
    fun deleteKlanke(@PathVariable klankeId: UUID) {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Delete klanke is requested. Id: {}", klankeId)
        commonService.deleteKlanke(klankeId = klankeId, foedselsnummer = brukerIdent)
    }

    @PostMapping("/{klankeId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlanke(
        @PathVariable klankeId: UUID
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize klanke is requested. Id: {}", klankeId)
        val finalizedLocalDateTime = commonService.finalizeKlanke(klankeId = klankeId, bruker = bruker)
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
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Add vedlegg to klanke is requested. KlankeId: {}", klankeId)
        return vedleggService.addKlankevedlegg(
            klankeId = klankeId,
            multipart = vedlegg,
            foedselsnummer = brukerIdent
        ).toVedleggView()
    }

    @DeleteMapping("/{klankeId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID
    ) {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Delete vedlegg from klanke is requested. KlankeId: {}, VedleggId: {}", klankeId, vedleggId)
        if (!vedleggService.deleteVedleggFromKlanke(
                klankeId = klankeId,
                vedleggId = vedleggId,
                foedselsnummer = brukerIdent
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
    ): ResponseEntity<ByteArray> {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Get vedlegg to klanke is requested. KlankeId: {} - VedleggId: {}", klankeId, vedleggId)
        val content = vedleggService.getVedleggFromKlanke(
            klankeId = klankeId,
            vedleggId = vedleggId,
            foedselsnummer = brukerIdent
        )

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
    @GetMapping("/{klankeId}/pdf")
    fun getKlankePdf(
        @PathVariable klankeId: UUID
    ): ResponseEntity<Resource> {
        val brukerIdent = tokenUtil.getSubject()
        logger.debug("Get klanke pdf is requested. KlankeId: {}", klankeId)
        val (pathToMergedDocument, title) = commonService.getKlankePdf(klankeId = klankeId, foedselsnummer = brukerIdent)
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
        val klanke = commonService.getKlanke(klankeId = klankeId, foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer)

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

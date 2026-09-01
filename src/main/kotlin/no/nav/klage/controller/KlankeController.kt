package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.config.SecurityConfiguration.Companion.TOKEN_X
import no.nav.klage.controller.view.BooleanInput
import no.nav.klage.controller.view.DateInput
import no.nav.klage.controller.view.EditedView
import no.nav.klage.controller.view.KlankeFullInput
import no.nav.klage.controller.view.KlankeMinimalInput
import no.nav.klage.controller.view.KlankeView
import no.nav.klage.controller.view.StringInput
import no.nav.klage.controller.view.StringInputNullable
import no.nav.klage.controller.view.VedleggView
import no.nav.klage.controller.view.toVedleggView
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.domain.toHeartBeatServerSentEvent
import no.nav.klage.domain.toServerSentEvent
import no.nav.klage.service.CommonService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.time.Duration
import java.util.UUID

@RestController
@Tag(name = "klanker")
@ProtectedWithClaims(issuer = TOKEN_X, claimMap = ["acr=Level4"])
@RequestMapping("/api/klanker")
class KlankeController(
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
        @PathVariable klankeId: UUID,
    ): KlankeView {
        logger.debug("Get klanke is requested. Id: {}", klankeId)
        return commonService.getKlanke(klankeId = klankeId)
    }

    @GetMapping("/{klankeId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klankeId: UUID,
    ): String? {
        logger.debug("Get journalpost id is requested. KlankeId: {}", klankeId)
        return commonService.getJournalpostId(klankeId = klankeId)
    }

    @GetMapping("/{klankeId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getEvents(
        @PathVariable klankeId: UUID,
    ): Flux<ServerSentEvent<String>> {
        kotlin
            .runCatching {
                commonService.validateAccess(klankeId = klankeId)
            }.onFailure {
                throw KlankeNotFoundException()
            }
        logger.debug("Journalpostid events called for klankeId: {}", klankeId)
        // https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream: Flux<ServerSentEvent<String>> =
            Flux
                .interval(Duration.ofSeconds(10))
                .takeWhile { true }
                .map { tick -> tick.toHeartBeatServerSentEvent() }

        return kafkaEventClient
            .getEventPublisher()
            .mapNotNull { event -> jsonToEvent(event.data()) }
            .filter { it.klageAnkeId == klankeId.toString() }
            .mapNotNull { it.toServerSentEvent() }
            .mergeWith(heartbeatStream)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlanke(
        @RequestBody klankeFullInput: KlankeFullInput,
    ): KlankeView {
        logger.debug("Create klanke is requested.")
        return commonService.createKlanke(input = klankeFullInput)
    }

    @PutMapping
    fun createOrGetKlanke(
        @RequestBody klankeMinimalInput: KlankeMinimalInput,
    ): KlankeView {
        logger.debug("Create or update klanke for user is requested.")
        return commonService.getDraftOrCreateKlanke(input = klankeMinimalInput)
    }

    @PutMapping("/{klankeId}/fritekst")
    fun updateFritekst(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInput,
    ): EditedView {
        logger.debug("Update klanke fritekst is requested. Id: {}", klankeId)
        val modifiedByUser =
            commonService.updateFritekst(
                klankeId = klankeId,
                fritekst = input.value,
            )
        return EditedView(
            modifiedByUser = modifiedByUser,
        )
    }

    @PutMapping("/{klankeId}/usersaksnummer")
    fun updateUserSaksnummer(
        @PathVariable klankeId: UUID,
        @RequestBody input: StringInputNullable,
    ): EditedView {
        logger.debug("Update klanke userSaksnummer is requested. Id: {}", klankeId)
        val modifiedByUser =
            commonService.updateUserSaksnummer(
                klankeId = klankeId,
                userSaksnummer = input.value,
            )
        return EditedView(
            modifiedByUser = modifiedByUser,
        )
    }

    @PutMapping("/{klankeId}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable klankeId: UUID,
        @RequestBody input: DateInput,
    ): EditedView {
        logger.debug("Update klanke vedtakDate is requested. Id: {}", klankeId)
        val modifiedByUser =
            commonService.updateVedtakDate(
                klankeId = klankeId,
                vedtakDate = input.value,
            )
        return EditedView(
            modifiedByUser = modifiedByUser,
        )
    }

    @PutMapping("/{klankeId}/hasvedlegg")
    fun updateHasVedlegg(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
    ): EditedView {
        logger.debug("Update klanke hasVedlegg is requested. Id: {}", klankeId)
        val modifiedByUser =
            commonService.updateHasVedlegg(
                klankeId = klankeId,
                hasVedlegg = input.value,
            )
        return EditedView(
            modifiedByUser = modifiedByUser,
        )
    }

    @PutMapping("/{klankeId}/caseisatka")
    fun updateCaseIsAtKA(
        @PathVariable klankeId: UUID,
        @RequestBody input: BooleanInput,
    ): EditedView {
        logger.debug("updateCaseIsAtKA is requested. Id: {}", klankeId)
        val modifiedByUser =
            commonService.updateCaseIsAtKA(
                klankeId = klankeId,
                caseIsAtKA = input.value,
            )
        return EditedView(
            modifiedByUser = modifiedByUser,
        )
    }

    @DeleteMapping("/{klankeId}")
    fun deleteKlanke(
        @PathVariable klankeId: UUID,
    ) {
        logger.debug("Delete klanke is requested. Id: {}", klankeId)
        commonService.deleteKlanke(klankeId = klankeId)
    }

    @PostMapping("/{klankeId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlanke(
        @PathVariable klankeId: UUID,
    ): Map<String, String> {
        logger.debug("Finalize klanke is requested. Id: {}", klankeId)
        val finalizedLocalDateTime = commonService.finalizeKlanke(klankeId = klankeId)
        return mapOf(
            "finalizedDate" to finalizedLocalDateTime.toLocalDate().toString(),
            "modifiedByUser" to finalizedLocalDateTime.toString(),
        )
    }

    @PostMapping(value = ["/{klankeId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlanke(
        @PathVariable klankeId: UUID,
        @RequestParam vedlegg: MultipartFile,
    ): VedleggView {
        logger.debug("Add vedlegg to klanke is requested. KlankeId: {}", klankeId)
        return vedleggService
            .addKlankevedlegg(
                klankeId = klankeId,
                multipart = vedlegg,
            ).toVedleggView()
    }

    @DeleteMapping("/{klankeId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID,
    ) {
        logger.debug("Delete vedlegg from klanke is requested. KlankeId: {}, VedleggId: {}", klankeId, vedleggId)
        if (!vedleggService.deleteVedleggFromKlanke(
                klankeId = klankeId,
                vedleggId = vedleggId,
            )
        ) {
            throw KlankeNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{klankeId}/vedlegg/{vedleggId}")
    fun getVedleggFromKlanke(
        @PathVariable klankeId: UUID,
        @PathVariable vedleggId: UUID,
    ): ResponseEntity<ByteArray> {
        logger.debug("Get vedlegg to klanke is requested. KlankeId: {} - VedleggId: {}", klankeId, vedleggId)
        val content =
            vedleggService.getVedleggFromKlanke(
                klankeId = klankeId,
                vedleggId = vedleggId,
            )

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "vedlegg.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK,
        )
    }

    @ResponseBody
    @GetMapping("/{klankeId}/pdf")
    fun getKlankePdf(
        @PathVariable klankeId: UUID,
    ): ResponseEntity<Resource> {
        logger.debug("Get klanke pdf is requested. KlankeId: {}", klankeId)
        val (pathToMergedDocument, title) = commonService.getKlankePdf(klankeId = klankeId)
        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.APPLICATION_PDF
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$title.pdf\"")

        return ResponseEntity
            .ok()
            .headers(responseHeaders)
            .contentLength(pathToMergedDocument.toFile().length())
            .body(
                object : FileSystemResource(pathToMergedDocument) {
                    override fun getInputStream(): InputStream =
                        object : FileInputStream(pathToMergedDocument.toFile()) {
                            override fun close() {
                                super.close()
                                // Override to do this after client has downloaded file
                                Files.delete(file.toPath())
                            }
                        }
                },
            )
    }

    @ResponseBody
    @GetMapping("/{klankeId}/pdf/innsending")
    fun getKlankePdfForPrint(
        @PathVariable klankeId: UUID,
    ): ResponseEntity<ByteArray> {
        logger.debug("Get klanke pdf for print is requested. KlankeId: {}", klankeId)
        val klanke = commonService.getKlanke(klankeId = klankeId)

        val content = commonService.createKlankePdfWithFoersteside(klankeId)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "inline; filename=" + "${klanke.type.name.lowercase()}.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK,
        )
    }
}

package no.nav.klage.controller

import no.nav.klage.domain.anke.AnkeView
import no.nav.klage.domain.ankevedlegg.AnkeVedleggView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.UpdateMismatchException
import no.nav.klage.service.AnkeService
import no.nav.klage.service.AnkeVedleggService
import no.nav.klage.service.BrukerService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletResponse

@Profile("local", "dev-gcp")
@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RequestMapping("/anker")
class AnkeController(
    private val brukerService: BrukerService,
    private val ankeService: AnkeService,
    private val ankeVedleggService: AnkeVedleggService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping("/draft")
    fun getDraftAnkeByQuery(
        @RequestParam internalSaksnummer: String,
        @RequestParam fullmaktsgiver: String?
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Get draft anke for user is requested.")
        secureLogger.debug(
            "Get draft anke for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getLatestDraftAnkeByParams(
            bruker,
            internalSaksnummer,
            fullmaktsgiver
        )
    }

    @GetMapping("/{ankeId}")
    fun getAnke(
        @PathVariable ankeId: Int
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Get anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getAnke(ankeId, bruker)
    }

    @GetMapping("/{ankeId}/journalpostid")
    fun getJournalpostId(
        @PathVariable ankeId: Int
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. AnkeId: {}", ankeId)
        secureLogger.debug(
            "Get journalpost id is requested. AnkeId: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getJournalpostId(ankeId, bruker)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnke(
        @RequestBody anke: AnkeView, response: HttpServletResponse
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create anke is requested.")
        secureLogger.debug(
            "Create anke is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.createAnke(anke, bruker)
    }

    @PutMapping("/{ankeId}")
    fun updateAnke(
        @PathVariable ankeId: Int,
        @RequestBody anke: AnkeView,
        response: HttpServletResponse
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Update anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (anke.id != ankeId) {
            throw UpdateMismatchException("Id in anke does not match resource id")
        }
        ankeService.updateAnke(anke, bruker)
    }

    @DeleteMapping("/{ankeId}")
    fun deleteAnke(@PathVariable ankeId: Int) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete anke is requested. Id: {}", ankeId)
        secureLogger.debug(
            "Delete anke is requested. Id: {}, fnr: {}",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        ankeService.deleteAnke(ankeId, bruker)
    }

    @PostMapping("/{ankeId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeAnke(
        @PathVariable ankeId: Int
    ): HttpStatus {
        return HttpStatus.OK
//        val bruker = brukerService.getBruker()
//        logger.debug("Finalize klage is requested. Id: {}", ankeId)
//        secureLogger.debug(
//            "Finalize klage is requested. Id: {}, fnr: {}",
//            ankeId,
//            bruker.folkeregisteridentifikator.identifikasjonsnummer
//        )
//        val finalizedInstant = klageService.finalizeKlage(ankeId, bruker)
//        val zonedDateTime = ZonedDateTime.ofInstant(finalizedInstant, ZoneId.of("Europe/Oslo"))
//        return mapOf(
//            "finalizedDate" to zonedDateTime.toLocalDate().toString(),
//            "modifiedByUser" to zonedDateTime.toLocalDateTime().toString()
//        )
    }

    @PostMapping(value = ["/{ankeId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addAnkeVedleggToAnke(
        @PathVariable ankeId: Int,
        @RequestParam vedlegg: MultipartFile
    ): AnkeVedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to anke is requested. KlageId: {}", ankeId)
        secureLogger.debug(
            "Add vedlegg to anke is requested. AnkeId: {}, fnr: {} ",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val temporaryAnkeVedlegg = ankeVedleggService.addAnkeVedlegg(ankeId, vedlegg, bruker)
        return ankeVedleggService.expandAnkeVedleggToAnkeVedleggView(temporaryAnkeVedlegg, bruker)
    }

    @DeleteMapping("/{ankeId}/vedlegg/{vedleggId}")
    fun deleteAnkeVedlegg(
        @PathVariable ankeId: Int,
        @PathVariable vedleggId: Int
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete vedlegg from anke is requested. AnkeId: {}, VedleggId: {}", ankeId, vedleggId)
        secureLogger.debug(
            "Delete vedlegg from anke is requested. AnkeId: {}, vedleggId: {}, fnr: {} ",
            ankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!ankeVedleggService.deleteAnkeVedlegg(ankeId, vedleggId, bruker)) {
            throw AnkeNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{ankeId}/vedlegg/{vedleggId}")
    fun getAnkeVedleggFromKlage(
        @PathVariable ankeId: Int,
        @PathVariable vedleggId: Int
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to anke is requested. AnkeId: {} - VedleggId: {}", ankeId, vedleggId)
        secureLogger.debug(
            "Vedlegg from anke is requested. AnkeId: {}, vedleggId: {}, fnr: {} ",
            ankeId,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = ankeVedleggService.getAnkeVedlegg(vedleggId, bruker)

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
        @PathVariable ankeId: Int
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke pdf is requested. AnkeId: {}", ankeId)
        secureLogger.debug(
            "Get anke pdf is requested. AnkeId: {}, fnr: {} ",
            ankeId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = ankeService.getAnkePdf(ankeId, bruker)

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
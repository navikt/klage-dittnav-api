package no.nav.klage.controller

import io.swagger.annotations.Api
import no.nav.klage.domain.Tema
import no.nav.klage.domain.anke.AnkeView
import no.nav.klage.domain.anke.NewAnkeRequest
import no.nav.klage.domain.ankevedlegg.AnkeVedleggView
import no.nav.klage.domain.availableanke.AvailableAnkeView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.UpdateMismatchException
import no.nav.klage.service.AnkeService
import no.nav.klage.service.AnkeVedleggService
import no.nav.klage.service.AvailableAnkeService
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
@Api(tags = ["anker-prefixed"])
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RequestMapping("/api/anker")
class AnkeControllerPrefixed(
    private val brukerService: BrukerService,
    private val ankeService: AnkeService,
    private val ankeVedleggService: AnkeVedleggService,
    private val availableAnkeService: AvailableAnkeService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping("/available")
    fun getAllAvailableAnkerForUser(
        @RequestParam tema: Tema? = null
    ): List<AvailableAnkeView> {
        val bruker = brukerService.getBruker()
        logger.debug("Get all available anke possibilities for user is requested.")
        secureLogger.debug(
            "Get all available anke possibilities for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        return availableAnkeService.getAllAvailableAnkeViewForUser(bruker, tema)
    }

    @GetMapping("/draft")
    fun getDraftAnkeByQuery(
        @RequestParam ankeInternalSaksnummer: String,
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
            ankeInternalSaksnummer,
            fullmaktsgiver
        )
    }

    @GetMapping("/{ankeInternalSaksnummer}")
    fun getAnke(
        @PathVariable ankeInternalSaksnummer: String
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Get anke is requested. Internal ref: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getAnke(ankeInternalSaksnummer, bruker)
    }

    @GetMapping("/{ankeInternalSaksnummer}/journalpostid")
    fun getJournalpostId(
        @PathVariable ankeInternalSaksnummer: String
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Get journalpost id is requested. Internal ref: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeService.getJournalpostId(ankeInternalSaksnummer, bruker)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnke(
        @RequestBody input: NewAnkeRequest,
        response: HttpServletResponse
    ): AnkeView {
        val bruker = brukerService.getBruker()
        logger.debug("Create anke is requested.")
        secureLogger.debug(
            "Create anke is requested for user with fnr {}, internal anke ref {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            input.ankeInternalSaksnummer
        )
        return ankeService.createAnke(input, bruker)
    }

    @PutMapping("/{ankeInternalSaksnummer}")
    fun updateAnke(
        @PathVariable ankeInternalSaksnummer: String,
        @RequestBody anke: AnkeView,
        response: HttpServletResponse
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Update anke is requested. Internal ref: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (anke.ankeInternalSaksnummer != ankeInternalSaksnummer) {
            throw UpdateMismatchException("Internal ref in anke does not match resource id")
        }
        ankeService.updateAnke(anke, bruker)
    }

    @DeleteMapping("/{ankeInternalSaksnummer}")
    fun deleteAnke(@PathVariable ankeInternalSaksnummer: String) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Delete anke is requested. Internal ref: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        ankeService.deleteAnke(ankeInternalSaksnummer, bruker)
    }

    @PostMapping("/{ankeInternalSaksnummer}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeAnke(
        @PathVariable ankeInternalSaksnummer: String
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

    @PostMapping(value = ["/{ankeInternalSaksnummer}/vedlegg"], consumes = ["multipart/form-data"])
    fun addAnkeVedleggToAnke(
        @PathVariable ankeInternalSaksnummer: String,
        @RequestParam vedlegg: MultipartFile
    ): AnkeVedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Add vedlegg to anke is requested. Internal ref: {}, fnr: {} ",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val temporaryAnkeVedlegg = ankeVedleggService.addAnkeVedlegg(ankeInternalSaksnummer, vedlegg, bruker)
        return ankeVedleggService.expandAnkeVedleggToAnkeVedleggView(temporaryAnkeVedlegg, bruker)
    }

    @DeleteMapping("/{ankeInternalSaksnummer}/vedlegg/{vedleggId}")
    fun deleteAnkeVedlegg(
        @PathVariable ankeInternalSaksnummer: String,
        @PathVariable vedleggId: Int
    ) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete vedlegg from anke is requested. Internal ref: {}, VedleggId: {}", ankeInternalSaksnummer, vedleggId)
        secureLogger.debug(
            "Delete vedlegg from anke is requested. Internal ref: {}, vedleggId: {}, fnr: {} ",
            ankeInternalSaksnummer,
            vedleggId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        if (!ankeVedleggService.deleteAnkeVedlegg(vedleggId, bruker)) {
            throw AnkeNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/{ankeInternalSaksnummer}/vedlegg/{vedleggId}")
    fun getAnkeVedleggFromAnke(
        @PathVariable ankeInternalSaksnummer: String,
        @PathVariable vedleggId: Int
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get vedlegg to anke is requested. Internal ref: {} - VedleggId: {}", ankeInternalSaksnummer, vedleggId)
        secureLogger.debug(
            "Vedlegg from anke is requested. Internal ref: {}, vedleggId: {}, fnr: {} ",
            ankeInternalSaksnummer,
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
    @GetMapping("/{ankeInternalSaksnummer}/pdf")
    fun getAnkePdf(
        @PathVariable ankeInternalSaksnummer: String
    ): ResponseEntity<ByteArray> {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke pdf is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Get anke pdf is requested. Internal ref: {}, fnr: {} ",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )

        val content = ankeService.getAnkePdf(ankeInternalSaksnummer, bruker)

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
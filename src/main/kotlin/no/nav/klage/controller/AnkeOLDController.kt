package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.controller.view.DateInput
import no.nav.klage.controller.view.EditedView
import no.nav.klage.controller.view.StringInput
import no.nav.klage.domain.ankeOLD.AnkeOLDView
import no.nav.klage.domain.ankeOLD.NewAnkeOLDRequest
import no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLDView
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.UpdateMismatchException
import no.nav.klage.service.AnkeOLDService
import no.nav.klage.service.AnkeOLDVedleggService
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
@Tag(name = "anker-old")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api/old/anker")
class AnkeOLDController(
    private val brukerService: BrukerService,
    private val ankeOLDService: AnkeOLDService,
    private val ankeOLDVedleggService: AnkeOLDVedleggService,
    private val availableAnkeService: AvailableAnkeService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

//    @GetMapping("/available")
//    fun getAllAvailableAnkerForUser(
//        @RequestParam tema: Tema? = null
//    ): List<AvailableAnkeView> {
//        val bruker = brukerService.getBruker()
//        logger.debug("Get all available anke possibilities for user is requested.")
//        secureLogger.debug(
//            "Get all available anke possibilities for user is requested. Fnr: {}",
//            bruker.folkeregisteridentifikator.identifikasjonsnummer
//        )
//
//        return availableAnkeService.getAllAvailableAnkeViewForUser(bruker, tema)
//    }

    @GetMapping("/draft")
    fun getDraftAnkeByQuery(
        @RequestParam ankeInternalSaksnummer: String,
        @RequestParam fullmaktsgiver: String?
    ): AnkeOLDView {
        val bruker = brukerService.getBruker()
        logger.debug("Get draft anke for user is requested.")
        secureLogger.debug(
            "Get draft anke for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeOLDService.getLatestDraftAnkeByParams(
            bruker,
            ankeInternalSaksnummer,
            fullmaktsgiver
        )
    }

    @GetMapping("/{ankeInternalSaksnummer}")
    fun getAnke(
        @PathVariable ankeInternalSaksnummer: String
    ): AnkeOLDView {
        val bruker = brukerService.getBruker()
        logger.debug("Get anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Get anke is requested. Internal ref: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return ankeOLDService.getAnke(ankeInternalSaksnummer, bruker)
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
        return ankeOLDService.getJournalpostId(ankeInternalSaksnummer, bruker)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnke(
        @RequestBody input: NewAnkeOLDRequest,
        response: HttpServletResponse
    ): AnkeOLDView {
        val bruker = brukerService.getBruker()
        logger.debug("Create anke is requested.")
        secureLogger.debug(
            "Create anke is requested for user with fnr {}, internal anke ref {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            input.ankeInternalSaksnummer
        )
        return ankeOLDService.createAnke(input, bruker)
    }

    @PutMapping("/{ankeInternalSaksnummer}")
    fun updateAnke(
        @PathVariable ankeInternalSaksnummer: String,
        @RequestBody anke: AnkeOLDView,
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
        ankeOLDService.updateAnke(anke, bruker)
    }

    @PutMapping("/{ankeInternalSaksnummer}/fritekst")
    fun updateFritekst(
        @PathVariable ankeInternalSaksnummer: String,
        @RequestBody input: StringInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke fritekst is requested. Id: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Update anke fritekst is requested. Id: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = ankeOLDService.updateFritekst(ankeInternalSaksnummer, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
    }

    @PutMapping("/{ankeInternalSaksnummer}/vedtakdate")
    fun updateVedtakDate(
        @PathVariable ankeInternalSaksnummer: String,
        @RequestBody input: DateInput,
        response: HttpServletResponse
    ): EditedView {
        val bruker = brukerService.getBruker()
        logger.debug("Update anke vedtakDate is requested. Id: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Update anke vedtakDate is requested. Id: {}, fnr: {}",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val modifiedByUser = ankeOLDService.updateVedtakDate(ankeInternalSaksnummer, input.value, bruker)
        return EditedView(
            modifiedByUser = modifiedByUser
        )
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
        ankeOLDService.deleteAnke(ankeInternalSaksnummer, bruker)
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
    ): AnkeVedleggOLDView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to anke is requested. Internal ref: {}", ankeInternalSaksnummer)
        secureLogger.debug(
            "Add vedlegg to anke is requested. Internal ref: {}, fnr: {} ",
            ankeInternalSaksnummer,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val temporaryAnkeVedlegg = ankeOLDVedleggService.addAnkeVedlegg(ankeInternalSaksnummer, vedlegg, bruker)
        return ankeOLDVedleggService.expandAnkeVedleggToAnkeVedleggView(temporaryAnkeVedlegg, bruker)
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
        if (!ankeOLDVedleggService.deleteAnkeVedlegg(vedleggId, bruker)) {
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

        val content = ankeOLDVedleggService.getAnkeVedlegg(vedleggId, bruker)

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

        val content = ankeOLDService.getAnkePdf(ankeInternalSaksnummer, bruker)

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
package no.nav.klage.controller

import no.nav.klage.domain.Tema
import no.nav.klage.domain.Vedtak
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.exception.UpdateMismatchException
import no.nav.klage.domain.klage.KlageView
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.service.BrukerService
import no.nav.klage.service.KlageService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.servlet.http.HttpServletResponse

@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class KlageController(
    private val brukerService: BrukerService,
    private val klageService: KlageService,
    private val vedleggService: VedleggService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @GetMapping("/klager")
    fun getKlager(): List<KlageView> {
        val bruker = brukerService.getBruker()
        logger.debug("Get klager for user is requested.")
        secureLogger.debug(
            "Get klager for user is requested. Fnr: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getDraftKlagerByFnr(bruker)
    }

    @GetMapping("/klager/draft")
    fun getDraftKlageByQuery(
        @RequestParam tema: Tema,
        @RequestParam ytelse: String?,
        @RequestParam internalSaksnummer: String?
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Get klager for user is requested.")
        secureLogger.debug(
            "Get klager for user is requested. Fnr: {}, tema: {}",
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema
        )
        return klageService.getLatestDraftKlageByParams(bruker, tema, ytelse, internalSaksnummer)
    }

    @GetMapping("/klager/{klageId}")
    fun getKlage(
        @PathVariable klageId: Int
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Get klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Get klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getKlage(klageId, bruker)
    }

    @GetMapping("/klager/{klageId}/journalpostid")
    fun getJournalpostId(
        @PathVariable klageId: Int
    ): String? {
        val bruker = brukerService.getBruker()
        logger.debug("Get journalpost id is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Get journalpost id is requested. KlageId: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.getJournalpostId(klageId, bruker)
    }

    @PostMapping("/klager")
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlage(
        @RequestBody klage: KlageView, response: HttpServletResponse
    ): KlageView {
        val bruker = brukerService.getBruker()
        logger.debug("Create klage is requested.")
        secureLogger.debug(
            "Create klage is requested for user with fnr {}.",
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        return klageService.createKlage(klage, bruker)
    }

    @PutMapping("/klager/{klageId}")
    fun updateKlage(
        @PathVariable klageId: Int,
        @RequestBody klage: KlageView,
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
        klageService.updateKlage(klage, bruker)
    }

    @DeleteMapping("/klager/{klageId}")
    fun deleteKlage(@PathVariable klageId: Int) {
        val bruker = brukerService.getBruker()
        logger.debug("Delete klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Delete klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        klageService.deleteKlage(klageId, bruker)
    }

    @PostMapping("/klager/{klageId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlage(
        @PathVariable klageId: Int
    ): Map<String, String> {
        val bruker = brukerService.getBruker()
        logger.debug("Finalize klage is requested. Id: {}", klageId)
        secureLogger.debug(
            "Finalize klage is requested. Id: {}, fnr: {}",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val finalizedInstant = klageService.finalizeKlage(klageId, bruker)
        val zonedDateTime = ZonedDateTime.ofInstant(finalizedInstant, ZoneId.of("Europe/Oslo"))
        return mapOf(
                "finalizedDate" to zonedDateTime.toLocalDate().toString(),
                "modifiedByUser" to zonedDateTime.toLocalDateTime().toString()
        )
    }

    @PostMapping(value = ["/klager/{klageId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlage(
        @PathVariable klageId: Int,
        @RequestParam vedlegg: MultipartFile
    ): VedleggView {
        val bruker = brukerService.getBruker()
        logger.debug("Add vedlegg to klage is requested. KlageId: {}", klageId)
        secureLogger.debug(
            "Add Vedlegg to klage is requested. KlageId: {}, fnr: {} ",
            klageId,
            bruker.folkeregisteridentifikator.identifikasjonsnummer
        )
        val temporaryVedlegg = vedleggService.addVedlegg(klageId, vedlegg, bruker)
        return vedleggService.expandVedleggToVedleggView(temporaryVedlegg, bruker)
    }

    @DeleteMapping("/klager/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: Int,
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
        if (!vedleggService.deleteVedlegg(klageId, vedleggId, bruker)) {
            throw KlageNotFoundException("Attachment not found.")
        }
    }

    @ResponseBody
    @GetMapping("/klager/{klageId}/vedlegg/{vedleggId}")
    fun getVedleggFromKlage(
        @PathVariable klageId: Int,
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
    @GetMapping("/klager/{klageId}/pdf")
    fun getKlagePdf(
        @PathVariable klageId: Int
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


    @GetMapping("/vedtak")
    fun getVedtak(): List<Vedtak> {
        logger.debug("Get vedtak is requested.")
        return listOf()
    }
}

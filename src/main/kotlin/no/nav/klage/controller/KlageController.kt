package no.nav.klage.controller

import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.Vedtak
import no.nav.klage.domain.klage.KlageView
import no.nav.klage.service.BrukerService
import no.nav.klage.service.KlageService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
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
    }

    @GetMapping("/klager")
    fun getKlager(): List<KlageView> {
        return klageService.getKlager()
    }

    @PostMapping("/klager")
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlage(
        @RequestBody klage: KlageView, response: HttpServletResponse
    ): KlageView {
        logger.debug("Create klage is requested.")
        return klageService.createKlage(klage, brukerService.getBruker())
    }

    @PutMapping("/klager/{klageId}")
    fun updateKlage(
        @PathVariable klageId: Int,
        @RequestBody klage: KlageView,
        response: HttpServletResponse
    ): KlageView {
        logger.debug("Update klage is requested. Id: {}", klageId)
        if (klage.id != klageId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id in klage does not match resource id")
        }
        return klageService.updateKlage(klage, brukerService.getBruker())
    }

    @DeleteMapping("/klager/{klageId}")
    fun deleteKlage(@PathVariable klageId: Int) {
        logger.debug("Delete klage is requested. Id: ", { klageId })
        klageService.deleteKlage(klageId, brukerService.getBruker())
    }

    @PostMapping("/klager/{klageId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlage(
        @PathVariable klageId: Int
    ) {
        logger.debug("Finalize klage is requested. Id: {}", klageId)
        klageService.finalizeKlage(klageId, brukerService.getBruker())
    }

    @PostMapping(value = ["/klager/{klageId}/vedlegg"], consumes = ["multipart/form-data"])
    fun addVedleggToKlage(
        @PathVariable klageId: Int,
        @RequestParam vedlegg: MultipartFile
    ): Vedlegg {
        logger.debug("Add vedlegg to klage is requested. KlageId: {}", klageId)
        return vedleggService.addVedlegg(klageId, vedlegg)
    }

    @DeleteMapping("/klager/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: Int,
        @PathVariable vedleggId: Int
    ) {
        logger.debug("Delete vedlegg from klage is requested. KlageId: {}, VedleggId: {}", klageId, vedleggId)
        if (!vedleggService.deleteVedlegg(klageId, vedleggId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found.")
        }
    }

    @GetMapping("/vedtak")
    fun getVedtak(): List<Vedtak> {
        logger.debug("Get vedtak is requested.")
        return listOf()
    }
}

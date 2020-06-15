package no.nav.klage.controller

import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.domain.Vedtak
import no.nav.klage.domain.klage.KlageView
import no.nav.klage.service.BrukerService
import no.nav.klage.service.KlageService
import no.nav.klage.service.VedleggService
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
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
        return klageService.createKlage(klage, brukerService.getBruker())
    }

    @PutMapping("/klager/{klageId}")
    fun updateKlage(
        @PathVariable klageId: Int,
        @RequestBody klage: KlageView,
        response: HttpServletResponse
    ): KlageView {
        if (klage.id != klageId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id in klage does not match resource id")
        }
        return klageService.updateKlage(klage, brukerService.getBruker())
    }

    @DeleteMapping("/klager/{klageId}")
    fun deleteKlage(@PathVariable klageId: Int) {
        klageService.deleteKlage(klageId, brukerService.getBruker())
    }

    @PostMapping("/klager/{klageId}/vedlegg")
    fun addVedleggToKlage(
        @PathVariable klageId: Int,
        @ModelAttribute vedlegg: VedleggWrapper
    ): Vedlegg {
        return vedleggService.addVedlegg(klageId, vedlegg)
    }

    @PostMapping("/klager/{klageId}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalizeKlage(
        @PathVariable klageId: Int
    ) {
        klageService.finalizeKlage(klageId, brukerService.getBruker())
    }

    @DeleteMapping("/klager/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: Int,
        @PathVariable vedleggId: Int
    ) {
        val fnr = "From token"
        vedleggService.deleteVedlegg(fnr, klageId, vedleggId)
    }

    @GetMapping("/vedtak")
    fun getVedtak(): List<Vedtak> = listOf()
}

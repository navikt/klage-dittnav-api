package no.nav.klage.controller

import no.nav.klage.domain.Klage
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.domain.Vedtak
import no.nav.klage.getLogger
import no.nav.klage.service.KlageService
import no.nav.klage.clients.pdl.HentPdlPersonResponse
import no.nav.klage.service.VedleggService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import javax.servlet.http.HttpServletResponse

@RestController
class KlageController(
    private val klageService: KlageService,
    private val vedleggService: VedleggService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    //TODO: Security: verify user token for all

    @GetMapping("/bruker")
    fun getBruker(@RequestParam fnr: String): HentPdlPersonResponse {
        return klageService.getPersonInfo(fnr)
    }

    @GetMapping("/klager")
    fun getKlager(): List<Klage> {
        return klageService.getKlager()
    }

    @PostMapping("/klager")
    @ResponseStatus(HttpStatus.CREATED)
    fun createKlage(
        @RequestBody klage: Klage, response: HttpServletResponse
    ): Klage {
        return klageService.createKlage(klage)
    }

    @PutMapping("/klager/{id}")
    fun updateKlage(
        @PathVariable id: Int,
        @RequestBody klage: Klage,
        response: HttpServletResponse
    ): Klage {
        if (klage.id != id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id in klage does not match resource id")
        }
        return klageService.updateKlage(klage)
    }

    @DeleteMapping("/klager/{id}")
    fun deleteKlage(@PathVariable id: Int) {
        klageService.deleteKlage(id)
    }

    @PutMapping("/klager/{id}/vedlegg")
    fun putVedlegg(
        @PathVariable id: Int,
        @ModelAttribute vedlegg: VedleggWrapper
    ): Klage {
        val fnr = "From token"
        vedleggService.putVedlegg(fnr, id, vedlegg)
        return klageService.getKlage(id)
    }

    @DeleteMapping("/klager/{klageId}/vedlegg/{vedleggId}")
    fun deleteVedlegg(
        @PathVariable klageId: Int,
        @PathVariable vedleggId: String
    ) {
        val fnr = "From token"
        vedleggService.deleteVedlegg(fnr, klageId, vedleggId)
    }

    @GetMapping("/vedtak")
    fun getVedtak(): List<Vedtak> = listOf()
}

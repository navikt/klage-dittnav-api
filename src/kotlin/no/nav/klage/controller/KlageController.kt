package no.nav.klage.controller

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Vedtak
import no.nav.klage.getLogger
import no.nav.klage.service.KlageService
import no.nav.klage.services.pdl.HentPdlPersonResponse
import no.nav.klage.services.pdl.PdlClient
import no.nav.klage.services.saf.SafClient
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import javax.servlet.http.HttpServletResponse

@RestController
class KlageController(private val klageService: KlageService) {

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

    @GetMapping("/vedtak")
    fun getVedtak(): List<Vedtak> {
        val fnr = "TODO from token?"
        return safClient.getVedtak(fnr)
    }
}

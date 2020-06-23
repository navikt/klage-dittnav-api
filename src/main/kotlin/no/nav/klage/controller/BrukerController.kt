package no.nav.klage.controller

import no.nav.klage.domain.Bruker
import no.nav.klage.service.BrukerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class BrukerController(private val brukerService: BrukerService) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getBruker()
    }

    @GetMapping("/enhet/{enhetId}")
    fun getEnhetNavn(@PathVariable enhetId: Int): String {
        return brukerService.getEnhetsnavn(enhetId)
    }
}

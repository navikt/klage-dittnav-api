package no.nav.klage.controller

import io.swagger.annotations.Api
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.service.BrukerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Api(tags = ["bruker-prefixed"])
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RequestMapping("/api")
class BrukerControllerPrefixed(private val brukerService: BrukerService) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getBruker()
    }

    @GetMapping("/fullmaktsgiver/{tema}/{fnr}")
    fun getFullmaktsgiver(@PathVariable tema: Tema, @PathVariable fnr: String): Bruker {
        return brukerService.getFullmaktsgiver(tema, fnr)
    }
}

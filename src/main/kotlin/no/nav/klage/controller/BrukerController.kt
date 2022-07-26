package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.service.BrukerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "bruker")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class BrukerController(private val brukerService: BrukerService) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getBruker(useOboToken = false)
    }

    @GetMapping("/fullmaktsgiver/{tema}/{fnr}")
    fun getFullmaktsgiver(@PathVariable tema: Tema, @PathVariable fnr: String): Bruker {
        return brukerService.getFullmaktsgiver(tema, fnr)
    }
}

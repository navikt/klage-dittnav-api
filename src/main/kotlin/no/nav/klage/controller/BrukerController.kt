package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.config.SecurityConfiguration.Companion.TOKEN_X
import no.nav.klage.controller.view.GetPersonInput
import no.nav.klage.domain.Bruker
import no.nav.klage.service.BrukerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "bruker")
@ProtectedWithClaims(issuer = TOKEN_X, claimMap = ["acr=Level4"])
@RequestMapping("/api")
class BrukerController(
    private val brukerService: BrukerService,
) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getCurrentBruker()
    }

    @PostMapping("/bruker")
    fun getPerson(@RequestBody input: GetPersonInput): Bruker {
        return brukerService.getBruker(fnr = input.fnr, tema = input.tema)
    }
}

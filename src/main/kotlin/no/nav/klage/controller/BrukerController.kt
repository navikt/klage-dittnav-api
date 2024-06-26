package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.controller.view.AuthenticationStatus
import no.nav.klage.domain.Bruker
import no.nav.klage.service.BrukerService
import no.nav.klage.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "bruker")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api")
class BrukerController(
    private val brukerService: BrukerService,
    private val tokenUtil: TokenUtil,
) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getBruker()
    }

    @GetMapping("/bruker/authenticated")
    @Unprotected
    fun getAuthenticationStatus(): AuthenticationStatus {
        return AuthenticationStatus(
            authenticated = tokenUtil.isAuthenticated(),
            tokenx = tokenUtil.isAuthenticated(),
            selvbetjening = tokenUtil.isSelvbetjeningAuthenticated(),
        )
    }
}

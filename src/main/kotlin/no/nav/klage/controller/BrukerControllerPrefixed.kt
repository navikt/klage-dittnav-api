package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.service.BrukerService
import no.nav.klage.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "bruker-prefixed")
@ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
@RequestMapping("/api")
class BrukerControllerPrefixed(
    private val brukerService: BrukerService,
    private val tokenUtil: TokenUtil,
) {
    @GetMapping("/bruker")
    fun getBruker(): Bruker {
        return brukerService.getBruker()
    }

    @GetMapping("/fullmaktsgiver/{tema}/{fnr}")
    fun getFullmaktsgiver(@PathVariable tema: Tema, @PathVariable fnr: String): Bruker {
        return brukerService.getFullmaktsgiver(tema, fnr)
    }

    @GetMapping("/bruker/authenticated")
    @Unprotected
    fun getAuthenticationStatus(): AuthenticationStatus {
        return AuthenticationStatus(
            isAuthenticated = tokenUtil.isAuthenticated()
        )
    }

    data class AuthenticationStatus(
        val isAuthenticated: Boolean,
    )
}

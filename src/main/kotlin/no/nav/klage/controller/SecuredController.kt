package no.nav.klage.controller

import no.nav.klage.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

//Made for testing purposes, remove after verifying login.
@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class SecuredController(private val tokenUtil: TokenUtil) {

    @GetMapping("/secure")
    fun getSecured(): String {
        return "Secured! Current subject: ${tokenUtil.getSubject()}"
    }
}
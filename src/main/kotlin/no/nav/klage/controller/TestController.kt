package no.nav.klage.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
class TestController {

    @ProtectedWithClaims(issuer = "tokenx")
    @GetMapping("/test/protected")
    fun test(): String {
        return "Beskyttet, hei"
    }

    @Unprotected
    @GetMapping("/test/unprotected")
    fun testUbeskyttet(): String {
        return "Fritt fram, hei"
    }
}
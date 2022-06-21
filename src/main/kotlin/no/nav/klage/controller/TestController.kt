package no.nav.klage.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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

    @ProtectedWithClaims(issuer = "tokenx")
    @GetMapping("/api/test/protected")
    fun test2(): String {
        return "Beskyttet, hei"
    }

    @Unprotected
    @GetMapping("/api/test/unprotected")
    fun testUbeskyttet2(): String {
        return "Fritt fram, hei"
    }
}
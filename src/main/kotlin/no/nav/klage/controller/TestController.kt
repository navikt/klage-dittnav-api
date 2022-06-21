package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "test")
@RestController
@Profile("dev-gcp", "test")
class TestController {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

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

    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    @GetMapping("/api/test/protected")
    fun test2(): String {
        logger.debug("Beskyttet fungerte")
        return "Beskyttet, hei"
    }

    @Unprotected
    @GetMapping("/api/test/unprotected")
    fun testUbeskyttet2(): String {
        return "Fritt fram, hei"
    }
}
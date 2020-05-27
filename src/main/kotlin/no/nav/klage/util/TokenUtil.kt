package no.nav.klage.util

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtil(private val ctxHolder: TokenValidationContextHolder) {

    private val issuer = "selvbetjening"

    fun getSubject(): String {
        val subject = ctxHolder.tokenValidationContext?.getClaims(issuer)?.subject
        return checkNotNull(subject) { "Subject not found in token" }
    }

    fun getToken(): String {
        val token = ctxHolder.tokenValidationContext?.getJwtToken(issuer)?.tokenAsString
        return checkNotNull(token) { "Token must be present" }
    }
}


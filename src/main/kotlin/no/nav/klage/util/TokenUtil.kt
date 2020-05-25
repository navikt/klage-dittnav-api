package no.nav.klage.util

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtil(private val ctxHolder: TokenValidationContextHolder) {

    private val issuer = "selvbetjening"

    fun getSubject(): String? {
        return ctxHolder.tokenValidationContext?.getClaims(issuer)?.subject
    }

    fun getToken(): String? {
        return ctxHolder.tokenValidationContext?.getJwtToken(issuer)?.tokenAsString
    }
}


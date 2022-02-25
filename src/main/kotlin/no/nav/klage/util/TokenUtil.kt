package no.nav.klage.util

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtil(private val ctxHolder: TokenValidationContextHolder) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val issuer = "selvbetjening"
    }

    fun getSubject(): String {
        val token = ctxHolder.tokenValidationContext?.getClaims(issuer)

        if (token?.get("pid") != null) {
            logger.debug("Token: Found pid")
        } else if (token?.get("sub") != null) {
            logger.debug("Token: Found sub")
        } else {
            logger.debug("Token: Found none")
        }

        val subject = ctxHolder.tokenValidationContext?.getClaims(issuer)?.subject
        return checkNotNull(subject) { "Subject not found in token" }
    }

    fun getToken(): String {
        val token = ctxHolder.tokenValidationContext?.getJwtToken(issuer)?.tokenAsString
        return checkNotNull(token) { "Token must be present" }
    }

    fun getExpiry(): Long? = ctxHolder.tokenValidationContext?.getClaims(issuer)?.expirationTime?.time

}


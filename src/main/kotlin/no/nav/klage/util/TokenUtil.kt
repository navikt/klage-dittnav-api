package no.nav.klage.util

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.stereotype.Component

@Component
class TokenUtil(
    private val ctxHolder: TokenValidationContextHolder,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val issuer = "tokenx"
        private val secureLogger = getSecureLogger()
    }

    fun getSubject(): String {
        val token = ctxHolder.tokenValidationContext?.getClaims(issuer)

        val subject =
            if (token?.get("pid") != null) {
                token.get("pid").toString()
            } else if (token?.subject != null) {
                token.subject.toString()
            } else {
                throw JwtTokenValidatorException("pid/sub not found in token")
            }
        
        return subject
    }

    fun getToken(): String {
        val token = ctxHolder.tokenValidationContext?.getJwtToken(issuer)?.tokenAsString
        return checkNotNull(token) { "Token must be present" }
    }

    fun getOnBehalfOfTokenWithPdlScope(): String {
        val clientProperties = clientConfigurationProperties.registration["pdl-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        secureLogger.debug("oboToken for pdl: {}", response.accessToken)
        return response.accessToken
    }

    fun getExpiry(): Long? = ctxHolder.tokenValidationContext?.getClaims(issuer)?.expirationTime?.time

}


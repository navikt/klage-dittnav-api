package no.nav.klage.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenUtil(
    private val ctxHolder: TokenValidationContextHolder,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val request: HttpServletRequest,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val issuer = "tokenx"
        private val oldIssuer = "selvbetjening"
        private val secureLogger = getSecureLogger()
    }

    fun getSubject(useTokenX: Boolean = true): String {
        val token = ctxHolder.tokenValidationContext?.getClaims(if (useTokenX) issuer else oldIssuer)

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

    fun getToken(useTokenX: Boolean = true): String {
        val token = ctxHolder.tokenValidationContext?.getJwtToken(if (useTokenX) issuer else oldIssuer)?.tokenAsString
        return checkNotNull(token) { "Token must be present" }
    }

    fun isAuthenticated(): Boolean {
        ctxHolder.tokenValidationContext?.getJwtToken(issuer) ?: return false
        //TODO: Finn en måte å bruke token-support på til dette.
        if (getExpiryFromIdPortenToken(request.getHeader("idporten-token")) - 100000 < System.currentTimeMillis()) {
            return false
        }
        return true
    }

    fun isSelvbetjeningAuthenticated(): Boolean {
        ctxHolder.tokenValidationContext?.getJwtToken(oldIssuer) ?: return false
        //TODO: Finn en måte å bruke token-support på til dette.
        if (getSelvbetjeningExpiry()!!.minus(100000) < System.currentTimeMillis()) {
            return false
        }
        return true
    }

    fun getOnBehalfOfTokenWithPdlScope(): String {
        val clientProperties = clientConfigurationProperties.registration["pdl-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getOnBehalfOfTokenWithSafselvbetjeningScope(): String {
        val clientProperties = clientConfigurationProperties.registration["safselvbetjening-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getSelvbetjeningExpiry(): Long? = ctxHolder.tokenValidationContext?.getClaims(oldIssuer)?.expirationTime?.time

    fun getOnBehalfOfTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-onbehalfof"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getExpiryFromIdPortenToken(token: String): Long {
        val correctPartOfToken = Base64.getDecoder().decode(token.split(".")[1])
        val value = jacksonObjectMapper().readTree(correctPartOfToken)
        return value["exp"].asLong() * 1000
    }

}


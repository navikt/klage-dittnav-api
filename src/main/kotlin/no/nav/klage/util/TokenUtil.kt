package no.nav.klage.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
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
        val token = ctxHolder.getTokenValidationContext().getClaims(if (useTokenX) issuer else oldIssuer)

        val subject =
            if (token.get("pid") != null) {
                token.get("pid").toString()
            } else {
                token.subject.toString()
            }

        return subject
    }

    fun getToken(useTokenX: Boolean = true): String {
        val token = ctxHolder.getTokenValidationContext().getJwtToken(if (useTokenX) issuer else oldIssuer)?.encodedToken
        return checkNotNull(token) { "Token must be present" }
    }

    fun isAuthenticated(): Boolean {
        ctxHolder.getTokenValidationContext().getJwtToken(issuer) ?: return false
        //TODO: Finn en måte å bruke token-support på til dette.
        return getExpiryFromIdPortenToken(request.getHeader("idporten-token")) - 100000 >= System.currentTimeMillis()
    }

    fun isSelvbetjeningAuthenticated(): Boolean {
        ctxHolder.getTokenValidationContext().getJwtToken(oldIssuer) ?: return false
        //TODO: Finn en måte å bruke token-support på til dette.
        return getSelvbetjeningExpiry()!!.minus(100000) >= System.currentTimeMillis()
    }

    fun getOnBehalfOfTokenWithPdlScope(): String {
        val clientProperties = clientConfigurationProperties.registration["pdl-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken!!
    }

    fun getAppAccessTokenWithPdlScope(): String {
        val clientProperties = clientConfigurationProperties.registration["pdl-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken!!
    }

    fun getOnBehalfOfTokenWithSafselvbetjeningScope(): String {
        val clientProperties = clientConfigurationProperties.registration["safselvbetjening-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken!!
    }

    fun getSelvbetjeningExpiry(): Long? = ctxHolder.getTokenValidationContext().getClaims(oldIssuer).expirationTime?.time

    fun getAppAccessTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken!!
    }

    fun getExpiryFromIdPortenToken(token: String): Long {
        val correctPartOfToken = Base64.getDecoder().decode(token.split(".")[1])
        val value = jacksonObjectMapper().readTree(correctPartOfToken)
        return value["exp"].asLong() * 1000
    }

}


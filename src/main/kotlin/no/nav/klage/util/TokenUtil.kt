package no.nav.klage.util

import no.nav.klage.config.SecurityConfiguration.Companion.TOKEN_X
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtil(
    private val ctxHolder: TokenValidationContextHolder,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    fun getSubject(): String = ctxHolder.getTokenValidationContext().getClaims(TOKEN_X).getStringClaim("pid")

    fun getOnBehalfOfTokenWithSafSelvbetjeningScope(): String {
        val clientProperties = clientConfigurationProperties.registration["safselvbetjening-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getAppAccessTokenWithKlageFileApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-file-api-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getAppAccessTokenWithKlageFSSProxyScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-fss-proxy-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getOnBehalfOfTokenWithKlageLookupScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-lookup-onbehalfof"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }

    fun getAppAccessTokenWithKlageLookupScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-lookup-maskintilmaskin"]!!
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.access_token!!
    }
}

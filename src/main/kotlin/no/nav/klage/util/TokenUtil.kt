package no.nav.klage.util

import no.nav.klage.auth.TexasClient
import no.nav.klage.auth.currentTokenXToken
import no.nav.klage.auth.pid
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TokenUtil(
    private val texasClient: TexasClient,
    @Value($$"${SAFSELVBETJENING_AUDIENCE}")
    private val safSelvbetjeningAudience: String,
    @Value($$"${KLAGE_LOOKUP_AUDIENCE}")
    private val klageLookupAudience: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_FILE_API_APP_NAME}/.default")
    private val klageFileApiScope: String,
    @Value($$"api://${FSS_CLUSTER}.klage.klage-fss-proxy/.default")
    private val klageFssProxyScope: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_LOOKUP_APP_NAME}/.default")
    private val klageLookupScope: String,
) {
    /** The fødselsnummer of the logged in citizen. Only on the request thread of a TokenX request. */
    fun getSubject(): String = currentTokenXToken()?.pid ?: throw IllegalStateException("No logged in citizen in this request.")

    fun getOnBehalfOfTokenWithSafSelvbetjeningScope(): String = texasClient.tokenXExchangeForCurrentUser(safSelvbetjeningAudience)

    fun getAppAccessTokenWithKlageFileApiScope(): String = texasClient.machineToMachineToken(klageFileApiScope)

    fun getAppAccessTokenWithKlageFSSProxyScope(): String = texasClient.machineToMachineToken(klageFssProxyScope)

    fun getOnBehalfOfTokenWithKlageLookupScope(): String = texasClient.tokenXExchangeForCurrentUser(klageLookupAudience)

    fun getAppAccessTokenWithKlageLookupScope(): String = texasClient.machineToMachineToken(klageLookupScope)
}

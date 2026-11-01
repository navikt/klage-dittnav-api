package no.nav.klage.util

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.auth.TexasClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class TokenUtilTest {
    private val texasClient = mockk<TexasClient>()
    private val tokenUtil =
        TokenUtil(
            texasClient = texasClient,
            safSelvbetjeningAudience = "dev-fss:teamdokumenthandtering:safselvbetjening",
            klageLookupAudience = "dev-gcp:klage:klage-lookup",
            klageFileApiScope = "api://dev-gcp.klage.klage-file-api/.default",
            klageFssProxyScope = "api://dev-fss.klage.klage-fss-proxy/.default",
            klageLookupScope = "api://dev-gcp.klage.klage-lookup/.default",
        )

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `reads pid from the current TokenX token`() {
        authenticate(claims = mapOf("pid" to "12345678910"))

        assertThat(tokenUtil.getSubject()).isEqualTo("12345678910")
    }

    @Test
    fun `fails when the current token has no pid`() {
        authenticate(claims = mapOf("idtyp" to "app"))

        assertThrows<IllegalStateException> { tokenUtil.getSubject() }
    }

    @Test
    fun `fails outside an authenticated request`() {
        assertThrows<IllegalStateException> { tokenUtil.getSubject() }
    }

    @Test
    fun `asks Texas for tokens with the right targets`() {
        every { texasClient.tokenXExchangeForCurrentUser(target = "dev-fss:teamdokumenthandtering:safselvbetjening") } returns "saf"
        every { texasClient.tokenXExchangeForCurrentUser(target = "dev-gcp:klage:klage-lookup") } returns "lookup-tokenx"
        every { texasClient.machineToMachineToken(target = "api://dev-gcp.klage.klage-file-api/.default") } returns "file"
        every { texasClient.machineToMachineToken(target = "api://dev-fss.klage.klage-fss-proxy/.default") } returns "fss-proxy"
        every { texasClient.machineToMachineToken(target = "api://dev-gcp.klage.klage-lookup/.default") } returns "lookup-m2m"

        assertThat(tokenUtil.getOnBehalfOfTokenWithSafSelvbetjeningScope()).isEqualTo("saf")
        assertThat(tokenUtil.getOnBehalfOfTokenWithKlageLookupScope()).isEqualTo("lookup-tokenx")
        assertThat(tokenUtil.getAppAccessTokenWithKlageFileApiScope()).isEqualTo("file")
        assertThat(tokenUtil.getAppAccessTokenWithKlageFSSProxyScope()).isEqualTo("fss-proxy")
        assertThat(tokenUtil.getAppAccessTokenWithKlageLookupScope()).isEqualTo("lookup-m2m")
    }

    private fun authenticate(claims: Map<String, Any>) {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "subject")
                .claims { it.putAll(claims) }
                .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }
}

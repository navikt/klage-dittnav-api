package no.nav.klage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.controller.view.AuthenticationStatus
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Identifikator
import no.nav.klage.domain.Navn
import no.nav.klage.service.BrukerService
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
class ApiTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    @MockkBean
    lateinit var brukerService: BrukerService

    private val FNR = "12345678910"

    val mapper = jacksonObjectMapper()

    @BeforeEach
    fun beforeEach() {
        every { brukerService.getBruker() } returns Bruker(
            navn = Navn(fornavn = "", mellomnavn = null, etternavn = ""),
            adresse = null,
            kontaktinformasjon = null,
            folkeregisteridentifikator = Identifikator(type = "", identifikasjonsnummer = ""),
            tokenExpires = null,
        )
    }

    @Test
    fun `kall på GET bruker med gyldig token gir forventet resultat`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/bruker")
                .header("Authorization", "Bearer ${tokenxToken(fnr = FNR)}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `kall på GET bruker uten token gir forventet resultat`() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/bruker")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        val problemOutput = mapper.readValue(response.andReturn().response.contentAsString, ProblemDetail::class.java)
        assertEquals("No authorization header in request", problemOutput.detail)
    }

    @Test
    fun `kall på GET bruker med utgått token gir forventet resultat`() {
        val token = tokenxToken(fnr = FNR, expiry = -100)

        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/bruker")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        val problemOutput = mapper.readValue(response.andReturn().response.contentAsString, ProblemDetail::class.java)
        assertEquals("No valid token found in validation context", problemOutput.detail)
    }

    @Test
    fun `kall på GET bruker med feil audience i token gir forventet resultat`() {
        val token = tokenxToken(fnr = FNR, audience = "noeheltannet")

        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/bruker")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        val problemOutput = mapper.readValue(response.andReturn().response.contentAsString, ProblemDetail::class.java)
        assertEquals("No valid token found in validation context", problemOutput.detail)
    }

    @Test
    fun contextLoads() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }

    @Test
    fun apiDocsLoads() {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs?group=internal"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Nested
    inner class Authenticated {

        @Test
        fun `kall på GET authenticated uten token gir tokenx false`() {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/bruker/authenticated")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val output = mapper.readValue(response.contentAsString, AuthenticationStatus::class.java)
            assertFalse(output.tokenx)
            assertFalse(output.selvbetjening)
        }

        @Test
        fun `kall på GET authenticated med selvbetjeningstoken gir tokenx false og selvbetjening true`() {
            val token = selvbetjeningToken(fnr = FNR)

            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/bruker/authenticated")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val output = mapper.readValue(response.contentAsString, AuthenticationStatus::class.java)
            assertFalse(output.tokenx)
            assertTrue(output.selvbetjening)
        }

        @Test
        fun `kall på GET authenticated med tokenx-token gir tokenx true`() {
            val token = tokenxToken(fnr = FNR)

            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/bruker/authenticated")
                    .header("Authorization", "Bearer $token")
                    .header("idporten-token", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val output = mapper.readValue(response.contentAsString, AuthenticationStatus::class.java)
            assertTrue(output.tokenx)
        }

        @Test
        fun `kall på GET authenticated med tokenx-token og utgått idportenToken gir tokenx false`() {
            val token = tokenxToken(fnr = FNR)
            val idportenToken = tokenxToken(fnr = FNR, expiry = 1)

            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/bruker/authenticated")
                    .header("Authorization", "Bearer $token")
                    .header("idporten-token", "Bearer $idportenToken")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val output = mapper.readValue(response.contentAsString, AuthenticationStatus::class.java)
            assertFalse(output.tokenx)
            assertFalse(output.selvbetjening)
        }
    }

    fun tokenxToken(
        fnr: String,
        audience: String = "klage-dittnav-api-client-id",
        issuerId: String = "tokenx",
        clientId: String = "klage-dittnav-client-id",
        claims: Map<String, Any> = mapOf(
            "acr" to "Level4",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to fnr,
        ),
        expiry: Long = 3600,
    ): String {

        return server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = expiry,
            )
        ).serialize()
    }

    fun selvbetjeningToken(
        fnr: String,
        audience: String = "klage-dittnav-api-client-id",
        issuerId: String = "selvbetjening",
        clientId: String = "klage-dittnav-client-id",
        claims: Map<String, Any> = mapOf(
            "acr" to "Level4",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to fnr,
        ),
        expiry: Long = 3600,
    ): String {

        return server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = expiry
            )
        ).serialize()
    }
}
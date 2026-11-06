package no.nav.klage

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.auth.test.MockAzure
import no.nav.klage.auth.test.TokenXSecurityContractTest
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Identifikator
import no.nav.klage.domain.Navn
import no.nav.klage.service.BrukerService
import no.nav.klage.service.CommonService
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
class ApiTest : TokenXSecurityContractTest() {
    companion object {
        private val mockAzure = MockAzure()

        @JvmStatic
        @DynamicPropertySource
        fun azureProperties(registry: DynamicPropertyRegistry) {
            mockAzure.registerProperties(registry)
        }

        private const val ALLOWED_ORIGIN = "http://localhost:3000"
    }

    @MockkBean
    lateinit var brukerService: BrukerService

    @MockkBean(relaxed = true)
    lateinit var commonService: CommonService

    // The real client starts a Kafka consumer, which fails without a broker.
    @MockkBean(relaxed = true)
    lateinit var kafkaEventClient: KafkaEventClient

    private val azurePath = "/internal/klanker/${UUID.randomUUID()}/journalpostid"

    override fun protectedRequest(): MockHttpServletRequestBuilder = get("/api/bruker")

    override fun unauthenticatedPaths() =
        listOf(
            "/internal/health",
            "/api/innsendingsytelser/NB",
            "/favicon.ico",
            "/v3/api-docs",
        )

    @BeforeEach
    fun beforeEach() {
        every { brukerService.getCurrentBruker() } returns
            Bruker(
                navn = Navn(fornavn = "", etternavn = ""),
                folkeregisteridentifikator = Identifikator(type = "", identifikasjonsnummer = ""),
            )
        every { commonService.getJournalpostIdWithoutValidation(any()) } returns "journalpost-id"
    }

    @Test
    fun `internal klanke endpoints accept Azure token from pre-authorized app`() {
        mockMvc
            .perform(get(azurePath).bearer(mockAzure.token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.journalpostId").value("journalpost-id"))
    }

    @Test
    fun `internal klanke endpoints reject request without token`() {
        mockMvc.perform(get(azurePath)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `internal klanke endpoints reject Azure token from app that is not pre-authorized`() {
        mockMvc.perform(get(azurePath).bearer(mockAzure.token(azp = "unknown-client"))).andExpect(status().isUnauthorized)
    }

    @Test
    fun `internal klanke endpoints reject TokenX token`() {
        mockMvc.perform(get(azurePath).bearer(mockTokenX.token())).andExpect(status().isUnauthorized)
    }

    @Test
    fun `citizen endpoints reject Azure token`() {
        mockMvc.perform(get("/api/bruker").bearer(mockAzure.token())).andExpect(status().isUnauthorized)
    }

    @Test
    fun `pdf for citizens who are not logged in needs no token`() {
        val result =
            mockMvc
                .perform(post("/api/pdf/klanke").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andReturn()
        assertNotEquals(401, result.response.status)
    }

    @Test
    fun `dev only endpoint for klage without login requires token outside dev`() {
        mockMvc.perform(post("/internal/uinnloggetklage")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `CORS preflight from allowed origin succeeds without token`() {
        mockMvc
            .perform(
                options("/api/bruker")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `401 has CORS headers so the frontend can read it`() {
        mockMvc
            .perform(get("/api/bruker").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
    }

    @Test
    fun `health shows no details`() {
        mockMvc
            .perform(get("/internal/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
    }

    @Test
    fun apiDocsLoads() {
        mockMvc
            .perform(get("/v3/api-docs?group=internal"))
            .andExpect(status().isOk)
    }

    private fun MockHttpServletRequestBuilder.bearer(token: String) = header(HttpHeaders.AUTHORIZATION, "Bearer $token")
}

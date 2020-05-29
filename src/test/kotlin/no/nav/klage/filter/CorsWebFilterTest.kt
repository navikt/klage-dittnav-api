package no.nav.klage.filter

import no.nav.klage.controller.KlageController
import no.nav.klage.controller.SecuredController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
internal class CorsWebFilterTest {

    @MockBean
    lateinit var controller: KlageController

    @MockBean
    lateinit var securedController: SecuredController

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `response should include Access-Control-Allow-Origin header`() {
        val response: WebTestClient.ResponseSpec = webTestClient.get()
            .uri("/bruker")
            .exchange()

        response.expectHeader().exists("Access-Control-Allow-Origin")
    }
}
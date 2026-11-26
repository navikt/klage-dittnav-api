package no.nav.klage.clients

import io.mockk.mockk
import no.nav.klage.domain.exception.AttachmentNotFoundException
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class FileClientTest {
    private val tokenUtil = mockk<TokenUtil>(relaxed = true)

    @Test
    fun `not found from file store throws AttachmentNotFoundException`() {
        val fileClient = FileClient(fileWebClient = webClientReturning(HttpStatus.NOT_FOUND), tokenUtil = tokenUtil)

        assertThrows<AttachmentNotFoundException> { fileClient.getVedleggFile("ref") }
    }

    @Test
    fun `server error from file store is not mapped to AttachmentNotFoundException`() {
        val fileClient = FileClient(fileWebClient = webClientReturning(HttpStatus.INTERNAL_SERVER_ERROR), tokenUtil = tokenUtil)

        assertThrows<WebClientResponseException.InternalServerError> { fileClient.getVedleggFile("ref") }
    }

    private fun webClientReturning(status: HttpStatus): WebClient =
        WebClient
            .builder()
            .exchangeFunction(ExchangeFunction { Mono.just(ClientResponse.create(status).build()) })
            .build()
}

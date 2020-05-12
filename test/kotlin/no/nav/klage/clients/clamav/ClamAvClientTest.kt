package no.nav.klage.clients.clamav

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class ClamAvClientTest {

    @Test
    fun `ok response returns true`() {
        val clientResponse: ClientResponse = ClientResponse
            .create(HttpStatus.OK)
            .header("Content-Type","application/json")
            .body(okResponse).build()

        val shortCircuitingExchangeFunction = ExchangeFunction {
            Mono.just(clientResponse)
        }

        val webClient = WebClient.builder().exchangeFunction(shortCircuitingExchangeFunction).build()
        val clamClient = ClamAvClient(webClient)

        assertTrue(clamClient.scan(ByteArray(0)))
    }

    @Test
    fun `found response returns false`() {
        val clientResponse: ClientResponse = ClientResponse
            .create(HttpStatus.OK)
            .header("Content-Type","application/json")
            .body(foundResponse).build()

        val shortCircuitingExchangeFunction = ExchangeFunction {
            Mono.just(clientResponse)
        }

        val webClient = WebClient.builder().exchangeFunction(shortCircuitingExchangeFunction).build()
        val clamClient = ClamAvClient(webClient)

        assertFalse(clamClient.scan(ByteArray(0)))
    }

    @Test
    fun `response with multiple entries returns false`() {
        val clientResponse: ClientResponse = ClientResponse
            .create(HttpStatus.OK)
            .header("Content-Type","application/json")
            .body(multipleResponse).build()

        val shortCircuitingExchangeFunction = ExchangeFunction {
            Mono.just(clientResponse)
        }

        val webClient = WebClient.builder().exchangeFunction(shortCircuitingExchangeFunction).build()
        val clamClient = ClamAvClient(webClient)

        assertFalse(clamClient.scan(ByteArray(0)))
    }


    @Language("json")
    private val okResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "OK"
              }
            ]
        """.trimIndent()

    @Language("json")
    private val foundResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "FOUND"
              }
            ]
        """.trimIndent()

    @Language("json")
    private val multipleResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "FOUND"
              },
              {
                "Filename": "testfile",
                "Result": "OK"
              }
            ]
        """.trimIndent()
}

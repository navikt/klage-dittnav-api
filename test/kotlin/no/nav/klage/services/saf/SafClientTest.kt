package no.nav.klage.services.saf

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class SafClientTest {

    @Test
    fun `json with 3 elements and one correct title yields 1 Vedtak`() {
        val clientResponse: ClientResponse = ClientResponse
            .create(HttpStatus.OK)
            .header("Content-Type","application/json")
            .body(safJson).build()

        val shortCircuitingExchangeFunction = ExchangeFunction {
            Mono.just(clientResponse)
        }

        val webClientBuilder = WebClient
            .builder()
            .exchangeFunction(shortCircuitingExchangeFunction)

        val safClient = SafClient(webClientBuilder.build())

        val vedtakList = safClient.getVedtak("123456789")

        Assertions.assertEquals(1, vedtakList.size)
    }

    @Language("json")
    private val safJson = """
        {
          "data": {
            "dokumentoversiktBruker": {
              "journalposter": [
                {
                  "journalpostId": "429111291",
                  "tittel": "Vedtak test",
                  "tema": "test",
                  "journalfoerendeEnhet": "test",
                  "datoOpprettet": "2018-01-01T12:00:00"
                },
                {
                  "journalpostId": "429108246",
                  "tittel": "test",
                  "tema": "test",
                  "journalfoerendeEnhet": "test",
                  "datoOpprettet": "2018-01-01T12:00:00"
                },
                {
                  "journalpostId": "428965411",
                  "tittel": "test",
                  "tema": "test",
                  "journalfoerendeEnhet": "test",
                  "datoOpprettet": "2018-01-01T12:00:00"
                }
              ]
            }
          }
        }
    """.trimIndent()

}

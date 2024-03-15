package no.nav.klage.clients.safselvbetjening

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import org.slf4j.Logger
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import java.nio.file.Path

@Component
class SafselvbetjeningRestClient(
    private val safselvbetjeningWebClient: WebClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Retryable
    fun downloadDocumentAsMono(
        journalpostId: String,
        dokumentInfoId: String,
        variantFormat: String = "ARKIV",
        pathToFile: Path,
    ): Mono<Void> {
        return try {
            runWithTimingAndLogging {
                val flux: Flux<DataBuffer> = safselvbetjeningWebClient.get()
                    .uri(
                        "/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}",
                        journalpostId,
                        dokumentInfoId,
                        variantFormat
                    )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer ${tokenUtil.getOnBehalfOfTokenWithSafselvbetjeningScope()}"
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::isError) { response ->
                        logErrorResponse(response, ::downloadDocumentAsMono.name, secureLogger)
                    }
                    .bodyToFlux(DataBuffer::class.java)
                DataBufferUtils.write(flux, pathToFile)
            }
        } catch (badRequest: WebClientResponseException.BadRequest) {
            logger.warn("Got a 400 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw badRequest
        } catch (unauthorized: WebClientResponseException.Unauthorized) {
            logger.warn("Got a 401 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw unauthorized
        } catch (forbidden: WebClientResponseException.Forbidden) {
            logger.warn("Got a 403 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw forbidden
        } catch (notFound: WebClientResponseException.NotFound) {
            logger.warn("Got a 404 fetching dokument with journalpostId $journalpostId, dokumentInfoId $dokumentInfoId and variantFormat $variantFormat")
            throw notFound
        }
    }


    fun <T> runWithTimingAndLogging(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = System.currentTimeMillis()
            logger.debug("Time it took to call saf: ${end - start} millis")
        }
    }

    fun logErrorResponse(response: ClientResponse, functionName: String, logger: Logger): Mono<RuntimeException> {
        return response.bodyToMono(String::class.java).map {
            val errorString =
                "Got ${response.statusCode()} when requesting $functionName - response body: '$it'"
            logger.error(errorString)
            RuntimeException(errorString)
        }
    }
}
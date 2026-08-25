package no.nav.klage.clients.klagelookup

import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.slf4j.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono


@Component
class KlageLookupClient(
    private val klageLookupWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    @Retryable
    fun getRepresentasjonsforhold(): RepresentasjonsforholdView {
        return runWithTimingAndLogging {
            val token = "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageLookupScope()}"

            klageLookupWebClient.get()
                .uri("/external/representasjon/kan-representere")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    token,
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(
                        response = response,
                        functionName = ::getRepresentasjonsforhold.name,
                        logger = teamLogger,
                    )
                }
                .bodyToMono<RepresentasjonsforholdView>()
                .block() ?: throw RuntimeException("Could not get representasjonsdata")
        }
    }

    @Retryable
    fun getPerson(fnr: String, tema: Tema?): PersonResponse {
        return runWithTimingAndLogging {
            klageLookupWebClient.post()
                .uri("/external/person")
                .bodyValue(
                    GetPersonRequest(
                        fnr = fnr,
                        tema = tema,
                    )
                )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageLookupScope()}",
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(
                        response = response,
                        functionName = ::getPerson.name,
                        logger = teamLogger,
                    )
                }
                .bodyToMono<PersonResponse>()
                .block() ?: throw RuntimeException("Could not get person. Response was null.")
        }
    }

    @Retryable
    fun getPersonAsSystemUser(fnr: String): PersonWithAllInfoResponse {
        return runWithTimingAndLogging {
            klageLookupWebClient.post()
                .uri("/person")
                .bodyValue(
                    GetPersonAsSystemUserRequest(
                        fnr = fnr,
                    )
                )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getAppAccessTokenWithKlageLookupScope()}",
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(
                        response = response,
                        functionName = ::getPersonAsSystemUser.name,
                        logger = teamLogger,
                    )
                }
                .bodyToMono<PersonWithAllInfoResponse>()
                .block() ?: throw RuntimeException("Could not get person as system user. Response was null.")
        }
    }

    private fun <T> runWithTimingAndLogging(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = System.currentTimeMillis()
            logger.debug("Time it took to call klage-lookup: ${end - start} millis")
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
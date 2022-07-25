package no.nav.klage.clients.foerstesidegenerator

import no.nav.klage.clients.foerstesidegenerator.domain.*
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getSecureLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FoerstesidegeneratorClient(
    private val foerstesidegeneratorWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val secureLogger = getSecureLogger()
    }

    fun createFoersteside(): ByteArray {
        runCatching {

            val res = foerstesidegeneratorWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageFSSProxyScope()}")
                //Values (except fnr) taken from actual nav.no request test
                .bodyValue(
                    PostFoerstesideRequest(
                        spraakkode = Spraakkode.NB,
                        adresse = null,
                        netsPostboks = "1400",
                        avsender = null,
                        bruker = Bruker(
                            brukerId = "10018210091",
                            brukerType = Brukertype.PERSON
                        ),
                        ukjentBrukerPersoninfo = null,
                        tema = "DAG",
                        behandlingstema = null,
                        arkivtittel = "Klage/anke",
                        vedleggsliste = listOf("Annet"),
                        navSkjemaId = "NAV 90-00.08",
                        overskriftstittel = "En tittel",
                        dokumentlisteFoersteside = listOf("NAV 90-00.08 Klage/anke", "Annet"),
                        foerstesidetype = Foerstesidetype.SKJEMA,
                        enhetsnummer = null,
                        arkivsak = null,
                    )
                )
                .retrieve()
                .bodyToMono<PostFoerstesideResponse>()
                .block() ?: throw RuntimeException("Null response when getting foersteside")

            return res.foersteside

        }.onFailure {
            secureLogger.error("Could not fetch foersteside", it)
            throw RuntimeException("Could not fetch foersteside")
        }

        error("?")
    }

}

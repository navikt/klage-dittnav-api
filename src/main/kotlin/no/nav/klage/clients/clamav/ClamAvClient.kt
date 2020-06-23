package no.nav.klage.clients.clamav

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ClamAvClient(private val clamAvWebClient: WebClient) {

    private val logger = LoggerFactory.getLogger(ClamAvClient::class.java)

    fun scan(file: ByteArray): Boolean {
        val response = clamAvWebClient.put()
            .bodyValue(file)
            .retrieve()
            .bodyToMono<List<ScanResult>>()
            .block() ?: listOf()

        if(response.size != 1) {
            logger.warn("Wrong size response from virus scan.")
            return false
        }

        val (filename, result) = response[0]
        return when(result) {
            ClamAvResult.OK -> true
            ClamAvResult.FOUND -> {
                logger.warn("$filename has virus")
                false
            }
            ClamAvResult.ERROR -> {
                logger.warn("Error from virus scan on file $filename")
                false
            }
        }
    }
}

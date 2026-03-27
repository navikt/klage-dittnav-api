package no.nav.klage.clients.clamav

import org.slf4j.LoggerFactory
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ClamAvClient(private val clamAvWebClient: WebClient) {

    private val logger = LoggerFactory.getLogger(ClamAvClient::class.java)

    @Retryable
    fun hasVirus(file: ByteArray): Boolean {
        val fileSizeMB = file.size / 1_048_576.0
        logger.debug("Scanning document of size {} MB", String.format("%.2f", fileSizeMB))

        val startTime = System.currentTimeMillis()
        val result =
            clamAvWebClient.put()
                .bodyValue(file)
                .retrieve()
                .bodyToMono<List<ScanResult>>()
                .block()?.firstOrNull() ?: throw RuntimeException("Received empty response from ClamAV")

        val durationMs = System.currentTimeMillis() - startTime
        logger.debug("ClamAV scan completed in {} ms for file of {} MB. Result: {}", durationMs, String.format("%.2f", fileSizeMB), result.result)

        return when (result.result) {
            ClamAvResult.OK -> false
            ClamAvResult.FOUND -> {
                logger.warn("Virus found in file: {}. Virus: {}", result.filename, result.virus)
                true
            }
            ClamAvResult.ERROR -> {
                logger.error("Error scanning file for virus: {}. Error: {}", result.filename, result.error)
                throw RuntimeException("Error from ClamAV virus scan on file: ${result.filename}. Error: ${result.error}")
            }
        }
    }
}

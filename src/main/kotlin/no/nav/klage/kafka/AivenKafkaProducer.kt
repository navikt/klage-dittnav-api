package no.nav.klage.kafka

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AivenKafkaProducer(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value($$"${KAFKA_TOPIC}")
    lateinit var topic: String

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
        private const val SEND_TIMEOUT_SECONDS = 30L
    }

    /**
     * Sends the payload and waits for broker acknowledgement.
     * @throws KafkaSendException if the payload could not be confirmed sent.
     */
    fun sendToKafka(klageAnkeToKafka: AggregatedKlageAnke) {
        logger.debug("Sending klanke {} to Kafka topic: {}", klageAnkeToKafka.id, topic)
        val json = klageAnkeToKafka.toJson()
        try {
            aivenKafkaTemplate.send(topic, json).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            logger.debug("Klanke {} sent to Kafka.", klageAnkeToKafka.id)
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            logger.error("Could not send klanke ${klageAnkeToKafka.id} to Kafka. Check team-logs for more information.")
            teamLogger.error("Could not send klanke ${klageAnkeToKafka.id} to Kafka", e)
            throw KafkaSendException(message = "Could not send klanke ${klageAnkeToKafka.id} to Kafka", cause = e)
        }
    }

    fun AggregatedKlageAnke.toJson(): String = jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(this)
}

class KafkaSendException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

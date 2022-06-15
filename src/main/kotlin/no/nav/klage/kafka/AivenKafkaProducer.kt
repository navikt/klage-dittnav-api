package no.nav.klage.kafka

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.klage.AggregatedKlage
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class AivenKafkaProducer(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>
) {

    @Value("\${KAFKA_TOPIC}")
    lateinit var topic: String

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun sendToKafka(klage: AggregatedKlage) {
        logger.debug("Sending to Kafka topic: {}", topic)
        val json = klage.toJson()
        secureLogger.debug("Sending to Kafka topic: {}\npayload: {}", topic, json)
        runCatching {
            aivenKafkaTemplate.send(topic, json).get()
            logger.debug("Payload sent to Kafka.")
        }.onFailure {
            val errorMessage =
                "Could not send payload to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send payload to Kafka", it)
        }
    }

    fun AggregatedKlage.toJson(): String = jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(this)
}

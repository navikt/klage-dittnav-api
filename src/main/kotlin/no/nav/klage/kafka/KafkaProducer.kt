package no.nav.klage.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.klage.domain.AggregatedKlage
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {

    @Value("\${KAFKA_TOPIC}")
    lateinit var topic: String

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
        private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    fun sendToKafka(klage: AggregatedKlage) {
        logger.debug("Sending to Kafka topic: {}", topic)
        secureLogger.debug("Sending to Kafka topic: {}\nKlage: {}", topic, klage)
        kotlin.runCatching {
            kafkaTemplate.send(topic, klage.toJson())
            logger.debug("Klage sent to Kafka.")
        }.onFailure {
            val errorMessage = "Could not send klage to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send klage to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }

    fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}

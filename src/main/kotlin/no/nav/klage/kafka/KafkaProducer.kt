package no.nav.klage.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.klage.domain.AggregatedKlage
import no.nav.klage.util.getLogger
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
        private val objectMapper = ObjectMapper()
    }

    fun sendToKafka(klage: AggregatedKlage) {
        logger.debug("Sending to Kafka topic: {}\nKlage: {}", topic, klage)

        kafkaTemplate.send(topic, klage.toJson())

        logger.debug("Klage sent to kafka")
    }

    fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}

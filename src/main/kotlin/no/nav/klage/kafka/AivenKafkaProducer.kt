package no.nav.klage.kafka

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
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
        private val teamLogger = getTeamLogger()
    }

    fun sendToKafka(klageAnkeToKafka: AggregatedKlageAnke) {
        logger.debug("Sending to Kafka topic: {}", topic)
        val json = klageAnkeToKafka.toJson()
        runCatching {
            aivenKafkaTemplate.send(topic, json).get()
            logger.debug("Payload sent to Kafka.")
        }.onFailure {
            val errorMessage =
                "Could not send payload to Kafka. Check team-logs for more information."
            logger.error(errorMessage)
            teamLogger.error("Could not send payload to Kafka", it)
        }
    }

    fun AggregatedKlageAnke.toJson(): String = jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(this)
}

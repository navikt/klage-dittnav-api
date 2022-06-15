package no.nav.klage.kafka

import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class AivenKafkaProducer(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun publishToKafkaTopic(klagebehandlingId: UUID, json: String, topic: String) {
        logger.debug("Sending to Kafka topic: {}", topic)
        secureLogger.debug("Sending to Kafka topic: {}\npayload: {}", topic, json)
        runCatching {
            aivenKafkaTemplate.send(topic, klagebehandlingId.toString(), json).get()
            logger.debug("Payload sent to Kafka.")
        }.onFailure {
            val errorMessage =
                "Could not send payload to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send payload to Kafka", it)
        }
    }
}

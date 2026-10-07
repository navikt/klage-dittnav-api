package no.nav.klage.kafka

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.createTestAggregatedKlageAnke
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class AivenKafkaProducerTest {
    private val kafkaTemplate: KafkaTemplate<String, String> = mockk()
    private val producer = AivenKafkaProducer(aivenKafkaTemplate = kafkaTemplate).apply { topic = "test-topic" }

    @Test
    fun `sendToKafka returns when broker acknowledges`() {
        every { kafkaTemplate.send("test-topic", any<String>()) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, String>>())

        assertDoesNotThrow { producer.sendToKafka(createTestAggregatedKlageAnke()) }
    }

    @Test
    fun `sendToKafka throws KafkaSendException when send fails`() {
        every { kafkaTemplate.send("test-topic", any<String>()) } returns
            CompletableFuture.failedFuture(RuntimeException("broker unavailable"))

        assertThrows<KafkaSendException> { producer.sendToKafka(createTestAggregatedKlageAnke()) }
    }
}

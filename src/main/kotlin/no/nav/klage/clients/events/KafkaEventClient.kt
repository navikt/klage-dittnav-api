package no.nav.klage.clients.events

import no.nav.klage.domain.Event
import no.nav.klage.domain.jsonToEvent
import no.nav.klage.util.getLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Each pod has one consumer with a unique group id, so every pod receives every event.
 * Events are parsed once and multicast to connected SSE clients. Events arriving without clients are dropped,
 * as clients load current state from the database when they connect.
 */
@Service
class KafkaEventClient(
    @Value($$"${INTERNAL_EVENT_TOPIC}")
    internalEventTopic: String,
    internalEventConsumerFactory: ConsumerFactory<String, String>,
) : SmartLifecycle {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    private val sink = Sinks.many().multicast().directBestEffort<Event>()

    private val container =
        KafkaMessageListenerContainer(
            internalEventConsumerFactory,
            ContainerProperties(internalEventTopic).apply {
                setMessageListener(MessageListener<String, String> { record -> onRecord(record = record) })
                ackMode = ContainerProperties.AckMode.BATCH
            },
        )

    @Volatile
    private var running = false

    fun events(): Flux<Event> = sink.asFlux()

    override fun start() {
        container.start()
        running = true
    }

    override fun stop() {
        container.stop()
        sink.tryEmitComplete()
        running = false
    }

    override fun isRunning(): Boolean = running

    private fun onRecord(record: ConsumerRecord<String, String>) {
        // A parsing failure is skipped here, so it cannot terminate the clients' SSE streams.
        val event =
            runCatching { jsonToEvent(record.value()) }
                .onFailure { logger.error("Could not parse internal event at offset {}, skipping it", record.offset(), it) }
                .getOrNull() ?: return

        val result = sink.tryEmitNext(event)
        if (result != Sinks.EmitResult.OK && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            logger.warn("Could not emit internal event at offset {}: {}", record.offset(), result)
        }
    }
}

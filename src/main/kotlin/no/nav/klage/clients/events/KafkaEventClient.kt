package no.nav.klage.clients.events

import jakarta.annotation.PostConstruct
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.ConnectableFlux
import reactor.kafka.receiver.KafkaReceiver

@Service
class KafkaEventClient(
    private val kafkaReceiver: KafkaReceiver<String, String>
) {

    private lateinit var eventPublisher: ConnectableFlux<ServerSentEvent<String>>

    @PostConstruct
    fun init() {
        eventPublisher = kafkaReceiver.receive()
            .map { consumerRecord -> ServerSentEvent.builder(consumerRecord.value()).build() }
            .publish()

        // subscribes to the KafkaReceiver -> starts consumption (without observers attached)
        eventPublisher.connect()
    }

    fun getEventPublisher() = eventPublisher
}
package no.nav.klage.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.clients.events.SseShutdownSignal
import no.nav.klage.domain.Event
import no.nav.klage.domain.JOURNALPOST_ID_EVENT_NAME
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.journalpostIdEvent
import no.nav.klage.service.CommonService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class KlankeControllerEventsTest {
    private val klankeId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val otherKlankeId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
    private val commonService = mockk<CommonService>()
    private val kafkaEventClient = mockk<KafkaEventClient>()
    private val sseShutdownSignal = SseShutdownSignal()
    private val controller =
        KlankeController(
            vedleggService = mockk(),
            kafkaEventClient = kafkaEventClient,
            commonService = commonService,
            sseShutdownSignal = sseShutdownSignal,
        )

    // Hot like production: KafkaEventClient uses a connected publish(), which drops events nobody is subscribed to.
    private val kafkaSink = Sinks.many().multicast().directBestEffort<ServerSentEvent<String>>()

    @BeforeEach
    fun setup() {
        every { commonService.validateAccess(klankeId) } just runs
        every { kafkaEventClient.getEventPublisher() } returns kafkaSink.asFlux().publish().also { it.connect() }
    }

    @Test
    fun `completes with existing journalpostId when journalpost was created before connecting`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } returns "from-db"

        val events = collectEvents()

        assertEquals(1, events.size)
        assertEquals(JOURNALPOST_ID_EVENT_NAME, events.first().event())
        assertEquals("from-db", events.first().data())
    }

    @Test
    fun `receives journalpostId published while the database lookup is running`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } answers {
            publishToKafka(journalpostIdEvent(klankeId = klankeId, journalpostId = "from-kafka"))
            null
        }

        val events = collectEvents()

        assertEquals(1, events.size)
        assertEquals("from-kafka", events.first().data())
    }

    @Test
    fun `ignores published journalpostId for other klanke`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } answers {
            publishToKafka(journalpostIdEvent(klankeId = otherKlankeId, journalpostId = "from-kafka-other-klanke"))
            publishToKafka(journalpostIdEvent(klankeId = klankeId, journalpostId = "from-kafka"))
            null
        }

        val events = collectEvents()

        assertEquals(1, events.size)
        assertEquals("from-kafka", events.first().data())
    }

    @Test
    fun `emits a single event when journalpostId is both stored and published`() {
        // The Kafka event completes the stream before the lookup returns, so the stored value is discarded.
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } answers {
            publishToKafka(journalpostIdEvent(klankeId = klankeId, journalpostId = "from-kafka"))
            "from-db"
        }

        val events = collectEvents()

        assertEquals(1, events.size)
        assertEquals("from-kafka", events.first().data())
    }

    @Test
    fun `ignores blank stored journalpostId`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } returns ""

        val events =
            controller
                .getEvents(klankeId)
                .take(Duration.ofMillis(500))
                .collectList()
                .block(Duration.ofSeconds(5))!!

        assertTrue(events.isEmpty())
    }

    @Test
    fun `ignores blank published journalpostId`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } answers {
            publishToKafka(journalpostIdEvent(klankeId = klankeId, journalpostId = ""))
            publishToKafka(journalpostIdEvent(klankeId = klankeId, journalpostId = "from-kafka"))
            null
        }

        val events = collectEvents()

        assertEquals(1, events.size)
        assertEquals("from-kafka", events.first().data())
    }

    @Test
    fun `completes open stream when the application shuts down`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } returns null

        val events =
            controller
                .getEvents(klankeId)
                .doOnSubscribe {
                    Schedulers.parallel().schedule({ sseShutdownSignal.onShutdown() }, 200, TimeUnit.MILLISECONDS)
                }.collectList()
                .block(Duration.ofSeconds(5))!!

        assertTrue(events.isEmpty())
    }

    @Test
    fun `completes immediately when connecting after shutdown has started`() {
        every { commonService.getJournalpostIdWithoutValidation(klankeId) } returns null
        sseShutdownSignal.onShutdown()

        val events = collectEvents()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `throws KlankeNotFoundException when access is denied`() {
        every { commonService.validateAccess(klankeId) } throws RuntimeException("no access")

        assertThrows<KlankeNotFoundException> { controller.getEvents(klankeId) }

        verify(exactly = 0) { kafkaEventClient.getEventPublisher() }
        verify(exactly = 0) { commonService.getJournalpostIdWithoutValidation(any()) }
    }

    private fun collectEvents(): List<ServerSentEvent<String>> =
        controller
            .getEvents(klankeId)
            .collectList()
            .block(Duration.ofSeconds(5))!!

    private fun publishToKafka(event: Event) {
        val json = jacksonObjectMapper().writeValueAsString(event)
        kafkaSink.tryEmitNext(ServerSentEvent.builder(json).build()).orThrow()
    }
}

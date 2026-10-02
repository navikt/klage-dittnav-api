package no.nav.klage.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.createTestAggregatedKlageAnke
import no.nav.klage.createTestKlanke
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.KlankeMarkedCompletedEvent
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.kafka.KafkaSendException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class KlankeSenderServiceTest {
    private val commonService: CommonService = mockk()
    private val kafkaProducer: AivenKafkaProducer = mockk()

    private val klankeSenderService =
        KlankeSenderService(
            commonService = commonService,
            kafkaProducer = kafkaProducer,
        )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { commonService.markKlankeAsDone(any()) } returns Unit
    }

    @Test
    fun `sendAndMarkDone sends payload and marks klanke as DONE`() {
        val klanke = givenKlanke(status = KlageAnkeStatus.SENDING)
        val payload = givenPayloadFor(klanke.id)
        every { kafkaProducer.sendToKafka(payload) } returns Unit

        klankeSenderService.sendAndMarkDone(klankeId = klanke.id)

        verify(exactly = 1) { kafkaProducer.sendToKafka(payload) }
        verify(exactly = 1) { commonService.markKlankeAsDone(klanke.id) }
    }

    @Test
    fun `sendAndMarkDone does nothing when klanke is already DONE`() {
        val klanke = givenKlanke(status = KlageAnkeStatus.DONE)

        klankeSenderService.sendAndMarkDone(klankeId = klanke.id)

        verify(exactly = 0) { commonService.createAggregatedKlankeAsSystemUser(any()) }
        verify(exactly = 0) { kafkaProducer.sendToKafka(any()) }
        verify(exactly = 0) { commonService.markKlankeAsDone(any()) }
    }

    @Test
    fun `sendAndMarkDone does nothing when klanke is DELETED`() {
        val klanke = givenKlanke(status = KlageAnkeStatus.DELETED)

        klankeSenderService.sendAndMarkDone(klankeId = klanke.id)

        verify(exactly = 0) { kafkaProducer.sendToKafka(any()) }
        verify(exactly = 0) { commonService.markKlankeAsDone(any()) }
    }

    @Test
    fun `sendAndMarkDone throws and does not mark DONE when Kafka fails`() {
        val klanke = givenKlanke(status = KlageAnkeStatus.SENDING)
        givenPayloadFor(klanke.id)
        every { kafkaProducer.sendToKafka(any()) } throws kafkaSendException()

        assertThrows<KafkaSendException> {
            klankeSenderService.sendAndMarkDone(klankeId = klanke.id)
        }

        verify(exactly = 0) { commonService.markKlankeAsDone(any()) }
    }

    @Test
    fun `scheduler stops at first Kafka failure`() {
        val first = givenKlanke(status = KlageAnkeStatus.SENDING)
        val second = givenKlanke(status = KlageAnkeStatus.SENDING)
        givenPayloadFor(first.id)
        givenPayloadFor(second.id)
        every { commonService.findKlankeIdsToSend() } returns listOf(first.id, second.id)
        every { kafkaProducer.sendToKafka(any()) } throws kafkaSendException()

        assertDoesNotThrow { klankeSenderService.sendKlankerMarkedCompleted() }

        verify(exactly = 1) { kafkaProducer.sendToKafka(any()) }
        verify(exactly = 0) { commonService.getKlankeWithoutValidation(second.id) }
        verify(exactly = 0) { commonService.markKlankeAsDone(any()) }
    }

    @Test
    fun `scheduler continues after non-Kafka failure`() {
        val first = givenKlanke(status = KlageAnkeStatus.SENDING)
        val second = givenKlanke(status = KlageAnkeStatus.SENDING)
        every { commonService.createAggregatedKlankeAsSystemUser(first) } throws RuntimeException("klage-lookup down")
        val secondPayload = givenPayloadFor(second.id)
        every { commonService.findKlankeIdsToSend() } returns listOf(first.id, second.id)
        every { kafkaProducer.sendToKafka(secondPayload) } returns Unit

        assertDoesNotThrow { klankeSenderService.sendKlankerMarkedCompleted() }

        verify(exactly = 0) { commonService.markKlankeAsDone(first.id) }
        verify(exactly = 1) { commonService.markKlankeAsDone(second.id) }
    }

    @Test
    fun `listener does not throw when sending fails`() {
        val klanke = givenKlanke(status = KlageAnkeStatus.SENDING)
        givenPayloadFor(klanke.id)
        every { kafkaProducer.sendToKafka(any()) } throws kafkaSendException()

        assertDoesNotThrow {
            klankeSenderService.onKlankeMarkedCompleted(KlankeMarkedCompletedEvent(klankeId = klanke.id))
        }

        verify(exactly = 0) { commonService.markKlankeAsDone(any()) }
    }

    private fun givenKlanke(status: KlageAnkeStatus) =
        createTestKlanke(status = status, markedCompleted = LocalDateTime.now()).also {
            every { commonService.getKlankeWithoutValidation(it.id) } returns it
        }

    private fun givenPayloadFor(klankeId: UUID) =
        createTestAggregatedKlageAnke(id = klankeId).also { payload ->
            every {
                commonService.createAggregatedKlankeAsSystemUser(match { it.id == klankeId })
            } returns payload
        }

    private fun kafkaSendException() = KafkaSendException(message = "Kafka down", cause = RuntimeException("timeout"))
}

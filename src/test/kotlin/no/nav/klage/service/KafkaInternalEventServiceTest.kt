package no.nav.klage.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.db.PostgresIntegrationTestBase
import no.nav.klage.domain.journalpostIdEvent
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import java.util.UUID
import java.util.concurrent.CompletableFuture

@ActiveProfiles("dbtest")
@DataJpaTest
@Import(KafkaInternalEventService::class)
class KafkaInternalEventServiceTest : PostgresIntegrationTestBase() {
    @MockkBean(relaxed = true)
    private lateinit var aivenKafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    private val event = journalpostIdEvent(klankeId = UUID.fromString("00000000-0000-0000-0000-000000000000"), journalpostId = "123")

    @Test
    fun `publishes to Kafka only after the transaction commits`() {
        every { aivenKafkaTemplate.send(any<String>(), any<String>()) } returns CompletableFuture.completedFuture(mockk())

        applicationEventPublisher.publishEvent(event)
        verify(exactly = 0) { aivenKafkaTemplate.send(any<String>(), any<String>()) }

        TestTransaction.flagForCommit()
        TestTransaction.end()

        verify(exactly = 1) { aivenKafkaTemplate.send(any<String>(), any<String>()) }
    }

    @Test
    fun `does not publish to Kafka when the transaction rolls back`() {
        applicationEventPublisher.publishEvent(event)

        TestTransaction.flagForRollback()
        TestTransaction.end()

        verify(exactly = 0) { aivenKafkaTemplate.send(any<String>(), any<String>()) }
    }
}

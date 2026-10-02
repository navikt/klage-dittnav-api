package no.nav.klage.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.KlankeMarkedCompletedEvent
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.kafka.KafkaSendException
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

/**
 * Sends finalized klanker (status SENDING) to Kafka and marks them DONE when confirmed sent.
 *
 * The listener sends right after the finalize transaction commits. The scheduler retries anything still SENDING.
 * Both share the same ShedLock, so only one pod sends at a time. If the listener is skipped because the lock is
 * held, the next scheduler run picks the klanke up.
 */
@Service
class KlankeSenderService(
    private val commonService: CommonService,
    private val kafkaProducer: AivenKafkaProducer,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
        private const val LOCK_NAME = "sendKlankeToKafka"
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @SchedulerLock(name = LOCK_NAME)
    fun onKlankeMarkedCompleted(event: KlankeMarkedCompletedEvent) {
        try {
            sendAndMarkDone(klankeId = event.klankeId)
        } catch (e: Exception) {
            logFailure(klankeId = event.klankeId, e = e)
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    @SchedulerLock(name = LOCK_NAME)
    fun sendKlankerMarkedCompleted() {
        val klankeIds = commonService.findKlankeIdsToSend()
        if (klankeIds.isEmpty()) return

        logger.debug("Found {} klanker with status SENDING", klankeIds.size)

        for (klankeId in klankeIds) {
            try {
                sendAndMarkDone(klankeId = klankeId)
            } catch (e: KafkaSendException) {
                // Kafka is likely down. Stop here so the run stays well within lockAtMostFor; retry next run.
                logFailure(klankeId = klankeId, e = e)
                return
            } catch (e: Exception) {
                logFailure(klankeId = klankeId, e = e)
            }
        }
    }

    /**
     * Sends one klanke to Kafka and marks it DONE.
     */
    fun sendAndMarkDone(klankeId: UUID) {
        val klanke = commonService.getKlankeWithoutValidation(klankeId = klankeId)
        if (klanke.status != KlageAnkeStatus.SENDING) return
        val payload = commonService.createAggregatedKlankeAsSystemUser(klanke = klanke)
        kafkaProducer.sendToKafka(klageAnkeToKafka = payload)
        commonService.markKlankeAsDone(klankeId = klanke.id)
    }

    private fun logFailure(
        klankeId: UUID,
        e: Exception,
    ) {
        logger.error("Could not send klanke $klankeId to Kafka. Will retry. See team-logs for more details.")
        teamLogger.error("Could not send klanke $klankeId to Kafka", e)
    }
}

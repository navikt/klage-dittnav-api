package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class VedleggMetrics(private val meterRegistry: MeterRegistry) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_VEDLEGG_TYPE = "klager_vedlegg_type"

        private const val SUMMARY_VEDLEGG_SIZE = "klager_vedlegg_size"
        private const val SUMMARY_VEDLEGG_PER_USER = "klager_vedlegg_per_user"

        private const val TIMER_VEDLEGG = "klager_vedlegg_timer"
    }

    fun registerVedleggSize(sizeInKb: Double) {
        try {
            meterRegistry.summary(SUMMARY_VEDLEGG_SIZE).record(sizeInKb)
        } catch (e: Exception) {
            logger.warn("registerVedleggSize failed", e)
        }
    }

    fun registerNumberOfVedleggPerUser(numberOfVedlegg: Double) {
        try {
            meterRegistry.summary(SUMMARY_VEDLEGG_PER_USER).record(numberOfVedlegg)
        } catch (e: Exception) {
            logger.warn("registerVedleggSize failed", e)
        }
    }

    fun incrementVedleggType(contentType: String) {
        try {
            meterRegistry.counter(COUNTER_VEDLEGG_TYPE, "contentType", contentType).increment()
        } catch (e: Exception) {
            logger.warn("incrementVedleggType failed", e)
        }
    }

    fun registerTimeUsed(timeUsed: Long) {
        try {
            meterRegistry.timer(TIMER_VEDLEGG).record(Duration.ofMillis(timeUsed))
        } catch (e: Exception) {
            logger.warn("registerTimeUsed failed", e)
        }
    }
}

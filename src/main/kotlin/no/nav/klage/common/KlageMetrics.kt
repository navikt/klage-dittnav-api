package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component

@Component
class KlageMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_FINALIZED = "klager_finalized"
    }

    fun incrementKlagerFinalized() {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerFinalized failed", e)
        }
    }
}
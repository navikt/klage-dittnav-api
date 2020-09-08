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
        private const val COUNTER_KLAGER_REFERRER = "klager_referrer"
        private const val COUNTER_KLAGER_INITIALIZED = "klager_initialized"
    }

    fun incrementKlagerInitialized() {
        try {
            meterRegistry.counter(COUNTER_KLAGER_INITIALIZED).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerInitialized failed", e)
        }
    }

    fun incrementReferrer(referrer: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_REFERRER, "referrer", referrer).increment()
        } catch (e: Exception) {
            logger.warn("incrementReferrer failed", e)
        }
    }

    fun incrementKlagerFinalized(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerFinalized failed", e)
        }
    }

}

package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component
@Component
class KlageMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_FINALIZED = "klager_finalized"
        private const val COUNTER_KLAGER_INITIALIZED = "klager_initialized"
        private const val COUNTER_KLAGER_GRUNN = "klager_grunn"
        private const val COUNTER_KLAGER_FINALIZED_FULLMAKT = "klager_finalized_fullmakt"
    }

    fun incrementKlagerInitialized(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_INITIALIZED, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerInitialized failed", e)
        }
    }

    fun incrementKlagerFinalized(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerFinalized failed", e)
        }
    }

    fun incrementKlagerGrunn(ytelse: String, checkboxesSelected: Set<CheckboxEnum>) {
        try {
            checkboxesSelected.forEach {
                meterRegistry.counter(COUNTER_KLAGER_GRUNN, "ytelse", ytelse, "grunn", it.name).increment()
            }
        } catch (e: Exception) {
            logger.warn("incrementKlagerGrunn failed", e)
        }
    }

    fun incrementFullmakt(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED_FULLMAKT, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerFinalized failed", e)
        }
    }

}

package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component
@Component
class KlageAnkeMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_FINALIZED = "klager_finalized"
        private const val COUNTER_KLAGER_INITIALIZED = "klager_initialized"
        private const val COUNTER_KLAGER_FINALIZED_GRUNN = "klager_finalized_grunn"
        private const val COUNTER_KLAGER_FINALIZED_FULLMAKT = "klager_finalized_fullmakt"
        private const val COUNTER_KLAGER_OPTIONAL_SAKSNUMMER = "klager_optional_saksnummer"
        private const val COUNTER_KLAGER_OPTIONAL_VEDTAKSDATO = "klager_optional_vedtaksdato"
        private const val COUNTER_KLAGER_FINALIZED_TITLE = "klager_finalized_title"

        private const val COUNTER_ANKER_FINALIZED = "anker_finalized"
        private const val COUNTER_ANKER_INITIALIZED = "anker_initialized"
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
                meterRegistry.counter(COUNTER_KLAGER_FINALIZED_GRUNN, "ytelse", ytelse, "grunn", it.name).increment()
            }
        } catch (e: Exception) {
            logger.warn("incrementKlagerGrunn failed", e)
        }
    }

    fun incrementFullmakt(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED_FULLMAKT, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementFullmakt failed", e)
        }
    }

    fun incrementOptionalSaksnummer(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_OPTIONAL_SAKSNUMMER, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementOptionalSaksnummer failed", e)
        }
    }

    fun incrementOptionalVedtaksdato(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_OPTIONAL_VEDTAKSDATO, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementOptionalVedtaksdato failed", e)
        }
    }

    fun incrementKlagerFinalizedTitle(title: TitleEnum) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED_TITLE, "tittel", title.nb).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlageTitle failed", e)
        }
    }

    fun incrementAnkerInitialized(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_ANKER_INITIALIZED, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementAnkerInitialized failed", e)
        }
    }

    fun incrementAnkerFinalized(ytelse: String) {
        try {
            meterRegistry.counter(COUNTER_ANKER_FINALIZED, "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementAnkerFinalized failed", e)
        }
    }

}

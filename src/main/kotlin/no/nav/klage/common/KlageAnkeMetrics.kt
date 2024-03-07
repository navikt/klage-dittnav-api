package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.domain.Type
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component

@Component
class KlageAnkeMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_FINALIZED_GRUNN = "klager_finalized_grunn"
        private const val COUNTER_KLAGER_OPTIONAL_SAKSNUMMER = "klager_optional_saksnummer"
        private const val COUNTER_KLAGER_OPTIONAL_VEDTAKSDATO = "klager_optional_vedtaksdato"
        private const val COUNTER_KLAGER_FINALIZED_TITLE = "klager_finalized_title"
    }

    fun incrementKlankerInitialized(ytelse: String, type: Type) {
        try {
            meterRegistry.counter(type.name.lowercase() + "r_initialized", "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlankerInitialized failed", e)
        }
    }

    fun incrementKlankerFinalized(ytelse: String, type: Type) {
        try {
            meterRegistry.counter(type.name.lowercase() + "r_finalized", "ytelse", ytelse).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerFinalized failed", e)
        }
    }

    fun incrementKlagerGrunn(ytelse: String, checkboxesSelected: List<CheckboxEnum>) {
        try {
            checkboxesSelected.forEach {
                meterRegistry.counter(COUNTER_KLAGER_FINALIZED_GRUNN, "ytelse", ytelse, "grunn", it.name).increment()
            }
        } catch (e: Exception) {
            logger.warn("incrementKlagerGrunn failed", e)
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

    fun incrementKlagerFinalizedTitle(title: Innsendingsytelse) {
        try {
            meterRegistry.counter(COUNTER_KLAGER_FINALIZED_TITLE, "tittel", title.nb).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlageTitle failed", e)
        }
    }

}

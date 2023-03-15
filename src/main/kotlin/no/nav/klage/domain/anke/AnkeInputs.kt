package no.nav.klage.domain.anke

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.LocalDate

data class AnkeInput(
    val internalSaksnummer: String?,
    val innsendingsytelse: Innsendingsytelse,
)

data class AnkeFullInput(
    val innsendingsytelse: Innsendingsytelse,
    val userSaksnummer: String? = null,
    val language: LanguageEnum,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String = "",
    val enhetsnummer: String? = null,
    val hasVedlegg: Boolean,
)



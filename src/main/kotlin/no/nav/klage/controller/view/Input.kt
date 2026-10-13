package no.nav.klage.controller.view

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.domain.Type
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import java.time.LocalDate

data class GetPersonInput(
    val fnr: String,
    val innsendingsytelse: Innsendingsytelse?,
)

data class StringInput(
    val value: String,
)

data class StringInputNullable(
    val value: String?,
)

data class DateInput(
    val value: LocalDate?,
)

data class BooleanInput(
    val value: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KlankeInput(
    val internalSaksnummer: String?,
    val sakSakstype: String?,
    val sakFagsaksystem: String?,
    val innsendingsytelse: Innsendingsytelse,
    val type: Type,
    val caseIsAtKA: Boolean?,
    val sakenGjelder: String?,
)

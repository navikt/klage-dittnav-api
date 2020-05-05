package no.nav.klage.domain

import java.time.LocalDate

data class Vedtak(
    val title: String,
    val vedtaksdato: LocalDate,
    val tema: String,
    val journalfoerendeEnhet: String
)

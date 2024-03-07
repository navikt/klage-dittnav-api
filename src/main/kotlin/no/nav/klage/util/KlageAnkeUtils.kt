package no.nav.klage.util

import no.nav.klage.domain.titles.Innsendingsytelse

fun klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer: String, klageAnkeIdentifikasjonsnummer: String): Boolean =
    usersIdentifikasjonsnummer == klageAnkeIdentifikasjonsnummer

fun klageAnkeIsLonnskompensasjon(innsendingsytelse: Innsendingsytelse): Boolean =
    innsendingsytelse == Innsendingsytelse.LONNSKOMPENSASJON

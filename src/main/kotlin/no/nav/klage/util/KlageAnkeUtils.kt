package no.nav.klage.util

import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse

fun klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer: String, klageAnkeIdentifikasjonsnummer: String): Boolean =
    usersIdentifikasjonsnummer == klageAnkeIdentifikasjonsnummer

fun klageAnkeIsLonnskompensasjon(innsendingsytelse: Innsendingsytelse): Boolean =
    innsendingsytelse == Innsendingsytelse.LONNSKOMPENSASJON

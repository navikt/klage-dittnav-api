package no.nav.klage.util

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun getFormattedLocalDateTime(): LocalDateTime =
    ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Oslo")).toLocalDateTime()

fun klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer: String, klageAnkeIdentifikasjonsnummer: String): Boolean =
    usersIdentifikasjonsnummer == klageAnkeIdentifikasjonsnummer

fun String.toTema() = try {
    Tema.valueOf(this)
} catch (e: IllegalArgumentException) {
    Tema.UKJ
}

fun String.toStatus() = try {
    KlageAnkeStatus.valueOf(this)
} catch (e: IllegalArgumentException) {
    KlageAnkeStatus.DRAFT
}

fun klageAnkeIsLonnskompensasjon(innsendingsytelse: Innsendingsytelse): Boolean =
    innsendingsytelse == Innsendingsytelse.LONNSKOMPENSASJON

fun getLanguageEnum(input: String?): LanguageEnum {
    return when (input) {
        null -> {
            LanguageEnum.NB
        }

        else -> {
            LanguageEnum.valueOf(input)
        }
    }
}
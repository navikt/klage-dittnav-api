package no.nav.klage.util

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun parseTitleKey(titleKey: TitleEnum?, ytelse: String?, tema: Tema): TitleEnum {
    return when {
        titleKey != null -> titleKey
        ytelse != null && TitleEnum.getTitleKeyFromNbTitle(ytelse) != null -> TitleEnum.getTitleKeyFromNbTitle(ytelse)!!
        else -> TitleEnum.valueOf(tema.name)
    }
}

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

fun klageAnkeIsLonnskompensasjon(tema: Tema, titleKey: TitleEnum): Boolean =
    tema == Tema.DAG && titleKey == TitleEnum.LONNSKOMPENSASJON

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

fun getTitleEnum(titleKey: String?, ytelse: String?, tema: String): TitleEnum {
    return when (titleKey) {
        null -> {
            if (ytelse != null && TitleEnum.getTitleKeyFromNbTitle(ytelse) != null) {
                TitleEnum.getTitleKeyFromNbTitle(ytelse)!!
            } else {
                TitleEnum.valueOf(tema)
            }
        }
        else -> {
            TitleEnum.valueOf(titleKey)
        }
    }
}
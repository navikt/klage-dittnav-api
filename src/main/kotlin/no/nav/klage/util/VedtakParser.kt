package no.nav.klage.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val earlierVedtakText = "Tidligere vedtak"

var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun vedtakFromDate(vedtakDate: LocalDate?): String? {
    return when {
        vedtakDate != null -> {
            "$earlierVedtakText - ${vedtakDate.format(formatter)}"
        }
        else -> null
    }
}
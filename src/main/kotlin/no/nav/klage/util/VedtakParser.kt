package no.nav.klage.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val EARLIER_VEDTAK_TEXT = "Tidligere vedtak"

var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun vedtakFromDate(vedtakDate: LocalDate?): String? =
    when {
        vedtakDate != null -> {
            "$EARLIER_VEDTAK_TEXT - ${vedtakDate.format(formatter)}"
        }

        else -> {
            null
        }
    }

package no.nav.klage.util

import no.nav.klage.domain.klage.VedtakType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val earlierVedtakText = "Tidligere vedtak"
val latestVedtakText = "Siste vedtak"
val earlierVedtakRegex = Regex("\\d{2}.\\d{2}.\\d{4}$")

var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun vedtakTypeFromVedtak(vedtak: String): VedtakType? {
    return if (vedtak.startsWith(earlierVedtakText)) {
        VedtakType.EARLIER
    } else if (vedtak.startsWith(latestVedtakText)) {
        VedtakType.LATEST
    } else null
}

fun vedtakDateFromVedtak(vedtak: String): LocalDate? {
    val isEarlierVedtak = vedtak.startsWith(earlierVedtakText)
    return if (isEarlierVedtak) {
        val matchedDate = earlierVedtakRegex.find(vedtak)
        if (matchedDate != null) {
            LocalDate.parse(matchedDate.value, formatter)
        } else null
    } else null
}

fun vedtakFromTypeAndDate(vedtakType: VedtakType?, vedtakDate: LocalDate?): String? {
    when {
        vedtakType != null -> {
            if (vedtakType == VedtakType.LATEST) return vedtakType.toVedtakTypeText()
            return if (vedtakDate != null) {
                "${vedtakType.toVedtakTypeText()} - ${vedtakDate.format(formatter)}"
            } else {
                vedtakType.toVedtakTypeText()
            }
        }
        vedtakDate != null -> {
            return "$earlierVedtakText - ${vedtakDate.format(formatter)}"
        }
        else -> return null
    }
}

private fun VedtakType.toVedtakTypeText(): String {
    return when {
        this === VedtakType.EARLIER -> {
            earlierVedtakText
        }
        this === VedtakType.LATEST -> {
            latestVedtakText
        }
        else -> ""
    }
}
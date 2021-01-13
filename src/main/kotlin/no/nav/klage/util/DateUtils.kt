package no.nav.klage.util

import java.time.LocalDate

fun dateIsWithinInclusiveRange(referenceDate: LocalDate, startDate: LocalDate, endDate: LocalDate): Boolean {
    return referenceDate.isAfter(startDate.minusDays(1)) && referenceDate.isBefore(endDate.plusDays(1))
}
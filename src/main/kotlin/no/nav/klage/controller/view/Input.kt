package no.nav.klage.controller.view

import no.nav.klage.domain.klage.CheckboxEnum
import java.time.LocalDate

data class StringInput(
    val value: String
)

data class DateInput(
    val value: LocalDate?
)

data class CheckboxesSelectedInput(
    val value: Set<CheckboxEnum>
)
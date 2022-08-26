package no.nav.klage.domain.ankeOLD

import no.nav.klage.domain.LanguageEnum

data class NewAnkeOLDRequest (
    val ankeInternalSaksnummer: String,
    val language: LanguageEnum = LanguageEnum.NB
)

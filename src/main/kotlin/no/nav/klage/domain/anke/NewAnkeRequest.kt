package no.nav.klage.domain.anke

import no.nav.klage.domain.LanguageEnum

data class NewAnkeRequest (
    val ankeInternalSaksnummer: String,
    val language: LanguageEnum = LanguageEnum.NB
)

package no.nav.klage.clients.klagelookup

import no.nav.klage.kodeverk.Tema

data class GetPersonRequest(
    val fnr: String,
    val tema: Tema?,
)

data class GetPersonAsSystemUserRequest(
    val fnr: String,
)

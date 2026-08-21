package no.nav.klage.domain

data class Bruker(
    val navn: Navn,
    val folkeregisteridentifikator: Identifikator,
)

data class Identifikator(
    val type: String = "FNR",
    val identifikasjonsnummer: String
)

data class Navn(
    val fornavn: String,
    val etternavn: String
)
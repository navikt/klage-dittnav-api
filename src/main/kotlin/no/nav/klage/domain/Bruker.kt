package no.nav.klage.domain

data class Bruker
    (
    val navn: Navn,
    val adresse: Adresse?,
    val kontaktinformasjon: Kontaktinformasjon?
)

data class Navn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
)

data class Adresse(
    val adressenavn: String?,
    val postnummer: String?,
    val poststed: String?,
    val husnummer: String?,
    val husbokstav: String?
)

data class Kontaktinformasjon(
    val telefonnummer: String?,
    val epost: String?
)


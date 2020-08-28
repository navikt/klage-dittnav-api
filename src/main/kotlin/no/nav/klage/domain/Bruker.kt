package no.nav.klage.domain

data class Bruker
    (
    val navn: Navn,
    val adresse: Adresse?,
    val kontaktinformasjon: Kontaktinformasjon?,
    val folkeregisteridentifikator: Identifikator,
    val tokenExpires: Long? = null
)

data class Identifikator(
    val type: String,
    val identifikasjonsnummer: String
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Adresse(
    val adressenavn: String?,
    val postnummer: String?,
    val poststed: String?,
    val husnummer: String?,
    val husbokstav: String?
) {
    fun toKlageskjemaString(): String =
        "${adressenavn.orEmpty()} " +
                "${husnummer.orEmpty()} " +
                "${husbokstav.orEmpty()}, " +
                "${postnummer.orEmpty()}, " +
                poststed.orEmpty()
}

data class Kontaktinformasjon(
    val telefonnummer: String?,
    val epost: String?
)

private fun String?.orEmpty(): String = this ?: ""


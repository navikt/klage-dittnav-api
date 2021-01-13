package no.nav.klage.clients.pdl

import no.nav.klage.domain.Tema
import java.time.LocalDate
import java.time.LocalDateTime


data class HentPdlPersonResponse(val data: HentPerson?, val errors: List<PdlError>?)

data class HentPerson(val hentPerson: Person?)
data class Person(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<Navn>,
    val bostedsadresse: List<Bostedsadresse>,
    val telefonnummer: List<Telefonnummer>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>
)

data class Adressebeskyttelse(
    val gradering: AdressebeskyttelseGradering
)

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
}


data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Bostedsadresse(
    val angittFlyttedato: LocalDate?,
    val coAdressenavn: String?,
    val vegadresse: VegAdresse?,
    val adresse: String?,
    val ukjentBosted: UkjentBosted?
)

data class Telefonnummer(
    val landskode: String?,
    val nummer: String?,
    val prioritet: String?
)

enum class Endringstype {
    OPPRETT,
    KORRIGER,
    OPPHOER,
}

data class Endring(
    val type: Endringstype?,
    val registrert: LocalDateTime?,
    val registrertAv: String?,
    val systemKilde: String?,
    val kilde: String?
)

data class VegAdresse(
    val matrikkelId: Int?,
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetsnummer: String?,
    val adressenavn: String?,
    val kommunenummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
    val koordinater: Koordinater?
)

data class UkjentBosted(
    val bostedskommune: String?
)

data class Koordinater(
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val kvalitet: Int?
)

data class Folkeregisteridentifikator(
    val identifikasjonsnummer: String,
    val type: String,
    val status: String
)

data class Fullmakt(
    val motpartsPersonident: String,
    val motpartsRolle: FullmaktsRolle,
    val omraader: List<Tema>,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate
)

enum class FullmaktsRolle {
    FULLMAKTSGIVER, FULLMEKTIG
}

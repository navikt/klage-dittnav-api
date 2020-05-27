package no.nav.klage.service

import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.Adresse
import no.nav.klage.domain.Bruker
import no.nav.pam.geography.PostDataDAO
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class BrukerService(private val pdlClient: PdlClient) {

    private val postDataDAO = PostDataDAO()

    fun getBruker(): Bruker {
        val personinfo = pdlClient.getPersonInfo()
        return mapToBruker(personinfo)
    }

    private fun mapToBruker(personInfo: HentPdlPersonResponse): Bruker {
        if (personInfo.errors != null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, personInfo.errors[0].message)
        }

        val pdlNavn = personInfo.data?.hentPerson?.navn?.firstOrNull()
        val bostedsadresse = personInfo.data?.hentPerson?.bostedsadresse?.firstOrNull()
        val pdlAdresse = bostedsadresse?.vegadresse
        val pdlTelefonnummer = personInfo.data?.hentPerson?.telefonnummer?.firstOrNull()
        val pdlFolkeregisteridentifikator = personInfo.data?.hentPerson?.folkeregisteridentifikator?.firstOrNull()
        return Bruker(
            navn = pdlNavn!!.toBrukerNavn(),
            adresse = pdlAdresse?.toBrukerAdresse(),
            kontaktinformasjon = pdlTelefonnummer?.toKontaktinformasjon(),
            folkeregisteridentifikator = pdlFolkeregisteridentifikator?.identifikasjonsnummer
        )
    }

    private fun Navn.toBrukerNavn() = no.nav.klage.domain.Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn
    )

    private fun VegAdresse.toBrukerAdresse() = Adresse(
        adressenavn = adressenavn,
        postnummer = postnummer,
        poststed = postDataDAO.findPostData(postnummer).get().city,
        husnummer = husnummer,
        husbokstav = husbokstav
    )

    private fun Telefonnummer.toKontaktinformasjon() = no.nav.klage.domain.Kontaktinformasjon(
        telefonnummer = "$landskode $nummer",
        epost = null
    )
}
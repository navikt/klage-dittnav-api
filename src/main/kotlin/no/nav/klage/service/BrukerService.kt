package no.nav.klage.service

import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.Adresse
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Identifikator
import no.nav.klage.util.TokenUtil
import no.nav.pam.geography.PostDataDAO
import org.springframework.stereotype.Service

@Service
class BrukerService(
    private val pdlClient: PdlClient,
    private val tokenUtil: TokenUtil
) {

    private val postDataDAO = PostDataDAO()

    fun getBruker(): Bruker {
        val personinfo = pdlClient.getPersonInfo()
        return mapToBruker(personinfo)
    }

    private fun mapToBruker(personInfo: HentPdlPersonResponse): Bruker {
        if (personInfo.errors != null) {
            throw RuntimeException(personInfo.errors[0].message)
        }

        val pdlNavn = personInfo.data?.hentPerson?.navn?.firstOrNull()
        checkNotNull(pdlNavn) { "Navn missing" }

        val bostedsadresse = personInfo.data.hentPerson.bostedsadresse.firstOrNull()
        val pdlAdresse = bostedsadresse?.vegadresse
        val pdlTelefonnummer = personInfo.data.hentPerson.telefonnummer.firstOrNull()

        val pdlFolkeregisteridentifikator = personInfo.data.hentPerson.folkeregisteridentifikator.firstOrNull()
        checkNotNull(pdlFolkeregisteridentifikator) { "Folkeregisteridentifikator missing" }

        return Bruker(
            navn = pdlNavn.toBrukerNavn(),
            adresse = pdlAdresse?.toBrukerAdresse(),
            kontaktinformasjon = pdlTelefonnummer?.toKontaktinformasjon(),
            folkeregisteridentifikator = pdlFolkeregisteridentifikator.toIdentifikator(),
            tokenExpires = tokenUtil.getExpiry()
        )
    }

    private fun Folkeregisteridentifikator.toIdentifikator() = Identifikator(
        type = this.type,
        identifikasjonsnummer = this.identifikasjonsnummer
    )

    private fun Navn.toBrukerNavn() = no.nav.klage.domain.Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn
    )

    private fun VegAdresse.toBrukerAdresse() = try {
        Adresse(
                adressenavn = adressenavn,
                postnummer = postnummer,
                poststed = postDataDAO.findPostData(postnummer).get().city,
                husnummer = husnummer,
                husbokstav = husbokstav
        )
    } catch (e: NoSuchElementException) {
        Adresse(
                adressenavn = adressenavn,
                postnummer = postnummer,
                poststed = null,
                husnummer = husnummer,
                husbokstav = husbokstav
        )
    }

    private fun Telefonnummer.toKontaktinformasjon() = no.nav.klage.domain.Kontaktinformasjon(
        telefonnummer = "$landskode $nummer",
        epost = null
    )
}

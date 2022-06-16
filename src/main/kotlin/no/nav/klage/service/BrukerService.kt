package no.nav.klage.service

import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.Adresse
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Identifikator
import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.FullmaktNotFoundException
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.pam.geography.PostDataDAO
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BrukerService(
    private val pdlClient: PdlClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${ALL_FULLMAKT_OMRAADER}")
    private lateinit var allFullmaktOmraader: String

    private val postDataDAO = PostDataDAO()

    fun getBruker(): Bruker {
        val personinfo = pdlClient.getPersonInfo()
        return mapToBruker(personinfo)
    }

    fun getFullmaktsgiver(tema: Tema, fnr: String): Bruker {
        if (fullmaktExists(tema, fnr)) {
            val fullmaktsgiverPersonInfo = pdlClient.getPersonInfoWithSystemUser(fnr)
            return mapToBruker(fullmaktsgiverPersonInfo)
        } else throw FullmaktNotFoundException()
    }

    fun mapToBruker(personInfo: HentPdlPersonResponse): Bruker {
        if (personInfo.errors != null) {
            logger.warn("Errors from pdl: ${personInfo.errors}")
            if (personInfo.errors[0].extensions.code == "unauthenticated") {
                throw JwtTokenUnauthorizedException("Invalid token used towards PDL")
            } else {
                throw RuntimeException(personInfo.errors[0].message)
            }
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

    fun verifyFullmakt(tema: Tema, fullmaktsGiverFnr: String) {
        when {
            !fullmaktExists(tema, fullmaktsGiverFnr) -> throw FullmaktNotFoundException()
        }
    }

    private fun fullmaktExists(tema: Tema, fullmaktsGiverFnr: String): Boolean {
        val fullmektigResponse = pdlClient.getFullmektigInfoWithSystemUser(fullmaktsGiverFnr)
        if (fullmektigResponse.data?.hentPerson == null) {
            throw FullmaktNotFoundException()
        }
        val fullmaktList = fullmektigResponse.data.hentPerson.fullmakt

        return fullmaktList.any { fullmakt ->
            tokenUtil.getSubject() == fullmakt.motpartsPersonident &&
                    fullmakt.motpartsRolle == FullmaktsRolle.FULLMEKTIG &&
                    (fullmakt.omraader.contains(tema.name) || fullmakt.omraader.contains(allFullmaktOmraader)) &&
                    LocalDate.now() in fullmakt.gyldigFraOgMed..fullmakt.gyldigTilOgMed
        }
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

    private fun VegAdresse.toBrukerAdresse() =
        Adresse(
            adressenavn = adressenavn,
            postnummer = postnummer,
            poststed = postDataDAO.findPostData(postnummer).orElse(null)?.city,
            husnummer = husnummer,
            husbokstav = husbokstav
        )

    private fun Telefonnummer.toKontaktinformasjon() = no.nav.klage.domain.Kontaktinformasjon(
        telefonnummer = "$landskode $nummer",
        epost = null
    )
}

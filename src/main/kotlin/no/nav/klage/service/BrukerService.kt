package no.nav.klage.service

import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.clients.klagelookup.PersonResponse
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Identifikator
import no.nav.klage.kodeverk.Tema
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class BrukerService(
    private val klageLookupClient: KlageLookupClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getCurrentBruker(): Bruker {
        val personinfo = klageLookupClient.getPerson(fnr = tokenUtil.getSubject(), tema = null)
        return mapToBruker(personinfo)
    }

    fun getBruker(
        fnr: String,
        tema: Tema?,
    ): Bruker {
        val personinfo = klageLookupClient.getPerson(fnr = fnr, tema = tema)
        return mapToBruker(personinfo)
    }

    fun mapToBruker(personResponse: PersonResponse): Bruker =
        Bruker(
            navn = personResponse.toBrukerNavn(),
            folkeregisteridentifikator =
                Identifikator(
                    identifikasjonsnummer = personResponse.foedselsnr,
                ),
        )

    private fun PersonResponse.toBrukerNavn() =
        no.nav.klage.domain.Navn(
            fornavn = fornavn,
            etternavn = etternavn,
        )
}

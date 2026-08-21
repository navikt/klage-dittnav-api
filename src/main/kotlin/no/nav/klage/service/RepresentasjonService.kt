package no.nav.klage.service

import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.kodeverk.Tema
import org.springframework.stereotype.Service

@Service
class RepresentasjonService(
    private val klageLookupClient: KlageLookupClient,
) {
    fun representasjonIsValid(representasjonsgiverFnr: String, tema: Tema): Boolean {
        val usersRepresentasjonsforhold = klageLookupClient.getRepresentasjonsforhold()
        val vergemaalExists =
            usersRepresentasjonsforhold.vergemaal.find { it.vergehaver == representasjonsgiverFnr }?.skriverettigheter?.contains(
                tema
            ) ?: false
        val fullmaktExists =
            usersRepresentasjonsforhold.fullmakt.find { it.fullmaktsgiver == representasjonsgiverFnr }?.skriverettigheter?.contains(
                tema
            ) ?: false

        return vergemaalExists || fullmaktExists
    }

    fun getFullmaktsgivereForTema(tema: Tema): List<String> {
        val usersRepresentasjonsforhold = klageLookupClient.getRepresentasjonsforhold()
        return buildSet {
            addAll(
                usersRepresentasjonsforhold.vergemaal
                    .filter { it.skriverettigheter.contains(tema) }
                    .map { it.vergehaver }
            )
            addAll(
                usersRepresentasjonsforhold.fullmakt
                    .filter { it.skriverettigheter.contains(tema) }
                    .map { it.fullmaktsgiver }
            )
        }.toList()
    }
}
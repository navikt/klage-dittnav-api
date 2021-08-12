package no.nav.klage.service

import no.nav.klage.clients.KabalApiClient
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.availableanke.AvailableAnke
import no.nav.klage.domain.availableanke.AvailableAnkeView
import no.nav.klage.domain.availableanke.toAvailableAnkeView
import no.nav.klage.domain.couldBeShownAsAvailableAnke
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class AvailableAnkeService(
    private val ankeRepository: AnkeRepository,
    private val kabalApiClient: KabalApiClient

) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAllAvailableAnkeViewForUser(bruker: Bruker, tema: Tema?): List<AvailableAnkeView> {
        return if (tema != null) {
            getAllAvailableAnkerForUserAndTema(bruker, tema)
        } else {
            getAllAvailableAnkerForUser(bruker)
        }
            .map { setCurrentStatus(it) }
            .filter { it.ankeStatus.couldBeShownAsAvailableAnke() }
            .map { it.toAvailableAnkeView() }
    }

    fun getAvailableAnke(internalSaksnummer: String, bruker: Bruker): AvailableAnke {
        return kabalApiClient.getSpecificAvailableAnkeForUser(
            bruker.folkeregisteridentifikator.identifikasjonsnummer,
            UUID.fromString(internalSaksnummer)
        )
    }

    fun getAllAvailableAnkerForUserAndTema(bruker: Bruker, tema: Tema): List<AvailableAnke> {
        return getAllAvailableAnkerForUser(bruker)
            .filter {
                it.tema == tema
            }
    }

    fun getAllAvailableAnkerForUser(bruker: Bruker): List<AvailableAnke> {
        return kabalApiClient.getAvailableAnkerForUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)
    }

    private fun setCurrentStatus(availableAnke: AvailableAnke): AvailableAnke {
        return try {
            val existingAnke = ankeRepository.getAnkeByInternalSaksnummer(availableAnke.internalSaksnummer)
            availableAnke.copy(ankeStatus = existingAnke.status)
        } catch (e: AnkeNotFoundException) {
            availableAnke
        }
    }
}
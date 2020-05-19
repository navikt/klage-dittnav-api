package no.nav.klage.service

import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Klage
import no.nav.klage.repository.KlageRepository
import no.nav.klage.clients.pdl.PdlClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageMetrics: KlageMetrics,
    private val pdlClient: PdlClient
) {

    fun getKlager(): List<Klage> {
        return klageRepository.getKlager()
    }

    fun getKlage(id: Int): Klage = klageRepository.getKlageById(id)

    fun createKlage(klage: Klage): Klage {
        return klageRepository.createKlage(klage).also { klageMetrics.incrementKlagerCreated() }
    }

    fun updateKlage(klage: Klage): Klage {
        return klageRepository.updateKlage(klage)
    }

    fun deleteKlage(id: Int) {
        klageRepository.deleteKlage(id)
    }

}

package no.nav.klage.service

import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Klage
import no.nav.klage.domain.Vedtak
import no.nav.klage.repository.KlageRepository
import no.nav.klage.services.pdl.HentPdlPersonResponse
import no.nav.klage.services.pdl.PdlClient
import no.nav.klage.services.saf.SafClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageMetrics: KlageMetrics,
    private val pdlClient: PdlClient,
    private val safClient: SafClient
) {

    fun getPersonInfo(fnr: String): HentPdlPersonResponse {
        return pdlClient.getPersonInfo(fnr)
    }

    fun getKlager(): List<Klage> {
        return klageRepository.getKlager()
    }

    fun createKlage(klage: Klage): Klage {
        return klageRepository.createKlage(klage).also { klageMetrics.incrementKlagerCreated() }
    }

    fun updateKlage(klage: Klage): Klage {
        return klageRepository.updateKlage(klage)
    }

    fun deleteKlage(id: Int) {
        klageRepository.deleteKlage(id)
    }

    fun getVedtak(fnr: String): List<Vedtak> = safClient.getVedtak(fnr)

}

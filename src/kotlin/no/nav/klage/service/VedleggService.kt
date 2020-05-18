package no.nav.klage.service

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.repository.VedleggRepository
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class VedleggService(
    private val clamAvClient: ClamAvClient,
    private val vedleggRepository: VedleggRepository
) {

    fun putVedlegg(fnr: String, klageId: Int, vedlegg: VedleggWrapper) {
        if (vedlegg.hasVirus()) {
            throw RuntimeException("Vedlegg har virus")
        }
        vedleggRepository.putVedlegg(fnr, klageId, vedlegg)
    }

    fun deleteVedlegg(fnr: String, klageId: Int, vedleggId: String) {
        vedleggRepository.deleteVedlegg(fnr, klageId, vedleggId)
    }

    private fun VedleggWrapper.hasVirus() = !clamAvClient.scan(this.contentAsBytes())
}

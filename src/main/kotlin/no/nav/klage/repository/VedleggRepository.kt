package no.nav.klage.repository

import no.nav.klage.domain.KlageDAO
import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.VedleggDAO
import no.nav.klage.domain.VedleggWrapper
import org.springframework.stereotype.Repository

@Repository
class VedleggRepository() {

    fun storeVedlegg(klageId: Int, vedlegg: VedleggWrapper, fileStorageId: String): Vedlegg {
        return VedleggDAO.new {
            this.tittel = vedlegg.tittel
            this.klageId = KlageDAO.findById(klageId)!!
            this.ref = fileStorageId
            this.type = vedlegg.type
            this.sizeInBytes = vedlegg.contentAsBytes().size
        }.toVedlegg()
    }

    fun getVedleggById(id: Int): Vedlegg {
        return VedleggDAO.findById(id)?.toVedlegg() ?: throw RuntimeException("Vedlegg not found")
    }

    fun deleteVedlegg(vedleggId: Int) {
        VedleggDAO.findById(vedleggId)?.delete()
    }

}

data class VedleggResponse(val id: String)

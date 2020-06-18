package no.nav.klage.repository

import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.VedleggDAO
import no.nav.klage.domain.klage.KlageDAO
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile

@Repository
class VedleggRepository() {

    fun storeVedlegg(klageId: Int, vedlegg: MultipartFile, fileStorageId: String): Vedlegg {
        return VedleggDAO.new {
            this.tittel = vedlegg.originalFilename
            this.klageId = KlageDAO.findById(klageId)!!
            this.ref = fileStorageId
            this.type = vedlegg.contentType
            this.sizeInBytes = vedlegg.bytes.size
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

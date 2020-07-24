package no.nav.klage.repository

import no.nav.klage.domain.vedlegg.Vedlegg
import no.nav.klage.domain.vedlegg.VedleggDAO
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile

@Repository
class VedleggRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun storeVedlegg(klageId: Int, vedlegg: MultipartFile, fileStorageId: String): Vedlegg {
        logger.debug("Storing vedlegg metadata in db. KlageId: {}", klageId)
        return VedleggDAO.new {
            this.tittel = vedlegg.originalFilename
            this.klageId = KlageDAO.findById(klageId)!!
            this.ref = fileStorageId
            this.contentType = vedlegg.contentType
            this.sizeInBytes = vedlegg.bytes.size
        }.toVedlegg().also {
            logger.debug("Vedlegg metadata stored successfully in db. Id: {}", it.id)
        }
    }

    fun getVedleggById(id: Int): Vedlegg {
        logger.debug("Fetching vedlegg metadata from db. VedleggId: {}", id)
        return VedleggDAO.findById(id)?.toVedlegg() ?: throw RuntimeException("Vedlegg not found")
    }

    fun deleteVedlegg(vedleggId: Int) {
        logger.debug("Deleting vedlegg metadata from db. VedleggId: {}", vedleggId)
        VedleggDAO.findById(vedleggId)?.delete()
    }

}

data class VedleggResponse(val id: String)

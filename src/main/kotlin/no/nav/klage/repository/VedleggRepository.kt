package no.nav.klage.repository

import no.nav.klage.domain.anke.AnkeDAO
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.vedlegg.Ankevedlegg
import no.nav.klage.domain.vedlegg.AnkevedleggDAO
import no.nav.klage.domain.vedlegg.Klagevedlegg
import no.nav.klage.domain.vedlegg.KlagevedleggDAO
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Repository
class VedleggRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun storeKlagevedlegg(klageId: Int, vedlegg: MultipartFile, fileStorageId: String): Klagevedlegg {
        logger.debug("Storing vedlegg metadata in db. KlageId: {}", klageId)
        return KlagevedleggDAO.new {
            this.tittel = vedlegg.originalFilename.toString()
            this.klage = KlageDAO.findById(klageId)!!
            this.ref = fileStorageId
            this.contentType = vedlegg.contentType.toString()
            this.sizeInBytes = vedlegg.bytes.size
        }.toVedlegg().also {
            logger.debug("Vedlegg metadata stored successfully in db. Id: {}", it.id)
        }
    }

    fun storeAnkevedlegg(ankeId: UUID, vedlegg: MultipartFile, fileStorageId: String): Ankevedlegg {
        logger.debug("Storing vedlegg metadata in db. AnkeId: {}", ankeId)
        return AnkevedleggDAO.new {
            this.tittel = vedlegg.originalFilename.toString()
            this.anke = AnkeDAO.findById(ankeId)!!
            this.ref = fileStorageId
            this.contentType = vedlegg.contentType.toString()
            this.sizeInBytes = vedlegg.bytes.size
        }.toVedlegg().also {
            logger.debug("Vedlegg metadata stored successfully in db. Id: {}", it.id)
        }
    }

    fun getKlagevedleggById(id: Int): Klagevedlegg {
        logger.debug("Fetching vedlegg metadata from db. VedleggId: {}", id)
        return KlagevedleggDAO.findById(id)?.toVedlegg() ?: throw KlageNotFoundException("Vedlegg not found")
    }

    fun getAnkevedleggById(id: Int): Ankevedlegg {
        logger.debug("Fetching vedlegg metadata from db. VedleggId: {}", id)
        return AnkevedleggDAO.findById(id)?.toVedlegg() ?: throw KlageNotFoundException("Vedlegg not found")
    }

    fun deleteVedleggFromKlage(vedleggId: Int) {
        logger.debug("Deleting vedlegg metadata from db. VedleggId: {}", vedleggId)
        KlagevedleggDAO.findById(vedleggId)?.delete()
    }

    fun deleteVedleggFromAnke(vedleggId: Int) {
        logger.debug("Deleting vedlegg metadata from db. VedleggId: {}", vedleggId)
        AnkevedleggDAO.findById(vedleggId)?.delete()
    }
}
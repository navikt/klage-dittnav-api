package no.nav.klage.repository

import no.nav.klage.domain.anke.AnkeDAO
import no.nav.klage.domain.ankevedlegg.AnkeVedlegg
import no.nav.klage.domain.ankevedlegg.AnkeVedleggDAO
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Repository
class AnkeVedleggRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun storeAnkeVedlegg(ankeId: Int, vedlegg: MultipartFile, fileStorageId: String, internalSaksnummer: String): AnkeVedlegg {
        logger.debug("Storing ankeVedlegg metadata in db. AnkeId: {}", ankeId)
        return AnkeVedleggDAO.new {
            this.tittel = vedlegg.originalFilename
            this.ankeId = AnkeDAO.findById(ankeId)!!
            this.ref = fileStorageId
            this.contentType = vedlegg.contentType
            this.sizeInBytes = vedlegg.bytes.size
            this.ankeInternalSaksnummer = UUID.fromString(internalSaksnummer)
        }.toAnkeVedlegg().also {
            logger.debug("Vedlegg metadata stored successfully in db. Id: {}", it.id)
        }
    }

    fun getAnkeVedleggById(id: Int): AnkeVedlegg {
        logger.debug("Fetching ankeVedlegg metadata from db. VedleggId: {}", id)
        return AnkeVedleggDAO.findById(id)?.toAnkeVedlegg() ?: throw AnkeNotFoundException("AnkeVedlegg not found")
    }

    fun deleteAnkeVedlegg(ankeVedleggId: Int) {
        logger.debug("Deleting ankeVedlegg metadata from db. AnkeVedleggId: {}", ankeVedleggId)
        AnkeVedleggDAO.findById(ankeVedleggId)?.delete()
    }
}

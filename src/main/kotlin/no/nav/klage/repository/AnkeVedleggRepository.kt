package no.nav.klage.repository

import no.nav.klage.domain.ankeOLD.AnkeOLDDAO
import no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLDDAO
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

    fun storeAnkeVedlegg(ankeId: Int, vedlegg: MultipartFile, fileStorageId: String, internalSaksnummer: String): no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLD {
        logger.debug("Storing ankeVedlegg metadata in db. AnkeId: {}", ankeId)
        return AnkeVedleggOLDDAO.new {
            this.tittel = vedlegg.originalFilename.toString()
            this.ankeId = AnkeOLDDAO.findById(ankeId)!!
            this.ref = fileStorageId
            this.contentType = vedlegg.contentType.toString()
            this.sizeInBytes = vedlegg.bytes.size
            this.ankeInternalSaksnummer = UUID.fromString(internalSaksnummer)
        }.toAnkeVedlegg().also {
            logger.debug("Vedlegg metadata stored successfully in db. Id: {}", it.id)
        }
    }

    fun getAnkeVedleggById(id: Int): no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLD {
        logger.debug("Fetching ankeVedlegg metadata from db. VedleggId: {}", id)
        return AnkeVedleggOLDDAO.findById(id)?.toAnkeVedlegg() ?: throw AnkeNotFoundException("AnkeVedlegg not found")
    }

    fun deleteAnkeVedlegg(ankeVedleggId: Int) {
        logger.debug("Deleting ankeVedlegg metadata from db. AnkeVedleggId: {}", ankeVedleggId)
        AnkeVedleggOLDDAO.findById(ankeVedleggId)?.delete()
    }
}

package no.nav.klage.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import no.nav.klage.domain.VedleggDAO
import no.nav.klage.domain.VedleggWrapper
import org.springframework.stereotype.Repository

@Repository
class VedleggRepository(private val gcsStorage: Storage) {

    private val bucketName = "klagevedlegg"

    fun putVedlegg(fnr: String, klageId: Int, vedlegg: VedleggWrapper) {
        val vedleggCreated = VedleggDAO.new {
            this.tittel = vedlegg.tittel
            this.klageId = klageId
        }

        val objectName = "$fnr/$klageId/${vedleggCreated.id.value}"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()
        gcsStorage.create(blobInfo, vedlegg.contentAsBytes())

        vedleggCreated.apply {
            gcsRef = objectName
        }
    }

    fun deleteVedlegg(fnr: String, klageId: Int, vedleggId: Int) {
        val objectName = "$fnr/$klageId/$vedleggId"
        gcsStorage.delete(BlobId.of(bucketName, objectName))
        VedleggDAO.findById(vedleggId)?.delete()
    }

}

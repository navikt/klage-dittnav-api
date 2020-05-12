package no.nav.klage.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import no.nav.klage.domain.Vedlegg
import org.springframework.stereotype.Repository

@Repository
class VedleggRepository(private val gcsStorage: Storage) {

    private val bucketName = "klagevedlegg"

    fun putVedlegg(fnr: String, klageId: Int, vedlegg: Vedlegg) {
        val objectName = "$fnr/$klageId/${vedlegg.id}"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()
        gcsStorage.create(blobInfo, vedlegg.contentAsBytes())
    }

    fun deleteVedlegg(fnr: String, klageId: Int, vedleggId: String) {
        val objectName = "$fnr/$klageId/$vedleggId"
        gcsStorage.delete(BlobId.of(bucketName, objectName))
    }

}

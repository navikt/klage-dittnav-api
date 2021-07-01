package no.nav.klage.vedlegg

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile

class AttachmentValidator(
    private val clamAvClient: ClamAvClient,
    private val maxAttachmentSize: DataSize,
    private val maxTotalSize: DataSize
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun validateAttachment(vedlegg: MultipartFile, totalSizeExistingAttachments: Int) {
        logger.debug("Validating attachment.")
        //Can this happen?
        if (vedlegg.isEmpty) {
            logger.warn("Attachment is empty")
            throw AttachmentIsEmptyException()
        }

        //This limit could be set other places (Spring), since we only upload one at a time
        if (vedlegg.isTooLarge()) {
            logger.warn("Attachment too large")
            throw AttachmentTooLargeException()
        }

        if (totalSizeExistingAttachments + vedlegg.bytes.size > maxTotalSize.toBytes()) {
            logger.warn("Attachment total too large")
            throw AttachmentTotalTooLargeException()
        }

        if (vedlegg.hasVirus()) {
            logger.warn("Attachment has virus")
            throw AttachmentHasVirusException()
        }

        if (vedlegg.isPDF() && vedlegg.isEncrypted()) {
            logger.warn("Attachment is encrypted")
            throw AttachmentEncryptedException()
        }

        logger.debug("Validation successful.")
    }

    private fun MultipartFile.hasVirus() = !clamAvClient.scan(this.bytes)

    private fun MultipartFile.isTooLarge() = this.bytes.size > maxAttachmentSize.toBytes()

    private fun MultipartFile.isEncrypted(): Boolean {
        return try {
            val temp: PDDocument = PDDocument.load(this.bytes)
            temp.close()
            false
        } catch (ipe: InvalidPasswordException) {
            true
        }
    }

    private fun MultipartFile.isPDF() =
        MediaType.valueOf(Tika().detect(this.bytes)) == MediaType.APPLICATION_PDF

}

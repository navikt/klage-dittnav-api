package no.nav.klage.vedlegg

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.util.unit.DataSize

class AttachmentValidator(
    private val clamAvClient: ClamAvClient,
    private val maxAttachmentSize: DataSize,
    private val maxTotalSize: DataSize
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun validateAttachment(bytes: ByteArray, totalSizeExistingAttachments: Int, filename: String) {
        logger.debug("Validating attachment.")

        if (bytes.isEmpty()) {
            logger.warn("Attachment is empty")
            throw AttachmentIsEmptyException()
        }

        if (filename.length > 196) {
            logger.warn("Filename too long. Filename length: {}", filename.length)
            throw AttachmentFilenameTooLongException()
        }

        //This limit could be set other places (Spring), since we only upload one at a time
        if (bytes.size > maxAttachmentSize.toBytes()) {
            logger.warn("Attachment too large")
            throw AttachmentTooLargeException()
        }

        if (totalSizeExistingAttachments + bytes.size > maxTotalSize.toBytes()) {
            logger.warn("Attachment total too large")
            throw AttachmentTotalTooLargeException()
        }

        if (!clamAvClient.scan(bytes)) {
            logger.warn("Attachment has virus")
            throw AttachmentHasVirusException()
        }

        val mediaType = MediaType.valueOf(Tika().detect(bytes))
        if (mediaType == MediaType.APPLICATION_PDF && isEncrypted(bytes)) {
            logger.warn("Attachment is encrypted")
            throw AttachmentEncryptedException()
        }

        logger.debug("Validation successful.")
    }

    private fun isEncrypted(bytes: ByteArray): Boolean {
        return try {
            val temp: PDDocument = Loader.loadPDF(RandomAccessReadBuffer(bytes))
            temp.close()
            false
        } catch (ipe: InvalidPasswordException) {
            true
        }
    }


}

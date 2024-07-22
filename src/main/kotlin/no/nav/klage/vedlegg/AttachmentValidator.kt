package no.nav.klage.vedlegg

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentHasVirusException
import no.nav.klage.domain.exception.AttachmentIsEmptyException
import no.nav.klage.util.getLogger
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.File

@Component
class AttachmentValidator(
    private val clamAvClient: ClamAvClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun validateAttachment(file: File) {
        logger.debug("Validating attachment.")

        if (file.length() == 0L) {
            logger.warn("Attachment is empty")
            throw AttachmentIsEmptyException()
        }

        if (clamAvClient.hasVirus(file)) {
            logger.warn("Attachment has virus")
            throw AttachmentHasVirusException()
        }

        if (file.isPDF() && file.isEncrypted()) {
            logger.warn("Attachment is encrypted")
            throw AttachmentEncryptedException()
        }

        logger.debug("Validation successful.")
    }

    private fun File.isEncrypted(): Boolean {
        return try {
            val temp: PDDocument = Loader.loadPDF(this)
            temp.close()
            false
        } catch (ipe: InvalidPasswordException) {
            true
        }
    }

    private fun File.isPDF() =
        MediaType.valueOf(Tika().detect(this)) == MediaType.APPLICATION_PDF

}

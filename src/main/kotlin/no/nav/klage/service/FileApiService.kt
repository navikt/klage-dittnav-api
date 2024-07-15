package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileApiService(
    private val fileClient: FileClient,
    private val attachmentValidator: AttachmentValidator,
    private val image2PDF: Image2PDF
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun uploadFileAsPDF(file: File): String {
        attachmentValidator.validateAttachment(file = file)

        return fileClient.uploadVedleggResource(
            resource = image2PDF.convertIfImage(file = file)
        )
    }
}
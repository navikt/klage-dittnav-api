package no.nav.klage.vedlegg

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.domain.exception.*
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

    fun validateAttachment(vedlegg: VedleggWrapper, totalSizeExistingAttachments: Int) {
        //Can this happen?
        if (vedlegg.isEmpty()) {
            throw AttachmentIsEmptyException()
        }

        //This limit could be set other places (Spring), since we only upload one at a time
        if (vedlegg.isTooLarge()) {
            throw AttachmentTooLargeException()
        }

        if (totalSizeExistingAttachments + vedlegg.contentAsBytes().size > maxTotalSize.toBytes()) {
            throw AttachmentTotalTooLargeException()
        }

        if (vedlegg.hasVirus()) {
            throw AttachmentHasVirusException()
        }

        if (vedlegg.isPDF() && vedlegg.isEncrypted()) {
            throw AttachmentEncryptedException()
        }

    }

    private fun VedleggWrapper.isEmpty() = this.contentAsBytes().isEmpty()

    private fun VedleggWrapper.hasVirus() = !clamAvClient.scan(this.contentAsBytes())

    private fun VedleggWrapper.isTooLarge() = this.contentAsBytes().size > maxAttachmentSize.toBytes()

    private fun VedleggWrapper.isEncrypted(): Boolean {
        return try {
            PDDocument.load(this.contentAsBytes())
            false
        } catch (ipe: InvalidPasswordException) {
            true
        }
    }

    private fun VedleggWrapper.isPDF() =
        MediaType.valueOf(Tika().detect(this.contentAsBytes())) == MediaType.APPLICATION_PDF

}
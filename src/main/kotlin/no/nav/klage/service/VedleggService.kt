package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
@Transactional
class VedleggService(
    private val klankeRepository: KlankeRepository,
    private val image2PDF: Image2PDF,
    private val attachmentValidator: AttachmentValidator,
    private val vedleggMetrics: VedleggMetrics,
    private val fileClient: FileClient,
    private val validationService: ValidationService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun addKlankevedlegg(klankeId: UUID, multipart: MultipartFile, foedselsnummer: String): Vedlegg {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(multipart.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(multipart.contentType ?: "unknown")
        attachmentValidator.validateAttachment(multipart, existingKlanke.attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(multipart.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, multipart.originalFilename!!)

        val vedleggToSave = Vedlegg(
            tittel = multipart.originalFilename.toString(),
            ref = vedleggIdInFileStore,
            contentType = multipart.contentType.toString(),
            sizeInBytes = multipart.bytes.size,
        )
        existingKlanke.vedlegg.add(
            vedleggToSave
        ).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
        return vedleggToSave
    }

    fun deleteVedleggFromKlanke(klankeId: UUID, vedleggId: UUID, foedselsnummer: String): Boolean {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            existingKlanke.vedlegg.remove(vedlegg)
            return fileClient.deleteVedleggFile(vedlegg.ref)
        } else {
            logger.error("No vedlegg found with this id: $vedleggId")
            return false
        }
    }

    fun getVedleggFromKlanke(klankeId: UUID, vedleggId: UUID, foedselsnummer: String): ByteArray {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke, false)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return fileClient.getVedleggFile(vedlegg.ref)
        } else {
            throw RuntimeException("No vedlegg found with this id: $vedleggId")
        }
    }


    private fun Klanke.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

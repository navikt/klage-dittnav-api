package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.exception.AttachmentGoneException
import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.domain.exception.AttachmentNotFoundException
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

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

    fun addKlankevedlegg(
        klankeId: UUID,
        multipart: MultipartFile,
    ): Vedlegg {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke)
        val timeStart = System.currentTimeMillis()
        val tittel = multipart.originalFilename ?: throw AttachmentMissingFilenameException()
        val bytes = multipart.bytes
        vedleggMetrics.registerVedleggSize(bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(multipart.contentType ?: "unknown")
        attachmentValidator.validateAttachment(
            bytes = bytes,
            totalSizeExistingAttachments = existingKlanke.attachmentsTotalSize(),
            filename = tittel,
        )
        // Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(vedleggFile = convertedBytes, tittel = tittel)

        val vedleggToSave =
            Vedlegg(
                tittel = tittel,
                ref = vedleggIdInFileStore,
                contentType = multipart.contentType.toString(),
                sizeInBytes = bytes.size,
            )
        existingKlanke.vedlegg
            .add(
                vedleggToSave,
            ).also {
                vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
            }
        return vedleggToSave
    }

    fun deleteVedleggFromKlanke(
        klankeId: UUID,
        vedleggId: UUID,
    ): Boolean {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            existingKlanke.vedlegg.remove(vedlegg)
            return fileClient.deleteVedleggFile(vedlegg.ref)
        } else {
            logger.error("No vedlegg found with this id: $vedleggId")
            return false
        }
    }

    fun getVedleggFromKlanke(
        klankeId: UUID,
        vedleggId: UUID,
    ): ByteArray {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlanke)

        // klage-arkiver-journalpost deletes the files after archiving, so they are gone once the klanke is finalized.
        if (existingKlanke.isFinalized()) {
            throw AttachmentGoneException()
        }

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return try {
                fileClient.getVedleggFile(vedlegg.ref)
            } catch (e: AttachmentNotFoundException) {
                logger.error(
                    "File missing in file store for vedlegg {} in draft klanke {} (ref {}).",
                    vedlegg.id,
                    existingKlanke.id,
                    vedlegg.ref,
                )
                throw e
            }
        } else {
            logger.error(
                "Vedlegg not found in db for vedlegg {} in draft klanke {}.",
                vedleggId,
                existingKlanke.id,
            )
            throw AttachmentNotFoundException()
        }
    }

    private fun Klanke.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

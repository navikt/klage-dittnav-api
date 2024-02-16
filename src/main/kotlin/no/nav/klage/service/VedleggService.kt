package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.repository.KlageRepository
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
    private val klageRepository: KlageRepository,
    private val image2PDF: Image2PDF,
    private val attachmentValidator: AttachmentValidator,
    private val vedleggMetrics: VedleggMetrics,
    private val fileClient: FileClient,
    private val validationService: ValidationService,
    private val ankeRepository: AnkeRepository,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun addKlagevedlegg(klageId: UUID, multipart: MultipartFile, bruker: Bruker): Vedlegg {
        val existingKlage = klageRepository.findById(klageId).get()
        validationService.checkKlankeStatus(existingKlage)
        validationService.validateKlankeAccess(existingKlage, bruker)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(multipart.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(multipart.contentType ?: "unknown")
        attachmentValidator.validateAttachment(multipart, existingKlage.attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(multipart.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, multipart.originalFilename!!)

        val vedleggToSave = Vedlegg(
            tittel = multipart.originalFilename.toString(),
            ref = vedleggIdInFileStore,
            contentType = multipart.contentType.toString(),
            sizeInBytes = multipart.bytes.size,
        )
        existingKlage.vedlegg.add(
            vedleggToSave
        ).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
        return vedleggToSave
    }

    fun addAnkevedlegg(ankeId: UUID, multipart: MultipartFile, bruker: Bruker): Vedlegg {
        val existingAnke = ankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(existingAnke)
        validationService.validateKlankeAccess(existingAnke, bruker)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(multipart.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(multipart.contentType ?: "unknown")
        attachmentValidator.validateAttachment(multipart, existingAnke.attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(multipart.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, multipart.originalFilename!!)
        val vedleggToSave = Vedlegg(
            tittel = multipart.originalFilename.toString(),
            ref = vedleggIdInFileStore,
            contentType = multipart.contentType.toString(),
            sizeInBytes = multipart.bytes.size,
        )
        existingAnke.vedlegg.add(
            vedleggToSave
        ).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
        return vedleggToSave
    }

    fun deleteVedleggFromKlanke(klankeId: UUID, vedleggId: UUID, bruker: Bruker): Boolean {
        val existingKlanke = klageRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(existingKlanke, bruker)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            existingKlanke.vedlegg.remove(vedlegg)
            return fileClient.deleteVedleggFile(vedlegg.ref)
        } else {
            logger.error("No vedlegg found with this id: $vedleggId")
            return false
        }
    }

    fun getVedleggFromKlage(klageId: UUID, vedleggId: UUID, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.findById(klageId).get()
        validationService.checkKlankeStatus(existingKlage, false)
        validationService.validateKlankeAccess(existingKlage, bruker)

        val vedlegg = existingKlage.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return fileClient.getVedleggFile(vedlegg.ref)
        } else {
            throw RuntimeException("No vedlegg found with this id: $vedleggId")
        }
    }

    fun getVedleggFromAnke(ankeId: UUID, vedleggId: UUID, bruker: Bruker): ByteArray {
        val existingAnke = ankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(existingAnke, false)
        validationService.validateKlankeAccess(existingAnke, bruker)

        val vedlegg = existingAnke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return fileClient.getVedleggFile(vedlegg.ref)
        } else {
            throw RuntimeException("No vedlegg found with this id: $vedleggId")
        }
    }

    private fun Klage.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }

    private fun Anke.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

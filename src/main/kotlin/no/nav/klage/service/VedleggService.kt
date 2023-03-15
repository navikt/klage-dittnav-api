package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.vedlegg.Ankevedlegg
import no.nav.klage.domain.vedlegg.Klagevedlegg
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.repository.KlageRepository
import no.nav.klage.repository.VedleggRepository
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
    private val vedleggRepository: VedleggRepository,
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

    fun addKlagevedlegg(klageId: String, vedlegg: MultipartFile, bruker: Bruker): Klagevedlegg {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(vedlegg.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(vedlegg.contentType ?: "unknown")
        attachmentValidator.validateAttachment(vedlegg, klageRepository.getKlageById(klageId).attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(vedlegg.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, vedlegg.originalFilename!!)
        return vedleggRepository.storeKlagevedlegg(klageId, vedlegg, vedleggIdInFileStore).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
    }

    fun addAnkevedlegg(ankeId: UUID, vedlegg: MultipartFile, bruker: Bruker): Ankevedlegg {
        val existingAnke = ankeRepository.getAnkeById(ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(vedlegg.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(vedlegg.contentType ?: "unknown")
        attachmentValidator.validateAttachment(vedlegg, ankeRepository.getAnkeById(ankeId).attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(vedlegg.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, vedlegg.originalFilename!!)
        return vedleggRepository.storeAnkevedlegg(
            ankeId = ankeId,
            vedlegg = vedlegg,
            fileStorageId = vedleggIdInFileStore
        ).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
    }

    fun deleteVedleggFromKlage(klageId: String, vedleggId: Int, bruker: Bruker): Boolean {
        val vedlegg = vedleggRepository.getKlagevedleggById(vedleggId)
        val existingKlage = klageRepository.getKlageById(vedlegg.klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        val deletedInGCS = fileClient.deleteVedleggFile(vedlegg.ref)
        vedleggRepository.deleteVedleggFromKlage(vedleggId)
        return deletedInGCS
    }

    fun deleteVedleggFromAnke(ankeId: UUID, vedleggId: Int, bruker: Bruker): Boolean {
        val vedlegg = vedleggRepository.getAnkevedleggById(vedleggId)
        val existingAnke = ankeRepository.getAnkeById(vedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        val deletedInGCS = fileClient.deleteVedleggFile(vedlegg.ref)
        vedleggRepository.deleteVedleggFromAnke(vedleggId)
        return deletedInGCS
    }

    fun getVedleggFromKlage(vedleggId: Int, bruker: Bruker): ByteArray {
        val vedlegg = vedleggRepository.getKlagevedleggById(vedleggId)
        val existingKlage = klageRepository.getKlageById(vedlegg.klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        return fileClient.getVedleggFile(vedlegg.ref)
    }

    fun getVedleggFromAnke(vedleggId: Int, bruker: Bruker): ByteArray {
        val vedlegg = vedleggRepository.getAnkevedleggById(vedleggId)
        val existingAnke = ankeRepository.getAnkeById(vedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return fileClient.getVedleggFile(vedlegg.ref)
    }

    fun expandVedleggToVedleggView(klagevedlegg: Klagevedlegg, bruker: Bruker): VedleggView {
        val existingKlage = klageRepository.getKlageById(klagevedlegg.klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klagevedlegg.toVedleggView()
    }

    fun expandVedleggToVedleggView(ankevedlegg: Ankevedlegg, bruker: Bruker): VedleggView {
        val existingAnke = ankeRepository.getAnkeById(ankevedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return ankevedlegg.toVedleggView()
    }

    private fun Klage.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }

    private fun Anke.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.vedlegg.Vedlegg
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.domain.vedlegg.toVedleggView
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
    private val validationService: ValidationService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun addVedlegg(klageId: Int, vedlegg: MultipartFile, bruker: Bruker): Vedlegg {
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
        return vedleggRepository.storeVedlegg(klageId, vedlegg, vedleggIdInFileStore).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
    }

    fun deleteVedlegg(klageId: Int, vedleggId: Int, bruker: Bruker): Boolean {
        val vedlegg = vedleggRepository.getVedleggById(vedleggId)
        val existingKlage = klageRepository.getKlageById(vedlegg.klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        val deletedInGCS = fileClient.deleteVedleggFile(vedlegg.ref)
        vedleggRepository.deleteVedlegg(vedleggId)
        return deletedInGCS
    }

    fun getVedlegg(vedleggId: Int, bruker: Bruker): ByteArray {
        val vedlegg = vedleggRepository.getVedleggById(vedleggId)
        val existingKlage = klageRepository.getKlageById(vedlegg.klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        return fileClient.getVedleggFile(vedlegg.ref)
    }

    fun expandVedleggToVedleggView(vedlegg: Vedlegg, bruker: Bruker): VedleggView {
        val existingKlage = klageRepository.getKlageById(vedlegg.klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        val content = fileClient.getVedleggFile(vedlegg.ref)
        return vedlegg.toVedleggView(Base64.getEncoder().encodeToString(content))
    }

    private fun Klage.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

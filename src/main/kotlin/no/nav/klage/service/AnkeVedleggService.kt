package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.ankevedlegg.AnkeVedlegg
import no.nav.klage.domain.ankevedlegg.AnkeVedleggView
import no.nav.klage.domain.ankevedlegg.toAnkeVedleggView
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.repository.AnkeVedleggRepository
import no.nav.klage.util.getLogger
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
@Transactional
class AnkeVedleggService(
    private val ankeVedleggRepository: AnkeVedleggRepository,
    private val ankeRepository: AnkeRepository,
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

    fun addAnkeVedlegg(ankeId: Int, vedlegg: MultipartFile, bruker: Bruker): AnkeVedlegg {
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
        return ankeVedleggRepository.storeAnkeVedlegg(ankeId, vedlegg, vedleggIdInFileStore).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
    }

    fun deleteAnkeVedlegg(ankeId: Int, ankeVedleggId: Int, bruker: Bruker): Boolean {
        val ankeVedlegg = ankeVedleggRepository.getAnkeVedleggById(ankeVedleggId)
        val existingAnke = ankeRepository.getAnkeById(ankeVedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke)
        validationService.validateAnkeAccess(existingAnke, bruker)
        val deletedInGCS = fileClient.deleteVedleggFile(ankeVedlegg.ref)
        ankeVedleggRepository.deleteAnkeVedlegg(ankeVedleggId)
        return deletedInGCS
    }

    fun getAnkeVedlegg(ankeVedleggId: Int, bruker: Bruker): ByteArray {
        val ankeVedlegg = ankeVedleggRepository.getAnkeVedleggById(ankeVedleggId)
        val existingAnke = ankeRepository.getAnkeById(ankeVedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        return fileClient.getVedleggFile(ankeVedlegg.ref)
    }

    fun expandAnkeVedleggToAnkeVedleggView(ankeVedlegg: AnkeVedlegg, bruker: Bruker): AnkeVedleggView {
        val existingAnke = ankeRepository.getAnkeById(ankeVedlegg.ankeId)
        validationService.checkAnkeStatus(existingAnke, false)
        validationService.validateAnkeAccess(existingAnke, bruker)
        val content = fileClient.getVedleggFile(ankeVedlegg.ref)
        return ankeVedlegg.toAnkeVedleggView(Base64.getEncoder().encodeToString(content))
    }

    private fun Anke.attachmentsTotalSize() = this.vedlegg.sumBy { it.sizeInBytes }
}
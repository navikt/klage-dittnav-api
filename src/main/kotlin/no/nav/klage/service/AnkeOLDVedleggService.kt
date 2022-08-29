package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.ankeOLD.AnkeOLD
import no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLDView
import no.nav.klage.domain.ankevedleggOLD.toAnkeVedleggView
import no.nav.klage.repository.AnkeRepositoryOLD
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
class AnkeOLDVedleggService(
    private val ankeVedleggRepository: AnkeVedleggRepository,
    private val ankeRepositoryOLD: AnkeRepositoryOLD,
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

    fun addAnkeVedlegg(internalSaksnummer: String, vedlegg: MultipartFile, bruker: Bruker): no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLD {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        val timeStart = System.currentTimeMillis()
        vedleggMetrics.registerVedleggSize(vedlegg.bytes.size.toDouble())
        vedleggMetrics.incrementVedleggType(vedlegg.contentType ?: "unknown")
        attachmentValidator.validateAttachment(vedlegg, ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer).attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(vedlegg.bytes)

        val vedleggIdInFileStore = fileClient.uploadVedleggFile(convertedBytes, vedlegg.originalFilename!!)
        return ankeVedleggRepository.storeAnkeVedlegg(ankeRepositoryOLD.getAnkeByInternalSaksnummer(internalSaksnummer).id!!, vedlegg, vedleggIdInFileStore, internalSaksnummer).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }
    }

    fun deleteAnkeVedlegg(ankeVedleggId: Int, bruker: Bruker): Boolean {
        val ankeVedlegg = ankeVedleggRepository.getAnkeVedleggById(ankeVedleggId)
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(ankeVedlegg.ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        val deletedInGCS = fileClient.deleteVedleggFile(ankeVedlegg.ref)
        ankeVedleggRepository.deleteAnkeVedlegg(ankeVedleggId)
        return deletedInGCS
    }

    fun getAnkeVedlegg(ankeVedleggId: Int, bruker: Bruker): ByteArray {
        val ankeVedlegg = ankeVedleggRepository.getAnkeVedleggById(ankeVedleggId)
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(ankeVedlegg.ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke, false)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        return fileClient.getVedleggFile(ankeVedlegg.ref)
    }

    fun expandAnkeVedleggToAnkeVedleggView(ankeVedlegg: no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLD, bruker: Bruker): AnkeVedleggOLDView {
        val existingAnke = ankeRepositoryOLD.getAnkeByInternalSaksnummer(ankeVedlegg.ankeInternalSaksnummer)
        validationService.checkAnkeStatusOLD(existingAnke, false)
        validationService.validateAnkeAccessOLD(existingAnke, bruker)
        val content = fileClient.getVedleggFile(ankeVedlegg.ref)
        return ankeVedlegg.toAnkeVedleggView(Base64.getEncoder().encodeToString(content))
    }

    private fun AnkeOLD.attachmentsTotalSize() = this.vedlegg.sumOf{ it.sizeInBytes }
}
package no.nav.klage.service

import jakarta.servlet.http.HttpServletRequest
import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload
import org.apache.tika.Tika
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import org.springframework.http.MediaType.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.math.min

@Service
@Transactional
class VedleggService(
    private val klankeRepository: KlankeRepository,
    private val image2PDF: Image2PDF,
    private val attachmentValidator: AttachmentValidator,
    private val vedleggMetrics: VedleggMetrics,
    private val fileClient: FileClient,
    private val validationService: ValidationService,
    private val fileApiService: FileApiService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun addKlankevedlegg(
        klankeId: UUID,
        bruker: Bruker,
        request: HttpServletRequest
    ): Vedlegg {
        logger.debug("Request: {}", request)
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(existingKlanke, bruker)
        var timeStart = System.currentTimeMillis()
        val filePath = Files.createTempFile(null, null)
        logger.debug("Created temp file in {} ms", System.currentTimeMillis() - timeStart)
        val contentLength = request.getHeader("Content-Length")?.toDouble() ?: 0.0
        vedleggMetrics.registerVedleggSize(contentLength)

        if (contentLength > 269484032) {
            throw AttachmentTooLargeException("For stort vedlegg")
        }

        if (existingKlanke.attachmentsTotalSize() + contentLength > 269484032) {
            throw AttachmentTooLargeException("Total størrelse på alle vedlegg er for stor")
        }

        logger.debug("Content-length {}", contentLength)

        val contentType = request.getHeader("Content-Type")

        logger.debug("Content-type {}", contentType)

        vedleggMetrics.incrementVedleggType(contentType ?: "unknown")

        val upload = JakartaServletFileUpload()
        var filename: String? = null
        val parts = upload.getItemIterator(request)
        logger.debug("Parts: {}", parts)
        parts.forEachRemaining { item ->
            logger.debug("item: {}", item)
            timeStart = System.currentTimeMillis()
            val inputStream = item.inputStream
            logger.debug("inputStream: {}", inputStream)
            logger.debug("Got input stream in {} ms", System.currentTimeMillis() - timeStart)
            if (!item.isFormField) {
                filename = item.name
                logger.debug("item.name: {}", item.name)
                try {
                    timeStart = System.currentTimeMillis()
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
                    logger.debug("Copied file to temp file in {} ms", System.currentTimeMillis() - timeStart)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to save file", e)
                } finally {
                    inputStream.close()
                }
            } else {
                try {
                    timeStart = System.currentTimeMillis()
                    logger.error("Shouldn't be here, $klankeId")
                    logger.debug("Read content in {} ms", System.currentTimeMillis() - timeStart)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to read content", e)
                } finally {
                    inputStream.close()
                }
            }
        }

        val file = filePath.toFile()

        val bytesForFiletypeDetection =
            file.inputStream()
                .readNBytes(min(DataSize.of(3, DataUnit.KILOBYTES).toBytes().toInt(), file.length().toInt()))
        //TODO: Sjekk innhold i db på disse
        val mediaType = valueOf(Tika().detect(bytesForFiletypeDetection))
        logger.debug("Media type {}", mediaType)
        val vedleggIdInFileStore = fileApiService.uploadFileAsPDF(file = file)
        logger.debug("Vedlegg id {}", vedleggIdInFileStore)

        val vedleggToSave = Vedlegg(
            tittel = filename ?: "Mangler tittel",
            ref = vedleggIdInFileStore,
            contentType = mediaType.toString(),
            sizeInBytes = contentLength.toInt(),
        )
        existingKlanke.vedlegg.add(
            vedleggToSave
        ).also {
            vedleggMetrics.registerTimeUsed(System.currentTimeMillis() - timeStart)
        }

        logger.debug("Vedlegg {}", vedleggIdInFileStore)
        return vedleggToSave

    }

    fun deleteVedleggFromKlanke(klankeId: UUID, vedleggId: UUID, bruker: Bruker): Boolean {
        val existingKlanke = klankeRepository.findById(klankeId).get()
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

    fun getVedleggFromKlanke(klankeId: UUID, vedleggId: UUID, bruker: Bruker): Resource {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke, false)
        validationService.validateKlankeAccess(existingKlanke, bruker)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return getVedleggAsResource(vedlegg.ref)
        } else {
            throw RuntimeException("No vedlegg found with this id: $vedleggId")
        }
    }

    fun getVedleggFromKlankeAsSignedUrl(klankeId: UUID, vedleggId: UUID, bruker: Bruker): String {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke, false)
        validationService.validateKlankeAccess(existingKlanke, bruker)

        val vedlegg = existingKlanke.vedlegg.find { it.id == vedleggId }

        if (vedlegg != null) {
            return fileClient.getVedleggFileAsSignedUrl(vedlegg.ref)
        } else {
            throw RuntimeException("No vedlegg found with this id: $vedleggId")
        }
    }

    fun getVedleggAsResource(vedleggRef: String): Resource {
        return fileClient.getVedleggAsResource(vedleggRef = vedleggRef)
    }


    private fun Klanke.attachmentsTotalSize() = this.vedlegg.sumOf { it.sizeInBytes }
}

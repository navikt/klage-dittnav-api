package no.nav.klage.service

import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.klage.Klage
import no.nav.klage.repository.KlageRepository
import no.nav.klage.repository.VedleggRepository
import no.nav.klage.repository.VedleggResponse
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
@Transactional
class VedleggService(
    private val vedleggRepository: VedleggRepository,
    private val klageRepository: KlageRepository,
    private val image2PDF: Image2PDF,
    private val vedleggWebClient: WebClient,
    private val attachmentValidator: AttachmentValidator
) {

    fun addVedlegg(klageId: Int, vedlegg: MultipartFile): Vedlegg {
        attachmentValidator.validateAttachment(vedlegg, klageRepository.getKlageById(klageId).attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(vedlegg.bytes)
        val vedleggIdInFileStore = uploadAttachmentToFilestore(convertedBytes, vedlegg.originalFilename!!)
        return vedleggRepository.storeVedlegg(klageId, vedlegg, vedleggIdInFileStore)
    }

    fun deleteVedlegg(klageId: Int, vedleggId: Int): Boolean {
        val vedlegg = vedleggRepository.getVedleggById(vedleggId)

        val deletedInGCS = vedleggWebClient
            .delete()
            .uri("/" + vedlegg.id.toString())
            .retrieve()
            .bodyToMono<Boolean>()
            .block()

        vedleggRepository.deleteVedlegg(vedleggId)
        return deletedInGCS!!
    }

    private fun uploadAttachmentToFilestore(bytes: ByteArray, originalFilename: String): String {
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", bytes).filename(originalFilename)
        val response = vedleggWebClient
            .post()
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono<VedleggResponse>()
            .block()

        requireNotNull(response)

        return response.id
    }

    private fun Klage.attachmentsTotalSize() = this.vedlegg.sumBy { it.sizeInBytes }
}

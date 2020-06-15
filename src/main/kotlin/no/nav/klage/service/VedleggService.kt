package no.nav.klage.service

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.repository.KlageRepository
import no.nav.klage.repository.VedleggRepository
import no.nav.klage.repository.VedleggResponse
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class VedleggService(
    private val vedleggRepository: VedleggRepository,
    private val klageRepository: KlageRepository,
    private val image2PDF: Image2PDF,
    private val vedleggWebClient: WebClient,
    private val attachmentValidator: AttachmentValidator
) {

    fun addVedlegg(klageId: Int, vedlegg: VedleggWrapper): Vedlegg {
        attachmentValidator.validateAttachment(vedlegg, klageRepository.getKlageById(klageId).attachmentsTotalSize())
        //Convert attachment (if not already pdf)
        val convertedBytes = image2PDF.convert(vedlegg.contentAsBytes())
        val vedleggIdInFileStore = uploadAttachmentToFilestore(convertedBytes)
        return vedleggRepository.storeVedlegg(klageId, vedlegg, vedleggIdInFileStore)
    }

    fun deleteVedlegg(fnr: String, klageId: Int, vedleggId: Int) {
        val vedlegg = vedleggRepository.getVedleggById(vedleggId)

        vedleggWebClient
            .delete()
            .attribute("id", vedlegg.ref)

        vedleggRepository.deleteVedlegg(vedleggId)
    }

    private fun uploadAttachmentToFilestore(bytes: ByteArray): String {
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", bytes)
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

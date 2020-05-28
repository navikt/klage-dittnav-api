package no.nav.klage.repository

import no.nav.klage.domain.KlageDAO
import no.nav.klage.domain.VedleggDAO
import no.nav.klage.domain.VedleggWrapper
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Repository
class VedleggRepository(private val vedleggWebClient: WebClient) {

    fun putVedlegg(klageId: Int, vedlegg: VedleggWrapper) {
        val vedleggId = vedlegg.store()
        VedleggDAO.new {
            this.tittel = vedlegg.tittel
            this.klageId = KlageDAO.findById(klageId)!!
            this.ref = vedleggId
            this.type = vedlegg.type
        }
    }

    fun deleteVedlegg(fnr: String, klageId: Int, vedleggId: Int) {
        VedleggDAO.findById(vedleggId)?.let {
            vedleggWebClient
                .delete()
                .attribute("id", it.ref)
            it.delete()
        }
    }

    private fun VedleggWrapper.store(): String {
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", this.contentAsBytes())
        val response = vedleggWebClient
            .post()
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono<VedleggResponse>()
            .block()

        requireNotNull(response)

        return response.id
    }

}

data class VedleggResponse(val id: String)

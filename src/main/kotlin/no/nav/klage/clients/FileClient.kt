package no.nav.klage.clients

import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FileClient(
    private val fileWebClient: WebClient,
    private val azureADClient: AzureADClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    //TODO: Rydd i fillageret nå som vi ikke lenger trenger det.

    fun uploadVedleggFile(vedleggFile: ByteArray, originalFilename: String): String {
        logger.debug("Uploading attachment to file store.")

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", vedleggFile).filename(originalFilename)
        val response = fileWebClient
            .post()
            .uri { it.path("/attachment").build() }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono<VedleggResponse>()
            .block()

        requireNotNull(response)

        logger.debug("Attachment uploaded to file store with id: {}", response.id)
        return response.id
    }


    fun getVedleggFile(vedleggRef: String): ByteArray {
        logger.debug("Fetching vedlegg file with vedlegg ref {}", vedleggRef)
        return fileWebClient.get()
            .uri { it.path("/attachment/{id}").build(vedleggRef) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("Attachment could not be fetched")
    }

    fun deleteVedleggFile(vedleggRef: String): Boolean {
        logger.debug("Deleting vedlegg file with vedlegg ref {}", vedleggRef)
        val deletedInFileStore = fileWebClient.delete()
            .uri { it.path("/attachment/{id}").build(vedleggRef) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .retrieve()
            .bodyToMono<Boolean>()
            .block()!!

        if (deletedInFileStore) {
            logger.debug("Attachment successfully deleted in file store.")
        } else {
            logger.debug("Attachment $vedleggRef was not deleted in file store. File could be missing from filestore.")
        }

        return deletedInFileStore
    }
}

data class VedleggResponse(val id: String)
package no.nav.klage.clients

import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.nio.file.Files

@Component
class FileClient(
    private val fileWebClient: WebClient,
    private val azureADClient: AzureADClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

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

    fun uploadVedleggResource(resource: Resource): String {
        logger.debug("Uploading attachment to file store.")

        var start = System.currentTimeMillis()
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", resource).contentType(MediaType.APPLICATION_PDF).filename("file")
        logger.debug("File added to body. Time taken: ${System.currentTimeMillis() - start} ms")

        start = System.currentTimeMillis()
        val response = fileWebClient
            .post()
            .uri("/attachment")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                logErrorResponse(response, ::uploadVedleggResource.name, secureLogger)
            }
            .bodyToMono<VedleggResponse>()
            .block()

        logger.debug("Response received. Time taken: ${System.currentTimeMillis() - start} ms")
        requireNotNull(response)

        if (resource is FileSystemResource) {
            start = System.currentTimeMillis()
            resource.file.delete()
            logger.debug("File deleted. Time taken: ${System.currentTimeMillis() - start} ms")
        }

        logger.debug("Document uploaded to file store with id: {}", response.id)
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

    fun getVedleggAsResource(vedleggRef: String): Resource {
        logger.debug("Fetching vedlegg file as resource with vedlegg ref {}", vedleggRef)

        val dataBufferFlux = fileWebClient.get()
            .uri { it.path("/attachment/{id}/outputstream").build(vedleggRef) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                logErrorResponse(response, ::getVedleggAsResource.name, secureLogger)
            }
            .bodyToFlux(DataBuffer::class.java)

        val tempFile = Files.createTempFile(null, null)

        DataBufferUtils.write(dataBufferFlux, tempFile).block()
        return FileSystemResource(tempFile)
    }


    fun getVedleggFileAsSignedUrl(vedleggRef: String): String {
        logger.debug("Fetching vedlegg file (signed URL) with vedlegg ref {}", vedleggRef)
        return fileWebClient.get()
            .uri { it.path("/attachment/{id}/signedurl").build(vedleggRef) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageFileApiOidcToken()}")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                logErrorResponse(response, ::getVedleggFileAsSignedUrl.name, secureLogger)
            }
            .bodyToMono<String>()
            .block()!!
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
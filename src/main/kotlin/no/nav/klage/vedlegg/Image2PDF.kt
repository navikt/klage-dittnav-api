package no.nav.klage.vedlegg

import no.nav.klage.domain.exception.AttachmentCouldNotBeConvertedException
import no.nav.klage.util.getLogger
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream


@Component
class Image2PDF {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    private var supportedMediaTypes: List<MediaType>? = listOf(IMAGE_JPEG, IMAGE_PNG)

    private val A4: PDRectangle = PDRectangle.A4

    fun convert(bytes: ByteArray): ByteArray {
        val mediaType = MediaType.valueOf(Tika().detect(bytes))
        if (APPLICATION_PDF == mediaType) {
            return bytes
        }
        if (validImageTypes(mediaType)) {
            return embedImageInPDF(mediaType.subtype, bytes)
        }
        val exception =
            AttachmentCouldNotBeConvertedException("mediaType could not be converted: $mediaType")
        logger.warn("User tried to upload an unsupported file type", exception)
        throw exception
    }

    private fun embedImageInPDF(imgType: String, image: ByteArray): ByteArray {
        return embedImageInPDF(image, imgType)
    }

    private fun embedImageInPDF(image: ByteArray, imgType: String): ByteArray {
        try {
            PDDocument().use { doc ->
                ByteArrayOutputStream().use { outputStream ->
                    addPDFPageFromImage(
                        doc,
                        image,
                        imgType
                    )
                    doc.save(outputStream)
                    return outputStream.toByteArray()
                }
            }
        } catch (ex: Exception) {
            throw RuntimeException("Conversion of attachment failed", ex)
        }
    }

    private fun validImageTypes(mediaType: MediaType): Boolean {
        val validImageTypes = supportedMediaTypes!!.contains(mediaType)
        logger.debug("{} convert bytes, of type {}, to PDF", if (validImageTypes) "Will" else "Won't", mediaType)
        return validImageTypes
    }

    private fun addPDFPageFromImage(doc: PDDocument, origImg: ByteArray, imgFormat: String) {
        val page = PDPage(A4)
        doc.addPage(page)
        val scaledImg = ImageUtils.downToA4(origImg, imgFormat)
        try {
            PDPageContentStream(doc, page).use { contentStream ->
                val xImage: PDImageXObject = PDImageXObject.createFromByteArray(doc, scaledImg, "img")
                contentStream.drawImage(xImage, A4.lowerLeftX, A4.lowerLeftY)
            }
        } catch (ex: Exception) {
            throw RuntimeException("Converting attachment failed", ex)
        }
    }
}
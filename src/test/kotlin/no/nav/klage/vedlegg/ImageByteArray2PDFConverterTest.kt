package no.nav.klage.vedlegg

import org.apache.tika.Tika
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.nio.file.Files
import java.nio.file.Path

internal class ImageByteArray2PDFConverterTest {

    companion object {
        private val PDFSIGNATURE = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        private var converter = Image2PDF()
        private const val TEST_RESOURCES_FOLDER = "src/test/resources/"

        fun isPdf(fileContents: ByteArray): Boolean {
            return fileContents.copyOfRange(0, PDFSIGNATURE.size).contentEquals(
                PDFSIGNATURE
            )
        }

        private fun String.filepathToBytes() =
            Files.readAllBytes(Path.of(TEST_RESOURCES_FOLDER, this))
    }

    @Test
    fun `jpg converts to pdf`() {
        assertTrue(
            isPdf(
                converter.convert("pdf/jks.jpg".filepathToBytes())
            )
        )
    }

    @Test
    fun `png converts to pdf`() {
        assertTrue(
            isPdf(
                converter.convert("pdf/nav-logo.png".filepathToBytes())
            )
        )
    }

    @Test
    fun `gif fails when not configured`() {
        assertThrows(RuntimeException::class.java) {
            converter.convert("pdf/loading.gif".filepathToBytes())
        }
    }

    @Test
    fun `pdf remains unchanged`() {
        assertEquals(
            MediaType.APPLICATION_PDF,
            MediaType.valueOf(
                Tika().detect(
                    converter.convert(
                        "pdf/test123.pdf".filepathToBytes()
                    )
                )
            )
        )
    }

    @Test
    fun `whatever else is not allowed`() {
        assertThrows(RuntimeException::class.java) {
            converter.convert(byteArrayOf(1, 2, 3, 4))
        }
    }

    @Test
    fun `pdf with many pages does not fail`() {
        converter.convert("pdf/spring-framework-reference.pdf".filepathToBytes())
    }
}



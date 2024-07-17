package no.nav.klage.vedlegg

import org.apache.tika.Tika
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.io.File
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
    }

    @Test
    fun `jpg converts to pdf`() {
        val path = TEST_RESOURCES_FOLDER + "pdf/jks.jpg"
        val originalFile = File(path)
        val tempFile = originalFile.copyTo(File("$path.tmp"))
        assertTrue(
            isPdf(
                converter.convertIfImage(tempFile).contentAsByteArray
            )
        )
        tempFile.deleteOnExit()
    }

    @Test
    fun `png converts to pdf`() {
        val path = TEST_RESOURCES_FOLDER + "pdf/nav-logo.png"
        val originalFile = File(path)
        val tempFile = originalFile.copyTo(File("$path.tmp"))
        assertTrue(
            isPdf(
                converter.convertIfImage(tempFile).contentAsByteArray
            )
        )
        tempFile.deleteOnExit()
    }

    @Test
    fun `gif fails when not configured`() {
        assertThrows(RuntimeException::class.java) {
            converter.convertIfImage(File(TEST_RESOURCES_FOLDER + "pdf/loading.gif"))
        }
    }

    @Test
    fun `pdf remains unchanged`() {
        assertEquals(
            MediaType.APPLICATION_PDF,
            MediaType.valueOf(
                Tika().detect(
                    converter.convertIfImage(
                        File(
                            TEST_RESOURCES_FOLDER + "pdf/test123.pdf"
                        )
                    ).contentAsByteArray
                )
            )
        )
    }

    @Test
    fun `pdf with many pages does not fail`() {
        converter.convertIfImage(File(TEST_RESOURCES_FOLDER + "pdf/spring-framework-reference.pdf"))
    }
}



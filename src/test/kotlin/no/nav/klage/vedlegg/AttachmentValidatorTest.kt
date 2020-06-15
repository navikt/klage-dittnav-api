package no.nav.klage.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.VedleggWrapper
import no.nav.klage.domain.exception.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

internal class AttachmentValidatorTest {

    lateinit var validator: AttachmentValidator
    lateinit var clamAvClient: ClamAvClient

    @BeforeEach
    fun beforeEach() {
        clamAvClient = mockk()
        validator = AttachmentValidator(clamAvClient, DataSize.ofMegabytes(8), DataSize.ofMegabytes(32))
    }

    @Test
    fun `empty file throws AttachmentIsEmptyException`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns byteArrayOf()
        assertThrows<AttachmentIsEmptyException> {
            val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
            validator.validateAttachment(vedlegg, 0)
        }
    }

    @Test
    fun `file too large throws AttachmentTooLargeException`() {
        validator = AttachmentValidator(clamAvClient, DataSize.ofBytes(1), DataSize.ofBytes(2))
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns byteArrayOf(1, 1)
        assertThrows<AttachmentTooLargeException> {
            val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
            validator.validateAttachment(vedlegg, 0)
        }
    }

    @Test
    fun `file too large throws AttachmentTotalTooLargeException`() {
        validator = AttachmentValidator(clamAvClient, DataSize.ofBytes(1), DataSize.ofBytes(2))
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns byteArrayOf(1)
        assertThrows<AttachmentTotalTooLargeException> {
            val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
            validator.validateAttachment(vedlegg, 2)
        }
    }

    @Test
    fun `file with virus throws AttachmentHasVirusException`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns byteArrayOf(1)
        every { clamAvClient.scan(any()) } returns false
        assertThrows<AttachmentHasVirusException> {
            val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
            validator.validateAttachment(vedlegg, 0)
        }
    }

    @Test
    fun `pdf with password throws AttachmentEncryptedException`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns Files.readAllBytes(
            Path.of("src/test/resources/pdf/pdf-with-user-password.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        assertThrows<AttachmentEncryptedException> {
            val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
            validator.validateAttachment(vedlegg, 0)
        }
    }

    @Test
    fun `pdf with empty password works`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns Files.readAllBytes(
            Path.of("src/test/resources/pdf/pdf-with-empty-user-password.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
        validator.validateAttachment(vedlegg, 0)
    }

    @Test
    fun `valid file passes validation`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns Files.readAllBytes(
            Path.of("src/test/resources/pdf/test123.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        val vedlegg = VedleggWrapper(multipartFileMock, "tittel", "someType")
        validator.validateAttachment(vedlegg, 0)
    }

}
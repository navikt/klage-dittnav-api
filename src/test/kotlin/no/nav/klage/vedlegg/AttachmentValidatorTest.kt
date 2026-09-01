package no.nav.klage.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentFilenameTooLongException
import no.nav.klage.domain.exception.AttachmentHasVirusException
import no.nav.klage.domain.exception.AttachmentIsEmptyException
import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.domain.exception.AttachmentTotalTooLargeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path

internal class AttachmentValidatorTest {
    lateinit var validator: AttachmentValidator
    lateinit var clamAvClient: ClamAvClient

    @BeforeEach
    fun beforeEach() {
        clamAvClient = mockk()
        validator =
            AttachmentValidator(
                clamAvClient,
                maxAttachmentSize = DataSize.ofMegabytes(8),
                maxTotalSize = DataSize.ofMegabytes(32),
            )
    }

    @Test
    fun `empty file throws AttachmentIsEmptyException`() {
        assertThrows<AttachmentIsEmptyException> {
            validator.validateAttachment(bytes = byteArrayOf(), totalSizeExistingAttachments = 0, filename = "test.pdf")
        }
    }

    @Test
    fun `filename too long throws AttachmentFilenameTooLongException`() {
        assertThrows<AttachmentFilenameTooLongException> {
            validator.validateAttachment(bytes = byteArrayOf(1), totalSizeExistingAttachments = 0, filename = "a".repeat(193) + ".pdf")
        }
    }

    @Test
    fun `file too large throws AttachmentTooLargeException`() {
        validator =
            AttachmentValidator(clamAvClient = clamAvClient, maxAttachmentSize = DataSize.ofBytes(1), maxTotalSize = DataSize.ofBytes(2))
        assertThrows<AttachmentTooLargeException> {
            validator.validateAttachment(bytes = byteArrayOf(1, 1), totalSizeExistingAttachments = 0, filename = "test.pdf")
        }
    }

    @Test
    fun `file too large throws AttachmentTotalTooLargeException`() {
        validator =
            AttachmentValidator(clamAvClient = clamAvClient, maxAttachmentSize = DataSize.ofBytes(1), maxTotalSize = DataSize.ofBytes(2))
        assertThrows<AttachmentTotalTooLargeException> {
            validator.validateAttachment(bytes = byteArrayOf(1), totalSizeExistingAttachments = 2, filename = "test.pdf")
        }
    }

    @Test
    fun `file with virus throws AttachmentHasVirusException`() {
        every { clamAvClient.hasVirus(any()) } returns true
        assertThrows<AttachmentHasVirusException> {
            validator.validateAttachment(bytes = byteArrayOf(1), totalSizeExistingAttachments = 0, filename = "test.pdf")
        }
    }

    @Test
    fun `pdf with password throws AttachmentEncryptedException`() {
        val bytes =
            Files.readAllBytes(
                Path.of("src/test/resources/pdf/pdf-with-user-password.pdf"),
            )
        every { clamAvClient.hasVirus(any()) } returns false
        assertThrows<AttachmentEncryptedException> {
            validator.validateAttachment(bytes = bytes, totalSizeExistingAttachments = 0, filename = "pdf-with-user-password.pdf")
        }
    }

    @Test
    fun `pdf with empty password works`() {
        val bytes =
            Files.readAllBytes(
                Path.of("src/test/resources/pdf/pdf-with-empty-user-password.pdf"),
            )
        every { clamAvClient.hasVirus(any()) } returns false
        validator.validateAttachment(bytes = bytes, totalSizeExistingAttachments = 0, filename = "pdf-with-empty-user-password.pdf")
    }

    @Test
    fun `valid file passes validation`() {
        val bytes =
            Files.readAllBytes(
                Path.of("src/test/resources/pdf/test123.pdf"),
            )
        every { clamAvClient.hasVirus(any()) } returns false
        validator.validateAttachment(bytes = bytes, totalSizeExistingAttachments = 0, filename = "test123.pdf")
    }
}

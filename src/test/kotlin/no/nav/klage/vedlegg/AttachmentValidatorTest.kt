package no.nav.klage.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.*
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
        validator = AttachmentValidator(clamAvClient,
            maxAttachmentSize = DataSize.ofMegabytes(8),
            maxTotalSize = DataSize.ofMegabytes(32),
        )
    }

    @Test
    fun `empty file throws AttachmentIsEmptyException`() {
        assertThrows<AttachmentIsEmptyException> {
            validator.validateAttachment(byteArrayOf(), 0)
        }
    }

    @Test
    fun `file too large throws AttachmentTooLargeException`() {
        validator = AttachmentValidator(clamAvClient, DataSize.ofBytes(1), DataSize.ofBytes(2))
        assertThrows<AttachmentTooLargeException> {
            validator.validateAttachment(byteArrayOf(1, 1), 0)
        }
    }

    @Test
    fun `file too large throws AttachmentTotalTooLargeException`() {
        validator = AttachmentValidator(clamAvClient, DataSize.ofBytes(1), DataSize.ofBytes(2))
        assertThrows<AttachmentTotalTooLargeException> {
            validator.validateAttachment(byteArrayOf(1), 2)
        }
    }

    @Test
    fun `file with virus throws AttachmentHasVirusException`() {
        every { clamAvClient.scan(any()) } returns false
        assertThrows<AttachmentHasVirusException> {
            validator.validateAttachment(byteArrayOf(1), 0)
        }
    }

    @Test
    fun `pdf with password throws AttachmentEncryptedException`() {
        val bytes = Files.readAllBytes(
            Path.of("src/test/resources/pdf/pdf-with-user-password.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        assertThrows<AttachmentEncryptedException> {
            validator.validateAttachment(bytes, 0)
        }
    }

    @Test
    fun `pdf with empty password works`() {
        val bytes = Files.readAllBytes(
            Path.of("src/test/resources/pdf/pdf-with-empty-user-password.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        validator.validateAttachment(bytes, 0)
    }

    @Test
    fun `valid file passes validation`() {
        val bytes = Files.readAllBytes(
            Path.of("src/test/resources/pdf/test123.pdf")
        )
        every { clamAvClient.scan(any()) } returns true
        validator.validateAttachment(bytes, 0)
    }

}
